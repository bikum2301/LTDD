// File: src/main/java/com/ltdd/streamapp/gdrive/service/StorageService.java
package com.ltdd.streamapp.gdrive.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /**
     * Uploads a media file (music or video).
     * @param file The multipart file to upload.
     * @param mediaType Type of media ("MUSIC" or "VIDEO").
     * @param originalFilename The original name of the file, used for context or naming.
     * @return A unique identifier for the stored file (e.g., Google Drive File ID).
     */
    String uploadMediaFile(MultipartFile file, String mediaType, String originalFilename);

    /**
     * Uploads a profile picture.
     * @param file The multipart file to upload.
     * @param originalFilename The original name of the file.
     * @return A unique identifier for the stored profile picture.
     */
    String uploadProfilePicture(MultipartFile file, String originalFilename);

    /**
     * Deletes a stored file using its identifier.
     * @param fileIdentifier The unique identifier of the file to delete (e.g., Google Drive File ID).
     */
    void deleteMediaFile(String fileIdentifier);
}