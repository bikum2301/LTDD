// com.ltdd.streamapp.gdrive.config.GoogleDriveConfig.java
package com.ltdd.streamapp.gdrive.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleDriveConfig {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveConfig.class);

    @Value("${google.drive.credentials.file.path:google-credentials.json}")
    private String credentialsFilePath;

    @Value("${google.drive.application.name:StreamApp API GDrive}") // Đã có trong application.properties
    private String applicationName;

    @Value("${google.drive.parent.folder.id}")
    private String parentFolderId;


    @Bean
    public Drive googleDriveService() throws IOException, GeneralSecurityException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        InputStream credentialsStream = new ClassPathResource(credentialsFilePath).getInputStream();
        if (credentialsStream == null) {
            logger.error("Google Drive credentials file not found at classpath: {}", credentialsFilePath);
            throw new IOException("Credential file not found: " + credentialsFilePath);
        }
        logger.info("Loading Google Drive credentials from: {}", credentialsFilePath);


        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(Collections.singleton(DriveScopes.DRIVE));
        // DRIVE_FILE: Cho phép tạo, đọc, sửa, xóa file mà app tạo ra trong phạm vi scope.
        // Nếu bạn cần truy cập file không phải do app tạo, có thể cần DriveScopes.DRIVE
        // và đảm bảo service account có quyền truy cập các file/folder đó.

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        Drive drive = new Drive.Builder(httpTransport, jsonFactory, requestInitializer)
                .setApplicationName(applicationName)
                .build();
        logger.info("Google Drive service initialized for application: {}", applicationName);
        return drive;
    }

    @Bean("googleDriveParentFolderId") // Đặt tên cho bean này để @Qualifier có thể tìm thấy
    public String googleDriveParentFolderId() {
        if (parentFolderId == null || parentFolderId.isEmpty() || "YOUR_GOOGLE_DRIVE_PARENT_FOLDER_ID_HERE".equals(parentFolderId)) {
            logger.warn("Google Drive parent folder ID is not configured properly or using default placeholder!");
        }
        return parentFolderId;
    }
}