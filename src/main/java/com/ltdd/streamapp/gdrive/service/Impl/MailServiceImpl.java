// File: src/main/java/com/ltdd/streamapp/gdrive/service/Impl/MailServiceImpl.java
package com.ltdd.streamapp.gdrive.service.Impl;

import com.ltdd.streamapp.gdrive.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // Nếu fromEmail null hoặc rỗng (ví dụ khi dùng MailHog không cần set username),
            // mailSender có thể tự dùng giá trị mặc định hoặc cấu hình từ server.
            // Tuy nhiên, một số SMTP server yêu cầu from address.
            if (fromEmail != null && !fromEmail.isEmpty()) {
                 message.setFrom(fromEmail);
            } else {
                // Nếu dùng MailDev/MailHog, fromEmail có thể không cần thiết
                // Hoặc đặt một giá trị mặc định:
                message.setFrom("noreply@streamapp.com");
                logger.warn("spring.mail.username is not set. Using default 'noreply@streamapp.com' as From address.");
            }

            message.setTo(toEmail);
            message.setSubject("Your StreamApp OTP Code");
            message.setText("Your One-Time Password (OTP) for StreamApp is: " + otp +
                            "\nThis code will expire in 10 minutes." +
                            "\nIf you did not request this, please ignore this email.");
            mailSender.send(message);
            logger.info("OTP email sent to {}", toEmail);
        } catch (MailException e) {
            logger.error("Error sending OTP email to {}: {}", toEmail, e.getMessage(), e);
            // Quyết định có throw exception hay không. Hiện tại chỉ log lỗi.
            // throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }
}