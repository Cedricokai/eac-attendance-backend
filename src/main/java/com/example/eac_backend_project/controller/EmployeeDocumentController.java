package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.EmployeeDocumentService;
import com.example.eac_backend_project.dto.DocumentUploadRequest;
import com.example.eac_backend_project.dto.EmployeeDocumentDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee/{employeeId}/documents")
public class EmployeeDocumentController {

    @Autowired
    private EmployeeDocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long employeeId,
            @ModelAttribute @Valid DocumentUploadRequest request,
            Principal principal) {

        try {
            String uploadedBy = principal != null ? principal.getName() : "System";
            EmployeeDocumentDTO document = documentService.uploadDocument(employeeId, request, uploadedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document uploaded successfully");
            response.put("document", document);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload document: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getDocuments(@PathVariable Long employeeId) {
        try {
            List<EmployeeDocumentDTO> documents = documentService.getEmployeeDocuments(employeeId);
            return ResponseEntity.ok(documents);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocument(
            @PathVariable Long employeeId,
            @PathVariable Long documentId) {
        try {
            EmployeeDocumentDTO document = documentService.getDocument(employeeId, documentId);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<?> downloadDocument(
            @PathVariable Long employeeId,
            @PathVariable Long documentId) {
        try {
            byte[] fileContent = documentService.downloadDocument(employeeId, documentId);

            // Get document info to set filename
            EmployeeDocumentDTO document = documentService.getDocument(employeeId, documentId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", document.getOriginalFileName());
            headers.setContentLength(fileContent.length);

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to download document: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long employeeId,
            @PathVariable Long documentId) {
        try {
            documentService.deleteDocument(employeeId, documentId);
            return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete document: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDocuments(
            @PathVariable Long employeeId,
            @RequestParam String query) {
        try {
            List<EmployeeDocumentDTO> documents = documentService.searchDocuments(employeeId, query);
            return ResponseEntity.ok(documents);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/type/{documentType}")
    public ResponseEntity<?> getDocumentsByType(
            @PathVariable Long employeeId,
            @PathVariable String documentType) {
        try {
            List<EmployeeDocumentDTO> documents = documentService.getDocumentsByType(employeeId, documentType);
            return ResponseEntity.ok(documents);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> getDocumentCount(@PathVariable Long employeeId) {
        try {
            long count = documentService.getDocumentCount(employeeId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAllDocuments(@PathVariable Long employeeId) {
        try {
            documentService.deleteAllEmployeeDocuments(employeeId);
            return ResponseEntity.ok(Map.of("message", "All documents deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete documents: " + e.getMessage()));
        }
    }

    // Simple upload endpoint for direct file upload
    @PostMapping("/simple-upload")
    public ResponseEntity<?> simpleUpload(
            @PathVariable Long employeeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "description", required = false) String description,
            Principal principal) {

        try {
            DocumentUploadRequest request = new DocumentUploadRequest();
            request.setTitle(title);
            request.setDocumentType(documentType);
            request.setDescription(description);
            request.setFile(file);

            String uploadedBy = principal != null ? principal.getName() : "System";
            EmployeeDocumentDTO document = documentService.uploadDocument(employeeId, request, uploadedBy);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Document uploaded successfully",
                    "document", document
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}