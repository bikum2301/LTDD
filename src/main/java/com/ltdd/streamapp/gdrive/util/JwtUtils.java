// com.ltdd.streamapp.gdrive.util.JwtUtils.java
package com.ltdd.streamapp.gdrive.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys; // Quan trọng cho việc tạo key an toàn
import io.jsonwebtoken.security.SignatureException; // Bắt lỗi cụ thể hơn
import org.slf4j.Logger; // Sử dụng SLF4J logger
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey; // Dùng SecretKey
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecretString; // Đổi tên biến để rõ ràng là chuỗi secret ban đầu

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey key; // Sử dụng SecretKey đã được khởi tạo

    // Khởi tạo key một lần khi bean được tạo
    // @PostConstruct // Hoặc có thể làm trong constructor nếu jwtSecretString được inject qua constructor
    public void init() {
        // Tạo SecretKey từ chuỗi secret. Đảm bảo jwtSecretString đủ mạnh.
        // Đối với HS512, key nên có ít nhất 512 bits (64 bytes).
        // Nếu jwtSecretString của bạn không phải là base64 encoded và là một chuỗi text,
        // cần đảm bảo nó đủ dài và phức tạp.
        this.key = Keys.hmacShaKeyFor(jwtSecretString.getBytes(StandardCharsets.UTF_8));
    }

    public String generateJwtToken(String username) {
        if (this.key == null) init(); // Đảm bảo key đã được khởi tạo
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS512) // Sử dụng SecretKey và thuật toán
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        if (this.key == null) init(); // Đảm bảo key đã được khởi tạo
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        if (this.key == null) init(); // Đảm bảo key đã được khởi tạo
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}