// File: src/main/java/com/ltdd/streamapp/gdrive/service/Impl/MediaServiceImpl.java (BACKEND)
package com.ltdd.streamapp.gdrive.service.Impl;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File; // Drive File model
import com.ltdd.streamapp.gdrive.model.Media;
import com.ltdd.streamapp.gdrive.model.User;
import com.ltdd.streamapp.gdrive.payload.MediaResponse;
import com.ltdd.streamapp.gdrive.payload.MediaUploadRequest;
import com.ltdd.streamapp.gdrive.repository.MediaRepository;
import com.ltdd.streamapp.gdrive.repository.UserRepository;
import com.ltdd.streamapp.gdrive.service.MediaService;
import com.ltdd.streamapp.gdrive.service.StorageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;
// import ws.schild.jave.process.ffmpeg.FFMPEGProcess; // Bỏ comment nếu muốn set đường dẫn ffmpeg

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MediaServiceImpl implements MediaService {

    private static final Logger logger = LoggerFactory.getLogger(MediaServiceImpl.class);

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final Drive googleDriveService;

    @Value("${app.backend.base-url:http://localhost:9999}")
    private String backendBaseUrl;

    /*
    // Tùy chọn: Cấu hình đường dẫn FFMPEG nếu không nằm trong PATH hệ thống
    // Đường dẫn này cần được đặt trong một khối static hoặc một @Configuration bean
    // để đảm bảo nó được gọi một lần khi ứng dụng khởi động.
    static {
        try {
            // Thay thế bằng đường dẫn thực tế đến ffmpeg.exe (Windows) hoặc ffmpeg (Linux/macOS)
            // Ví dụ cho Windows: "C:/ffmpeg/bin/ffmpeg.exe"
            // Ví dụ cho Linux: "/usr/bin/ffmpeg"
            String ffmpegExecutablePath = "ffmpeg"; // Nếu ffmpeg đã có trong PATH
            // FFMPEGProcess.setFFMPEGExecutablePath(ffmpegExecutablePath);
            // logger.info("Attempted to set FFMPEG executable path to: {}", ffmpegExecutablePath);
        } catch (Exception e) {
            logger.warn("Could not set custom FFMPEG path: {}. JAVE2 will rely on ffmpeg being in the system PATH.", e.getMessage());
        }
    }
    */

    public MediaServiceImpl(MediaRepository mediaRepository,
                            UserRepository userRepository,
                            @Qualifier("googleDriveStorageService") StorageService storageService,
                            Drive googleDriveService) {
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.googleDriveService = googleDriveService;
    }

    @Override
    @Transactional
    public MediaResponse uploadMedia(String username, MultipartFile multipartFile, @Valid MediaUploadRequest uploadRequest) {
        if (multipartFile.isEmpty()) {
            throw new RuntimeException("Cannot upload empty media file.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found for media upload: " + username));

        String originalFilename = multipartFile.getOriginalFilename() != null ? multipartFile.getOriginalFilename() : "unknownfile";
        String fileId = storageService.uploadMediaFile(multipartFile, uploadRequest.getType(), originalFilename);

        if (fileId == null || fileId.isEmpty()) {
            throw new RuntimeException("Failed to upload media to storage, received null or empty fileId.");
        }

        Media media = new Media();
        media.setTitle(uploadRequest.getTitle());
        media.setDescription(uploadRequest.getDescription());
        media.setType(uploadRequest.getType().toUpperCase());
        media.setStorageIdentifier(fileId);
        media.setOwnerUsername(username);
        media.setPublic(uploadRequest.isPublic());
        // @PrePersist sẽ set uploadDate

        String durationStr = "00:00";
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("jave_", "_" + sanitizeFilenameForTemp(originalFilename));
            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                fos.write(multipartFile.getBytes());
            }
            logger.debug("Temporary file created for JAVE: {}", tempFile.toString());

            java.io.File a = tempFile.toFile();
            if (a.exists() && a.canRead()) {
                 MultimediaObject multimediaObject = new MultimediaObject(a);
                 MultimediaInfo info = multimediaObject.getInfo();
                 long durationMillis = info.getDuration();
                 if (durationMillis > 0) {
                    durationStr = formatDurationMillis(durationMillis);
                 } else if (durationMillis == -1) {
                    logger.warn("JAVE could not determine duration (returned -1) for file: {}. This might happen if ffmpeg is not found or the file is corrupted.", originalFilename);
                 }
                 logger.info("Extracted duration for file '{}': {} ms -> {}", originalFilename, durationMillis, durationStr);
            } else {
                logger.error("Temporary file for JAVE does not exist or is not readable: {}", tempFile.toString());
            }

        } catch (IOException e) {
            logger.error("IOException during temporary file creation or JAVE processing for '{}': {}", originalFilename, e.getMessage());
        } catch (EncoderException e) {
            // SỬA LỖI 1: EncoderException không có getErrorCode()
            // Thay vào đó, log toàn bộ message của nó, thường chứa thông tin lỗi từ ffmpeg.
            logger.error("JAVE EncoderException for file '{}': Message={}. Output from ffmpeg might be included in this message. Ensure ffmpeg is installed and in PATH.",
                         originalFilename, e.getMessage(), e); // Log cả stack trace
        } catch (UnsatisfiedLinkError ule) {
            logger.error("JAVE UnsatisfiedLinkError for file '{}': {}. This often means native FFmpeg libraries are missing or not found for your OS/architecture. Consider installing ffmpeg system-wide or ensure jave-nativebin dependencies are correct.", originalFilename, ule.getMessage(), ule);
        }
        catch (Exception e) {
            logger.error("Unexpected error extracting duration for file '{}': {}", originalFilename, e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    logger.debug("Temporary file deleted: {}", tempFile.toString());
                } catch (IOException e) {
                    // SỬA LỖI 2: Path không có getAbsolutePath(), dùng toString() hoặc toAbsolutePath().toString()
                    logger.warn("Could not delete temporary file: {}", tempFile.toAbsolutePath().toString(), e);
                }
            }
        }
        media.setDuration(durationStr);

        if ("MUSIC".equalsIgnoreCase(media.getType())) {
            media.setArtist(uploadRequest.getArtist() != null ? uploadRequest.getArtist() : "Unknown Artist");
            media.setAlbum(uploadRequest.getAlbum() != null ? uploadRequest.getAlbum() : "Unknown Album");
        }

        Media savedMedia = mediaRepository.save(media);
        logger.info("Media entity saved: ID = {}, Title = '{}', StorageID = '{}', Duration = '{}' by User '{}'",
                savedMedia.getId(), savedMedia.getTitle(), savedMedia.getStorageIdentifier(), savedMedia.getDuration(), username);

        return mapToMediaResponse(savedMedia, user);
    }

    private String formatDurationMillis(long millis) {
        if (millis < 0) {
            return "00:00";
        }
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private String sanitizeFilenameForTemp(String originalFilename) {
        if (originalFilename == null || originalFilename.trim().isEmpty()) return "tempfile";
        return originalFilename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getUserMedia(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        List<Media> mediaList = mediaRepository.findAllByOwnerUsername(username);
        return mediaList.stream()
                .map(media -> mapToMediaResponse(media, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getAllPublicMediaByType(String mediaType) {
        List<Media> mediaList = mediaRepository.findByIsPublicTrueAndTypeIgnoreCase(mediaType.toUpperCase());
        logger.info("Fetching public media by type: {}. Found: {} items.", mediaType.toUpperCase(), mediaList.size());
        return mediaList.stream().map(media -> {
            User owner = userRepository.findByUsername(media.getOwnerUsername()).orElse(null);
            return mapToMediaResponse(media, owner);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getAllPublicMedia() {
        List<Media> mediaList = mediaRepository.findByIsPublicTrue();
        logger.info("Fetching all public media. Found: {} items.", mediaList.size());
        return mediaList.stream().map(media -> {
            User owner = userRepository.findByUsername(media.getOwnerUsername()).orElse(null);
            return mapToMediaResponse(media, owner);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse getMediaDetails(String username, Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found with ID: " + mediaId));

        if (!media.isPublic() && (username == null || !media.getOwnerUsername().equals(username))) {
            logger.warn("User '{}' attempted to access private media ID '{}' owned by '{}'", username, mediaId, media.getOwnerUsername());
            throw new RuntimeException("You do not have permission to view this media.");
        }
        User owner = userRepository.findByUsername(media.getOwnerUsername()).orElse(null);
        return mapToMediaResponse(media, owner);
    }

    @Override
    @Transactional
    public void deleteMedia(String username, Long mediaId) {
        Media media = mediaRepository.findByIdAndOwnerUsername(mediaId, username)
                .orElseThrow(() -> new RuntimeException(
                        "Media not found with ID: " + mediaId + " or user '" + username + "' does not have permission to delete."));

        if (media.getStorageIdentifier() != null && !media.getStorageIdentifier().isEmpty()) {
            logger.info("Deleting media file from storage. Media ID: {}, Storage ID: {}", media.getId(), media.getStorageIdentifier());
            storageService.deleteMediaFile(media.getStorageIdentifier());
        }
        if (media.getThumbnailStorageIdentifier() != null && !media.getThumbnailStorageIdentifier().isEmpty()) {
            logger.info("Deleting thumbnail file from storage. Media ID: {}, Thumbnail Storage ID: {}", media.getId(), media.getThumbnailStorageIdentifier());
            storageService.deleteMediaFile(media.getThumbnailStorageIdentifier());
        }
        mediaRepository.delete(media);
        logger.info("Media record (ID: {}) deleted from database by user: {}", media.getId(), username);
    }

    private MediaResponse mapToMediaResponse(Media media, User owner) {
        if (media == null) return null;

        MediaResponse dto = new MediaResponse();
        dto.setId(media.getId());
        dto.setTitle(media.getTitle());
        dto.setDescription(media.getDescription());
        dto.setType(media.getType());
        dto.setOwnerUsername(media.getOwnerUsername());
        dto.setPublic(media.isPublic());
        dto.setDuration(media.getDuration());
        dto.setArtist(media.getArtist());
        dto.setAlbum(media.getAlbum());
        dto.setViewCount(media.getViewCount());

        if (media.getStorageIdentifier() != null) {
            String streamUrl = backendBaseUrl + "/api/media/stream/" + media.getStorageIdentifier();
            dto.setUrl(streamUrl);
            logger.debug("Mapped stream URL for media {}: {}", media.getId(), streamUrl);

            if (media.getThumbnailStorageIdentifier() != null) {
                dto.setThumbnailUrl("https://drive.google.com/thumbnail?id=" + media.getThumbnailStorageIdentifier() + "&sz=w320-h180");
            } else {
                 try {
                    File driveFile = googleDriveService.files().get(media.getStorageIdentifier())
                            .setFields("thumbnailLink")
                            .execute();
                    if (driveFile != null && driveFile.getThumbnailLink() != null) {
                        dto.setThumbnailUrl(driveFile.getThumbnailLink());
                        logger.debug("Mapped thumbnailLink (from main file) for media {}: {}", media.getId(), driveFile.getThumbnailLink());
                    } else {
                        logger.warn("No thumbnailLink from Drive for media ID (main file): {}", media.getStorageIdentifier());
                    }
                } catch (IOException e) {
                    logger.error("Could not retrieve thumbnailLink (from main file) for media ID {}: {}", media.getStorageIdentifier(), e.getMessage());
                }
            }
        }

        if (media.getUploadDate() != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(Locale.US)
                        .withZone(ZoneId.systemDefault());
                dto.setUploadDate(formatter.format(media.getUploadDate()));
            } catch (Exception e) {
                logger.error("Error formatting uploadDate for media {}: {}", media.getId(), e.getMessage());
                dto.setUploadDate(media.getUploadDate().toString());
            }
        }

        if (owner != null) {
            dto.setChannelName(owner.getFullName() != null && !owner.getFullName().isEmpty() ? owner.getFullName()
                    : owner.getUsername());
            dto.setChannelAvatarUrl(owner.getProfilePictureUrl());
        } else {
            dto.setChannelName("Unknown User");
        }
        return dto;
    }
}