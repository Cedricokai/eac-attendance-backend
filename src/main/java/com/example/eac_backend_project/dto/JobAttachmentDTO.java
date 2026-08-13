package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.attendance.JobAttachment;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class JobAttachmentDTO {
    private Long id;
    private Long jobId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadedAt;
    private LocalDate documentDate;
    private String description;

    public JobAttachmentDTO() {}

    public JobAttachmentDTO(JobAttachment attachment) {
        this.id = attachment.getId();
        this.jobId = attachment.getJobId(); // Use the helper method
        this.fileName = attachment.getFileName();
        this.filePath = attachment.getFilePath();
        this.fileSize = attachment.getFileSize();
        this.fileType = attachment.getFileType();
        this.uploadedAt = attachment.getUploadedAt();
        this.documentDate = attachment.getDocumentDate();
        this.description = attachment.getDescription();
    }

    // Alternative constructor
    public JobAttachmentDTO(Long id, Long jobId, String fileName, String filePath,
                            Long fileSize, String fileType, LocalDateTime uploadedAt, String description) {
        this.id = id;
        this.jobId = jobId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.uploadedAt = uploadedAt;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
