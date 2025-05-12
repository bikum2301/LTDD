// File: src/main/java/com/ltdd/streamapp/gdrive/config/DataInitializer.java
package com.ltdd.streamapp.gdrive.config; // Hoặc package bạn muốn

import com.ltdd.streamapp.gdrive.model.ERole;
import com.ltdd.streamapp.gdrive.model.Role;
import com.ltdd.streamapp.gdrive.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Quan trọng

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional // Đảm bảo các thao tác DB nằm trong một transaction
    public void run(String... args) throws Exception {
        logger.info("Checking and initializing roles...");

        // Kiểm tra và thêm ROLE_USER
        if (roleRepository.findByName(ERole.ROLE_USER).isEmpty()) {
            Role userRole = new Role();
            userRole.setName(ERole.ROLE_USER);
            roleRepository.save(userRole);
            logger.info("ROLE_USER initialized.");
        } else {
            logger.info("ROLE_USER already exists.");
        }

        // Kiểm tra và thêm ROLE_ADMIN
        if (roleRepository.findByName(ERole.ROLE_ADMIN).isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName(ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);
            logger.info("ROLE_ADMIN initialized.");
        } else {
            logger.info("ROLE_ADMIN already exists.");
        }

        // Kiểm tra và thêm ROLE_MODERATOR (nếu bạn dùng)
        if (roleRepository.findByName(ERole.ROLE_MODERATOR).isEmpty()) {
            Role modRole = new Role();
            modRole.setName(ERole.ROLE_MODERATOR);
            roleRepository.save(modRole);
            logger.info("ROLE_MODERATOR initialized.");
        } else {
            logger.info("ROLE_MODERATOR already exists.");
        }

        logger.info("Role initialization complete.");
    }
}