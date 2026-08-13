package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.SystemRole;
import com.example.eac_backend_project.entity.inventory.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserName(String userName);

    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") SystemRole systemRole);

    Optional<User> findFirstByOrderByIdAsc();

    List<User> findByEnabledTrue();

    Optional<User> findFirstByEmployee_IdAndEnabledTrue(Long employeeId);

    List<User> findByRoleAndEnabledTrue(SystemRole systemRole);

    boolean existsByUserName(String userName);

    boolean existsByEmail(String email);

    List<User> findByEmployee_Id(Long employeeId);

    List<User> findByRole_NameAndEnabledTrue(String roleName);

    @Query("""
        SELECT u
        FROM User u
        JOIN FETCH u.role r
        WHERE UPPER(r.name) = UPPER(:roleName)
          AND u.enabled = true
        ORDER BY u.name
        """)
    List<User> findByRoleNameAndEnabled(
            @Param("roleName") String roleName
    );

    @Query("""
        SELECT u
        FROM User u
        JOIN FETCH u.role r
        WHERE UPPER(r.name) = UPPER(:roleName)
          AND u.enabled = true
        ORDER BY u.name
        """)
    List<User> findByRoleName(
            @Param("roleName") String roleName
    );

    @Query("""
        SELECT u
        FROM User u
        JOIN FETCH u.role r
        WHERE UPPER(r.name) IN :roleNames
          AND u.enabled = true
        ORDER BY u.name
        """)
    List<User> findByRoleNames(
            @Param("roleNames") List<String> roleNames
    );

    long countByRole(Role role);

    Optional<User> findByPasswordResetToken(String token);
}
