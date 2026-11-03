package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.CompanyFileService;
import com.example.eac_backend_project.dto.files.*;
import com.example.eac_backend_project.entity.files.CompanyFile;
import com.example.eac_backend_project.entity.files.FileAuditTrail;
import com.example.eac_backend_project.entity.files.FileSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file-manager")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileManagerController {

    private final CompanyFileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            Authentication auth
    ) {
        try {
            CompanyFileDTO uploaded = fileService.uploadFile(file, description, auth);
            return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<CompanyFileDTO>> listFiles(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            Authentication auth
    ) {
        return ResponseEntity.ok(fileService.getFiles(auth, search, category));
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            Authentication auth
    ) {
        try {
            CompanyFile file = fileService.getAuthorizedFile(id, auth);
            Resource resource = fileService.downloadFile(id, auth);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/files/{id}/preview")
    public ResponseEntity<Resource> previewFile(
            @PathVariable Long id,
            Authentication auth
    ) {
        try {
            CompanyFile file = fileService.getAuthorizedFile(id, auth);
            Resource resource = fileService.downloadFile(id, auth);
            String contentType = file.getFileType();
            if (contentType == null) {
                try {
                    contentType = Files.probeContentType(Paths.get(file.getFilePath()));
                } catch (Exception ignored) {}
            }
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/files/{id}/content")
    public ResponseEntity<?> getFileTextContent(
            @PathVariable Long id,
            Authentication auth
    ) {
        try {
            String content = fileService.getFileTextContent(id, auth);
            return ResponseEntity.ok(Map.of("content", content));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/files/{id}/edit")
    public ResponseEntity<?> editInBrowser(
            @PathVariable Long id,
            @RequestBody FileEditRequest request,
            Authentication auth
    ) {
        try {
            CompanyFileDTO saved = fileService.saveInBrowserEdit(id, request, auth);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/files/{id}/share")
    public ResponseEntity<?> shareFile(
            @PathVariable Long id,
            @RequestBody FileShareRequest request,
            Authentication auth
    ) {
        try {
            fileService.shareFile(id, request, auth);
            return ResponseEntity.ok(Map.of("message", "File shared successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/files/{id}/history")
    public ResponseEntity<List<CompanyFileDTO>> getVersionHistory(
            @PathVariable Long id,
            Authentication auth
    ) {
        return ResponseEntity.ok(fileService.getVersionHistory(id, auth));
    }

    @GetMapping("/files/{id}/audit")
    public ResponseEntity<List<FileAuditTrail>> getAuditTrail(
            @PathVariable Long id,
            Authentication auth
    ) {
        return ResponseEntity.ok(fileService.getAuditTrail(id, auth));
    }

    @GetMapping("/settings")
    public ResponseEntity<FileSettingDTO> getSettings() {
        FileSetting setting = fileService.getSettings();
        return ResponseEntity.ok(FileSettingDTO.builder()
                .maxUploadSizeMb(setting.getMaxUploadSizeMb())
                .allowedExtensions(setting.getAllowedExtensions())
                .updatedBy(setting.getUpdatedBy())
                .build());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateSettings(
            @RequestBody FileSettingDTO dto,
            Authentication auth
    ) {
        try {
            FileSettingDTO updated = fileService.updateSettings(dto, auth);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/files/{id}/revision", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadRevision(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "editNote", required = false) String editNote,
            Authentication auth
    ) {
        try {
            CompanyFileDTO saved = fileService.uploadRevision(id, file, editNote, auth);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/files/{id}")
    public ResponseEntity<?> deleteFile(
            @PathVariable Long id,
            Authentication auth
    ) {
        try {
            fileService.deleteFile(id, auth);
            return ResponseEntity.ok(Map.of("message", "File moved to Recycle Bin successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // RECYCLE BIN ENDPOINTS (ADMIN ONLY)
    // ==========================================

    @GetMapping("/recycle-bin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getRecycleBinFiles(Authentication auth) {
        try {
            List<CompanyFileDTO> deleted = fileService.getDeletedFiles(auth);
            return ResponseEntity.ok(deleted);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/recycle-bin/{id}/restore")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> restoreFile(
            @PathVariable Long id,
            Authentication auth
    ) {
        try {
            CompanyFileDTO restored = fileService.restoreFile(id, auth);
            return ResponseEntity.ok(restored);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/recycle-bin/{id}/purge")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> purgeFile(
            @PathVariable Long id,
            Authentication auth
    ) {
        try {
            fileService.purgeFile(id, auth);
            return ResponseEntity.ok(Map.of("message", "File permanently deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/recycle-bin/empty")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> emptyRecycleBin(Authentication auth) {
        try {
            fileService.emptyRecycleBin(auth);
            return ResponseEntity.ok(Map.of("message", "Recycle Bin emptied successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
