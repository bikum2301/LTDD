// File: src/main/java/com/ltdd/streamapp/gdrive/service/MailService.java
package com.ltdd.streamapp.gdrive.service;

public interface MailService {
    void sendOtpEmail(String toEmail, String otp);
}