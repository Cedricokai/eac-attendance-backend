package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {

    List<EmployeeDocument> findByEmployeeIdOrderByUploadDateDesc(Long employeeId);

    List<EmployeeDocument> findByEmployeeIdAndDocumentTypeOrderByUploadDateDesc(
            Long employeeId, String documentType
    );

    @Query("SELECT d FROM EmployeeDocument d WHERE d.employee.id = :employeeId " +
            "AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(d.tags) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY d.uploadDate DESC")
    List<EmployeeDocument> searchByEmployeeIdAndKeyword(
            @Param("employeeId") Long employeeId,
            @Param("searchTerm") String searchTerm
    );

    void deleteByEmployeeId(Long employeeId);

    boolean existsByEmployeeIdAndFileName(Long employeeId, String fileName);

    EmployeeDocument findByEmployeeIdAndId(Long employeeId, Long id);

    @Query("SELECT COUNT(d) FROM EmployeeDocument d WHERE d.employee.id = :employeeId")
    long countByEmployeeId(@Param("employeeId") Long employeeId);
}