// com.ltdd.streamapp.gdrive.repository.UserRepository.java
package com.ltdd.streamapp.gdrive.repository;

import com.ltdd.streamapp.gdrive.model.User; // Sửa import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email); // Giữ lại nếu cần thiết cho logic khác
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Optional<User> findByVerificationCode(String verificationCode);
}