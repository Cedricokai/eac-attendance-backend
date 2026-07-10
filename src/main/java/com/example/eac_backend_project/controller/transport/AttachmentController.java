package com.example.eac_backend_project.controller.transport;

import com.example.eac_backend_project.Service.transport.AttachmentService;
import com.example.eac_backend_project.dto.AttachmentDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("vehicleId") Long vehicleId) {
        try {
            // Validate files
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body("No files provided");
            }

            // Check file sizes (max 10MB)
            for (MultipartFile file : files) {
                if (file.getSize() > 10 * 1024 * 1024) { // 10MB in bytes
                    return ResponseEntity.badRequest()
                            .body("File too large: " + file.getOriginalFilename() + ". Maximum size is 10MB.");
                }
            }

            List<AttachmentDTO> attachments = attachmentService.uploadFiles(files, vehicleId);
            return ResponseEntity.ok(attachments);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading files: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<AttachmentDTO>> getVehicleAttachments(@PathVariable Long vehicleId) {
        try {
            List<AttachmentDTO> attachments = attachmentService.getAttachmentsByVehicleId(vehicleId);
            return ResponseEntity.ok(attachments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long attachmentId) {
        try {
            AttachmentDTO attachmentDTO = attachmentService.getAttachment(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Attachment not found"));

            byte[] fileData = attachmentService.getFileData(attachmentId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", attachmentDTO.getFileName());
            headers.setContentLength(fileData.length);

            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error reading file: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/view/{attachmentId}")
    public ResponseEntity<byte[]> viewFile(@PathVariable Long attachmentId) {
        try {
            AttachmentDTO attachmentDTO = attachmentService.getAttachment(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Attachment not found"));

            byte[] fileData = attachmentService.getFileData(attachmentId);

            HttpHeaders headers = new HttpHeaders();
            String contentType = getContentType(attachmentDTO.getFileName());
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(fileData.length);

            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long attachmentId) {
        try {
            boolean deleted = attachmentService.deleteAttachment(attachmentId);
            if (deleted) {
                return ResponseEntity.ok("Attachment deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting attachment: " + e.getMessage());
        }
    }

    // Helper methods
    private String getContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String extension = fileName.toLowerCase();
        if (extension.contains(".")) {
            extension = extension.substring(extension.lastIndexOf(".") + 1);
        }

        switch (extension) {
            case "pdf": return "application/pdf";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            case "svg": return "image/svg+xml";
            case "txt": return "text/plain";
            case "csv": return "text/csv";
            case "json": return "application/json";
            case "xml": return "application/xml";
            case "html": return "text/html";
            case "css": return "text/css";
            case "js": return "application/javascript";
            default: return "application/octet-stream";
        }
    }
}