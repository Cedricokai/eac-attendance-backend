package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.FileStorageService;
import com.example.eac_backend_project.entity.attendance.JobAttachment;
import com.example.eac_backend_project.repository.JobAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileDownloadController {

    @Autowired
    private  JobAttachmentRepository attachmentRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long attachmentId) {
        try {
            Optional<JobAttachment> attachmentOpt = attachmentRepository.findById(attachmentId);
            if (attachmentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            JobAttachment attachment = attachmentOpt.get();
            Path filePath = Paths.get(attachment.getFilePath());

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/preview/{attachmentId}")
    public ResponseEntity<Resource> previewFile(@PathVariable Long attachmentId) {
        try {
            Optional<JobAttachment> attachmentOpt = attachmentRepository.findById(attachmentId);
            if (attachmentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            JobAttachment attachment = attachmentOpt.get();
            Path filePath = Paths.get(attachment.getFilePath());

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + attachment.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}