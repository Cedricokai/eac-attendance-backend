package com.example.eac_backend_project.repository;



import java.util.Optional;

import com.example.eac_backend_project.entity.inventory.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserName(String userName);
}