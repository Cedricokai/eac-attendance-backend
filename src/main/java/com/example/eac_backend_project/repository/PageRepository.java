package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.inventory.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    Optional<Page> findByPath(String path);
    Optional<Page> findByName(String name);

    @Query("SELECT p FROM Page p WHERE p.isPublic = true OR EXISTS " +
            "(SELECT r FROM p.roles r WHERE r.name IN :roleNames) " +
            "ORDER BY p.displayOrder, p.name")
    List<Page> findAccessiblePages(@Param("roleNames") List<String> roleNames);

    @Query("SELECT DISTINCT p FROM Page p LEFT JOIN FETCH p.roles")
    List<Page> findAllWithRoles();

    // NEW: Fetch page with users
    @Query("SELECT DISTINCT p FROM Page p LEFT JOIN FETCH p.roles LEFT JOIN FETCH p.users WHERE p.id = :pageId")
    Optional<Page> findByIdWithUsers(@Param("pageId") Long pageId);

    // Custom query methods
    long countByIsPublic(Boolean isPublic);
    long countByIsActive(Boolean isActive);

    @Query("SELECT COUNT(p) FROM Page p WHERE SIZE(p.roles) > 0")
    long countPagesWithRoles();

    // NEW: Count pages with user assignments
    @Query("SELECT COUNT(p) FROM Page p WHERE SIZE(p.users) > 0")
    long countPagesWithUsers();

    @Query("SELECT p FROM Page p WHERE p.isActive = true AND (p.isPublic = true OR EXISTS " +
            "(SELECT r FROM p.roles r WHERE r.name IN :roleNames)) " +
            "ORDER BY p.displayOrder, p.name")
    List<Page> findActiveAccessiblePages(@Param("roleNames") List<String> roleNames);

    @Query("SELECT DISTINCT p FROM Page p LEFT JOIN FETCH p.roles WHERE p.isActive = true")
    List<Page> findAllActiveWithRoles();

    List<Page> findByModuleOrderByDisplayOrderAsc(Page.Module module);
}