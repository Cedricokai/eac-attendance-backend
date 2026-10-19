package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.dto.LeaveListDTO;
import com.example.eac_backend_project.repository.LeaveRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/leave")
public class LeaveListController {
    private final LeaveRepository leaves;
    private final EmployeeRepository employees;
    private final UserRepository users;
    public LeaveListController(LeaveRepository leaves, EmployeeRepository employees, UserRepository users) {
        this.leaves = leaves;
        this.employees = employees;
        this.users = users;
    }

    @GetMapping("/list")
    @Transactional(readOnly = true)
    public ResponseEntity<List<LeaveListDTO>> list(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) return ResponseEntity.status(401).build();
        var allowed = Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_HUMAN_RESOURCES");
        boolean all = authentication.getAuthorities().stream().anyMatch(a -> allowed.contains(a.getAuthority()));
        if (all) return ResponseEntity.ok(leaves.findListRows(null));
        var user = users.findByUserName(authentication.getName()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return ResponseEntity.ok(List.of());
        var employee = employees.findByEmail(user.getEmail()).orElse(null);
        // A missing employee mapping must never fall back to the full register.
        if (employee == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(leaves.findListRows(employee.getId()));
    }
}
