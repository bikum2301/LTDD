// File: src/main/java/com/ltdd/streamapp/gdrive/payload/ProfileUpdateRequest.java (BACKEND)
package com.ltdd.streamapp.gdrive.payload;

import jakarta.validation.constraints.Email; // Giữ lại nếu bạn vẫn muốn nhận email (dù không dùng để update)
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor; // Thêm constructor không tham số
import lombok.AllArgsConstructor; // Thêm constructor tất cả tham số (tùy chọn)


@Data
@NoArgsConstructor // Cho phép tạo object và set field sau
@AllArgsConstructor // Tạo constructor với tất cả các field (tùy chọn)
public class ProfileUpdateRequest {

    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;

    // Email này hiện tại không dùng để CẬP NHẬT email của user,
    // mà chỉ là một phần của DTO mà client có thể gửi lên.
    // Backend sẽ tìm user bằng username từ token.
    // Nếu bạn không muốn client gửi email này nữa, có thể bỏ nó đi.
    @Email(message = "Please provide a valid email format if included")
    private String email;

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio; // << TRƯỜNG QUAN TRỌNG CẦN THÊM
}