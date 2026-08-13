package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Exceptions.AttendanceRequestException;
import com.example.eac_backend_project.Service.AttendanceRequestService;
import com.example.eac_backend_project.dto.AttendanceRequestCreateDTO;
import com.example.eac_backend_project.dto.AttendanceRequestBatchCreateDTO;
import com.example.eac_backend_project.dto.AttendanceRequestResponseDTO;
import com.example.eac_backend_project.dto.AttendanceRequestReviewDTO;
import com.example.eac_backend_project.entity.attendance.AttendanceRequestStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/attendance-requests")
public class AttendanceRequestController {

    private final AttendanceRequestService service;

    public AttendanceRequestController(AttendanceRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AttendanceRequestResponseDTO> create(@Valid @RequestBody AttendanceRequestCreateDTO request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> createBatch(
            @Valid @RequestBody AttendanceRequestBatchCreateDTO request
    ) {
        List<AttendanceRequestResponseDTO> results = service.createBatch(request.requests());
        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", results.size() + " attendance request(s) submitted",
                "data", results
        ));
    }

    @GetMapping
    public Page<AttendanceRequestResponseDTO> find(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "requestedAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 500), parseSort(sort));
        return service.find(parseStatus(status), employeeId, startDate, endDate, search, pageable);
    }

    @GetMapping("/{id}")
    public AttendanceRequestResponseDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@Valid @RequestBody AttendanceRequestReviewDTO request) {
        List<AttendanceRequestResponseDTO> results = service.verify(request.requestIds());
        return Map.of(
                "success", true,
                "message", results.size() + " attendance request(s) verified",
                "data", results
        );
    }

    @PostMapping("/add-to-attendance")
    public Map<String, Object> addToAttendance(@Valid @RequestBody AttendanceRequestReviewDTO request) {
        List<AttendanceRequestResponseDTO> results = service.addToAttendance(request.requestIds());
        return Map.of(
                "success", true,
                "message", results.size() + " verified request(s) added to attendance records",
                "data", results
        );
    }

    @PostMapping("/remove-from-attendance")
    public Map<String, Object> removeFromAttendance(@Valid @RequestBody AttendanceRequestReviewDTO request) {
        List<AttendanceRequestResponseDTO> results = service.removeFromAttendance(request.requestIds());
        return Map.of(
                "success", true,
                "message", results.size() + " request-linked attendance record(s) removed",
                "data", results
        );
    }

    @PostMapping("/reject")
    public Map<String, Object> reject(@Valid @RequestBody AttendanceRequestReviewDTO request) {
        List<AttendanceRequestResponseDTO> results = service.reject(request.requestIds(), request.rejectionReason());
        return Map.of(
                "success", true,
                "message", results.size() + " attendance request(s) rejected",
                "data", results
        );
    }

    @ExceptionHandler(AttendanceRequestException.class)
    public ResponseEntity<Map<String, Object>> handleAttendanceRequestException(AttendanceRequestException exception) {
        return ResponseEntity.status(exception.getStatus()).body(Map.of(
                "success", false,
                "message", exception.getMessage()
        ));
    }

    private AttendanceRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) return null;
        try {
            return AttendanceRequestStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AttendanceRequestException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Status must be Pending, Verified, or Rejected");
        }
    }

    private Sort parseSort(String value) {
        String[] parts = value == null ? new String[0] : value.split(",", 2);
        String property = parts.length == 0 || parts[0].isBlank() ? "requestedAt" : parts[0];
        if (!Set.of("requestedAt", "date", "status", "id").contains(property)) property = "requestedAt";
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
