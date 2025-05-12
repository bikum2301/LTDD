// com.ltdd.streamapp.gdrive.config.SecurityConfig.java
package com.ltdd.streamapp.gdrive.config;

// Sửa các import cho khớp với package mới
import com.ltdd.streamapp.gdrive.filter.JwtAuthenticationFilter;
import com.ltdd.streamapp.gdrive.service.CustomUserDetailsService;
import com.ltdd.streamapp.gdrive.util.JwtUtils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // Thêm nếu cần
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// import org.springframework.security.config.Customizer; // Nếu dùng .cors(Customizer.withDefaults())

@Configuration
@EnableWebSecurity // Nên thêm @EnableWebSecurity để rõ ràng hơn
@EnableMethodSecurity(prePostEnabled = true) // Cho phép @PreAuthorize, @PostAuthorize
public class SecurityConfig {

    // CustomUserDetailsService đã được inject tự động nếu nó là một @Service
    // private final CustomUserDetailsService customUserDetailsService;
    // private final JwtUtils jwtUtils;

    // public SecurityConfig(CustomUserDetailsService customUserDetailsService, JwtUtils jwtUtils) {
    //     this.customUserDetailsService = customUserDetailsService;
    //     this.jwtUtils = jwtUtils;
    // }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(CustomUserDetailsService customUserDetailsServiceSvc) {
        // Tham số customUserDetailsServiceSvc sẽ được Spring tự động inject
        // nếu CustomUserDetailsService là một @Service bean.
        return customUserDetailsServiceSvc;
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtils jwtUtils, CustomUserDetailsService customUserDetailsService) {
        // Tạo bean JwtAuthenticationFilter ở đây để Spring quản lý
        // và để có thể inject JwtUtils, CustomUserDetailsService
        return new JwtAuthenticationFilter(jwtUtils, customUserDetailsService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
            	    .requestMatchers("/api/auth/**").permitAll()
            	    .requestMatchers("/h2-console/**").permitAll()
            	    .requestMatchers("/api/media/public/**").permitAll() // Cho phép lấy danh sách public
            	    .requestMatchers(HttpMethod.GET, "/api/media/stream/**").permitAll() // << CHO PHÉP STREAMING
            	    .anyRequest().authenticated()
            	)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        return http.build();
    }
}