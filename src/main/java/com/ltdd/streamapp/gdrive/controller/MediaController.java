// File: src/main/java/com/ltdd/streamapp/gdrive/controller/MediaController.java
package com.ltdd.streamapp.gdrive.controller;

import com.google.api.services.drive.Drive; // << THÊM IMPORT NÀY
import com.ltdd.streamapp.gdrive.payload.MediaResponse;
import com.ltdd.streamapp.gdrive.payload.MediaUploadRequest;
import com.ltdd.streamapp.gdrive.payload.MessageResponse;
import com.ltdd.streamapp.gdrive.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource; // Dùng cho cách stream đơn giản
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody; // Dùng cho streaming hiệu quả


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);
    private final MediaService mediaService;
    private final Drive googleDriveService; // Inject Drive service

    public MediaController(MediaService mediaService, Drive googleDriveService) {
        this.mediaService = mediaService;
        this.googleDriveService = googleDriveService; // Khởi tạo
    }

    @PostMapping("/upload")
    public ResponseEntity<MediaResponse> uploadMedia(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("data") MediaUploadRequest mediaUploadRequest,
            @AuthenticationPrincipal UserDetails currentUser) {

        if (file.isEmpty()) {
            throw new RuntimeException("Error: File to upload cannot be empty.");
        }
        if (currentUser == null) {
            throw new RuntimeException("Error: User not authenticated for media upload.");
        }
        MediaResponse uploadedMediaResponse = mediaService.uploadMedia(currentUser.getUsername(), file, mediaUploadRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedMediaResponse);
    }

    @GetMapping
    public ResponseEntity<List<MediaResponse>> getUserMedia(@AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("Error: User not authenticated to get user media.");
        }
        List<MediaResponse> mediaList = mediaService.getUserMedia(currentUser.getUsername());
        return ResponseEntity.ok(mediaList);
    }

    @GetMapping("/public")
    public ResponseEntity<List<MediaResponse>> getPublicMedia(@RequestParam(name = "type", required = false) String mediaType) {
        List<MediaResponse> publicMediaList;
        if (mediaType != null && !mediaType.trim().isEmpty()) {
            publicMediaList = mediaService.getAllPublicMediaByType(mediaType.toUpperCase());
        } else {
            publicMediaList = mediaService.getAllPublicMedia(); // Hàm này bạn đã implement ở Service
        }
        return ResponseEntity.ok(publicMediaList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaResponse> getMediaDetails(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails currentUser) {
        String username = (currentUser != null) ? currentUser.getUsername() : null;
        MediaResponse media = mediaService.getMediaDetails(username, id);
        return ResponseEntity.ok(media);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedia(@PathVariable Long id,
                                         @AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("Error: User not authenticated to delete media.");
        }
        mediaService.deleteMedia(currentUser.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINT MỚI CHO STREAMING ---
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=(\\d*)-(\\d*)");
    private static final int DEFAULT_BUFFER_SIZE = 20480; // 20KB, có thể điều chỉnh
    private static final long DEFAULT_CHUNK_SIZE = 1024 * 1024 * 2; // 2MB, chunk size mặc định nếu client không yêu cầu endRange


    @GetMapping("/stream/{fileIdOnDrive}")
    public void streamMediaFile(@PathVariable String fileIdOnDrive,
                                HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        logger.info("Stream request for file ID: {} with Range header: {}", fileIdOnDrive, request.getHeader(HttpHeaders.RANGE));
        com.google.api.services.drive.model.File driveFileMetadata;
        try {
            driveFileMetadata = googleDriveService.files().get(fileIdOnDrive)
                    .setFields("id, name, mimeType, size") // Chỉ cần size và mimeType ở đây
                    .execute();
        } catch (IOException e) {
            logger.error("DRIVE API ERROR - Fetching metadata for file ID {}: {}", fileIdOnDrive, e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File metadata not found on Google Drive.");
            return;
        }

        if (driveFileMetadata == null || driveFileMetadata.getSize() == null) {
            logger.error("DRIVE METADATA ERROR - File metadata or size is null for file ID: {}", fileIdOnDrive);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found or size unknown.");
            return;
        }

        long fileSize = driveFileMetadata.getSize();
        String fileName = driveFileMetadata.getName();
        String mimeType = determineMimeType(driveFileMetadata.getMimeType(), fileName); // Hàm helper để xác định mimeType

        response.setContentType(mimeType);
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes"); // Rất quan trọng

        long startRange = 0;
        long endRange = fileSize - 1; // Mặc định stream toàn bộ file

        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            Matcher matcher = RANGE_PATTERN.matcher(rangeHeader);
            if (matcher.find()) {
                try {
                    String startGroup = matcher.group(1);
                    if (startGroup != null && !startGroup.isEmpty()) {
                        startRange = Long.parseLong(startGroup);
                    }
                    // endGroup có thể rỗng, nghĩa là stream đến cuối
                    String endGroup = matcher.group(2);
                    if (endGroup != null && !endGroup.isEmpty()) {
                        endRange = Long.parseLong(endGroup);
                    }
                    // Đảm bảo endRange không vượt quá kích thước file
                    endRange = Math.min(endRange, fileSize - 1);

                } catch (NumberFormatException e) {
                    logger.warn("Invalid Range header format: '{}'. Defaulting to full stream.", rangeHeader);
                    startRange = 0;
                    endRange = fileSize - 1;
                }
            }
        }

        // Validate range một lần nữa sau khi đã parse và điều chỉnh
        if (startRange < 0 || startRange >= fileSize || endRange < startRange /*|| endRange >= fileSize đã được Math.min xử lý*/) {
            logger.warn("Processed Range Not Satisfiable: Start={}, End={}, FileSize={}. Sending 416.", startRange, endRange, fileSize);
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
            return;
        }

        long contentLengthToServe = (endRange - startRange) + 1;
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLengthToServe));

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + startRange + "-" + endRange + "/" + fileSize);
            logger.info("Streaming PARTIAL content for '{}' (ID: {}), Range: bytes={}-{}, Serving: {} bytes",
                    fileName, fileIdOnDrive, startRange, endRange, contentLengthToServe);
        } else {
            response.setStatus(HttpServletResponse.SC_OK); // 200
            logger.info("Streaming FULL content for '{}' (ID: {}), Serving: {} bytes",
                    fileName, fileIdOnDrive, contentLengthToServe);
        }


        InputStream driveInputStream = null;
        OutputStream responseOutputStream = null;
        try {
            // Quan trọng: Google Drive API không hỗ trợ tải một phần (byte range) trực tiếp
            // qua executeMediaAsInputStream() theo cách chuẩn HTTP Range header.
            // Chúng ta phải lấy toàn bộ stream và tự skip đến vị trí mong muốn.
            driveInputStream = googleDriveService.files().get(fileIdOnDrive).executeMediaAsInputStream();
            responseOutputStream = response.getOutputStream();

            if (startRange > 0) {
                long actuallySkipped = driveInputStream.skip(startRange);
                if (actuallySkipped < startRange) {
                    logger.error("STREAM SKIP ERROR for file {}: Could not skip to the desired start range. Requested skip: {}, Actually skipped: {}. Client may receive incorrect data.",
                            fileIdOnDrive, startRange, actuallySkipped);
                    // Không thể sendError ở đây vì header có thể đã được gửi.
                    // Client có thể sẽ gặp lỗi khi nhận dữ liệu không mong muốn.
                    return; // Nên thoát sớm
                }
                logger.debug("Successfully skipped {} bytes to reach startRange {} for file {}", actuallySkipped, startRange, fileIdOnDrive);
            }

            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;
            long totalBytesTransferred = 0;

            while (totalBytesTransferred < contentLengthToServe && (bytesRead = driveInputStream.read(buffer, 0, (int) Math.min(buffer.length, contentLengthToServe - totalBytesTransferred))) != -1) {
                try {
                    responseOutputStream.write(buffer, 0, bytesRead);
                    totalBytesTransferred += bytesRead;
                } catch (IOException e) {
                    logger.warn("IOException writing to response output stream for file {}: {} (Client might have closed connection). Transferred: {} bytes.",
                            fileIdOnDrive, e.getMessage(), totalBytesTransferred);
                    break;
                }
            }
            responseOutputStream.flush();
            logger.info("Finished streaming. Total bytes transferred: {} for file ID: {}", totalBytesTransferred, fileIdOnDrive);

        } catch (IOException e) {
            logger.error("IOException during streaming content for file ID {}: {}", fileIdOnDrive, e.getMessage(), e);
            // Nếu lỗi xảy ra sau khi header đã được gửi, chúng ta không thể thay đổi status code nữa.
            // Client sẽ nhận được một stream không hoàn chỉnh.
        } finally {
            if (driveInputStream != null) {
                try { driveInputStream.close(); } catch (IOException e) { logger.error("Error closing Drive InputStream for {}: {}", fileIdOnDrive, e.getMessage(), e); }
            }
            // OutputStream của response sẽ được servlet container tự đóng, nhưng flush là tốt.
        }
    }

    // Hàm helper để xác định mimeType (có thể để trong một lớp Util)
    private String determineMimeType(String driveMimeType, String fileName) {
        if (driveMimeType != null && (driveMimeType.startsWith("video/") || driveMimeType.startsWith("audio/"))) {
            return driveMimeType;
        }
        // Fallback nếu mimeType từ Drive không phù hợp
        if (fileName != null) {
            String fnLower = fileName.toLowerCase();
            if (fnLower.endsWith(".mp4")) return "video/mp4";
            if (fnLower.endsWith(".mkv")) return "video/x-matroska";
            if (fnLower.endsWith(".webm")) return "video/webm";
            if (fnLower.endsWith(".mp3")) return "audio/mpeg";
            if (fnLower.endsWith(".m4a")) return "audio/mp4";
            if (fnLower.endsWith(".ogg")) return "audio/ogg";
            if (fnLower.endsWith(".wav")) return "audio/wav";
        }
        return driveMimeType != null ? driveMimeType : "application/octet-stream";
    }
}