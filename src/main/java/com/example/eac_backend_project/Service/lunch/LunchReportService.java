package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.dto.lunch.DailyLunchSummaryDTO;
import com.example.eac_backend_project.dto.lunch.DailyTrendDTO;
import com.example.eac_backend_project.dto.lunch.DepartmentDistributionDTO;
import com.example.eac_backend_project.dto.lunch.KitchenReportDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.lunch.LunchAssignment;
import com.example.eac_backend_project.entity.lunch.LunchServing;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.lunch.LunchAssignmentRepository;
import com.example.eac_backend_project.repository.lunch.LunchServingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LunchReportService {

    private final LunchAssignmentRepository assignmentRepository;
    private final LunchServingRepository servingRepository;
    private final AttendanceRepository attendanceRepository;

    // ----- Existing methods -----
    public DailyLunchSummaryDTO getDailySummary(LocalDate date) {
        long assigned = assignmentRepository.findActiveAssignmentsForDate(date).size();
        long served = servingRepository.countByLunchDate(date);
        long present = attendanceRepository.findByDateAndCheckInIsNotNull(date).size();
        long pending = Math.max(0, assigned - served);
        long absent = Math.max(0, present - assigned);
        long notAssigned = Math.max(0, present - assigned);

        DailyLunchSummaryDTO dto = new DailyLunchSummaryDTO();
        dto.setDate(date);
        dto.setPresentEmployees(present);
        dto.setAssignedEmployees(assigned);
        dto.setServedEmployees(served);
        dto.setPendingLunch(pending);
        dto.setAbsentEmployees(absent);
        dto.setNotAssignedEmployees(notAssigned);
        return dto;
    }

    public KitchenReportDTO getKitchenReport(LocalDate date) {
        DailyLunchSummaryDTO summary = getDailySummary(date);
        KitchenReportDTO report = new KitchenReportDTO();
        report.setDate(date);
        report.setTotalRequired(summary.getAssignedEmployees());
        report.setAssigned(summary.getAssignedEmployees());
        report.setServed(summary.getServedEmployees());
        report.setRemaining(summary.getPendingLunch());
        return report;
    }

    // ========== NEW: daily trend for dashboard ==========
    public List<DailyTrendDTO> getDailyTrend(LocalDate start, LocalDate end) {
        List<DailyTrendDTO> trend = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            long assigned = assignmentRepository.findActiveAssignmentsForDate(current).size();
            long served = servingRepository.countByLunchDate(current);
            trend.add(new DailyTrendDTO(current, assigned, served));
            current = current.plusDays(1);
        }
        return trend;
    }

    // ========== NEW: department distribution for dashboard ==========
    public List<DepartmentDistributionDTO> getDepartmentDistribution(LocalDate date) {
        // Fetch all active assignments for the date
        List<LunchAssignment> assignments = assignmentRepository.findActiveAssignmentsForDate(date);
        // Group by department from the employee
        Map<String, Long> departmentCounts = assignments.stream()
                .map(la -> la.getEmployee().getDepartment())
                .filter(dept -> dept != null && !dept.isEmpty())
                .collect(Collectors.groupingBy(dept -> dept, Collectors.counting()));

        // Convert to DTOs
        List<DepartmentDistributionDTO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : departmentCounts.entrySet()) {
            result.add(new DepartmentDistributionDTO(entry.getKey(), entry.getValue()));
        }
        // If no data, add a placeholder (optional)
        if (result.isEmpty()) {
            result.add(new DepartmentDistributionDTO("No Data", 0));
        }
        return result;
    }
}