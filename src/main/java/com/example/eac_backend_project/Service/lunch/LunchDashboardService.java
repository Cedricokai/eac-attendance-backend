package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.dto.lunch.DashboardStatsDTO;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.lunch.LunchAssignmentRepository;
import com.example.eac_backend_project.repository.lunch.LunchServingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LunchDashboardService {

    private final AttendanceRepository attendanceRepository;
    private final LunchAssignmentRepository assignmentRepository;
    private final LunchServingRepository servingRepository;

    public DashboardStatsDTO getStats(LocalDate date) {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        long present = attendanceRepository.findByDateAndCheckInIsNotNull(date).size();
        long assigned = assignmentRepository.findActiveAssignmentsForDate(date).size();
        long served = servingRepository.countByLunchDate(date);
        long pending = Math.max(0, assigned - served);
        long absent = Math.max(0, present - assigned);
        long notAssigned = Math.max(0, present - assigned);

        stats.setTodayPresent(present);
        stats.setTodayAssigned(assigned);
        stats.setTodayServed(served);
        stats.setPendingLunch(pending);
        stats.setAbsentEmployees(absent);
        stats.setNotAssignedEmployees(notAssigned);

        // Placeholder for costs – will be enhanced later
        BigDecimal costPerLunch = getCostPerLunch();
        stats.setTodayEstimatedCost(costPerLunch.multiply(BigDecimal.valueOf(served)));

        return stats;
    }

    private BigDecimal getCostPerLunch() {
        // TODO: read from LunchSettings
        return BigDecimal.valueOf(5.50);
    }
}