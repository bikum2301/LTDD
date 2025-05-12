// com.ltdd.streamapp.gdrive.payload.ErrorResponse.java
package com.ltdd.streamapp.gdrive.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;   // General error type e.g., "Bad Request", "Unauthorized"
    private String message; // Specific error message from exception or validation
    private String path;
    // private Object details; // (Tùy chọn) For more detailed error info, like validation errors list
}