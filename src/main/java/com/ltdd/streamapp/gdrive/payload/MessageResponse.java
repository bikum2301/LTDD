// com.ltdd.streamapp.gdrive.payload.MessageResponse.java
package com.ltdd.streamapp.gdrive.payload;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageResponse {
    private String message;
    private String token; // For login
    private String url;   // For uploads or general purpose URL
    private Object data;  // (Tùy chọn) Cho các response cần trả thêm dữ liệu nhỏ

    public MessageResponse(String message) {
        this.message = message;
    }

    public MessageResponse(String message, String tokenOrUrl, boolean isToken) {
        this.message = message;
        if (isToken) {
            this.token = tokenOrUrl;
        } else {
            this.url = tokenOrUrl;
        }
    }
    // Constructor cho trường hợp chỉ có message và token (login)
    public static MessageResponse loginSuccess(String token) {
        MessageResponse response = new MessageResponse("Login successful");
        response.setToken(token);
        return response;
    }
    // Constructor cho trường hợp chỉ có message và url (upload)
    public static MessageResponse uploadSuccess(String message, String url) {
        MessageResponse response = new MessageResponse(message);
        response.setUrl(url);
        return response;
    }
}