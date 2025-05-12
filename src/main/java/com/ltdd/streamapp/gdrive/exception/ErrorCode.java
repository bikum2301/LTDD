package com.ltdd.streamapp.gdrive.exception;

public enum ErrorCode {
    USER_NOT_FOUND(404, "User not found"),
    MEDIA_NOT_FOUND(404, "Media not found"),
    ROLE_NOT_FOUND(500, "Role not found"),
    INVALID_OTP(400, "Invalid OTP"),
    EXPIRED_OTP(400, "OTP has expired"),
    EMAIL_ALREADY_EXISTS(400, "Email is already in use"),
    USERNAME_ALREADY_EXISTS(400, "Username is already taken"),
    UPLOAD_FAILED(500, "File upload failed"),
    DELETE_FAILED(500, "File deletion failed"),
    PROFILE_UPDATE_FAILED(500, "Profile update failed"),
    UNAUTHORIZED(401, "Unauthorized - Authentication token was missing or invalid"),
    FORBIDDEN(403, "Forbidden - You don't have permission to access this resource"),
    BAD_REQUEST(400, "Bad Request - The request was invalid or cannot be otherwise served"),
    VALIDATION_ERROR(400, "Validation Error - One or more fields are invalid"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error - An unexpected error occurred");

    private final int status;
    private final String message; // Đổi defaultMessage thành message

    ErrorCode(int status, String message) { // Đổi defaultMessage thành message
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() { // ĐỔI TÊN GETTER THÀNH getMessage()
        return message;
    }
}