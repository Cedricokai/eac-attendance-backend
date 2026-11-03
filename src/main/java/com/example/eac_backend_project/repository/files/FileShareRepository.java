package com.example.eac_backend_project.repository.files;

import com.example.eac_backend_project.entity.files.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, Long> {

    List<FileShare> findByFileId(Long fileId);

    List<FileShare> findBySharedWithUsername(String username);

    List<FileShare> findBySharedWithEmail(String email);

    boolean existsByFileIdAndSharedWithUsername(Long fileId, String username);

    boolean existsByFileIdAndSharedWithEmail(Long fileId, String email);

    void deleteByFileId(Long fileId);
}
