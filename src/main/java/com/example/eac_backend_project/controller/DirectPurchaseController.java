package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.DirectPurchaseService;
import com.example.eac_backend_project.dto.DirectPurchaseRequestDTO;
import com.example.eac_backend_project.dto.DirectPurchaseResponseDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/direct-purchases")
public class DirectPurchaseController {

    @Autowired
    private DirectPurchaseService service;

    // Create new request
    @PostMapping
    public ResponseEntity<DirectPurchaseResponseDTO> createRequest(@Valid @RequestBody DirectPurchaseRequestDTO dto) {
        DirectPurchaseResponseDTO created = service.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Get all requests
    @GetMapping
    public ResponseEntity<List<DirectPurchaseResponseDTO>> getAllRequests() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    // Get request by ID
    @GetMapping("/{id}")
    public ResponseEntity<DirectPurchaseResponseDTO> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRequestById(id));
    }

    // Approve by accountant
    @PutMapping("/{id}/approve")
    public ResponseEntity<DirectPurchaseResponseDTO> approveRequest(@PathVariable Long id,
                                                                    @RequestBody(required = false) Map<String, String> payload) {
        String notes = payload != null ? payload.get("notes") : null;
        return ResponseEntity.ok(service.approveRequest(id, notes));
    }

    // Reject by accountant
    @PutMapping("/{id}/reject")
    public ResponseEntity<DirectPurchaseResponseDTO> rejectRequest(@PathVariable Long id,
                                                                   @RequestBody(required = false) Map<String, String> payload) {
        String notes = payload != null ? payload.get("notes") : null;
        return ResponseEntity.ok(service.rejectRequest(id, notes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        service.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }

    // Mark as paid
    @PutMapping("/{id}/pay")
    public ResponseEntity<DirectPurchaseResponseDTO> payRequest(@PathVariable Long id,
                                                                @RequestBody(required = false) Map<String, String> payload) {
        String notes = payload != null ? payload.get("notes") : null;
        return ResponseEntity.ok(service.payRequest(id, notes));
    }

    @PutMapping("/{id}/reviewer-approve")
    public ResponseEntity<DirectPurchaseResponseDTO> reviewerApprove(@PathVariable Long id,
                                                                     @RequestBody(required = false) Map<String, String> payload) {
        String notes = payload != null ? payload.get("notes") : null;
        return ResponseEntity.ok(service.reviewerApprove(id, notes));
    }
}