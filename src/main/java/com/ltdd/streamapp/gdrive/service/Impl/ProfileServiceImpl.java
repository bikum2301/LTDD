// File: src/main/java/com/ltdd/streamapp/gdrive/service/Impl/ProfileServiceImpl.java (BACKEND)
package com.ltdd.streamapp.gdrive.service.Impl;

import com.ltdd.streamapp.gdrive.model.User;
import com.ltdd.streamapp.gdrive.payload.ProfileUpdateRequest;
import com.ltdd.streamapp.gdrive.payload.UserProfileResponse;
import com.ltdd.streamapp.gdrive.repository.UserRepository;
import com.ltdd.streamapp.gdrive.service.ProfileService;
import com.ltdd.streamapp.gdrive.service.StorageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final StorageService storageService;

    public ProfileServiceImpl(UserRepository userRepository,
                              @Qualifier("googleDriveStorageService") StorageService storageService) {
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public void updateProfile(String username, @Valid ProfileUpdateRequest profileRequest) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Attempt to update profile for non-existent user: {}", username);
                    return new RuntimeException("User not found with username: " + username);
                });

        // Cập nhật FullName nếu được cung cấp trong request
        if (profileRequest.getFullName() != null) {
            // Bạn có thể muốn kiểm tra xem fullName có thực sự thay đổi không trước khi set
            // if (!profileRequest.getFullName().equals(user.getFullName())) {
            //    user.setFullName(profileRequest.getFullName());
            // }
            // Hoặc đơn giản là set luôn
            user.setFullName(profileRequest.getFullName());
            logger.debug("Updating fullName for user '{}' to '{}'", username, profileRequest.getFullName());
        }

        // Cập nhật Bio nếu được cung cấp trong request
        // Cho phép set bio thành chuỗi rỗng (để xóa bio) hoặc null (nếu DTO cho phép)
        if (profileRequest.getBio() != null) {
            user.setBio(profileRequest.getBio());
            logger.debug("Updating bio for user '{}' to '{}'", username, profileRequest.getBio());
        }
        // Lưu ý: Nếu client gửi null cho bio và bạn muốn giữ lại bio cũ, bạn cần thêm logic kiểm tra
        // ví dụ: if (profileRequest.getBio() != null) user.setBio(profileRequest.getBio());
        // Nhưng thường thì nếu client gửi DTO, việc không có field đó hoặc field đó là null
        // có thể được hiểu là không muốn thay đổi, hoặc muốn set thành null/rỗng.
        // Logic hiện tại: nếu client gửi bio (kể cả rỗng), nó sẽ được cập nhật.

        // Email không được cập nhật qua API này theo thiết kế hiện tại.
        // Trường email trong ProfileUpdateRequest nếu có, chỉ mang tính thông tin
        // hoặc để backend xác thực (dù không cần thiết vì đã có username từ token).

        userRepository.save(user);
        logger.info("Profile for user '{}' updated in database.", username);
    }

    @Override
    @Transactional
    public String uploadProfilePicture(String username, MultipartFile file) {
        if (file.isEmpty()) {
            logger.warn("Attempt to upload empty profile picture for user: {}", username);
            throw new RuntimeException("Cannot upload empty file for profile picture.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Attempt to upload profile picture for non-existent user: {}", username);
                    return new RuntimeException("User not found: " + username);
                });

        // Xóa ảnh cũ trên Google Drive nếu có
        if (user.getProfilePictureStorageIdentifier() != null && !user.getProfilePictureStorageIdentifier().isEmpty()) {
            try {
                logger.info("Attempting to delete old profile picture (Storage ID: {}) for user: {}", user.getProfilePictureStorageIdentifier(), username);
                storageService.deleteMediaFile(user.getProfilePictureStorageIdentifier());
                logger.info("Old profile picture (Storage ID: {}) deleted successfully for user: {}", user.getProfilePictureStorageIdentifier(), username);
            } catch (Exception e) {
                logger.error("Could not delete old profile picture (Storage ID: {}) for user {}: {}", user.getProfilePictureStorageIdentifier(), username, e.getMessage());
                // Không block việc upload ảnh mới nếu xóa ảnh cũ lỗi, nhưng cần ghi log rõ
            }
        }

        String fileId = storageService.uploadProfilePicture(file, file.getOriginalFilename());
        if (fileId == null || fileId.isEmpty()) {
            logger.error("Storage service failed to return a file ID for profile picture upload for user: {}", username);
            throw new RuntimeException("Failed to upload profile picture to storage.");
        }
        logger.info("New profile picture uploaded to storage for user: {}, File ID: {}", username, fileId);


        user.setProfilePictureStorageIdentifier(fileId); // Lưu File ID mới

        // Xây dựng URL hiển thị (ví dụ: thumbnail từ Google Drive)
        String displayUrl = "https://drive.google.com/thumbnail?id=" + fileId + "&sz=s220"; // sz=s220 là kích thước thumbnail
        user.setProfilePictureUrl(displayUrl);
        logger.debug("Setting profilePictureUrl for user '{}' to '{}'", username, displayUrl);
        logger.debug("Setting profilePictureStorageIdentifier for user '{}' to '{}'", username, fileId);


        userRepository.save(user);
        logger.info("Profile picture database record updated for user: {}, New File ID: {}, New Display URL: {}", username, fileId, displayUrl);
        return displayUrl; // Trả về URL mà client có thể hiển thị
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Attempt to get profile for non-existent user: {}", username);
                    return new RuntimeException("User not found with username: " + username);
                });

        // URL ảnh profile đã được xây dựng và lưu trong user.getProfilePictureUrl()
        // khi uploadProfilePicture được gọi.
        logger.debug("Fetching profile for user '{}': FullName='{}', Email='{}', Bio='{}', PicUrl='{}'",
                user.getUsername(), user.getFullName(), user.getEmail(), user.getBio(), user.getProfilePictureUrl());

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getProfilePictureUrl(), // Lấy URL đã được xử lý
                user.getBio()
        );
    }
}