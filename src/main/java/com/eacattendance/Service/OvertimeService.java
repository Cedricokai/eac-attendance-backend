package com.eacattendance.Service;

import com.eacattendance.Exceptions.ResourceNotFoundException;
import com.eacattendance.entity.Employee;
import com.eacattendance.entity.Overtime;
import com.eacattendance.entity.Overview;
import com.eacattendance.repository.EmployeeRepository;
import com.eacattendance.repository.OvertimeRepository;
import com.eacattendance.repository.OverviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OvertimeService {
    private final OvertimeRepository overtimeRepository;
    private final EmployeeRepository employeeRepository;
    private final OverviewRepository overviewRepository;

    public OvertimeService(OvertimeRepository overtimeRepository,
                           EmployeeRepository employeeRepository, OverviewRepository overviewRepository) {
        this.overtimeRepository = overtimeRepository;
        this.employeeRepository = employeeRepository;
        this.overviewRepository = overviewRepository;
    }

    public List<Overtime> getAllOvertimes() {
        return overtimeRepository.findAll();
    }

    public Overtime getOvertimeById(Long id) {
        return overtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime not found"));
    }

    public Overtime createOvertime(Overtime overtime) {
        // Validate employee exists
        Employee employee = employeeRepository.findById(overtime.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Validate and calculate overtime hours
        calculateAndSetOvertimeHours(overtime);

        overtime.setEmployee(employee);
        return overtimeRepository.save(overtime);
    }

    public Overtime updateOvertime(Long id, Overtime overtimeDetails) {
        Overtime overtime = getOvertimeById(id);

        // Update basic fields
        overtime.setDate(overtimeDetails.getDate());
        overtime.setStatus(overtimeDetails.getStatus());

        // Handle time changes and recalculate hours if times changed
        if (overtimeDetails.getStartTime() != null && overtimeDetails.getEndTime() != null) {
            overtime.setStartTime(overtimeDetails.getStartTime());
            overtime.setEndTime(overtimeDetails.getEndTime());
            calculateAndSetOvertimeHours(overtime);
        }

        return overtimeRepository.save(overtime);
    }

    private void calculateAndSetOvertimeHours(Overtime overtime) {
        if (overtime.getStartTime() == null || overtime.getEndTime() == null) {
            throw new IllegalArgumentException("Both start time and end time must be provided");
        }

        LocalTime start = overtime.getStartTime();
        LocalTime end = overtime.getEndTime();

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Calculate difference in hours (with minute precision)
        double hours = ChronoUnit.MINUTES.between(start, end) / 60.0;
        overtime.setOvertimeHours(hours);
    }

    public void deleteOvertime(Long id) {
        Overtime overtime = getOvertimeById(id);
        overtimeRepository.delete(overtime);
    }

    public List<Employee> getAvailableEmployeesForDate(LocalDate date) {
        // Get all employees
        List<Employee> allEmployees = employeeRepository.findAll();

        // Get employees with overtime on this date
        List<Long> employeeIdsWithOvertime = overtimeRepository.findByDate(date)
                .stream()
                .map(o -> o.getEmployee().getId())
                .collect(Collectors.toList());

        // Filter out employees with existing overtime
        return allEmployees.stream()
                .filter(e -> !employeeIdsWithOvertime.contains(e.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Overview> validateOvertimes(List<Long> overtimeIds) {
        List<Overtime> overtimes = overtimeRepository.findAllById(overtimeIds);

        if (overtimes.isEmpty()) {
            throw new IllegalArgumentException("No overtime records selected");
        }

        return overtimes.stream().map(overtime -> {
            // First check if an overview already exists for this employee and date
            Optional<Overview> existingOverview = overviewRepository.findByEmployeeIdAndDate(
                    overtime.getEmployee().getId(),
                    overtime.getDate()
            );

            if (existingOverview.isPresent()) {
                // Update existing overview with overtime info
                Overview overview = existingOverview.get();
                overview.setOvertime(overtime);
                overview.setOvertimeHours(overtime.getOvertimeHours() != null ? overtime.getOvertimeHours() : 0.0);
                overview.setOvertimeRate(overtime.getEmployee().getMinimumRate() * 2);
                // Initialize hoursWorked if null
                if (overview.getHoursWorked() == null) {
                    overview.setHoursWorked(0.0);
                }
                return overviewRepository.save(overview);
            } else {
                // Create new overview
                Overview overview = new Overview();
                overview.setOvertime(overtime);
                overview.setEmployee(overtime.getEmployee());
                overview.setDate(overtime.getDate());
                overview.setOvertimeRate(overtime.getEmployee().getMinimumRate() * 2);
                overview.setOvertimeHours(overtime.getOvertimeHours() != null ? overtime.getOvertimeHours() : 0.0);
                overview.setStatus("Overtime");
                overview.setVersion(0);
                // Initialize hoursWorked
                overview.setHoursWorked(0.0);

                // Leave attendance null since this is overtime-only
                overview.setAttendance(null);

                return overviewRepository.save(overview);
            }
        }).collect(Collectors.toList());
    }


    @Transactional
    public void processOvertime(Overtime overtime) {
        Optional<Overview> overview = overviewRepository.findByAttendanceId(overtime.getAttendance().getId());

        if (overview.isPresent()) {
            Overview existingOverview = overview.get();
            existingOverview.setOvertimeHours(overtime.getOvertimeHours());
            overviewRepository.save(existingOverview);
        }
    }

}