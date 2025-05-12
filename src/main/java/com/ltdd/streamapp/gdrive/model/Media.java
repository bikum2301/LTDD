package com.ltdd.streamapp.gdrive.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@Data
@NoArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String type; // "VIDEO" or "MUSIC"

    // Sẽ lưu trữ Google Drive File ID ở đây
    @Column(nullable = false, name = "storage_identifier")
    private String storageIdentifier;

    // (Tùy chọn) Google Drive File ID của thumbnail nếu bạn quản lý riêng
    @Column(name = "thumbnail_storage_identifier")
    private String thumbnailStorageIdentifier;

    @Column(nullable = false)
    private String ownerUsername;

    @Column(nullable = false)
    private boolean isPublic;

    private String duration;
    private String artist;
    private String album;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private long viewCount = 0;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @PrePersist
    protected void onUpload() {
        if (this.uploadDate == null) { // Chỉ set nếu chưa có (hữu ích khi update)
           this.uploadDate = LocalDateTime.now();
        }
    }
}