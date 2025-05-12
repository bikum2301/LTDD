// File: src/main/java/com/ltdd/streamapp/gdrive/service/ProfileService.java
package com.ltdd.streamapp.gdrive.service;

import com.ltdd.streamapp.gdrive.payload.ProfileUpdateRequest;
import com.ltdd.streamapp.gdrive.payload.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    void updateProfile(String username, ProfileUpdateRequest profileRequest);

    /**
     * Uploads a profile picture for the given user.
     * @param username The username of the user.
     * @param file The profile picture file.
     * @return The public URL or an identifier for the uploaded picture.
     */
    String uploadProfilePicture(String username, MultipartFile file);

    UserProfileResponse getUserProfile(String username);
}