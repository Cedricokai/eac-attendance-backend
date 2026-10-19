package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.EmployeeIssuedItemService;
import com.example.eac_backend_project.Service.PageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/employee-issued-items")
public class EmployeeIssuedItemController {

    private final EmployeeIssuedItemService service;
    private final PageService pages;

    public EmployeeIssuedItemController(
            EmployeeIssuedItemService service,
            PageService pages
    ) {
        this.service = service;
        this.pages = pages;
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities()
                .stream()
                .anyMatch(a ->
                        "ROLE_ADMIN".equals(a.getAuthority())
                );
    }

    private void access(Authentication auth) {
        if (isAdmin(auth)) {
            return;
        }

        if (auth == null
                || !auth.isAuthenticated()
                || pages.getAccessiblePages(auth)
                .stream()
                .noneMatch(p ->
                        "/employee-issued-items".equals(p.getPath())
                                || "Employee Issued Items"
                                .equalsIgnoreCase(p.getName())
                )) {
            throw new AccessDeniedException(
                    "Employee Issued Items access is required."
            );
        }
    }

    @GetMapping
    public Object list(Authentication auth) {
        access(auth);
        return service.list();
    }

    @GetMapping("/employees")
    public Object employees(Authentication auth) {
        access(auth);
        return service.employees();
    }

    @GetMapping("/{id}/history")
    public Object history(
            Authentication auth,
            @PathVariable Long id
    ) {
        access(auth);
        return service.history(id);
    }

    /**
     * Existing single-item endpoint.
     */
    @PostMapping
    public Object issue(
            Authentication auth,
            @RequestBody EmployeeIssuedItemService.Issue request
    ) {
        access(auth);
        return service.issue(request, auth.getName());
    }

    /**
     * New transactional batch endpoint.
     *
     * Request:
     * {
     *   "items": [
     *      { ...issue 1... },
     *      { ...issue 2... }
     *   ]
     * }
     */
    @PostMapping("/batch")
    public Object issueBatch(
            Authentication auth,
            @RequestBody EmployeeIssuedItemService.BatchIssue request
    ) {
        access(auth);
        return service.issueBatch(request, auth.getName());
    }

    @PostMapping("/{id}/events")
    public Object change(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody EmployeeIssuedItemService.Change request
    ) {
        access(auth);
        return service.change(id, request, auth.getName());
    }

    @GetMapping("/policies")
    public Object policies(Authentication auth) {
        access(auth);
        return service.policies();
    }

    @PostMapping("/policies")
    public Object createPolicy(
            Authentication auth,
            @RequestBody EmployeeIssuedItemService.Policy input
    ) {
        if (!isAdmin(auth)) {
            throw new AccessDeniedException(
                    "Only administrators can change PPE replacement rules."
            );
        }

        return service.savePolicy(null, input);
    }

    @PutMapping("/policies/{id}")
    public Object updatePolicy(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody EmployeeIssuedItemService.Policy input
    ) {
        if (!isAdmin(auth)) {
            throw new AccessDeniedException(
                    "Only administrators can change PPE replacement rules."
            );
        }

        return service.savePolicy(id, input);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> forbidden(AccessDeniedException e) {
        return ResponseEntity
                .status(403)
                .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> invalid(ResponseStatusException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(Map.of("message", e.getReason()));
    }
}
