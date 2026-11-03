package com.example.eac_backend_project.repository.files;

import com.example.eac_backend_project.entity.files.FileAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAuditTrailRepository extends JpaRepository<FileAuditTrail, Long> {

    List<FileAuditTrail> findByFileIdOrderByPerformedAtDesc(Long fileId);

    void deleteByFileId(Long fileId);
}
