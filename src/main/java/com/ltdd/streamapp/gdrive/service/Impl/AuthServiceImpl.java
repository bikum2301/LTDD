// File: src/main/java/com/ltdd/streamapp/gdrive/service/Impl/AuthServiceImpl.java
package com.ltdd.streamapp.gdrive.service.Impl;

import com.ltdd.streamapp.gdrive.model.ERole;
import com.ltdd.streamapp.gdrive.model.Role;
import com.ltdd.streamapp.gdrive.model.User;
import com.ltdd.streamapp.gdrive.payload.LoginRequest;
import com.ltdd.streamapp.gdrive.payload.SignupRequest;
import com.ltdd.streamapp.gdrive.repository.RoleRepository;
import com.ltdd.streamapp.gdrive.repository.UserRepository;
import com.ltdd.streamapp.gdrive.service.AuthService;
import com.ltdd.streamapp.gdrive.service.MailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Thêm RoleRepository
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository, // Inject RoleRepository
                           PasswordEncoder passwordEncoder,
                           MailService mailService,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public User registerUser(@Valid SignupRequest request) { // Thêm @Valid
        if (userRepository.existsByUsername(request.getUsername())) {
            logger.warn("Registration attempt with existing username: {}", request.getUsername());
            throw new RuntimeException("Error: Username is already taken!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration attempt with existing email: {}", request.getEmail());
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());

        String otp = generateOtp();
        user.setVerificationCode(otp);
        user.setVerificationCodeExpiryTime(LocalDateTime.now().plusMinutes(10)); // OTP expires in 10 minutes
        user.setActive(false); // User is inactive until OTP verification

        // Assign default role (ROLE_USER)
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role USER is not found."));
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        logger.info("User registered: {}. OTP sent to email.", savedUser.getUsername());

        try {
            mailService.sendOtpEmail(user.getEmail(), otp);
            logger.info("OTP email sent to {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send OTP email to {}: {}", user.getEmail(), e.getMessage());
            // Decide if registration should fail or proceed with a warning.
            // For now, we'll let it proceed but log the error.
            // throw new RuntimeException("User registered, but failed to send OTP email. Please contact support.");
        }
        return savedUser;
    }

    @Override
    @Transactional
    public void verifyUser(String otp) {
        User user = userRepository.findByVerificationCode(otp)
                .orElseThrow(() -> {
                    logger.warn("OTP verification attempt with invalid OTP: {}", otp);
                    return new RuntimeException("Invalid OTP provided.");
                });

        if (user.getVerificationCodeExpiryTime().isBefore(LocalDateTime.now())) {
            logger.warn("OTP verification attempt with expired OTP for user: {}", user.getUsername());
            // Optionally, clear the expired OTP
            user.setVerificationCode(null);
            user.setVerificationCodeExpiryTime(null);
            userRepository.save(user);
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        user.setActive(true);
        user.setVerificationCode(null); // Clear OTP after successful verification
        user.setVerificationCodeExpiryTime(null);
        userRepository.save(user);
        logger.info("User {} verified successfully.", user.getUsername());
    }

    @Override
    public void authenticateUser(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // Optional: Update last login time
            userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
                if(!user.isActive()){
                    logger.warn("Login attempt by inactive user: {}", request.getUsername());
                    throw new BadCredentialsException("User account is not active. Please verify your email.");
                }
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                logger.info("User {} authenticated successfully.", request.getUsername());
            });

        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed for user {}: Invalid credentials", request.getUsername());
            throw new BadCredentialsException("Invalid username or password", e);
        } catch (Exception e) {
            logger.error("Authentication error for user {}: {}", request.getUsername(), e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateOtp() {
        Random random = new Random();
        int otpValue = 100000 + random.nextInt(900000); // Generates 6-digit OTP
        return String.valueOf(otpValue);
    }
}