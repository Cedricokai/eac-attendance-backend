package com.example.eac_backend_project.repository.files;

import com.example.eac_backend_project.entity.files.CompanyFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyFileRepository extends JpaRepository<CompanyFile, Long> {

    // For admins: retrieve all active root files (excluding deleted)
    @Query("SELECT f FROM CompanyFile f WHERE f.originalFileId IS NULL AND (f.isDeleted IS NULL OR f.isDeleted = false) ORDER BY f.createdAt DESC")
    List<CompanyFile> findAllRootFiles();

    // For recycle bin: retrieve all deleted root files
    @Query("SELECT f FROM CompanyFile f WHERE f.originalFileId IS NULL AND f.isDeleted = true ORDER BY f.deletedAt DESC")
    List<CompanyFile> findDeletedRootFiles();

    // Find all versions of a file (the root file + all its revisions)
    @Query("SELECT f FROM CompanyFile f WHERE f.id = :fileId OR f.originalFileId = :fileId ORDER BY f.version DESC")
    List<CompanyFile> findVersionHistory(@Param("fileId") Long fileId);

    // Find latest version of a file
    @Query("SELECT f FROM CompanyFile f WHERE (f.id = :fileId OR f.originalFileId = :fileId) AND (f.isDeleted IS NULL OR f.isDeleted = false) ORDER BY f.version DESC")
    List<CompanyFile> findRevisionsDescending(@Param("fileId") Long fileId);

    // Find active files uploaded by specific user
    @Query("SELECT f FROM CompanyFile f WHERE f.uploadedBy = :uploadedBy AND f.originalFileId IS NULL AND (f.isDeleted IS NULL OR f.isDeleted = false) ORDER BY f.createdAt DESC")
    List<CompanyFile> findByUploadedByAndOriginalFileIdIsNullOrderByCreatedAtDesc(@Param("uploadedBy") String uploadedBy);

    // Find files by ID list (excluding deleted)
    @Query("SELECT f FROM CompanyFile f WHERE f.id IN :ids AND (f.isDeleted IS NULL OR f.isDeleted = false)")
    List<CompanyFile> findByIdIn(@Param("ids") List<Long> ids);

    // Find child revisions by root original file id
    List<CompanyFile> findByOriginalFileId(Long originalFileId);
}
