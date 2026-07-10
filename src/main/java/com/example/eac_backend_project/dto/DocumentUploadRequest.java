package com.example.eac_backend_project.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class DocumentUploadRequest {
    private String title;
    private String documentType;
    private String description;
    private String tags;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private MultipartFile file;
}