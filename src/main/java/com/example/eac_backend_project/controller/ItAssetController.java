package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.ItAssetService;
import com.example.eac_backend_project.dto.ItAssetDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/it-assets")
public class ItAssetController {

    private final ItAssetService itAssetService;

    public ItAssetController(ItAssetService itAssetService) {
        this.itAssetService = itAssetService;
    }

    @GetMapping
    public ResponseEntity<List<ItAssetDTO>> getAllAssets() {
        return ResponseEntity.ok(itAssetService.getAllAssets());
    }

    @GetMapping("/my-assets")
    public ResponseEntity<List<ItAssetDTO>> getMyAssets(Authentication auth) {
        return ResponseEntity.ok(itAssetService.getMyAssets(auth));
    }

    @GetMapping("/public")
    public ResponseEntity<List<ItAssetDTO>> getPublicAssets() {
        return ResponseEntity.ok(itAssetService.getPublicAssets());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ItAssetDTO>> getAssetsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(itAssetService.getAssetsByEmployee(employeeId));
    }

    @PostMapping
    public ResponseEntity<?> createAsset(@RequestBody ItAssetDTO dto, Authentication auth) {
        try {
            String username = auth != null ? auth.getName() : "System";
            return ResponseEntity.ok(itAssetService.createAsset(dto, username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAsset(@PathVariable Long id, @RequestBody ItAssetDTO dto, Authentication auth) {
        try {
            String username = auth != null ? auth.getName() : "System";
            return ResponseEntity.ok(itAssetService.updateAsset(id, dto, username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/mark-returned")
    public ResponseEntity<?> markReturned(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        try {
            LocalDateTime returnedAt = null;
            String conditionNotes = null;
            if (payload != null) {
                String returnedAtStr = payload.get("returnedAt");
                if (returnedAtStr != null && !returnedAtStr.isBlank()) {
                    returnedAt = LocalDateTime.parse(returnedAtStr);
                }
                conditionNotes = payload.get("conditionNotes");
            }
            return ResponseEntity.ok(itAssetService.markReturned(id, returnedAt, conditionNotes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/mark-unreturned")
    public ResponseEntity<?> markUnreturned(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(itAssetService.markUnreturned(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/mark-public")
    public ResponseEntity<?> togglePublic(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> payload
    ) {
        try {
            Boolean isPublic = payload != null ? payload.get("isPublic") : null;
            return ResponseEntity.ok(itAssetService.togglePublic(id, isPublic));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
        try {
            itAssetService.deleteAsset(id);
            return ResponseEntity.ok(Map.of("message", "Asset deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

