// File: src/main/java/com/ltdd/streamapp/gdrive/controller/AuthController.java
package com.ltdd.streamapp.gdrive.controller;

// import com.ltdd.streamapp.gdrive.model.User; // Không cần User ở đây nữa
import com.ltdd.streamapp.gdrive.payload.LoginRequest;
import com.ltdd.streamapp.gdrive.payload.MessageResponse;
import com.ltdd.streamapp.gdrive.payload.SignupRequest;
import com.ltdd.streamapp.gdrive.service.AuthService;
import com.ltdd.streamapp.gdrive.util.JwtUtils;
import jakarta.validation.Valid; // Import @Valid
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600) // Cho phép CORS từ mọi nguồn (điều chỉnh cho production)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    public AuthController(AuthService authService, JwtUtils jwtUtils) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) { // Thêm @Valid
        authService.registerUser(signUpRequest); // registerUser giờ có thể không trả về User
        return ResponseEntity.ok(new MessageResponse("User registered successfully. Please check your email for the OTP."));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestParam("code") String code) {
        // Kiểm tra code không rỗng ở đây hoặc trong service
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: OTP code cannot be empty."));
        }
        authService.verifyUser(code);
        return ResponseEntity.ok(new MessageResponse("User verified successfully. You can now login."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) { // Thêm @Valid
        authService.authenticateUser(loginRequest); // Sẽ throw exception nếu lỗi
        String token = jwtUtils.generateJwtToken(loginRequest.getUsername());
        // Sử dụng static factory method nếu có
        return ResponseEntity.ok(MessageResponse.loginSuccess(token));
    }
}