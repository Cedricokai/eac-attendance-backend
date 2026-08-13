package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.DirectPurchaseService;
import com.example.eac_backend_project.dto.DirectPurchaseApproverDTO;
import com.example.eac_backend_project.dto.DirectPurchaseRequestDTO;
import com.example.eac_backend_project.dto.DirectPurchaseResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/direct-purchases")
public class DirectPurchaseController {

    private final DirectPurchaseService service;

    public DirectPurchaseController(DirectPurchaseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DirectPurchaseResponseDTO> createRequest(
            @Valid @RequestBody DirectPurchaseRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createRequest(dto));
    }

    @GetMapping
    public ResponseEntity<List<DirectPurchaseResponseDTO>>
    getAllRequests() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    @GetMapping("/approvers/{role}")
    public ResponseEntity<List<DirectPurchaseApproverDTO>>
    getApprovers(@PathVariable String role) {
        return ResponseEntity.ok(service.getApprovers(role));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<DirectPurchaseResponseDTO>
    getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRequestById(id));
    }

    @PutMapping("/{id:\\d+}/approve")
    public ResponseEntity<DirectPurchaseResponseDTO> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false)
            Map<String, String> payload
    ) {
        String notes =
                payload == null ? null : payload.get("notes");

        return ResponseEntity.ok(
                service.approveRequest(id, notes)
        );
    }

    @PutMapping("/{id:\\d+}/reviewer-approve")
    public ResponseEntity<DirectPurchaseResponseDTO> reviewerApprove(
            @PathVariable Long id,
            @RequestBody(required = false)
            Map<String, String> payload
    ) {
        String notes =
                payload == null ? null : payload.get("notes");

        return ResponseEntity.ok(
                service.reviewerApprove(id, notes)
        );
    }

    @PutMapping("/{id:\\d+}/reject")
    public ResponseEntity<DirectPurchaseResponseDTO> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false)
            Map<String, String> payload
    ) {
        String notes =
                payload == null ? null : payload.get("notes");

        return ResponseEntity.ok(
                service.rejectRequest(id, notes)
        );
    }

    @PutMapping("/{id:\\d+}/pay")
    public ResponseEntity<DirectPurchaseResponseDTO> payRequest(
            @PathVariable Long id,
            @RequestBody(required = false)
            Map<String, String> payload
    ) {
        String notes =
                payload == null ? null : payload.get("notes");

        return ResponseEntity.ok(
                service.payRequest(id, notes)
        );
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteRequest(
            @PathVariable Long id
    ) {
        service.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }
}
