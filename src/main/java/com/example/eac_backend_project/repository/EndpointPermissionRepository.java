package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.EndpointPermission;
import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EndpointPermissionRepository extends JpaRepository<EndpointPermission, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from EndpointPermission p where p.id=:id")
    java.util.Optional<EndpointPermission> lock(@org.springframework.data.repository.query.Param("id") Long id);

    @Query("""
    SELECT DISTINCT ep
    FROM EndpointPermission ep
    LEFT JOIN FETCH ep.roles
    LEFT JOIN FETCH ep.users
    WHERE ep.enabled = true
""")
    List<EndpointPermission> findAllActiveWithRolesAndUsers();

}

