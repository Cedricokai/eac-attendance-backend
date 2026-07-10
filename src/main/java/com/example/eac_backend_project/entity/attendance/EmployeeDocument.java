package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "document_type")
    private String documentType = "GENERAL";

    @Column(length = 1000)
    private String description;

    @Column(name = "tags")
    private String tags;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @PrePersist
    protected void onCreate() {
        this.uploadDate = LocalDateTime.now();
        if (this.issueDate == null) {
            this.issueDate = LocalDate.now();
        }
    }

    // Helper method to get file extension
    public String getFileExtension() {
        if (fileName == null) return "";
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) return "";
        return fileName.substring(lastIndex + 1).toLowerCase();
    }

    // Helper method to check if document is expired
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }
}