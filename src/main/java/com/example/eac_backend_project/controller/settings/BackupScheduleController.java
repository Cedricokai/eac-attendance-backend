package com.example.eac_backend_project.controller.settings;

import com.example.eac_backend_project.Service.BackupScheduleService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/settings/database-backups")
public class BackupScheduleController {
    private final BackupScheduleService service;
    public BackupScheduleController(BackupScheduleService service) { this.service = service; }
    private void admin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
            throw new AccessDeniedException("Administrator access is required");
    }
    @GetMapping("/schedule") public BackupScheduleService.Status status(Authentication auth) { admin(auth); return service.status(); }
    @PutMapping("/schedule") public BackupScheduleService.Status save(Authentication auth, @RequestBody BackupScheduleService.Settings settings) throws IOException { admin(auth); return service.save(settings); }
    @PostMapping("/server") public ResponseEntity<BackupScheduleService.Status> start(Authentication auth) throws IOException { admin(auth); return ResponseEntity.accepted().body(service.start()); }
    @ExceptionHandler(AccessDeniedException.class) public ResponseEntity<?> forbidden() { return ResponseEntity.status(403).body(Map.of("message", "Administrator access is required")); }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> invalid(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason())); }
    @ExceptionHandler(IOException.class) public ResponseEntity<?> storage() { return ResponseEntity.status(500).body(Map.of("message", "Cannot access backup storage. Check the configured mount, permissions and free space.")); }
}
