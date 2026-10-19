package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.dto.OvertimeListDTO;
import com.example.eac_backend_project.entity.attendance.Attendance;
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
            System.out.println("Overtime request date: " + overtime.getDate());

            // Validate employee exists
            Employee employee = employeeRepository.findById(overtime.getEmployee().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

            overtime.setEmployee(employee);

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

            // Attendance has already split the worked hours using the current schedule.
            if (overtime.getAttendance() != null && overtime.getAttendance().getId() != null) {
                overtime.setRawOvertimeHours(overtime.getAttendance().getOvertime() != null
                        ? overtime.getAttendance().getOvertime() : 0.0);
                applyConfiguredOvertimeHours(overtime);
            } else if (overtime.getStartTime() != null && overtime.getEndTime() != null) {
                calculateAndSetOvertimeHours(overtime);
                System.out.println("Overtime hours calculated: " + overtime.getOvertimeHours());
            } else if (overtime.getOvertimeHours() == null) {
                throw new IllegalArgumentException("Either start/end times or overtime hours must be provided");
            } else {
                applyConfiguredOvertimeHours(overtime);
            }

            // Calculate overtime pay with multiplier effect
            calculateOvertimePay(overtime);
            System.out.println("Overtime pay calculated: " + overtime.getCalculatedOvertimePay());

            applyJobApprovalStatus(overtime);

            // âœ… IMPORTANT: The multiplierAppliedToHours flag should already be set in the overtime object
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
            Long jobId,
            boolean unassignedJob,
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
                jobId,
                unassignedJob,
                normalizedStatus,
                normalizedSearch,
                minHours,
                maxHours,
                minAmount,
                maxAmount,
                pageable
        );
    }

    private boolean applyHolidayAttendanceHours(Overtime overtime) {

        if (overtime == null || overtime.getAttendance() == null) {
            return false;
        }

        Attendance attendance = overtime.getAttendance();

        if (!"Holiday Present".equalsIgnoreCase(attendance.getStatus())
                && !"HP".equalsIgnoreCase(attendance.getStatus())) {
            return false;
        }

        SystemSettings settings = getSystemSettings();

        double holidayMultiplier = requireConfiguredMultiplier(
                settings.getHolidayOvertimeMultiplier(),
                "Holiday Overtime Multiplier"
        );

        double normalHours = attendance.getMinimumHour();

        double attendanceOvertime =
                attendance.getOvertime() != null
                        ? attendance.getOvertime()
                        : 0.0;

        /*
         * ==========================================================
         * TIMESHEET SYNC
         * ==========================================================
         *
         * Timesheet has ALREADY calculated the holiday overtime.
         *
         * Example received from Timesheet:
         *
         * NH = 8
         * OT = 16
         *
         * Therefore:
         *
         * Final OT = 16
         *
         * DO NOT:
         *
         * (8 + 16) x 2 - 8
         *
         * because that would incorrectly become 40.
         */
        if ("TIMESHEET_SYNC".equalsIgnoreCase(attendance.getMethod())) {

            double finalOvertimeHours = attendanceOvertime;

            /*
             * We can reverse the holiday formula to determine
             * the original physical/raw OT.
             *
             * Original formula:
             *
             * Final OT =
             * ((NH + Raw OT) x Holiday Multiplier) - NH
             *
             * Therefore:
             *
             * Raw OT =
             * ((Final OT + NH) / Holiday Multiplier) - NH
             *
             * Example:
             *
             * NH       = 8
             * Final OT = 16
             * Holiday  = 2
             *
             * Raw OT =
             * ((16 + 8) / 2) - 8
             * = 12 - 8
             * = 4
             */
            double rawOvertimeHours =
                    Math.max(
                            0.0,
                            ((finalOvertimeHours + normalHours)
                                    / holidayMultiplier)
                                    - normalHours
                    );

            overtime.setRawOvertimeHours(rawOvertimeHours);

            overtime.setOvertimeHours(finalOvertimeHours);

            /*
             * Holiday multiplier has already been included
             * by Timesheet.
             */
            overtime.setMultiplierAppliedToHours(true);

            /*
             * Prevent another multiplier from being applied.
             */
            overtime.setOvertimeMultiplier(1.0);

            System.out.println(
                    "HP TIMESHEET_SYNC => NH: "
                            + normalHours
                            + ", Timesheet Final OT: "
                            + finalOvertimeHours
                            + ", Derived Raw OT: "
                            + rawOvertimeHours
                            + ", Holiday Multiplier: "
                            + holidayMultiplier
            );

            return true;
        }

        /*
         * ==========================================================
         * NORMAL ATTENDANCE / BIOMETRIC / MANUAL
         * ==========================================================
         *
         * For normal Attendance records, attendance.overtime
         * still represents RAW overtime.
         *
         * Example:
         *
         * Worked = 12
         * NH = 8
         * Raw OT = 4
         *
         * Final OT =
         * (12 x 2) - 8
         * = 16
         */

        double rawOvertimeHours = attendanceOvertime;

        double workedHours =
                normalHours + rawOvertimeHours;

        double finalOvertimeHours =
                Math.max(
                        0.0,
                        (workedHours * holidayMultiplier)
                                - normalHours
                );

        overtime.setRawOvertimeHours(
                rawOvertimeHours
        );

        overtime.setOvertimeHours(
                finalOvertimeHours
        );

        overtime.setMultiplierAppliedToHours(
                true
        );

        /*
         * Holiday multiplier is already represented
         * in Final OT.
         */
        overtime.setOvertimeMultiplier(
                1.0
        );

        System.out.println(
                "HP ATTENDANCE => Worked: "
                        + workedHours
                        + ", NH: "
                        + normalHours
                        + ", Raw OT: "
                        + rawOvertimeHours
                        + ", Holiday Multiplier: "
                        + holidayMultiplier
                        + ", Final OT: "
                        + finalOvertimeHours
        );

        return true;
    }

    private boolean usesFixedProjectHours(Overtime overtime) {
        var attendance = overtime.getAttendance();
        return ConfiguredJobAttendanceHours.applies(attendance)
                && (attendance.getDate() == null || !ConfiguredJobAttendanceHours.isWeeklyOffDay(
                        attendance, getSystemSettings().getWeekendDays()));
    }

    private boolean applyFixedJobOvertime(Overtime overtime) {
        if (!usesFixedProjectHours(overtime)) return false;
        Job job = overtime.getAttendance().getEmployee().getJob();
        double rawHours = job.getMinimumOvertimeHours() != null ? job.getMinimumOvertimeHours() : 0.0;
        overtime.setRawOvertimeHours(rawHours);
        overtime.setMultiplierAppliedToHours(true);
        overtime.setOvertimeHours(rawHours * determineOvertimeMultiplier(overtime));
        return true;
    }

    private void calculateAndSetOvertimeHours(Overtime overtime) {
        if (applyHolidayAttendanceHours(overtime)) return;
        if (applyFixedJobOvertime(overtime)) return;
        if (overtime.getStartTime() == null || overtime.getEndTime() == null) {
            throw new IllegalArgumentException("Both start time and end time must be provided");
        }

        LocalTime start = overtime.getStartTime();
        LocalTime end = overtime.getEndTime();

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        double rawHours = ChronoUnit.MINUTES.between(start, end) / 60.0;

        // âœ… Always store raw hours
        overtime.setRawOvertimeHours(rawHours);

        Job job = overtime.getEmployee() != null ? overtime.getEmployee().getJob() : null;
        if (job != null && Boolean.TRUE.equals(job.getApplyOvertimeMultiplierToHours())) {
            overtime.setMultiplierAppliedToHours(true);
        }
        double payableHours = rawHours;
        if (job != null && Boolean.TRUE.equals(job.getEnforceMinimumOvertime())
                && job.getMinimumOvertimeHours() != null && job.getMinimumOvertimeHours() > 0) {
            payableHours = Math.max(payableHours, job.getMinimumOvertimeHours());
        }

        double multiplier = determineOvertimeMultiplier(overtime);

        if (usesMultiplierOnHours(overtime)) {
            // Apply multiplier to hours - THIS IS THE KEY CHANGE
            // When toggle is ON, multiply raw hours by the multiplier
            overtime.setOvertimeHours(payableHours * multiplier);
        } else {
            // Keep hours raw (no multiplier applied to hours)
            overtime.setOvertimeHours(payableHours);
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

        if (usesMultiplierOnHours(overtime)) {
            // ON mode only stores adjusted hours. Payroll calculates the amount.
            overtime.setCalculatedOvertimePay(0.0);
        } else {
            // Multiplier applied to pay
            overtime.setCalculatedOvertimePay(baseRate * overtime.getOvertimeHours() * multiplier);
        }
    }

    private double determineOvertimeMultiplier(Overtime overtime) {

        if (overtime != null
                && overtime.getAttendance() != null
                && (
                "Holiday Present".equalsIgnoreCase(
                        overtime.getAttendance().getStatus()
                )
                        ||
                        "HP".equalsIgnoreCase(
                                overtime.getAttendance().getStatus()
                        )
        )) {

            return 1.0;
        }

        if (usesFixedProjectHours(overtime)) {
            Job job = overtime.getAttendance().getEmployee().getJob();

            return requireConfiguredMultiplier(
                    job.getOvertimeMultiplier() != null
                            ? job.getOvertimeMultiplier()
                            : getSystemSettings().getDefaultOvertimeMultiplier(),
                    "Job or Default Overtime Multiplier"
            );
        }

        SystemSettings settings = getSystemSettings();

        if (isWeekendAttendanceOvertime(overtime)) {
            return requireConfiguredMultiplier(
                    settings.getDefaultOvertimeMultiplier(),
                    "Default Overtime Multiplier"
            );
        }

        Job job =
                overtime.getEmployee() != null
                        ? overtime.getEmployee().getJob()
                        : null;

        if (job != null
                && job.getOvertimeMultiplier() != null
                && job.getOvertimeMultiplier() > 0) {

            return job.getOvertimeMultiplier();
        }

        if (usesMultiplierOnHours(overtime)) {
            return requireConfiguredMultiplier(
                    settings.getDefaultOvertimeMultiplier(),
                    "Default Overtime Multiplier"
            );
        }

        if (overtime.getDate() != null
                && overtime.getDate().getDayOfWeek() == DayOfWeek.SUNDAY
                && Boolean.TRUE.equals(settings.getDoubleTimeOnSunday())) {

            return requireConfiguredMultiplier(
                    settings.getSundayOvertimeMultiplier(),
                    "Sunday Overtime Multiplier"
            );
        }

        if (overtime.getDate() != null
                && isHoliday(overtime.getDate())) {

            return requireConfiguredMultiplier(
                    settings.getHolidayOvertimeMultiplier(),
                    "Holiday Overtime Multiplier"
            );
        }

        if (Boolean.TRUE.equals(settings.getTimeAndHalfAfter8Hours())
                && overtime.getOvertimeHours() != null
                && overtime.getOvertimeHours() > 8) {

            return requireConfiguredMultiplier(
                    settings.getTimeAndHalfMultiplier(),
                    "Time & Half Multiplier"
            );
        }

        Double requestMultiplier =
                overtime.getOvertimeMultiplier();

        if (requestMultiplier != null
                && requestMultiplier > 0) {

            return requestMultiplier;
        }

        return requireConfiguredMultiplier(
                settings.getDefaultOvertimeMultiplier(),
                "Default Overtime Multiplier"
        );
    }

    private double requireConfiguredMultiplier(Double multiplier, String settingName) {
        if (multiplier == null || multiplier <= 0) {
            throw new IllegalStateException(
                    settingName + " has not been configured in System Settings."
            );
        }
        return multiplier;
    }

    private boolean usesMultiplierOnHours(Overtime overtime) {
        if (overtime == null) {
            return false;
        }

        // HP overtimeHours already contains:
        // (NH x Holiday Multiplier) + (Raw OT x Default Multiplier).
        if (overtime.getAttendance() != null
                && "Holiday Present".equalsIgnoreCase(
                overtime.getAttendance().getStatus()
        )) {
            return true;
        }

        // Weekend/off-day attendance always multiplies the overtime HOURS.
        // The job toggle only decides the treatment of non-weekend overtime.
        if (isWeekendAttendanceOvertime(overtime)) {
            return true;
        }

        Job job =
                overtime.getEmployee() != null
                        ? overtime.getEmployee().getJob()
                        : null;

        return Boolean.TRUE.equals(overtime.getMultiplierAppliedToHours())
                || (job != null
                && Boolean.TRUE.equals(
                job.getApplyOvertimeMultiplierToHours()
        ));
    }

    private boolean isWeekendAttendanceOvertime(Overtime overtime) {
        if (overtime == null || overtime.getEmployee() == null || overtime.getDate() == null) {
            return false;
        }

        Employee employee = overtime.getEmployee();
        com.example.eac_backend_project.entity.settings.Category category = employee.getCategory();

        if (category == null || !Boolean.TRUE.equals(category.getWeekendAttendanceAsOvertime())) {
            return false;
        }

        String offDays = employee.getWeeklyOffDays();
        if (offDays == null || offDays.isBlank() || "INHERIT".equalsIgnoreCase(offDays)) {
            offDays = category.getWeeklyOffDays();
        }

        if (offDays == null || offDays.isBlank() || "INHERIT".equalsIgnoreCase(offDays)) {
            SystemSettings settings = getSystemSettings();
            offDays = settings.getWeekendDays();
        }

        if (offDays == null || offDays.isBlank()) {
            return false;
        }

        // Project convention: Sunday=0 ... Saturday=6.
        int configuredDay = overtime.getDate().getDayOfWeek().getValue() % 7;
        for (String value : offDays.split(",")) {
            if (String.valueOf(configuredDay).equals(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private void applyJobApprovalStatus(Overtime overtime) {
        Job job = overtime.getEmployee() != null ? overtime.getEmployee().getJob() : null;
        if (job != null && Boolean.TRUE.equals(job.getAutoApproveOvertime())) {
            overtime.setStatus("Approved");
        } else if (overtime.getStatus() == null || overtime.getStatus().isBlank()) {
            overtime.setStatus("Pending");
        }
    }

    private void applyConfiguredOvertimeHours(Overtime overtime) {
        if (applyHolidayAttendanceHours(overtime)) return;
        if (applyFixedJobOvertime(overtime)) return;
        double rawHours = overtime.getRawOvertimeHours() != null
                ? overtime.getRawOvertimeHours()
                : overtime.getOvertimeHours();
        overtime.setRawOvertimeHours(rawHours);

        Job job = overtime.getEmployee() != null ? overtime.getEmployee().getJob() : null;
        if (job != null && Boolean.TRUE.equals(job.getApplyOvertimeMultiplierToHours())) {
            overtime.setMultiplierAppliedToHours(true);
        }

        double payableHours = rawHours;
        if (job != null && Boolean.TRUE.equals(job.getEnforceMinimumOvertime())
                && job.getMinimumOvertimeHours() != null && job.getMinimumOvertimeHours() > 0) {
            payableHours = Math.max(payableHours, job.getMinimumOvertimeHours());
        }

        overtime.setOvertimeHours(usesMultiplierOnHours(overtime)
                ? payableHours * determineOvertimeMultiplier(overtime)
                : payableHours);
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
        double multiplier = overtime.getOvertimeMultiplier() != null && overtime.getOvertimeMultiplier() > 0
                ? overtime.getOvertimeMultiplier()
                : determineOvertimeMultiplier(overtime);

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

                overtime.setEmployee(employee);

                // Set base hourly rate
                Double baseRate = employee.getMinimumRate() != null ?
                        employee.getMinimumRate() : getDefaultHourlyRate();
                overtime.setBaseHourlyRate(baseRate);

                // Validate and calculate overtime hours
                if (overtime.getStartTime() != null && overtime.getEndTime() != null) {
                    calculateAndSetOvertimeHours(overtime);
                } else if (overtime.getOvertimeHours() == null) {
                    throw new IllegalArgumentException("Either start/end times or overtime hours must be provided");
                } else {
                    applyConfiguredOvertimeHours(overtime);
                }

                // Calculate overtime pay
                calculateOvertimePay(overtime);

                applyJobApprovalStatus(overtime);

                // Set default multiplier flag if not provided
                if (overtime.getMultiplierAppliedToHours() == null) {
                    overtime.setMultiplierAppliedToHours(false);
                }

                // Set default status if not provided
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

                overtime.setEmployee(employee);

                // Set base hourly rate
                Double baseRate = employee.getMinimumRate() != null ?
                        employee.getMinimumRate() : getDefaultHourlyRate();
                overtime.setBaseHourlyRate(baseRate);

                // Validate and calculate overtime hours
                if (overtime.getStartTime() != null && overtime.getEndTime() != null) {
                    calculateAndSetOvertimeHours(overtime);
                } else if (overtime.getOvertimeHours() == null) {
                    throw new IllegalArgumentException("Either start/end times or overtime hours must be provided");
                } else {
                    applyConfiguredOvertimeHours(overtime);
                }

                // Calculate overtime pay
                calculateOvertimePay(overtime);

                applyJobApprovalStatus(overtime);

                if (overtime.getMultiplierAppliedToHours() == null) {
                    overtime.setMultiplierAppliedToHours(false);
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
