// File: src/main/java/com/ltdd/streamapp/gdrive/service/Impl/CustomUserDetailsService.java
package com.ltdd.streamapp.gdrive.service;

import com.ltdd.streamapp.gdrive.model.User;
import com.ltdd.streamapp.gdrive.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority; // Sửa import nếu cần
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Thêm transactional

import java.util.Collection; // Sửa import nếu cần
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true) // Thêm transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User Not Found with username: " + username));

        if (!user.isActive()) { // Kiểm tra user có active không
            throw new UsernameNotFoundException("User account is not active: " + username);
        }

        Collection<? extends GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(), // accountNonExpired, credentialsNonExpired, accountNonLocked -> true by default
                true,
                true,
                true,
                authorities
        );
    }
}