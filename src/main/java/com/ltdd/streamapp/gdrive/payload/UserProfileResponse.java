// TẠO MỚI FILE NÀY: com.ltdd.streamapp.gdrive.payload.UserProfileResponse.java
package com.ltdd.streamapp.gdrive.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String profilePictureUrl; // URL đầy đủ mà client có thể hiển thị trực tiếp
    private String bio;
    // Bạn có thể thêm các trường khác từ User entity nếu client cần
    // ví dụ: LocalDateTime createdAt; (nhưng thường sẽ format thành String)
}