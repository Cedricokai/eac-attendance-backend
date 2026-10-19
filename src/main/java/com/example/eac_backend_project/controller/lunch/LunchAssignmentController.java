package com.example.eac_backend_project.controller.lunch;

import com.example.eac_backend_project.Service.UserService;
import com.example.eac_backend_project.Service.lunch.LunchAssignmentService;
import com.example.eac_backend_project.dto.lunch.LunchAssignmentDTO;
import com.example.eac_backend_project.dto.lunch.WeeklyAssignmentDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.lunch.LunchAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lunch/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
public class LunchAssignmentController {

    private final LunchAssignmentService assignmentService;
    private final UserService userService;

    @GetMapping("/active")
    public ResponseEntity<List<LunchAssignment>> getActiveAssignments() {
        return ResponseEntity.ok(assignmentService.getAllActiveAssignments());
    }

    @GetMapping("/date")
    public ResponseEntity<List<LunchAssignment>> getAssignmentsForDate(@RequestParam(required = false) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(assignmentService.getAssignmentsForDate(date));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LunchAssignment>> getAssignmentsForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsForEmployee(employeeId));
    }

    @GetMapping("/employee/{employeeId}/active")
    public ResponseEntity<LunchAssignment> getActiveAssignmentForEmployee(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return assignmentService.getActiveAssignmentForEmployee(employeeId, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyAssignmentDTO> getWeeklyAssignment(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        WeeklyAssignmentDTO dto = assignmentService.getWeeklyAssignment(employeeId, startDate);
        return ResponseEntity.ok(dto);
    }

    // ✅ FIXED: Return JSON response instead of Void
    @PostMapping("/weekly")
    public ResponseEntity<Map<String, Object>> createOrUpdateWeeklyAssignment(@RequestBody WeeklyAssignmentDTO dto) {
        User currentUser = userService.getCurrentUser();
        assignmentService.saveWeeklyAssignment(dto, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Weekly assignments saved successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-weekly")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyWeeklyAssignment(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(assignmentService.getMyWeeklyAssignment(userService.getCurrentUser(), startDate));
    }

    // Employee identity and cutoff are enforced by the service.
    @PostMapping("/my-weekly")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateMyWeeklyAssignment(@RequestBody WeeklyAssignmentDTO dto) {
        User currentUser = userService.getCurrentUser();
        assignmentService.saveWeeklyAssignmentForEmployee(dto, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Your weekly meals have been saved successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<LunchAssignment> createAssignment(@RequestBody LunchAssignmentDTO dto) {
        User currentUser = userService.getCurrentUser();

        LunchAssignment assignment = new LunchAssignment();
        Employee employee = new Employee();
        employee.setId(dto.getEmployeeId());
        assignment.setEmployee(employee);
        assignment.setStartDate(dto.getStartDate());
        assignment.setEndDate(dto.getEndDate());
        assignment.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        assignment.setReason(dto.getReason());

        LunchAssignment created = assignmentService.createAssignment(assignment, currentUser);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LunchAssignment> updateAssignment(@PathVariable Long id,
                                                            @RequestBody LunchAssignmentDTO dto) {
        User currentUser = userService.getCurrentUser();
        LunchAssignment assignment = new LunchAssignment();
        assignment.setStartDate(dto.getStartDate());
        assignment.setEndDate(dto.getEndDate());
        assignment.setStatus(dto.getStatus());
        assignment.setReason(dto.getReason());
        LunchAssignment updated = assignmentService.updateAssignment(id, assignment, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        assignmentService.deleteAssignment(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendAssignment(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        assignmentService.suspendAssignment(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateAssignment(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        assignmentService.activateAssignment(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleMealError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }
}
