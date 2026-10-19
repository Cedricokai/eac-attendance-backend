package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.PortalRequestService;
import com.example.eac_backend_project.dto.AccountAccessRequestDTO;
import com.example.eac_backend_project.dto.SupportRequestDTO;
import com.example.eac_backend_project.entity.portal.AccountAccessRequest;
import com.example.eac_backend_project.entity.portal.SupportRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class PortalRequestController {
    private final PortalRequestService service;
    public PortalRequestController(PortalRequestService service) { this.service = service; }

    @PostMapping("/account-requests")
    public ResponseEntity<?> submitAccountRequest(@Valid @RequestBody AccountAccessRequestDTO dto) {
        AccountAccessRequest saved = service.submitAccountRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Account request submitted for approval", "id", saved.getId(), "reference", saved.getReference(), "status", saved.getStatus()));
    }

    @PostMapping("/support-requests")
    public ResponseEntity<?> submitSupportRequest(@Valid @RequestBody SupportRequestDTO dto) {
        SupportRequest saved = service.submitSupportRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Support request submitted", "id", saved.getId(), "reference", saved.getReference(), "status", saved.getStatus()));
    }

    @GetMapping("/admin/account-requests")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<AccountAccessRequest> accountRequests() { return service.getAccountRequests(); }

    @PatchMapping("/admin/account-requests/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AccountAccessRequest reviewAccountRequest(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication authentication) {
        return service.reviewAccountRequest(id, body.get("status"), body.get("notes"), authentication.getName());
    }

    @GetMapping("/admin/support-requests")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<SupportRequest> supportRequests() { return service.getSupportRequests(); }

    @PatchMapping("/admin/support-requests/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public SupportRequest updateSupportRequest(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.updateSupportRequest(id, body.get("status"), body.get("notes"), body.get("assignedTo"));
    }
}
