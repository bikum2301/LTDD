// File: src/main/java/com/ltdd/streamapp/gdrive/controller/ProfileController.java
package com.ltdd.streamapp.gdrive.controller;

import com.ltdd.streamapp.gdrive.payload.MessageResponse;
import com.ltdd.streamapp.gdrive.payload.ProfileUpdateRequest;
import com.ltdd.streamapp.gdrive.payload.UserProfileResponse; // Import UserProfileResponse
import com.ltdd.streamapp.gdrive.service.ProfileService;
import jakarta.validation.Valid; // Import @Valid
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Endpoint mới để get thông tin profile hiện tại
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(@AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            // Endpoint này yêu cầu xác thực, nên currentUser không bao giờ null nếu security đúng
            // Tuy nhiên, để an toàn, có thể throw lỗi hoặc trả về UNAUTHORIZED
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                  .body(null); // Hoặc một UserProfileResponse lỗi
        }
        UserProfileResponse userProfile = profileService.getUserProfile(currentUser.getUsername());
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest profileRequest, // Thêm @Valid
                                           @AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                  .body(new MessageResponse("Error: User not authenticated."));
        }
        profileService.updateProfile(currentUser.getUsername(), profileRequest);
        return ResponseEntity.ok(new MessageResponse("Profile updated successfully"));
    }

    @PostMapping("/upload-profile-picture")
    public ResponseEntity<?> uploadProfilePicture(@RequestParam("file") MultipartFile file,
                                                  @AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                  .body(new MessageResponse("Error: User not authenticated."));
        }
        if (file.isEmpty()){
             return ResponseEntity.badRequest().body(new MessageResponse("Error: File to upload cannot be empty."));
        }
        String imageUrl = profileService.uploadProfilePicture(currentUser.getUsername(), file);
        // Sử dụng static factory method nếu có trong MessageResponse
        return ResponseEntity.ok(MessageResponse.uploadSuccess("Profile picture uploaded successfully", imageUrl));
    }
}