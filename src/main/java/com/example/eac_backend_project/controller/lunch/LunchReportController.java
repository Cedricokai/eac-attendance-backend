package com.example.eac_backend_project.controller.lunch;

import com.example.eac_backend_project.Service.lunch.LunchAssignmentService;
import com.example.eac_backend_project.Service.lunch.LunchReportService;
import com.example.eac_backend_project.dto.lunch.DailyLunchSummaryDTO;
import com.example.eac_backend_project.dto.lunch.DailyTrendDTO;
import com.example.eac_backend_project.dto.lunch.DepartmentDistributionDTO;
import com.example.eac_backend_project.dto.lunch.KitchenReportDTO;
import com.example.eac_backend_project.entity.lunch.LunchAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lunch/reports")
@RequiredArgsConstructor
public class LunchReportController {

    private final LunchReportService reportService;
    private final LunchAssignmentService assignmentService; // ADDED for assignments/range

    // ----- Existing endpoints -----
    @GetMapping("/daily-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<DailyLunchSummaryDTO> getDailySummary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(reportService.getDailySummary(date));
    }

    @GetMapping("/kitchen")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN')")
    public ResponseEntity<KitchenReportDTO> getKitchenReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(reportService.getKitchenReport(date));
    }

    // ========== NEW ENDPOINTS ==========

    @GetMapping("/assignments/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<List<LunchAssignment>> getAssignmentsForDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(assignmentService.getAssignmentsForDateRange(start, end));
    }

    @GetMapping("/daily-trend")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<List<DailyTrendDTO>> getDailyTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportService.getDailyTrend(start, end));
    }

    @GetMapping("/department-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<List<DepartmentDistributionDTO>> getDepartmentDistribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.getDepartmentDistribution(date));
    }
}