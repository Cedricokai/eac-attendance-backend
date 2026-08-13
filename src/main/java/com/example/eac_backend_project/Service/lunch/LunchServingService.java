package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.dto.lunch.DailyServingStatsDTO;
import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.lunch.LunchAssignment;
import com.example.eac_backend_project.entity.lunch.LunchHistory;
import com.example.eac_backend_project.entity.lunch.LunchServing;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.lunch.LunchAssignmentRepository;
import com.example.eac_backend_project.repository.lunch.LunchHistoryRepository;
import com.example.eac_backend_project.repository.lunch.LunchServingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LunchServingService {

    private final LunchServingRepository servingRepository;
    private final LunchAssignmentRepository assignmentRepository;
    private final LunchHistoryRepository historyRepository;
    private final AttendanceRepository attendanceRepository;

    /**
     * Get all served records for a specific date.
     */
    public List<LunchServing> getServedForDate(LocalDate date) {
        return servingRepository.findByLunchDate(date);
    }

    /**
     * Count the number of served records for a specific date.
     */
    public long countServedForDate(LocalDate date) {
        return servingRepository.countByLunchDate(date);
    }

    /**
     * Get serving record for a specific employee on a specific date.
     */
    public Optional<LunchServing> getServingForEmployee(Long employeeId, LocalDate date) {
        return servingRepository.findByEmployeeIdAndLunchDate(employeeId, date);
    }

    /**
     * Serve lunch to an employee.
     */
    @Transactional
    public LunchServing serveLunch(Long employeeId, LocalDate date, User servedBy, String device, String remarks,
                                   boolean isPresent, String location) {
        // Validate: Check if employee already served today
        Optional<LunchServing> existing = servingRepository.findByEmployeeIdAndLunchDate(employeeId, date);
        if (existing.isPresent()) {
            throw new RuntimeException("Employee already served lunch today");
        }

        // Validate: Check if employee has an active lunch assignment for today
        Optional<LunchAssignment> assignment = assignmentRepository.findActiveAssignmentByEmployeeAndDate(employeeId, date);
        if (assignment.isEmpty()) {
            throw new RuntimeException("Employee does not have an active lunch assignment for today");
        }

        // Validate: Employee must be present
        if (!isPresent) {
            throw new RuntimeException("Employee is not present today, cannot serve lunch");
        }

        // Build the serving record
        LunchServing serving = new LunchServing();
        Employee employee = new Employee();
        employee.setId(employeeId);
        serving.setEmployee(employee);
        serving.setLunchDate(date);
        serving.setServingTime(LocalDateTime.now());
        serving.setServedBy(servedBy);
        serving.setDevice(device);
        serving.setRemarks(remarks);
        serving.setAttendanceStatus(isPresent ? "PRESENT" : "ABSENT");
        serving.setLocation(location != null ? location : "MKV");

        LunchServing saved = servingRepository.save(serving);

        // Log the action in history
        LunchHistory history = new LunchHistory();
        history.setEmployee(employee);
        history.setAction("SERVED");
        history.setDetails("{\"servingId\": " + saved.getId() + ", \"location\": \"" + location + "\"}");
        history.setPerformedBy(servedBy);
        history.setPerformedDate(LocalDateTime.now());
        history.setLunchDate(date);
        historyRepository.save(history);

        return saved;
    }

    /**
     * Get all served records for a specific location and date.
     */
    public List<LunchServing> getServedByLocation(String location, LocalDate date) {
        return servingRepository.findByLocationAndLunchDate(location, date);
    }

    /**
     * Get location distribution statistics for a specific date.
     */
    public Map<String, Object> getLocationStats(LocalDate date) {
        List<Object[]> results = servingRepository.countByLocationGrouped(date);
        Map<String, Object> stats = new HashMap<>();
        long total = 0;

        for (Object[] row : results) {
            String location = (String) row[0];
            Long count = (Long) row[1];
            stats.put(location, count);
            total += count;
        }

        stats.put("total", total);
        stats.put("date", date);
        return stats;
    }

    /**
     * Get daily serving statistics including attendance, assignments, and serving data.
     */
    public DailyServingStatsDTO getDailyServingStats(LocalDate date) {
        DailyServingStatsDTO stats = new DailyServingStatsDTO();
        stats.setDate(date);

        // Get attendance for the date
        List<Attendance> attendances = attendanceRepository.findByDate(date);
        long totalEmployees = attendances.size();
        long presentCount = attendances.stream()
                .filter(a -> a.getCheckIn() != null)
                .count();
        long absentCount = totalEmployees - presentCount;

        // Get lunch assignments for the date
        List<LunchAssignment> assignments = assignmentRepository.findActiveAssignmentsForDate(date);
        long assignedCount = assignments.size();

        // Get served records for the date
        List<LunchServing> servedRecords = servingRepository.findByLunchDate(date);
        long servedCount = servedRecords.size();
        long pendingCount = assignedCount - servedCount;

        // Location stats
        Map<String, Long> locationStats = servedRecords.stream()
                .filter(s -> s.getLocation() != null)
                .collect(Collectors.groupingBy(
                        LunchServing::getLocation,
                        Collectors.counting()
                ));

        if (locationStats.isEmpty()) {
            locationStats = new HashMap<>();
        }

        // Meal stats
        Map<String, Long> mealStats = new HashMap<>();
        if (!assignments.isEmpty() && assignments.get(0).getMeal() != null) {
            mealStats = assignments.stream()
                    .filter(a -> a.getMeal() != null && a.getMeal().getName() != null)
                    .collect(Collectors.groupingBy(
                            a -> a.getMeal().getName(),
                            Collectors.counting()
                    ));
        }

        stats.setTotalEmployees(totalEmployees);
        stats.setPresentCount(presentCount);
        stats.setAssignedCount(assignedCount);
        stats.setServedCount(servedCount);
        stats.setPendingCount(Math.max(0, pendingCount));
        stats.setAbsentCount(absentCount);
        stats.setLocationStats(locationStats);
        stats.setMealStats(mealStats);

        return stats;
    }

    /**
     * Get serving statistics for a date range.
     */
    public Map<String, Object> getServingStatsForDateRange(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();

        List<LunchServing> servings = servingRepository.findByLunchDateBetween(startDate, endDate);
        long totalServed = servings.size();

        Map<LocalDate, Long> dailyServed = servings.stream()
                .collect(Collectors.groupingBy(
                        LunchServing::getLunchDate,
                        Collectors.counting()
                ));

        Map<String, Long> locationDistribution = servings.stream()
                .filter(s -> s.getLocation() != null)
                .collect(Collectors.groupingBy(
                        LunchServing::getLocation,
                        Collectors.counting()
                ));

        Map<Long, Long> employeeServingCount = servings.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getEmployee().getId(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> topEmployees = employeeServingCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> empMap = new HashMap<>();
                    empMap.put("employeeId", entry.getKey());
                    empMap.put("count", entry.getValue());
                    return empMap;
                })
                .collect(Collectors.toList());

        stats.put("startDate", startDate);
        stats.put("endDate", endDate);
        stats.put("totalServed", totalServed);
        stats.put("dailyServed", dailyServed);
        stats.put("locationDistribution", locationDistribution);
        stats.put("topEmployees", topEmployees);

        return stats;
    }

    /**
     * Get today's serving progress.
     */
    public Map<String, Object> getTodayServingProgress() {
        LocalDate today = LocalDate.now();

        List<Attendance> attendances = attendanceRepository.findByDate(today);
        long presentCount = attendances.stream()
                .filter(a -> a.getCheckIn() != null)
                .count();

        List<LunchAssignment> assignments = assignmentRepository.findActiveAssignmentsForDate(today);
        long assignedCount = assignments.size();

        List<LunchServing> served = servingRepository.findByLunchDate(today);
        long servedCount = served.size();

        long eligibleCount = Math.min(presentCount, assignedCount);

        Map<String, Object> progress = new HashMap<>();
        progress.put("date", today);
        progress.put("presentCount", presentCount);
        progress.put("assignedCount", assignedCount);
        progress.put("servedCount", servedCount);
        progress.put("eligibleCount", eligibleCount);
        progress.put("progressPercentage", eligibleCount > 0 ? (servedCount * 100.0 / eligibleCount) : 0);
        progress.put("pendingCount", Math.max(0, eligibleCount - servedCount));

        return progress;
    }
}