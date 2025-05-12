// com.ltdd.streamapp.gdrive.repository.RoleRepository.java
package com.ltdd.streamapp.gdrive.repository;

import com.ltdd.streamapp.gdrive.model.ERole; // Sửa import
import com.ltdd.streamapp.gdrive.model.Role;  // Sửa import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
}