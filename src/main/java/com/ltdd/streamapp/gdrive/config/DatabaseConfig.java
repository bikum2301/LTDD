// com.ltdd.streamapp.gdrive.config.DatabaseConfig.java
package com.ltdd.streamapp.gdrive.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
// THAY ĐỔI QUAN TRỌNG: Cập nhật basePackages cho JpaRepositories
@EnableJpaRepositories(basePackages = "com.ltdd.streamapp.gdrive.repository")
public class DatabaseConfig {

    // Các @Value này vẫn hữu ích nếu bạn muốn truy cập các giá trị này
    // trong bean này, mặc dù Spring Boot tự động cấu hình DataSource.
    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    // Bạn có thể thêm các cấu hình tùy chỉnh cho DataSource ở đây nếu cần,
    // nhưng với H2 và cấu hình trong application.properties, thường không cần thiết.
}