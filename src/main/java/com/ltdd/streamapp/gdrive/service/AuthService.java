// File: src/main/java/com/ltdd/streamapp/gdrive/service/AuthService.java
package com.ltdd.streamapp.gdrive.service;

import com.ltdd.streamapp.gdrive.model.User;
import com.ltdd.streamapp.gdrive.payload.LoginRequest;
import com.ltdd.streamapp.gdrive.payload.SignupRequest;

public interface AuthService {

    User registerUser(SignupRequest request);

    void verifyUser(String otp);

    // Sẽ trả về User hoặc UserDetails nếu cần, hoặc void nếu chỉ xác thực
    void authenticateUser(LoginRequest request);

    String generateOtp();
}