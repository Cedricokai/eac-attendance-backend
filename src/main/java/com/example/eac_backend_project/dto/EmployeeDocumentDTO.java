package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.attendance.EmployeeDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDocumentDTO {
    private Long id;
    private String title;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String documentType;
    private String description;
    private String tags;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private LocalDateTime uploadDate;
    private String uploadedBy;
    private String downloadUrl;
    private boolean expired;

    // Constructor from entity
    public EmployeeDocumentDTO(EmployeeDocument entity,
                               String baseUrl, Long employeeId) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.fileName = entity.getFileName();
        this.originalFileName = entity.getOriginalFileName();
        this.fileType = entity.getFileType();
        this.fileSize = entity.getFileSize();
        this.documentType = entity.getDocumentType();
        this.description = entity.getDescription();
        this.tags = entity.getTags();
        this.issueDate = entity.getIssueDate();
        this.expiryDate = entity.getExpiryDate();
        this.uploadDate = entity.getUploadDate();
        this.uploadedBy = entity.getUploadedBy();
        this.expired = entity.isExpired();
        this.downloadUrl = baseUrl + "/api/employee/" + employeeId + "/documents/" + entity.getId() + "/download";
    }
}