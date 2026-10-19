package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.entity.inventory.InventoryDocument;
import com.example.eac_backend_project.repository.InventoryDocumentRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@RestController
@RequestMapping("/api/inventory/documents")
public class InventoryDocumentController {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv", "image/jpeg", "image/png", "application/octet-stream"
    );
    private final InventoryDocumentRepository repository;

    public InventoryDocumentController(InventoryDocumentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return repository.findAllByOrderByUploadedAtDesc().stream().map(this::summary).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam String name,
            @RequestParam String location,
            @RequestParam String type,
            Authentication authentication) throws IOException {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Document name is required");
        if (location == null || location.isBlank()) throw new IllegalArgumentException("Document location is required");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("A file is required");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File must not exceed 10 MB");
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) throw new IllegalArgumentException("Unsupported file type");

        InventoryDocument document = new InventoryDocument();
        document.setName(name.trim());
        document.setLocation(location.trim());
        document.setType(type == null || type.isBlank() ? "Other" : type.trim());
        document.setFileName(safeFileName(file.getOriginalFilename()));
        document.setContentType(contentType);
        document.setSize(file.getSize());
        document.setUploadedBy(authentication == null ? null : authentication.getName());
        document.setFileData(file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(summary(repository.save(document)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        InventoryDocument document = find(id);
        String fileName = safeFileName(document.getFileName()).replace("\"", "");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .contentLength(document.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(document.getFileData());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        InventoryDocument document = find(id);
        repository.delete(document);
        return ResponseEntity.noContent().build();
    }

    private InventoryDocument find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Inventory document not found"));
    }

    private Map<String, Object> summary(InventoryDocument d) {
        return Map.ofEntries(
                Map.entry("id", d.getId()), Map.entry("name", d.getName()),
                Map.entry("location", d.getLocation()), Map.entry("type", d.getType()),
                Map.entry("fileName", d.getFileName()), Map.entry("mimeType", d.getContentType()),
                Map.entry("size", d.getSize()), Map.entry("uploadedAt", d.getUploadedAt()),
                Map.entry("uploadedBy", d.getUploadedBy() == null ? "" : d.getUploadedBy())
        );
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) return "document";
        return value.replace("\\", "_").replace("/", "_").replace("\r", "").replace("\n", "");
    }
}
