// com.ltdd.streamapp.gdrive.repository.MediaRepository.java
package com.ltdd.streamapp.gdrive.repository;

import com.ltdd.streamapp.gdrive.model.Media; // Sửa import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Thêm @Repository nếu chưa có

import java.util.List;
import java.util.Optional;

@Repository // Nên thêm @Repository cho rõ ràng, dù Spring có thể tự phát hiện
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findAllByOwnerUsername(String ownerUsername);
    Optional<Media> findByIdAndOwnerUsername(Long id, String ownerUsername);
    List<Media> findByIsPublicTrue();
    List<Media> findByIsPublicTrueAndTypeIgnoreCase(String type);

    // (Tùy chọn) Nếu bạn muốn tìm theo storageIdentifier (Google Drive File ID)
    // Optional<Media> findByStorageIdentifier(String storageIdentifier);
}