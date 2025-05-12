// File: src/main/java/com/ltdd/streamapp/gdrive/service/MediaService.java (BACKEND)
package com.ltdd.streamapp.gdrive.service;

import com.ltdd.streamapp.gdrive.payload.MediaResponse;
import com.ltdd.streamapp.gdrive.payload.MediaUploadRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface MediaService {
    MediaResponse uploadMedia(String username, MultipartFile file, MediaUploadRequest uploadRequest);
    List<MediaResponse> getUserMedia(String username);
    MediaResponse getMediaDetails(String username, Long mediaId);
    void deleteMedia(String username, Long mediaId);
    List<MediaResponse> getAllPublicMediaByType(String mediaType); // mediaType có thể là null
    List<MediaResponse> getAllPublicMedia();
}