package com.example.eac_backend_project.repository;

import java.util.List;
import java.util.Optional;

import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.SystemRole;
import com.example.eac_backend_project.entity.inventory.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserName(String userName);

    // Add this method
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") SystemRole systemRole);

    // Add this method - Spring Data JPA will implement it automatically
    Optional<User> findFirstByOrderByIdAsc();

    // You might also want these methods:
    List<User> findByEnabledTrue();

    List<User> findByRoleAndEnabledTrue(SystemRole systemRole);

    boolean existsByUserName(String userName);

    boolean existsByEmail(String email);
    List<User> findByEmployee_Id(Long employeeId);

    List<User> findByRole_NameAndEnabledTrue(String roleName);

    // UserRepository
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.enabled = true")
    List<User> findByRoleNameAndEnabled(@Param("roleName") String roleName);

    // Add this method to find users by role name
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.enabled = true")
    List<User> findByRoleName(@Param("roleName") String roleName);

    // Add this method to find users with specific roles
    @Query("SELECT u FROM User u WHERE u.role.name IN :roleNames AND u.enabled = true")
    List<User> findByRoleNames(@Param("roleNames") List<String> roleNames);

    long countByRole(Role role);

    Optional<User> findByPasswordResetToken(String token);

}