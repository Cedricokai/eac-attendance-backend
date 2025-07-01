package com.eacattendance.Service;

import com.eacattendance.Exceptions.ResourceNotFoundException;
import com.eacattendance.entity.Employee;
import com.eacattendance.entity.Overview;
import com.eacattendance.entity.Timesheet;
import com.eacattendance.entity.TimesheetStatus;
import com.eacattendance.repository.EmployeeRepository;
import com.eacattendance.repository.OverviewRepository;
import com.eacattendance.repository.TimesheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TimesheetService {
    private final TimesheetRepository timesheetRepository;
    private final EmployeeRepository employeeRepository;

    private final OverviewRepository overviewRepository;

    public TimesheetService(TimesheetRepository timesheetRepository,
                            EmployeeRepository employeeRepository,
                            OverviewRepository overviewRepository) {
        this.timesheetRepository = timesheetRepository;
        this.employeeRepository = employeeRepository;
        this.overviewRepository = overviewRepository;
    }


    public List<Timesheet> getAllTimesheets() {
        return timesheetRepository.findAll();
    }

    public Timesheet getTimesheetById(Long id) {
        return timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found"));
    }

    @Transactional
    public Timesheet createTimesheet(Timesheet timesheet) {
        // Validate employee exists
        Employee employee = employeeRepository.findById(timesheet.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Check if timesheet already exists for this employee and date
        Timesheet existing = timesheetRepository.findByEmployeeIdAndDate(
                employee.getId(),
                timesheet.getDate()
        );

        if (existing != null) {
            throw new IllegalArgumentException("Timesheet already exists for this employee and date");
        }

        timesheet.setEmployee(employee);
        return timesheetRepository.save(timesheet);
    }

    @Transactional
    public List<Timesheet> createBatchTimesheets(List<Timesheet> timesheets) {
        return timesheets.stream()
                .map(this::createTimesheet)
                .collect(Collectors.toList());
    }

    @Transactional
    public Timesheet updateTimesheet(Long id, Timesheet timesheetDetails) {
        Timesheet timesheet = getTimesheetById(id);

        // Update basic fields
        timesheet.setRegularHours(timesheetDetails.getRegularHours());
        timesheet.setOvertimeHours(timesheetDetails.getOvertimeHours());
        timesheet.setBreakHours(timesheetDetails.getBreakHours());
        timesheet.setNotes(timesheetDetails.getNotes());

        return timesheetRepository.save(timesheet);
    }

    @Transactional
    public void deleteTimesheet(Long id) {
        Timesheet timesheet = getTimesheetById(id);
        timesheetRepository.delete(timesheet);
    }

    public List<Timesheet> getTimesheetsByDateRange(LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.findByDateBetween(startDate, endDate);
    }

    public List<Timesheet> getEmployeeTimesheets(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);
    }

    @Transactional
    public Timesheet updateTimesheetStatus(Long id, String status) {
        Timesheet timesheet = getTimesheetById(id);

        try {
            TimesheetStatus newStatus = TimesheetStatus.valueOf(status.toUpperCase());
            timesheet.setStatus(newStatus);
            return timesheetRepository.save(timesheet);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    @Transactional
    public List<Timesheet> bulkUpdateStatus(List<Long> ids, String status) {
        return ids.stream()
                .map(id -> updateTimesheetStatus(id, status))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Timesheet> generateFromOverview(LocalDate startDate, LocalDate endDate) {
        // Fetch overviews within the date range
        List<Overview> overviews = overviewRepository.findByDateBetween(startDate, endDate);
        List<Timesheet> newTimesheets = new ArrayList<>();

        for (Overview overview : overviews) {
            if (overview.getEmployee() == null || overview.getDate() == null) {
                continue; // Skip if required data is missing
            }

            Long employeeId = overview.getEmployee().getId();
            LocalDate overviewDate = overview.getDate();

            // Check if timesheet already exists for this employee and date
            if (timesheetRepository.existsByEmployeeIdAndDate(employeeId, overviewDate)) {
                continue;
            }

            // Create new timesheet from overview data
            Timesheet timesheet = new Timesheet();
            timesheet.setEmployee(overview.getEmployee());
            timesheet.setDate(overviewDate);
            timesheet.setOverview(overview);

            // Calculate hours (assuming standard 8-hour workday)
            double hoursWorked = overview.getHoursWorked();
            timesheet.setRegularHours(Math.min(hoursWorked, 8));
            timesheet.setOvertimeHours(Math.max(hoursWorked - 8, 0));
            timesheet.setBreakHours(overview.getMinimumHour() != null ? overview.getMinimumHour() : 0);
            timesheet.setStatus(TimesheetStatus.PENDING);

            newTimesheets.add(timesheet);
        }

        return timesheetRepository.saveAll(newTimesheets);
    }


    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

}