package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.dto.OvertimeListDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Overtime;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.OvertimeRepository;
import com.example.eac_backend_project.repository.settings.SystemSettingsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OvertimeService {
    private final OvertimeRepository overtimeRepository;
    private final EmployeeRepository employeeRepository;
//    private final OverviewRepository overviewRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final SettingsService settingsService;
    private final EmailService emailService;

    public OvertimeService(OvertimeRepository overtimeRepository,
                           EmployeeRepository employeeRepository,
                           SystemSettingsRepository systemSettingsRepository,
                           SettingsService settingsService,
                           EmailService emailService) {
        this.overtimeRepository = overtimeRepository;
        this.employeeRepository = employeeRepository;
        this.systemSettingsRepository = systemSettingsRepository;
        this.settingsService = settingsService;
        this.emailService = emailService;
    }

    public List<Overtime> getAllOvertimes() {
        return overtimeRepository.findAll();
    }

    public Overtime getOvertimeById(Long id) {
        return overtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime not found"));
    }

    @Transactional
    public Overtime createOvertime(Overtime overtime) {
        try {
            System.out.println("Creating overtime for employee: " + overtime.getEmployee().getId());
            System.out.println("Overtime data: " + overtime);

            // Validate employee exists
            Employee employee = employeeRepository.findById(overtime.getEmployee().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

            System.out.println("Employee found: " + employee.getId());

            // CRITICAL: If attendanceId is provided, link it
            if (overtime.getAttendance() != null && overtime.getAttendance().getId() != null) {
                System.out.println("Linking to attendance ID: " + overtime.getAttendance().getId());
            }

            // Set base hourly rate from employee or system settings
            Double baseRate = employee.getMinimumRate() != null ?
                    employee.getMinimumRate() : getDefaultHourlyRate();
            overtime.setBaseHourlyRate(baseRate);
            System.out.println("Base hourly rate set to: " + baseRate);

            // Validate and calculate overtime hours
            if (overtime.getStartTime() != null && overtime.getEndTime() != null) {
                calculateAndSetOvertimeHours(overtime);
                System.out.println("Overtime hours calculated: " + overtime.getOvertimeHours());
            } else if (overtime.getOvertimeHours() == null) {
                throw new IllegalArgumentException("Either start/end times or overtime hours must be provided");
            }

            // Calculate overtime pay with multiplier effect
            calculateOvertimePay(overtime);
            System.out.println("Overtime pay calculated: " + overtime.getCalculatedOvertimePay());

            overtime.setEmployee(employee);

            // ✅ IMPORTANT: The multiplierAppliedToHours flag should already be set in the overtime object
            // from the frontend. Just make sure it's being saved.
            if (overtime.getMultiplierAppliedToHours() == null) {
                overtime.setMultiplierAppliedToHours(false); // Default to false if not provided
            }
            System.out.println("Multiplier applied to hours: " + overtime.getMultiplierAppliedToHours());

            Overtime savedOvertime = overtimeRepository.save(overtime);
            System.out.println("Overtime saved with ID: " + savedOvertime.getId());

            return savedOvertime;

        } catch (Exception e) {
            System.err.println("Error creating overtime: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create overtime record: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public void notifySupervisorOfDelegatedRequest(Overtime overtime, User requestedBy) {
        Employee employee = employeeRepository.findById(overtime.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        overtime.setEmployee(employee);
        Employee supervisor = null;
        Job job = employee.getJob();
        if (job != null && job.getSupervisor() != null) {
            supervisor = job.getSupervisor();
        } else if (employee.getSupervisor() != null) {
            supervisor = employee.getSupervisor();
        }

        if (supervisor != null && supervisor.getEmail() != null && !supervisor.getEmail().isBlank()) {
            try {
                String supervisorName = ((supervisor.getFirstName() == null ? "" : supervisor.getFirstName()) + " "
                        + (supervisor.getLastName() == null ? "" : supervisor.getLastName())).trim();
                emailService.sendOvertimeRequestToSupervisor(
                        overtime,
                        supervisor.getEmail(),
                        supervisorName,
                        requestedBy
                );
                return;
            } catch (Exception e) {
                System.err.println("Failed to notify job supervisor; using overtime recipient fallback: " + e.getMessage());
            }
        }

        emailService.sendOvertimeRequestToConfiguredRecipients(overtime, requestedBy);
    }

    @Transactional(readOnly = true)
    public void notifySupervisorsOfDelegatedRequests(List<Overtime> overtimes, User requestedBy) {
        Map<String, List<Overtime>> requestsByRecipient = new LinkedHashMap<>();
        Map<String, String> recipientNames = new HashMap<>();
        List<Overtime> fallbackRequests = new ArrayList<>();

        for (Overtime overtime : overtimes) {
            Employee employee = employeeRepository.findById(overtime.getEmployee().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
            overtime.setEmployee(employee);

            Job job = employee.getJob();
            Employee supervisor = job != null && job.getSupervisor() != null
                    ? job.getSupervisor()
                    : employee.getSupervisor();

            if (supervisor == null || supervisor.getEmail() == null || supervisor.getEmail().isBlank()) {
                fallbackRequests.add(overtime);
                continue;
            }

            String email = supervisor.getEmail().trim().toLowerCase(Locale.ROOT);
            String name = ((supervisor.getFirstName() == null ? "" : supervisor.getFirstName()) + " "
                    + (supervisor.getLastName() == null ? "" : supervisor.getLastName())).trim();
            requestsByRecipient.computeIfAbsent(email, ignored -> new ArrayList<>()).add(overtime);
            recipientNames.putIfAbsent(email, name);
        }

        requestsByRecipient.forEach((email, requests) ->
                emailService.sendOvertimeRequestsToSupervisor(requests, email, recipientNames.get(email), requestedBy));

        if (!fallbackRequests.isEmpty()) {
            emailService.sendOvertimeRequestsToConfiguredRecipients(fallbackRequests, requestedBy);
        }
    }

    // Rest of your existing methods remain the same...
    public Overtime updateOvertime(Long id, Overtime overtimeDetails) {
        Overtime overtime = getOvertimeById(id);

        // Update basic fields
        overtime.setDate(overtimeDetails.getDate());
        overtime.setStatus(overtimeDetails.getStatus());
        overtime.setOvertimeMultiplier(overtimeDetails.getOvertimeMultiplier());

        // Handle time changes and recalculate hours if times changed
        if (overtimeDetails.getStartTime() != null && overtimeDetails.getEndTime() != null) {
            overtime.setStartTime(overtimeDetails.getStartTime());
            overtime.setEndTime(overtimeDetails.getEndTime());
            calculateAndSetOvertimeHours(overtime);
            calculateOvertimePay(overtime);
        }

        return overtimeRepository.save(overtime);
    }

    @Transactional(readOnly = true)
    public Page<OvertimeListDTO> getOvertimePage(
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            String status,
            String search,
            Double minHours,
            Double maxHours,
            Double minAmount,
            Double maxAmount,
            int page,
            int size
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "startDate and endDate are required"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "endDate must be on or after startDate"
            );
        }

        if (minHours != null &&
                maxHours != null &&
                minHours > maxHours) {
            throw new IllegalArgumentException(
                    "minHours cannot be greater than maxHours"
            );
        }

        if (minAmount != null &&
                maxAmount != null &&
                minAmount > maxAmount) {
            throw new IllegalArgumentException(
                    "minAmount cannot be greater than maxAmount"
            );
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 500);

        Long normalizedCategoryId =
                categoryId != null && categoryId > 0
                        ? categoryId
                        : null;

        String normalizedStatus =
                status == null ? "" : status.trim();

        String normalizedSearch =
                search == null ? "" : search.trim();

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Order.desc("date"),
                        Sort.Order.desc("id")
                )
        );

        return overtimeRepository.findOvertimePage(
                startDate,
                endDate,
                normalizedCategoryId,
                normalizedStatus,
                normalizedSearch,
                minHours,
                maxHours,
                minAmount,
                maxAmount,
                pageable
        );
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

        double rawHours = ChronoUnit.MINUTES.between(start, end) / 60.0;

        // ✅ Always store raw hours
        overtime.setRawOvertimeHours(rawHours);

        double multiplier = determineOvertimeMultiplier(overtime);

        if (Boolean.TRUE.equals(overtime.getMultiplierAppliedToHours())) {
            // Apply multiplier to hours - THIS IS THE KEY CHANGE
            // When toggle is ON, multiply raw hours by the multiplier
            overtime.setOvertimeHours(rawHours * multiplier);
        } else {
            // Keep hours raw (no multiplier applied to hours)
            overtime.setOvertimeHours(rawHours);
        }
    }

    @Transactional
    public Overtime toggleMultiplierAppliedToHours(Long id, Boolean newValue) {
        Overtime overtime = getOvertimeById(id);

        // Update the multiplier flag
        overtime.setMultiplierAppliedToHours(newValue);

        // Recalculate overtime hours and pay based on the new multiplier setting
        if (overtime.getStartTime() != null && overtime.getEndTime() != null) {
            // Recalculate hours with the new multiplier setting
            calculateAndSetOvertimeHours(overtime);
        }

        // Recalculate pay
        calculateOvertimePay(overtime);

        return overtimeRepository.save(overtime);
    }

    @Transactional
    public int batchToggleMultiplierAppliedToHours(List<Long> ids, Boolean newValue) {
        List<Overtime> overtimes = overtimeRepository.findAllById(ids);

        int updatedCount = 0;
        for (Overtime overtime : overtimes) {
            overtime.setMultiplierAppliedToHours(newValue);

            // Recalculate hours and pay for each overtime
            if (overtime.getStartTime() != null && overtime.getEndTime() != null) {
                calculateAndSetOvertimeHours(overtime);
            }
            calculateOvertimePay(overtime);

            updatedCount++;
        }

        overtimeRepository.saveAll(overtimes);
        return updatedCount;
    }

    private void calculateOvertimePay(Overtime overtime) {
        if (overtime.getOvertimeHours() == null || overtime.getBaseHourlyRate() == null) {
            return;
        }

        double baseRate = overtime.getBaseHourlyRate();
        double multiplier = determineOvertimeMultiplier(overtime);

        overtime.setOvertimeMultiplier(multiplier);

        if (Boolean.TRUE.equals(overtime.getMultiplierAppliedToHours())) {
            // Hours already multiplied
            overtime.setCalculatedOvertimePay(baseRate * overtime.getOvertimeHours());
        } else {
            // Multiplier applied to pay
            overtime.setCalculatedOvertimePay(baseRate * overtime.getOvertimeHours() * multiplier);
        }
    }

    private double determineOvertimeMultiplier(Overtime overtime) {
        SystemSettings settings = getSystemSettings();

        if (overtime.getDate().getDayOfWeek() == DayOfWeek.SUNDAY && Boolean.TRUE.equals(settings.getDoubleTimeOnSunday())) {
            return settings.getSundayOvertimeMultiplier() != null ? settings.getSundayOvertimeMultiplier() : 2.0;
        }

        if (isHoliday(overtime.getDate())) {
            return settings.getHolidayOvertimeMultiplier() != null ? settings.getHolidayOvertimeMultiplier() : 2.5;
        }

        if (Boolean.TRUE.equals(settings.getTimeAndHalfAfter8Hours()) && overtime.getOvertimeHours() != null && overtime.getOvertimeHours() > 8) {
            return settings.getTimeAndHalfMultiplier() != null ? settings.getTimeAndHalfMultiplier() : 1.5;
        }

        // ✅ default: DB value first, then fallback
        Double dbDefault = settings.getDefaultOvertimeMultiplier();
        Double requestMultiplier = overtime.getOvertimeMultiplier();

        return requestMultiplier != null ? requestMultiplier : (dbDefault != null ? dbDefault : 1.5);
    }


    private boolean isHoliday(LocalDate date) {
        // Implement holiday checking logic
        // This should check against your holidays table
        return false; // Placeholder - implement based on your holiday entity
    }

    private Double getDefaultHourlyRate() {
        SystemSettings settings = getSystemSettings();
        return settings.getHourlyRate() != null ? settings.getHourlyRate() : 10.0;
    }

    private SystemSettings getSystemSettings() {
        // Get the first system settings record
        return systemSettingsRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    // Create default settings if none exist
                    SystemSettings defaultSettings = new SystemSettings();
                    return systemSettingsRepository.save(defaultSettings);
                });
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
                .toList();

        // Filter out employees with existing overtime
        return allEmployees.stream()
                .filter(e -> !employeeIdsWithOvertime.contains(e.getId()))
                .collect(Collectors.toList());
    }

//    @Transactional
//    public List<Overview> validateOvertimes(List<Long> overtimeIds) {
//        List<Overtime> overtimes = overtimeRepository.findAllById(overtimeIds);
//
//        if (overtimes.isEmpty()) {
//            throw new IllegalArgumentException("No overtime records selected");
//        }
//
//        return overtimes.stream().map(overtime -> {
//            // Update overtime status to "Approved"
//            overtime.setStatus("Approved");
//            overtimeRepository.save(overtime);
//
//            // Rest of the existing code for creating/updating Overview...
//            Optional<Overview> existingOverview = overviewRepository.findByEmployeeIdAndDate(
//                    overtime.getEmployee().getId(),
//                    overtime.getDate()
//            );
//
//            Overview overview;
//            if (existingOverview.isPresent()) {
//                overview = existingOverview.get();
//                overview.setOvertime(overtime);
//                overview.setOvertimeHours(overtime.getOvertimeHours() != null ? overtime.getOvertimeHours() : 0.0);
//                overview.setOvertimeRate(overtime.getCalculatedOvertimePay() != null ?
//                        overtime.getCalculatedOvertimePay() : calculateOvertimeRate(overtime));
//            } else {
//                overview = new Overview();
//                overview.setOvertime(overtime);
//                overview.setEmployee(overtime.getEmployee());
//                overview.setDate(overtime.getDate());
//                overview.setOvertimeRate(overtime.getCalculatedOvertimePay() != null ?
//                        overtime.getCalculatedOvertimePay() : calculateOvertimeRate(overtime));
//                overview.setOvertimeHours(overtime.getOvertimeHours() != null ? overtime.getOvertimeHours() : 0.0);
//                overview.setStatus("Overtime");
//                overview.setVersion(0);
//                overview.setHoursWorked(0.0);
//                overview.setAttendance(null);
//            }
//
//            return overviewRepository.save(overview);
//        }).collect(Collectors.toList());
//    }

    @Transactional
    public List<Overtime> validateOvertimes(List<Long> overtimeIds) {
        List<Overtime> overtimes = overtimeRepository.findAllById(overtimeIds);

        if (overtimes.isEmpty()) {
            throw new IllegalArgumentException("No overtime records selected");
        }

        return overtimes.stream().map(overtime -> {
            // Update overtime status to "Approved"
            overtime.setStatus("Approved");
            return overtimeRepository.save(overtime);
        }).collect(Collectors.toList());
    }

    @Transactional
    public List<Overtime> rejectOvertimes(List<Long> overtimeIds) {
        List<Overtime> overtimes = overtimeRepository.findAllById(overtimeIds);

        if (overtimes.isEmpty()) {
            throw new IllegalArgumentException("No overtime records selected");
        }

        return overtimes.stream().map(overtime -> {
            // Update overtime status to "Rejected"
            overtime.setStatus("Rejected");
            return overtimeRepository.save(overtime);
        }).collect(Collectors.toList());
    }

    private Double calculateOvertimeRate(Overtime overtime) {
        if (overtime.getCalculatedOvertimePay() != null) {
            return overtime.getCalculatedOvertimePay();
        }

        double baseRate = overtime.getBaseHourlyRate() != null ?
                overtime.getBaseHourlyRate() : getDefaultHourlyRate();
        double hours = overtime.getOvertimeHours() != null ? overtime.getOvertimeHours() : 0.0;
        double multiplier = overtime.getOvertimeMultiplier() != null ?
                overtime.getOvertimeMultiplier() : 1.5;

        return baseRate * hours * multiplier;
    }

    @Transactional
    public void processOvertime(Overtime overtime) {
//        Optional<Overview> overview = overviewRepository.findByAttendanceId(overtime.getAttendance().getId());
//
//        if (overview.isPresent()) {
//            Overview existingOverview = overview.get();
//            existingOverview.setOvertimeHours(overtime.getOvertimeHours());
//            existingOverview.setOvertimeRate(overtime.getCalculatedOvertimePay());
//            overviewRepository.save(existingOverview);
//        }
    }

    // Additional method to get overtime summary for reporting
    public OvertimeSummary getOvertimeSummary(LocalDate startDate, LocalDate endDate) {
        List<Overtime> overtimes = overtimeRepository.findByDateBetween(startDate, endDate);

        double totalOvertimeHours = overtimes.stream()
                .mapToDouble(o -> o.getOvertimeHours() != null ? o.getOvertimeHours() : 0.0)
                .sum();

        double totalOvertimePay = overtimes.stream()
                .mapToDouble(o -> o.getCalculatedOvertimePay() != null ? o.getCalculatedOvertimePay() : 0.0)
                .sum();

        return new OvertimeSummary(totalOvertimeHours, totalOvertimePay, overtimes.size());
    }

    // ===== PAYROLL-RELATED HELPERS (Approved only) =====

    public Map<Long, Double> getApprovedOvertimeHoursMap(LocalDate startDate, LocalDate endDate) {
        List<Object[]> rows = overtimeRepository.summarizeApprovedOvertimeBetweenDates(startDate, endDate);

        Map<Long, Double> map = new HashMap<>();
        if (rows == null) return map;

        for (Object[] r : rows) {
            if (r != null && r.length >= 2 && r[0] != null && r[1] != null) {
                Long empId = ((Number) r[0]).longValue();
                Double hours = ((Number) r[1]).doubleValue();
                map.put(empId, hours);
            }
        }
        return map;
    }

    // Add this method for bulk creation
    @Transactional
    public List<Overtime> createBulkOvertime(List<Overtime> overtimes) {
        List<Overtime> createdOvertimes = new ArrayList<>();

        for (Overtime overtime : overtimes) {
            try {
                // Validate employee exists
                Employee employee = employeeRepository.findById(overtime.getEmployee().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + overtime.getEmployee().getId()));

                // Set base hourly rate
                Double baseRate = employee.getMinimumRate() != null ?
                        employee.getMinimumRate() : getDefaultHourlyRate();
                overtime.setBaseHourlyRate(baseRate);

                // Validate and calculate overtime hours
                if (overtime.getStartTime() != null && overtime.getEndTime() != null) {
                    calculateAndSetOvertimeHours(overtime);
                } else if (overtime.getOvertimeHours() == null) {
                    throw new IllegalArgumentException("Either start/end times or overtime hours must be provided");
                }

                // Calculate overtime pay
                calculateOvertimePay(overtime);

                // Set employee
                overtime.setEmployee(employee);

                // Set default multiplier flag if not provided
                if (overtime.getMultiplierAppliedToHours() == null) {
                    overtime.setMultiplierAppliedToHours(false);
                }

                // Set default status if not provided
                if (overtime.getStatus() == null) {
                    overtime.setStatus("Pending");
                }

                createdOvertimes.add(overtimeRepository.save(overtime));

            } catch (Exception e) {
                throw new RuntimeException("Failed to create overtime for employee " +
                        overtime.getEmployee().getId() + ": " + e.getMessage());
            }
        }

        return createdOvertimes;
    }

    // Add this method for bulk validation with duplicate check
    @Transactional
    public List<Overtime> createBulkOvertimeWithDuplicateCheck(List<Overtime> overtimes, boolean skipDuplicates) {
        List<Overtime> createdOvertimes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Overtime overtime : overtimes) {
            try {
                // Check for duplicate on same date
                boolean exists = overtimeRepository.existsByEmployeeIdAndDate(
                        overtime.getEmployee().getId(),
                        overtime.getDate()
                );

                if (exists) {
                    if (skipDuplicates) {
                        errors.add("Skipped - Overtime already exists for employee " +
                                overtime.getEmployee().getId() + " on " + overtime.getDate());
                        continue;
                    } else {
                        throw new IllegalArgumentException("Overtime already exists for employee " +
                                overtime.getEmployee().getId() + " on " + overtime.getDate());
                    }
                }

                // Create overtime
                Employee employee = employeeRepository.findById(overtime.getEmployee().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + overtime.getEmployee().getId()));

                // Set base hourly rate
                Double baseRate = employee.getMinimumRate() != null ?
                        employee.getMinimumRate() : getDefaultHourlyRate();
                overtime.setBaseHourlyRate(baseRate);

                // Validate and calculate overtime hours
                if (overtime.getStartTime() != null && overtime.getEndTime() != null) {
                    calculateAndSetOvertimeHours(overtime);
                } else if (overtime.getOvertimeHours() == null) {
                    throw new IllegalArgumentException("Either start/end times or overtime hours must be provided");
                }

                // Calculate overtime pay
                calculateOvertimePay(overtime);

                overtime.setEmployee(employee);

                if (overtime.getMultiplierAppliedToHours() == null) {
                    overtime.setMultiplierAppliedToHours(false);
                }

                if (overtime.getStatus() == null) {
                    overtime.setStatus("Pending");
                }

                createdOvertimes.add(overtimeRepository.save(overtime));

            } catch (Exception e) {
                if (!skipDuplicates) {
                    throw new RuntimeException("Failed to create overtime: " + e.getMessage());
                }
                errors.add("Error for employee " + overtime.getEmployee().getId() + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty() && skipDuplicates) {
            // Log errors but don't throw exception
            System.out.println("Bulk creation completed with errors: " + String.join(", ", errors));
        }

        return createdOvertimes;
    }

    // Add this method to get employees eligible for bulk overtime
    public List<Employee> getEligibleEmployeesForBulkOvertime(LocalDate date, List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            // Return all employees without overtime on this date
            return getAvailableEmployeesForDate(date);
        } else {
            // Return specific employees that don't have overtime on this date
            List<Employee> allEmployees = employeeRepository.findAllById(employeeIds);
            List<Long> employeesWithOvertime = overtimeRepository.findByDate(date)
                    .stream()
                    .map(o -> o.getEmployee().getId())
                    .collect(Collectors.toList());

            return allEmployees.stream()
                    .filter(e -> !employeesWithOvertime.contains(e.getId()))
                    .collect(Collectors.toList());
        }
    }

    public long countNonApprovedOvertimeInPeriod(LocalDate startDate, LocalDate endDate) {
        return overtimeRepository.countNonApprovedOvertimeInPeriod(startDate, endDate);
    }


    // Inner class for overtime summary
    public static class OvertimeSummary {
        private final double totalHours;
        private final double totalPay;
        private final int recordCount;

        public OvertimeSummary(double totalHours, double totalPay, int recordCount) {
            this.totalHours = totalHours;
            this.totalPay = totalPay;
            this.recordCount = recordCount;
        }

        // Getters
        public double getTotalHours() { return totalHours; }
        public double getTotalPay() { return totalPay; }
        public int getRecordCount() { return recordCount; }
    }
}
