package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.reports.IndividualReportScheduleService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/reports/attendance/individual-schedule")
public class IndividualReportScheduleController {
    private final IndividualReportScheduleService service;
    public IndividualReportScheduleController(IndividualReportScheduleService service) { this.service = service; }
    private void admin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())))
            throw new AccessDeniedException("Administrator access is required");
    }
    @GetMapping public IndividualReportScheduleService.Status get(Authentication auth) { admin(auth); return service.status(); }
    @PutMapping public IndividualReportScheduleService.Status save(Authentication auth, @RequestBody IndividualReportScheduleService.Settings settings) { admin(auth); return service.save(settings); }
    @ExceptionHandler(AccessDeniedException.class) public ResponseEntity<?> forbidden() { return ResponseEntity.status(403).body(Map.of("message", "Administrator access is required")); }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> invalid(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason())); }
}
