// File: src/main/java/com/ltdd/streamapp/gdrive/service/Impl/GoogleDriveStorageServiceImpl.java
package com.ltdd.streamapp.gdrive.service.Impl;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.ltdd.streamapp.gdrive.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;

@Service("googleDriveStorageService")
public class GoogleDriveStorageServiceImpl implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveStorageServiceImpl.class);
    private final Drive driveService;
    private final String parentFolderId; // Giá trị này được inject từ application.properties

    // Giá trị placeholder bạn dùng để kiểm tra xem ID đã được cấu hình chưa
    private static final String PARENT_FOLDER_ID_PLACEHOLDER = "YOUR_GOOGLE_DRIVE_PARENT_FOLDER_ID_HERE";


    public GoogleDriveStorageServiceImpl(Drive driveService,
                                         @Qualifier("googleDriveParentFolderId") String parentFolderId) {
        this.driveService = driveService;
        this.parentFolderId = parentFolderId;
        // Log giá trị parentFolderId khi khởi tạo để dễ debug
        if (PARENT_FOLDER_ID_PLACEHOLDER.equalsIgnoreCase(this.parentFolderId) || this.parentFolderId == null || this.parentFolderId.trim().isEmpty()) {
            logger.warn("!!! CRITICAL WARNING: Google Drive parent folder ID is NOT configured correctly or is using the default placeholder '{}'. Files will be uploaded to the root of the Service Account's Drive if this is not fixed. Current value: '{}'", PARENT_FOLDER_ID_PLACEHOLDER, this.parentFolderId);
        } else {
            logger.info("GoogleDriveStorageService initialized with parentFolderId: {}", this.parentFolderId);
        }
    }

    @Override
    public String uploadMediaFile(MultipartFile multipartFile, String mediaType, String originalFilename) {
        logger.info("Attempting to upload media file: '{}', type: {}", originalFilename, mediaType);
        return uploadFileToDrive(multipartFile, originalFilename, determineMimeType(originalFilename, mediaType), "media");
    }

    @Override
    public String uploadProfilePicture(MultipartFile multipartFile, String originalFilename) {
        logger.info("Attempting to upload profile picture: '{}'", originalFilename);
        return uploadFileToDrive(multipartFile, originalFilename, multipartFile.getContentType(), "profile");
    }

    private String uploadFileToDrive(MultipartFile multipartFile, String originalFilename, String mimeType, String fileTypePrefix) {
        try {
            String uniqueFileName = fileTypePrefix + "-" + UUID.randomUUID().toString().substring(0, 12) + "-" + sanitizeFilename(originalFilename);
            File fileMetadata = new File();
            fileMetadata.setName(uniqueFileName);

            // --- SỬA ĐIỀU KIỆN KIỂM TRA Ở ĐÂY ---
            if (this.parentFolderId != null &&
                !this.parentFolderId.trim().isEmpty() &&
                !PARENT_FOLDER_ID_PLACEHOLDER.equalsIgnoreCase(this.parentFolderId)) {
                fileMetadata.setParents(Collections.singletonList(this.parentFolderId));
                logger.info("Setting parent folder for '{}' to: {}", uniqueFileName, this.parentFolderId);
            } else {
                logger.warn("Parent folder ID ('{}') is either null, empty, or still the placeholder. File '{}' will be uploaded to the root of the Service Account's Drive.", this.parentFolderId, uniqueFileName);
                // Không set parent, file sẽ vào root Drive của Service Account
            }
            // --- KẾT THÚC SỬA ĐỔI ---


            InputStream inputStream = multipartFile.getInputStream();
            InputStreamContent mediaContent = new InputStreamContent(mimeType, inputStream);

            logger.debug("Uploading file to Google Drive. FileName: '{}', MimeType: '{}', ParentFolderID (if set): '{}'",
                         uniqueFileName, mimeType, (fileMetadata.getParents() != null ? fileMetadata.getParents().get(0) : "Root"));


            Drive.Files.Create insertRequest = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, webViewLink, webContentLink, thumbnailLink");

            File uploadedFile = insertRequest.execute();
            if (uploadedFile == null || uploadedFile.getId() == null) {
                logger.error("Google Drive API did not return a file ID after upload for: '{}'", uniqueFileName);
                throw new RuntimeException("Failed to upload file: Google Drive API did not return a file ID.");
            }

            logger.info("File successfully uploaded to Google Drive. Name: '{}', ID: '{}'",
                    uploadedFile.getName(), uploadedFile.getId());

            Permission permission = new Permission().setType("anyone").setRole("reader");
            driveService.permissions().create(uploadedFile.getId(), permission).execute();
            logger.info("Public read permission set for file ID: {}", uploadedFile.getId());

            return uploadedFile.getId();

        } catch (IOException e) {
            logger.error("IOException during file upload to Google Drive for '{}': {}", originalFilename, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to Google Drive due to IOException: " + e.getMessage(), e);
        } catch (Exception e) { // Bắt các lỗi chung khác từ Google API Client
            logger.error("Unexpected error during file upload to Google Drive for '{}': {}", originalFilename, e.getMessage(), e);
            // Log thêm chi tiết của GoogleJsonResponseException nếu có
            if (e instanceof com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                com.google.api.client.googleapis.json.GoogleJsonResponseException gjre = (com.google.api.client.googleapis.json.GoogleJsonResponseException) e;
                if (gjre.getDetails() != null) {
                    logger.error("Google API Error Details: Status Code: {}, Message: {}, Errors: {}",
                                 gjre.getStatusCode(), gjre.getDetails().getMessage(), gjre.getDetails().getErrors());
                }
            }
            throw new RuntimeException("Unexpected error during file upload to Google Drive: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteMediaFile(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            logger.warn("Attempted to delete media with null or empty fileId.");
            return;
        }
        try {
            driveService.files().delete(fileId).execute();
            logger.info("Successfully deleted file from Google Drive: ID = {}", fileId);
        } catch (IOException e) {
            logger.error("Failed to delete file from Google Drive: ID = {}. Error: {}", fileId, e.getMessage());
             if (e instanceof com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                com.google.api.client.googleapis.json.GoogleJsonResponseException gjre = (com.google.api.client.googleapis.json.GoogleJsonResponseException) e;
                if (gjre.getDetails() != null && gjre.getStatusCode() == 404) { // File not found
                    logger.warn("Attempted to delete file (ID: {}) that was not found on Google Drive.", fileId);
                    return; // Không cần throw lỗi nếu file không tìm thấy khi xóa
                }
            }
            // throw new RuntimeException("Failed to delete file from Google Drive: " + e.getMessage(), e); // Cân nhắc
        }
    }

    private String determineMimeType(String filename, String mediaType) {
        if (filename == null) filename = "";
        filename = filename.toLowerCase();

        if (filename.endsWith(".mp3")) return "audio/mpeg";
        if (filename.endsWith(".m4a")) return "audio/mp4";
        if (filename.endsWith(".wav")) return "audio/wav";
        if (filename.endsWith(".ogg")) return "audio/ogg";
        if (filename.endsWith(".aac")) return "audio/aac";


        if (filename.endsWith(".mp4")) return "video/mp4";
        if (filename.endsWith(".mov")) return "video/quicktime";
        if (filename.endsWith(".avi")) return "video/x-msvideo";
        if (filename.endsWith(".webm")) return "video/webm";
        if (filename.endsWith(".mkv")) return "video/x-matroska";
        if (filename.endsWith(".flv")) return "video/x-flv";
        if (filename.endsWith(".wmv")) return "video/x-ms-wmv";


        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".webp")) return "image/webp";


        if (mediaType != null) {
            if (mediaType.equalsIgnoreCase("MUSIC")) return "audio/mpeg"; // Default for music
            if (mediaType.equalsIgnoreCase("VIDEO")) return "video/mp4"; // Default for video
            if (mediaType.equalsIgnoreCase("IMAGE")) return "image/jpeg"; // Default for image
        }
        logger.warn("Could not determine specific MIME type for filename: '{}', mediaType: '{}', falling back to octet-stream", filename, mediaType);
        return "application/octet-stream";
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) return "unknown_file";
        // Loại bỏ các ký tự không an toàn, giữ lại dấu chấm để phân biệt extension
        // Thay thế khoảng trắng và nhiều ký tự đặc biệt bằng dấu gạch dưới
        // Giữ lại chữ cái, số, dấu chấm, gạch ngang, gạch dưới
        String sanitized = filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_").toLowerCase();
        // Giới hạn độ dài tên file (Google Drive có giới hạn)
        int maxLength = 200; // Giới hạn an toàn
        if (sanitized.length() > maxLength) {
            // Cố gắng giữ lại phần extension
            String extension = "";
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0 && sanitized.length() - lastDot < 10) { // Heuristic for extension
                extension = sanitized.substring(lastDot);
                sanitized = sanitized.substring(0, lastDot);
            }
            sanitized = sanitized.substring(0, Math.min(sanitized.length(), maxLength - extension.length())) + extension;
        }
        return sanitized;
    }
}