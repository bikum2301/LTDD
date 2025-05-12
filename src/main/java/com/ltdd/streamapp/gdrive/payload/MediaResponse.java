package com.ltdd.streamapp.gdrive.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; // Thêm NoArgsConstructor

@Data
@NoArgsConstructor // Thêm NoArgsConstructor để linh hoạt hơn khi tạo object
@AllArgsConstructor
public class MediaResponse {
    private Long id;
    private String title;
    private String description;
    private String type; // "MUSIC" or "VIDEO"

    // URL này sẽ là URL có thể stream được mà client dùng (ví dụ: link Google Drive đã xử lý)
    private String url;

    private String ownerUsername; // Thêm field này để client biết chủ sở hữu
    private boolean isPublic;     // Đảm bảo tên biến là isPublic để getter của Lombok là isPublic()

    // --- CÁC TRƯỜNG MỚI CẦN THÊM ĐỂ KHỚP VỚI CLIENT ANDROID ---
    private String thumbnailUrl; // URL thumbnail (ví dụ: link Google Drive đã xử lý cho thumbnail)
    private String duration;     // Định dạng "HH:MM:SS" hoặc "MM:SS"
    private String artist;       // Tên nghệ sĩ (cho music)
    private String album;        // Tên album (cho music)
    private String channelName;  // Tên kênh/người đăng (cho video)
    private String channelAvatarUrl; // URL avatar của kênh/người đăng
    private long viewCount;      // Số lượt xem
    private String uploadDate;   // Ngày đăng, định dạng String (ví dụ: "3 days ago", "Jul 20, 2024")

    // Constructor bạn cung cấp ban đầu chỉ có 6 tham số,
    // với các trường mới này, @AllArgsConstructor sẽ tạo constructor đầy đủ.
    // @NoArgsConstructor cho phép tạo object rỗng và set giá trị sau.
}