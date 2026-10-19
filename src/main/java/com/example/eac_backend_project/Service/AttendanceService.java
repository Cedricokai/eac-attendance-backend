package com.example.eac_backend_project.Service;



import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;

import com.example.eac_backend_project.dto.AttendanceImportDTO;

import com.example.eac_backend_project.dto.AttendanceListDTO;
import com.example.eac_backend_project.dto.AttendanceRangeCreateRequest;

import com.example.eac_backend_project.dto.BiometricRecordDTO;

import com.example.eac_backend_project.entity.attendance.*;

import com.example.eac_backend_project.entity.settings.AttendanceReportScheduleSettings;

import com.example.eac_backend_project.entity.settings.Category;

import com.example.eac_backend_project.entity.settings.SystemSettings;

import com.example.eac_backend_project.repository.*;

import com.example.eac_backend_project.repository.settings.AttendanceReportScheduleSettingsRepository;

import com.example.eac_backend_project.repository.settings.CategorySettingsRepository;

import com.example.eac_backend_project.repository.settings.SystemSettingsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDate;

import java.time.LocalTime;

import java.time.temporal.ChronoUnit;

import java.util.*;

import java.util.stream.Collectors;



@Service

public class AttendanceService {
    @org.springframework.beans.factory.annotation.Autowired
    private TimesheetRepository payrollTimesheetRepository;



    private final AttendanceRepository attendanceRepository;

    private final EmployeeRepository employeeRepository;

    private final OverviewRepository overviewRepository;

    private final CategorySettingsRepository categorySettingsRepository;



    @Autowired

    private LeaveRepository leaveRepository;



    @Autowired

    private SystemSettingsRepository systemSettingsRepository;



    @Autowired

    private OvertimeService overtimeService;



    @Autowired

    private OvertimeRepository overtimeRepository;



    @Autowired

    private AttendanceReportScheduleSettingsRepository scheduleRepository;



    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository, OverviewRepository overviewRepository, CategorySettingsRepository categorySettingsRepository) {

        this.attendanceRepository = attendanceRepository;

        this.employeeRepository = employeeRepository;

        this.overviewRepository = overviewRepository;

        this.categorySettingsRepository = categorySettingsRepository;

    }

    /** Legacy support. The attendance screen should use getAttendancePage(...). */
    @Transactional(readOnly = true)
    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<AttendanceListDTO> getAttendancePage(
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            String search,
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

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 500);

        String normalizedSearch =
                search == null ? "" : search.trim();

        Long normalizedCategoryId =
                categoryId != null && categoryId > 0
                        ? categoryId
                        : null;

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Order.desc("date"),
                        Sort.Order.asc("employee.id")
                )
        );

        return attendanceRepository.findAttendancePage(
                startDate,
                endDate,
                normalizedCategoryId,
                normalizedSearch,
                pageable
        );
    }



    public Attendance getAttendanceById(Long id) {

        return attendanceRepository.findById(id)

                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

    }



    @Transactional
    public void refreshPendingCheckInStatuses() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Africa/Accra"));
        for (Attendance attendance : attendanceRepository.findByDate(today)) {
            if (attendance.getCheckIn() != null && attendance.getCheckOut() == null
                    && "Half Day".equalsIgnoreCase(attendance.getStatus())) {
                applyCheckInStatus(attendance);
                attendanceRepository.save(attendance);
            }
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.eac_backend_project.Service.settings.AttendanceCodeSettingsService holidayCodeSettingsService;

    private boolean isAbsentOrLeave(Attendance attendance) {
        String status = attendance.getStatus() == null ? "" : attendance.getStatus().trim().toLowerCase(Locale.ROOT);
        return status.contains("leave") || status.contains("sick")
                || Set.of("absent", "a", "l", "s", "ml", "pl").contains(status);
    }

    private void applyCheckInStatus(Attendance attendance) {
        applyCheckInStatusBeforeProjectRule(attendance);
        if (!"TIMESHEET_SYNC".equals(attendance.getMethod())) {
            ProjectDailyHoursRule.apply(attendance, attendance.getEmployee() == null ? null : attendance.getEmployee().getJob());
        }
    }

    private void applyCheckInStatusBeforeProjectRule(Attendance attendance) {
        if (isAbsentOrLeave(attendance)) return;
        boolean holidayCredit = applyHolidayCredit(attendance);
        if (ConfiguredJobAttendanceHours.applies(attendance)
                && !isConfiguredWeeklyOffDay(attendance.getEmployee(), attendance.getDate())) {
            ConfiguredJobAttendanceHours.apply(attendance);
        }
        if (holidayCredit) return;
        boolean workedTimesheet = "TIMESHEET_SYNC".equals(attendance.getMethod())
                && ((attendance.getMinimumHour() > 0)
                    || (attendance.getOvertime() != null && attendance.getOvertime() > 0));
        if (!workedTimesheet && attendance.getCheckIn() == null && attendance.getCheckOut() == null
                && (isConfiguredWeeklyOffDay(attendance.getEmployee(), attendance.getDate())
                    || "Weekend Present".equals(attendance.getStatus())
                    || "Special Weekend Present".equals(attendance.getStatus()))) {
            attendance.setStatus("Off Day");
            attendance.setMinimumHour(0);
            attendance.setOvertime(0.0);
            return;
        }
        if (attendance.getCheckIn() == null) return;
        String status = attendance.getStatus();
        if (status != null && !Set.of("present", "late", "half day", "absent").contains(status.toLowerCase(Locale.ROOT))) return;
        LocalTime start = LocalTime.parse(getCategoryWorkStartTime(attendance.getEmployee()));
        attendance.setStatus(attendance.getCheckIn().isAfter(start) ? "Late"
                : "Present");
    }

    private boolean applyHolidayCredit(Attendance attendance) {
        String status = attendance.getStatus() == null
                ? ""
                : attendance.getStatus().toLowerCase(Locale.ROOT);

        // Preserve leave/absence and other non-attendance statuses on holidays.
        if (!Set.of(
                "present",
                "late",
                "half day",
                "holiday present",
                "holiday",
                "hp",
                "h"
        ).contains(status)) {
            return false;
        }

        // This special rule applies only to configured holidays.
        if (attendance.getDate() == null
                || attendanceRepository.countHolidaysOnDate(attendance.getDate()) == 0) {
            return false;
        }

        boolean hasCheckIn = attendance.getCheckIn() != null;
        boolean hasCheckOut = attendance.getCheckOut() != null;
        boolean worked = hasCheckIn && hasCheckOut;

        // Holiday not worked.
        if (!worked) {
            // Do not manufacture hours from an incomplete punch.
            if (hasCheckIn || hasCheckOut) {
                return false;
            }

            // Preserve explicit HP from timesheet sync where the sync has no punches.
            if ("TIMESHEET_SYNC".equals(attendance.getMethod())
                    && Set.of("holiday present", "hp").contains(status)) {
                return true;
            }

            double standardHours =
                    getEmployeeStandardHours(attendance.getEmployee());

            attendance.setStatus("Holiday");
            attendance.setMinimumHour(standardHours);
            attendance.setStandardHours(standardHours);
            attendance.setOvertime(0.0);
            return true;
        }

        /*
         * HOLIDAY PRESENT (HP)
         *
         * Attendance stores physical hours only:
         *
         * Worked = Check Out - Check In
         * NH     = min(Worked, Standard Hours)
         * Raw OT = max(Worked - Standard Hours, 0)
         *
         * Example:
         * 07:00 - 17:00 = 10 worked
         * Standard      = 8
         * NH            = 8
         * Raw OT        = 2
         *
         * Multipliers are applied later by OvertimeService.
         */
        double workedHours =
                calculateMinutesBetweenTimes(
                        attendance.getCheckIn(),
                        attendance.getCheckOut()
                ) / 60.0;

        double standardHours =
                getEmployeeStandardHours(attendance.getEmployee());

        double normalHours =
                Math.min(workedHours, standardHours);

        double rawOvertimeHours =
                Math.max(0.0, workedHours - standardHours);

        var code = holidayCodeSettingsService.getSettings("HP");

        // If HP overtime is explicitly disabled, keep only the worked NH.
        boolean allowOT =
                code == null || Boolean.TRUE.equals(code.getAllowOT());

        if (!allowOT) {
            rawOvertimeHours = 0.0;
        }

        attendance.setStatus("Holiday Present");
        attendance.setMinimumHour(normalHours);
        attendance.setStandardHours(standardHours);
        attendance.setOvertime(rawOvertimeHours);

        return true;
    }

    @Transactional
    public Attendance createAttendance(Attendance attendance) {

        // Validate employee exists

        Employee employee = employeeRepository.findById(attendance.getEmployee().getId())

                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));



        attendance.setEmployee(employee);

        // Calculate minimum hours if check-in and check-out are provided

        if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {

            calculateAndSetMinimumHours(attendance);

        }



        attendance.setEmployee(employee);
        // A late arrival replaces the placeholder instead of creating a second daily record.
        if (attendance.getId() == null && attendance.getDate() != null && attendance.getCheckIn() != null) {
            attendanceRepository.findByEmployeeIdAndDate(employee.getId(), attendance.getDate())
                    .filter(existing -> existing.getCheckIn() == null && existing.getCheckOut() == null
                            && (("Absent".equals(existing.getStatus()) && existing.getMinimumHour() == 0)
                            || "AUTO_HOLIDAY".equals(existing.getMethod())))
                    .ifPresent(existing -> { attendance.setId(existing.getId()); attendance.setVersion(existing.getVersion()); });
        }

        applyCheckInStatus(attendance);
        Attendance savedAttendance = attendanceRepository.save(attendance);



        // Create overtime record if applicable

        createOvertimeIfApplicable(savedAttendance);

        return savedAttendance;

    }



    public Attendance updateAttendance(Long id, Attendance attendanceDetails) {

        Attendance attendance = getAttendanceById(id);
        // Recalculation follows the employee's current project assignment.
        if (attendance.getEmployee() != null && attendance.getEmployee().getId() != null) {
            attendance.setEmployee(employeeRepository.findById(attendance.getEmployee().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found")));
        }



        // Update basic fields

        attendance.setShift(attendanceDetails.getShift());

        attendance.setWorkType(attendanceDetails.getWorkType());

        attendance.setDate(attendanceDetails.getDate());

        attendance.setStatus(attendanceDetails.getStatus());



        if (attendanceDetails.getCheckIn() != null) attendance.setCheckIn(attendanceDetails.getCheckIn());
        if (attendanceDetails.getCheckOut() != null) attendance.setCheckOut(attendanceDetails.getCheckOut());
        // Handle time changes and recalculate hours if times changed

        if (!isAbsentOrLeave(attendance) && attendance.getCheckIn() != null && attendance.getCheckOut() != null) {
            calculateAndSetMinimumHours(attendance);
        }



        applyCheckInStatus(attendance);
        Attendance savedAttendance = attendanceRepository.save(attendance);



        // Update overtime record if applicable

        createOvertimeIfApplicable(savedAttendance);

        return savedAttendance;

    }



    private void calculateAndSetMinimumHours(Attendance attendance) {
        LocalTime checkIn = attendance.getCheckIn();
        LocalTime checkOut = attendance.getCheckOut();
        Employee employee = attendance.getEmployee();

        // If this is one of the employee's configured weekly off-days and the
        // employee category says weekend/off-day attendance must be overtime,
        // ALL worked hours become raw overtime. No multiplier is applied here.
        // OvertimeService remains the only place that applies the multiplier
        // coming from backend SystemSettings / Job configuration.
        if (shouldTreatOffDayAttendanceAsOvertime(attendance)) {
            double workedHours = calculateMinutesBetweenTimes(checkIn, checkOut) / 60.0;
            attendance.setMinimumHour(0.0);
            attendance.setStandardHours(getEmployeeStandardHours(employee));
            attendance.setOvertime(Math.max(0.0, workedHours));
            return;
        }

        if (applyJobScheduleHours(attendance)) return;



        // Get fixed work start time

        String workStartTimeStr = getCategoryWorkStartTime(employee);

        LocalTime workStartTime = LocalTime.parse(workStartTimeStr);



        // Get standard hours

        double standardHours = getEmployeeStandardHours(employee);



        if (checkOut.isBefore(checkIn)) {
            double hours = calculateMinutesBetweenTimes(checkIn, checkOut) / 60.0;
            attendance.setMinimumHour(Math.min(hours, standardHours));
            attendance.setStandardHours(standardHours);
            attendance.setOvertime(Math.max(0, hours - standardHours));
            return;
        }

        // Calculate window end time

        LocalTime windowEndTime = workStartTime.plusMinutes((long)(standardHours * 60));



        // Calculate REGULAR HOURS (overlap with fixed window)

        double regularHours = 0.0;



        LocalTime overlapStart = checkIn.isBefore(workStartTime) ? workStartTime : checkIn;

        LocalTime overlapEnd = checkOut.isAfter(windowEndTime) ? windowEndTime : checkOut;



        if (overlapStart.isBefore(overlapEnd)) {

            long minutes;

            if (overlapEnd.isBefore(overlapStart)) {

                minutes = ChronoUnit.MINUTES.between(overlapStart, LocalTime.MIDNIGHT) +

                        ChronoUnit.MINUTES.between(LocalTime.MIDNIGHT, overlapEnd);

            } else {

                minutes = ChronoUnit.MINUTES.between(overlapStart, overlapEnd);

            }

            regularHours = minutes / 60.0;

        }



        regularHours = Math.min(regularHours, standardHours);



        // Calculate OVERTIME HOURS

        double overtimeHours = 0.0;



        // Overtime before work start

        if (checkIn.isBefore(workStartTime)) {

            long minutesBefore;

            if (checkOut.isBefore(workStartTime)) {

                minutesBefore = calculateMinutesBetweenTimes(checkIn, checkOut);

            } else {

                minutesBefore = calculateMinutesBetweenTimes(checkIn, workStartTime);

            }

            overtimeHours += minutesBefore / 60.0;

        }



        // Overtime after window end

        if (checkOut.isAfter(windowEndTime)) {

            long minutesAfter;

            if (checkIn.isAfter(windowEndTime)) {

                minutesAfter = calculateMinutesBetweenTimes(checkIn, checkOut);

            } else {

                minutesAfter = calculateMinutesBetweenTimes(windowEndTime, checkOut);

            }

            overtimeHours += minutesAfter / 60.0;

        }



        attendance.setMinimumHour(regularHours);

        attendance.setStandardHours(standardHours);

        attendance.setOvertime(overtimeHours);

    }



    /**
     * Returns true when attendance on this date must be treated entirely as
     * overtime. The decision is based on the employee/category configured
     * weekly off-days and Category.weekendAttendanceAsOvertime.
     *
     * Day numbering follows the rest of this project:
     * Sunday = 0, Monday = 1, ... Saturday = 6.
     */
    private boolean shouldTreatOffDayAttendanceAsOvertime(Attendance attendance) {
        if (attendance == null
                || attendance.getEmployee() == null
                || attendance.getDate() == null
                || attendance.getCheckIn() == null
                || attendance.getCheckOut() == null) {
            return false;
        }

        Employee employee = attendance.getEmployee();
        Category category = employee.getCategory();

        if (category == null || !Boolean.TRUE.equals(category.getWeekendAttendanceAsOvertime())) {
            return false;
        }

        return isConfiguredWeeklyOffDay(employee, attendance.getDate());
    }

    /**
     * Resolve weekly off-days in this order:
     * 1. Employee-specific weeklyOffDays
     * 2. Category weeklyOffDays
     * 3. System Settings weekendDays
     */
    private boolean isConfiguredWeeklyOffDay(Employee employee, LocalDate date) {
        if (employee == null || date == null) return false;

        String offDays = employee.getWeeklyOffDays();
        Category category = employee.getCategory();

        if (offDays == null || "INHERIT".equalsIgnoreCase(offDays)) {
            offDays = category != null ? category.getWeeklyOffDays() : null;
        }

        if (offDays == null || "INHERIT".equalsIgnoreCase(offDays)) {
            SystemSettings settings = systemSettingsRepository.findAll().stream().findFirst().orElse(null);
            offDays = settings != null ? settings.getWeekendDays() : null;
        }

        if (offDays == null || offDays.isBlank()) return false;

        int configuredDay = date.getDayOfWeek().getValue() % 7;
        return Arrays.stream(offDays.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .anyMatch(value -> value.equals(String.valueOf(configuredDay)));
    }

    private boolean hasJobSchedule(Employee employee) {
        return employee != null && employee.getJob() != null
                && employee.getJob().getWorkStartTime() != null;
    }

    private boolean applyJobScheduleHours(Attendance attendance) {
        Employee employee = attendance.getEmployee();
        if (!hasJobSchedule(employee) || attendance.getCheckIn() == null || attendance.getCheckOut() == null) return false;
        double standardHours = getEmployeeStandardHours(employee);
        JobScheduleHours.Result hours = JobScheduleHours.calculate(employee.getJob().getWorkStartTime(),
                standardHours, attendance.getCheckIn(), attendance.getCheckOut());
        attendance.setMinimumHour(hours.normalHours());
        attendance.setOvertime(hours.overtimeHours());
        attendance.setStandardHours(standardHours);
        return true;
    }

    private double getEmployeeStandardHours(Employee employee) {
        if (hasJobSchedule(employee) && employee.getJob().getStandardWorkHours() != null) {
            return employee.getJob().getStandardWorkHours().doubleValue();
        }

        if (employee == null) return 8.0;



        Category category = employee.getCategory();

        if (category == null) return 8.0;



        Integer hours = category.getStandardRateHours();

        return hours != null ? hours.doubleValue() : 8.0;

    }





    @Transactional

    public void deleteAttendance(Long id) {

        Attendance attendance = attendanceRepository.findById(id)

                .orElseThrow(() ->

                        new ResourceNotFoundException(

                                "Attendance not found with id: " + id

                        )

                );



        // Delete the overtime linked to this attendance first

        overtimeRepository.deleteByAttendanceId(id);



        // Delete the attendance afterward

        attendanceRepository.delete(attendance);

    }



    public List<Employee> getAvailableEmployeesForDate(LocalDate date) {

        // ONLY get active employees

        List<Employee> activeEmployees = employeeRepository.findByActiveTrue();



        // Get employees with attendance on this date

        List<Long> employeeIdsWithAttendance = attendanceRepository.findByDate(date)

                .stream()

                .filter(a -> !"AUTO_HOLIDAY".equals(a.getMethod()) || a.getCheckIn() != null || a.getCheckOut() != null)
                .map(a -> a.getEmployee().getId())

                .collect(Collectors.toList());



        // Filter out employees with existing attendance

        return activeEmployees.stream()

                .filter(e -> !employeeIdsWithAttendance.contains(e.getId()))

                .collect(Collectors.toList());

    }



    @Transactional

    public List<Overview> validateAttendances(List<Long> attendanceIds) {

        List<Attendance> attendances = attendanceRepository.findAllById(attendanceIds);

        if (attendances.isEmpty()) {

            throw new IllegalArgumentException("No attendance records selected");

        }

        return attendances.stream().map(attendance -> {

            // First check if an overview already exists for this employee and date

            Optional<Overview> existingOverview = overviewRepository.findByEmployeeIdAndDate(

                    attendance.getEmployee().getId(),

                    attendance.getDate()

            );

            if (existingOverview.isPresent()) {

                // Update existing overview with attendance info

                Overview overview = existingOverview.get();

                overview.setAttendance(attendance);

                overview.setHoursWorked(attendance.getMinimumHour());

                overview.setMinimumHour(attendance.getMinimumHour());

                overview.setStatus(attendance.getStatus());

                overview.setShift(attendance.getShift());

                return overviewRepository.save(overview);

            } else {

                // Create new overview

                Overview overview = getOverview(attendance);

                return overviewRepository.save(overview);

            }

        }).collect(Collectors.toList());

    }



    private static Overview getOverview(Attendance attendance) {

        Overview overview = new Overview();

        overview.setAttendance(attendance);

        overview.setEmployee(attendance.getEmployee());

        overview.setDate(attendance.getDate());

        overview.setHoursWorked(attendance.getMinimumHour());

        overview.setMinimumHour(attendance.getMinimumHour());

        overview.setStatus(attendance.getStatus());

        overview.setShift(attendance.getShift());

        overview.setVersion(0);

        return overview;

    }



    public List<Attendance> getAttendancesByDate(LocalDate date) {

        return attendanceRepository.findByDate(date);

    }



    @Transactional

    public Attendance processMobileAttendance(Attendance mobileAttendance) {

        // ONLY allow active employees

        Employee employee = employeeRepository.findByIdAndActiveTrue(mobileAttendance.getEmployee().getId())

                .orElseThrow(() -> new ResourceNotFoundException(

                        "Active employee not found with ID: " + mobileAttendance.getEmployee().getId()));



        if (mobileAttendance.getCheckIn() == null && mobileAttendance.getCheckOut() != null) {
            mobileAttendance.setDate(shiftDateForEvent(employee, mobileAttendance.getDate(), mobileAttendance.getCheckOut(), true));
        }
        // Check for existing record

        Optional<Attendance> existingRecord = attendanceRepository.findByEmployeeIdAndDate(

                employee.getId(),

                mobileAttendance.getDate()

        );



        Attendance attendance;

        if (existingRecord.isPresent()) {

            attendance = existingRecord.get();
            if ("AUTO_HOLIDAY".equals(attendance.getMethod())
                    && (mobileAttendance.getCheckIn() != null || mobileAttendance.getCheckOut() != null)) {
                attendance.setMinimumHour(0.0);
                attendance.setOvertime(0.0);
                attendance.setMethod("MOBILE");
                attendance.setStatus("Present");
            }

            if (attendance.getCheckIn() == null && mobileAttendance.getCheckIn() != null) {
                attendance.setCheckIn(mobileAttendance.getCheckIn());
            }



            if (mobileAttendance.getCheckOut() != null) {

                attendance.setCheckOut(mobileAttendance.getCheckOut());

                // After setting check-out, check if we now have both

                if (attendance.getCheckIn() != null) {

                    attendance.setStatus("Present");

                } else {

                    attendance.setStatus("Half Day");

                }

            }

        } else {

            if (mobileAttendance.getCheckIn() == null) {

                throw new IllegalStateException("First record must be CHECK_IN");

            }

            attendance = new Attendance();

            attendance.setEmployee(employee);

            attendance.setDate(mobileAttendance.getDate());

            attendance.setCheckIn(mobileAttendance.getCheckIn());

            attendance.setStatus("Half Day"); // Only check-in = Half Day

            attendance.setShift("Day");

        }



        // Set standard hours

        double standardHours = getEmployeeStandardHours(employee);

        attendance.setStandardHours(standardHours);



        // Calculate working hours if we have both

        calculateWorkingHours(attendance);



        applyCheckInStatus(attendance);
        Attendance savedAttendance = attendanceRepository.save(attendance);

        createOvertimeIfApplicable(savedAttendance);

        return savedAttendance;

    }



    private void calculateWorkingHours(Attendance attendance) {
        if (applyJobScheduleHours(attendance)) return;

        if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {

            Employee employee = attendance.getEmployee();



            // Get fixed work start time (e.g., 08:00)

            String workStartTimeStr = getCategoryWorkStartTime(employee);

            LocalTime workStartTime = LocalTime.parse(workStartTimeStr);



            // Get standard hours (e.g., 8 hours)

            double standardHours = getEmployeeStandardHours(employee);



            // Calculate the fixed window end time (workStartTime + standardHours)

            LocalTime windowEndTime = workStartTime.plusMinutes((long)(standardHours * 60));



            LocalTime checkIn = attendance.getCheckIn();

            LocalTime checkOut = attendance.getCheckOut();



            // Check if this is an overnight shift (check-out is before check-in)

            boolean isOvernight = checkOut.isBefore(checkIn) || checkOut.equals(checkIn);



            double regularHours = 0.0;

            double overtimeHours = 0.0;



            if (isOvernight) {

                // Handle overnight shift (e.g., 18:00 to 06:00 next day)



                // Calculate total hours using the overnight-aware method

                long totalMinutes = calculateMinutesBetweenTimes(checkIn, checkOut);

                double totalHours = totalMinutes / 60.0;



                // For overnight shifts, the "fixed window" concept may not apply directly

                // Option 1: Treat all hours as regular hours up to standardHours

                if (totalHours <= standardHours) {

                    regularHours = totalHours;

                    overtimeHours = 0.0;

                } else {

                    regularHours = standardHours;

                    overtimeHours = totalHours - standardHours;

                }



                // Option 2: Alternative logic - split by the fixed window crossing midnight

                // This depends on your business requirements for night shifts

                // For example, if workStartTime is 22:00 (10 PM) for night shifts,

                // you can implement custom logic here



            } else {

                // Handle normal day shift (same day)



                // Calculate REGULAR HOURS (overlap with fixed window)

                LocalTime overlapStart = checkIn.isBefore(workStartTime) ? workStartTime : checkIn;

                LocalTime overlapEnd = checkOut.isAfter(windowEndTime) ? windowEndTime : checkOut;



                if (overlapStart.isBefore(overlapEnd)) {

                    long minutes = ChronoUnit.MINUTES.between(overlapStart, overlapEnd);

                    regularHours = minutes / 60.0;

                }



                // Cap regular hours at standardHours

                regularHours = Math.min(regularHours, standardHours);



                // Calculate OVERTIME HOURS before work start

                if (checkIn.isBefore(workStartTime)) {

                    long minutesBefore;

                    if (checkOut.isBefore(workStartTime)) {

                        minutesBefore = calculateMinutesBetweenTimes(checkIn, checkOut);

                    } else {

                        minutesBefore = calculateMinutesBetweenTimes(checkIn, workStartTime);

                    }

                    overtimeHours += minutesBefore / 60.0;

                }



                // Calculate OVERTIME HOURS after window end

                if (checkOut.isAfter(windowEndTime)) {

                    long minutesAfter;

                    if (checkIn.isAfter(windowEndTime)) {

                        minutesAfter = calculateMinutesBetweenTimes(checkIn, checkOut);

                    } else {

                        minutesAfter = calculateMinutesBetweenTimes(windowEndTime, checkOut);

                    }

                    overtimeHours += minutesAfter / 60.0;

                }

            }



            // Set the values

            attendance.setMinimumHour(regularHours);  // Regular hours

            attendance.setOvertime(overtimeHours);     // Overtime hours

            attendance.setStandardHours(standardHours);

        }

    }



    public String getCategoryWorkStartTime(Employee employee) {
        if (hasJobSchedule(employee)) return employee.getJob().getWorkStartTime().toString();

        if (employee == null || employee.getCategory() == null) {

            // Fallback to system default

            SystemSettings settings = systemSettingsRepository.findFirstByOrderByIdAsc()

                    .orElse(new SystemSettings());

            return settings.getLateArrivalTime() != null ? settings.getLateArrivalTime() : "09:00";

        }



        Category category = employee.getCategory();

        if (category.getWorkStartTime() != null && !category.getWorkStartTime().isEmpty()) {

            return category.getWorkStartTime();

        }



        // Fallback to category's standard hours? No, that's hours count, not start time

        // Better to use system default

        SystemSettings settings = systemSettingsRepository.findFirstByOrderByIdAsc()

                .orElse(new SystemSettings());

        return settings.getLateArrivalTime() != null ? settings.getLateArrivalTime() : "09:00";

    }



    public Optional<Attendance> getMostRecentBiometric(Long employeeId) {

        return attendanceRepository.findFirstByEmployeeIdAndBiometricTrueOrderByTimestampDesc(employeeId);

    }



    public Employee getEmployeeById(Long id) {

        return employeeRepository.findById(id)

                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

    }



    @Transactional

    public List<Attendance> importAttendances(List<AttendanceImportDTO> dtos) {

        return dtos.stream().map(dto -> {

            Employee employee = employeeRepository.findById(dto.getEmployeeId())

                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + dto.getEmployeeId()));



            Attendance attendance = new Attendance();

            attendance.setEmployee(employee);

            attendance.setDate(dto.getDate());

            attendance.setCheckIn(dto.getCheckIn());

            attendance.setCheckOut(dto.getCheckOut());

            attendance.setMinimumHour(dto.getMinimumHour());
            boolean usedJobSchedule = applyJobScheduleHours(attendance);

            attendance.setShift(dto.getShift());

            attendance.setStatus(dto.getStatus());

            attendance.setWorkType(dto.getWorkType());



            applyCheckInStatus(attendance);
            Attendance savedAttendance = attendanceRepository.save(attendance);

            // Create overtime if applicable

            if (savedAttendance.getCheckIn() != null && savedAttendance.getCheckOut() != null) {

                double standardHours = getEmployeeStandardHours(employee);

                if (usedJobSchedule) {
                    createOvertimeIfApplicable(savedAttendance);
                } else if (savedAttendance.getMinimumHour() > standardHours) {

                    savedAttendance.setOvertime(savedAttendance.getMinimumHour() - standardHours);

                    createOvertimeIfApplicable(savedAttendance);

                }

            }

            return savedAttendance;

        }).collect(Collectors.toList());

    }



    private LocalDate biometricShiftDate(BiometricRecordDTO record) {
        Employee employee = employeeRepository.findByIdAndActiveTrue(record.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Active employee not found"));
        return shiftDateForEvent(employee, record.getTimestamp().toLocalDate(),
                record.getTimestamp().toLocalTime(), "SIGN OFF".equalsIgnoreCase(record.getAction()));
    }

    private LocalDate shiftDateForEvent(Employee employee, LocalDate date, LocalTime time, boolean checkout) {
        if (date == null || time == null || employee.getCategory() == null
                || !Boolean.TRUE.equals(employee.getCategory().getOvernightShift())) return date;
        LocalTime start = LocalTime.parse(getCategoryWorkStartTime(employee));
        // Morning departures belong to the shift that started the previous evening.
        if (checkout && time.isBefore(start)) return date.minusDays(1);
        return date;
    }

    @Transactional
    public List<Attendance> processBiometricRecords(List<BiometricRecordDTO> records) {

        return records.stream()

                .collect(Collectors.groupingBy(

                        r -> new EmployeeDateKey(r.getEmployeeId(), biometricShiftDate(r))

                ))

                .entrySet()

                .stream()

                .map(entry -> {

                    EmployeeDateKey key = entry.getKey();

                    List<BiometricRecordDTO> dayRecords = entry.getValue();



                    // ONLY allow active employees

                    Employee employee = employeeRepository.findByIdAndActiveTrue(key.employeeId())

                            .orElseThrow(() -> new ResourceNotFoundException(

                                    "Active employee not found with id: " + key.employeeId()));



                    Attendance attendance = attendanceRepository

                            .findByEmployeeIdAndDate(key.employeeId(), key.date())

                            .orElseGet(() -> {

                                Attendance a = new Attendance();

                                a.setEmployee(employee);

                                a.setDate(key.date());

                                a.setBiometric(true);
                                a.setShift(employee.getCategory() != null && Boolean.TRUE.equals(employee.getCategory().getOvernightShift()) ? "Night" : "Day");

                                a.setStatus("Half Day");

                                return a;

                            });



                    if ("AUTO_HOLIDAY".equals(attendance.getMethod())) {
                        attendance.setMinimumHour(0.0);
                        attendance.setOvertime(0.0);
                        attendance.setMethod("BIOMETRIC");
                        attendance.setBiometric(true);
                    }
                    // Apply biometric actions from this request

                    for (BiometricRecordDTO record : dayRecords) {

                        if ("SIGN ON".equalsIgnoreCase(record.getAction())) {

                            attendance.setCheckIn(record.getTimestamp().toLocalTime());

                        } else if ("SIGN OFF".equalsIgnoreCase(record.getAction())) {

                            attendance.setCheckOut(record.getTimestamp().toLocalTime());

                        }

                    }



                    boolean hasCheckIn = attendance.getCheckIn() != null;

                    boolean hasCheckOut = attendance.getCheckOut() != null;



                    String workStartTimeStr = getCategoryWorkStartTime(employee);

                    LocalTime workStartTime = LocalTime.parse(workStartTimeStr);



                    // Determine status

                    if (hasCheckIn && hasCheckOut) {

                        if (attendance.getCheckIn().isAfter(workStartTime)) {

                            attendance.setStatus("Late");

                        } else {

                            attendance.setStatus("Present");

                        }

                    } else if (hasCheckIn || hasCheckOut) {

                        attendance.setStatus("Half Day");

                    } else {

                        attendance.setStatus("Absent");

                    }



                    // Calculate standard hours for this employee

                    double standardHours = getEmployeeStandardHours(employee);

                    attendance.setStandardHours(standardHours);



                    // FIXED: Calculate hours using the overnight-aware method

                    if (hasCheckIn && hasCheckOut && !applyJobScheduleHours(attendance)) {

                        // Use calculateMinutesBetweenTimes instead of ChronoUnit.MINUTES.between

                        long minutes = calculateMinutesBetweenTimes(

                                attendance.getCheckIn(),

                                attendance.getCheckOut());

                        double hours = minutes / 60.0;

                        attendance.setMinimumHour(hours);



                        if (hours > standardHours) {

                            attendance.setOvertime(hours - standardHours);

                        } else {

                            attendance.setOvertime(0.0);

                        }

                    }



                    applyCheckInStatus(attendance);
                    Attendance savedAttendance = attendanceRepository.save(attendance);



                    if (savedAttendance.getOvertime() != null && savedAttendance.getOvertime() > 0) {

                        createOvertimeFromAttendance(savedAttendance);

                    }

                    return savedAttendance;

                })

                .collect(Collectors.toList());

    }



    // Helper record for grouping record

    record EmployeeDateKey(Long employeeId, LocalDate date) {}



    public List<Attendance> getTodaysIncompleteAttendances() {

        return attendanceRepository.findIncompleteAttendances(LocalDate.now());

    }



    @Transactional

    public void createAbsentRecordsForToday() {
        markAbsencesAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Africa/Accra")));
    }

    void markAbsencesAt(java.time.LocalDateTime now) {
        var settings = scheduleRepository.findFirstByOrderByIdAsc().orElse(null);
        if (settings == null || !settings.isAutoAbsentEnabled()) return;
        Set<Long> skipCategoryIds = getAllCategoryIdsToSkipForAutoAbsent(settings);
        List<Attendance> absentRecords = new ArrayList<>();
        Map<LocalDate, Set<Long>> recorded = new HashMap<>();
        Map<LocalDate, Set<Long>> onLeave = new HashMap<>();
        Map<LocalDate, Boolean> holidayDates = new HashMap<>();
        for (Employee employee : employeeRepository.findByActiveTrue()) {
            var category = employee.getCategory();
            if (category != null && skipCategoryIds.contains(category.getId())) continue;
            LocalTime startTime = LocalTime.parse(getCategoryWorkStartTime(employee));
            boolean overnight = category != null && Boolean.TRUE.equals(category.getOvernightShift());
            LocalTime cutoff = category != null && category.getAbsenceCutoffTime() != null
                    ? category.getAbsenceCutoffTime() : settings.getAutoAbsentCutoffTime();
            if (cutoff == null) continue;
            LocalDate shiftDate = overnight && now.toLocalTime().isBefore(startTime)
                    ? now.toLocalDate().minusDays(1) : now.toLocalDate();
            if (holidayDates.computeIfAbsent(shiftDate, date -> attendanceRepository.countHolidaysOnDate(date) > 0)) continue;
            var shiftStart = shiftDate.atTime(startTime);
            var cutoffAt = shiftDate.atTime(cutoff);
            if (overnight && cutoff.isBefore(startTime)) cutoffAt = cutoffAt.plusDays(1);
            if (!now.isAfter(shiftStart) || now.isBefore(cutoffAt)) continue;
            if (employee.getStartDate() != null && employee.getStartDate().isAfter(shiftDate)) continue;
            Set<Long> attendanceIds = recorded.computeIfAbsent(shiftDate, date -> attendanceRepository.findByDate(date)
                    .stream().map(a -> a.getEmployee().getId()).collect(Collectors.toSet()));
            if (attendanceIds.contains(employee.getId())) continue;
            Set<Long> leaveIds = onLeave.computeIfAbsent(shiftDate, date -> leaveRepository
                    .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual("Approved", date)
                    .stream().map(l -> l.getEmployee().getId()).collect(Collectors.toSet()));
            if (leaveIds.contains(employee.getId())) continue;
            Attendance absent = new Attendance();
            boolean offDay = isConfiguredWeeklyOffDay(employee, shiftDate);
            absent.setEmployee(employee); absent.setDate(shiftDate); absent.setStatus(offDay ? "Off Day" : "Absent");
            absent.setShift(overnight ? "Night" : "Day"); absent.setWorkType("Regular");
            absent.setMinimumHour(0); absent.setOvertime(0.0);
            absent.setStandardHours(getEmployeeStandardHours(employee));
            absent.setBiometric(false); absent.setMethod(offDay ? "AUTO_OFF" : "AUTO_ABSENT");
            absentRecords.add(absent); attendanceIds.add(employee.getId());
        }
        if (!absentRecords.isEmpty()) attendanceRepository.saveAll(absentRecords);
    }

    // Helper method to get all category IDs to skip

    private Set<Long> getAllCategoryIdsToSkipForAutoAbsent(AttendanceReportScheduleSettings settings) {

        Set<Long> skipCategoryIds = new HashSet<>();



        // 1. Get categories with skipAutoAbsent flag = true

        List<Category> flaggedSkipCategories = categorySettingsRepository.findBySkipAutoAbsentTrue();

        for (Category category : flaggedSkipCategories) {

            skipCategoryIds.add(category.getId());

        }



        // 2. Get global skip categories from schedule settings

        if (settings != null && settings.getAutoAbsentSkipCategories() != null) {

            skipCategoryIds.addAll(settings.getAutoAbsentSkipCategories());

        }



        return skipCategoryIds;

    }



    public boolean hasCheckedIn(Long employeeId, LocalDate date) {

        return attendanceRepository.existsByEmployeeIdAndDateAndCheckInIsNotNull(employeeId, date);

    }



    public boolean hasCheckedOut(Long employeeId, LocalDate date) {

        return attendanceRepository.existsByEmployeeIdAndDateAndCheckOutIsNotNull(employeeId, date);

    }



    public List<Attendance> getAllBiometricRecords() {

        return attendanceRepository.findByBiometricTrue();

    }



    public Map<Employee, List<Attendance>> getAttendancesForPeriod(LocalDate startDate, LocalDate endDate) {

        List<Attendance> attendances = attendanceRepository.findByDateBetween(startDate, endDate);

        return attendances.stream()

                .collect(Collectors.groupingBy(Attendance::getEmployee));

    }



    public List<Attendance> getRecentAttendances(LocalDate startDate, LocalDate endDate) {

        return attendanceRepository.findByDateBetweenOrderByDateDesc(startDate, endDate);

    }



    @Transactional

    public List<Attendance> createBatchAttendances(List<Attendance> attendances) {
        return createBatchAttendances(attendances, false);
    }

    @Transactional
    public List<Attendance> createBatchAttendances(List<Attendance> attendances, boolean overwriteExisting) {

        List<Attendance> saved = new ArrayList<>();



        for (Attendance attendance : attendances) {

            if (attendance.getEmployee() == null || attendance.getEmployee().getId() == null) {

                throw new IllegalArgumentException("Employee ID is required");

            }

            if (attendance.getDate() == null) {

                throw new IllegalArgumentException("Date is required");

            }



            // ONLY allow active employees

            Employee employee = employeeRepository.findByIdAndActiveTrue(attendance.getEmployee().getId())

                    .orElseThrow(() ->

                            new ResourceNotFoundException("Active employee not found with id: " + attendance.getEmployee().getId()));



            attendance.setEmployee(employee);

            attendance.setStatus(attendance.getStatus() != null ? attendance.getStatus() : "Present");

            attendance.setShift(attendance.getShift() != null ? attendance.getShift() : "Day");

            attendance.setWorkType(attendance.getWorkType() != null ? attendance.getWorkType() : "Regular");



            if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {

                calculateAndSetMinimumHours(attendance);

            }



            boolean exists = attendanceRepository

                    .existsByEmployeeIdAndDate(employee.getId(), attendance.getDate());



            if (exists) {
                var existing = attendanceRepository.findByEmployeeIdAndDate(employee.getId(), attendance.getDate()).orElse(null);
                if (existing == null) continue;
                if (!overwriteExisting && (!"AUTO_HOLIDAY".equals(existing.getMethod())
                        || existing.getCheckIn() != null || existing.getCheckOut() != null
                        || attendance.getCheckIn() == null)) continue;
                attendance.setId(existing.getId());
                attendance.setVersion(existing.getVersion());
            }



            if (overwriteExisting) {
                if (payrollTimesheetRepository != null) {
                    Timesheet sheet = attendance.getTimesheetId() == null
                            ? payrollTimesheetRepository.findByEmployeeIdAndDate(employee.getId(), attendance.getDate()).orElse(null)
                            : payrollTimesheetRepository.findById(attendance.getTimesheetId())
                                    .orElseThrow(() -> new IllegalArgumentException("Timesheet not found"));
                    if (sheet != null) {
                        if (!employee.getId().equals(sheet.getEmployee().getId()) || !attendance.getDate().equals(sheet.getDate())) {
                            throw new IllegalArgumentException("Timesheet must match attendance employee and date");
                        }
                        if (sheet.getJob() != null && !sheet.isPayrollApproved()) {
                            throw new IllegalArgumentException("Project timesheet " + sheet.getId() + " must be approved before attendance sync");
                        }
                        if (sheet.getJob() != null) {
                            attendance.setMinimumHour(sheet.getRegularHours());
                            attendance.setOvertime(sheet.getOvertimeHours());
                            attendance.setCheckIn(null);
                            attendance.setCheckOut(null);
                            String code = sheet.getAttendanceCode() == null ? "P" : sheet.getAttendanceCode();
                            attendance.setStatus(switch (code) {
                                case "A" -> "Absent";
                                case "OFF" -> "Off Day";
                                case "WP" -> "Weekend Present";
                                case "HP" -> "Holiday Present";
                                case "H" -> "Holiday";
                                case "L", "S", "ML", "PL" -> "On Leave";
                                default -> "Present";
                            });
                        }
                        ProjectDailyHoursRule.apply(attendance, sheet.getJob());
                        attendance.setTimesheetApprovalFingerprint(sheet.payrollFingerprint());
                        attendance.setTimesheetId(sheet.getId());
                        attendance.setProjectId(sheet.getJob() == null ? null : sheet.getJob().getId());
                        attendance.setProjectName(sheet.getJob() == null ? null : sheet.getJob().getName());
                    } else {
                        attendance.setTimesheetId(null);
                        attendance.setProjectId(null);
                        attendance.setProjectName(null);
                    }
                }
                attendance.setMethod("TIMESHEET_SYNC");
                attendance.setBiometric(false);
                if (attendance.getOvertime() == null) attendance.setOvertime(0.0);
                if (attendance.getStandardHours() == null) attendance.setStandardHours(getEmployeeStandardHours(employee));
            }
            applyCheckInStatus(attendance);
            if (overwriteExisting && attendance.getTimesheetApprovalFingerprint() != null) {
                Timesheet approvedSheet = payrollTimesheetRepository.findById(attendance.getTimesheetId()).orElse(null);
                if (approvedSheet != null) ProjectDailyHoursRule.apply(attendance, approvedSheet.getJob());
                attendance.setTimesheetApprovedWork(attendance.approvedWorkSignature());
            }
            Attendance savedAttendance = attendanceRepository.save(attendance);

            saved.add(savedAttendance);

            createOvertimeIfApplicable(savedAttendance);

        }



        return saved;

    }







    private void createOvertimeIfApplicable(Attendance attendance) {
        if (attendance == null || attendance.getId() == null) {
            return;
        }

        double rawOvertime =
                attendance.getOvertime() != null
                        ? attendance.getOvertime()
                        : 0.0;

        /*
         * HP must create/synchronize an Overtime record even when Raw OT = 0.
         * Example: NH 8, Raw OT 0, Holiday x2 => Final OT 16.
         */
        boolean holidayPresent =
                "Holiday Present".equalsIgnoreCase(attendance.getStatus());

        if (holidayPresent || rawOvertime > 0) {
            createOvertimeFromAttendance(attendance);
            return;
        }

        // Clear a previously-linked overtime record when this attendance no
        // longer produces overtime.
        overtimeRepository.findByAttendanceId(attendance.getId())
                .ifPresent(overtime -> {
                    overtime.setRawOvertimeHours(0.0);
                    overtime.setOvertimeHours(0.0);
                    overtime.setCalculatedOvertimePay(0.0);
                    overtimeRepository.save(overtime);
                });
    }



    private void createOvertimeFromAttendance(Attendance attendance) {
        try {
            if (attendance == null
                    || attendance.getId() == null
                    || attendance.getEmployee() == null
                    || attendance.getEmployee().getId() == null
                    || attendance.getDate() == null) {
                return;
            }

            double overtimeHours =
                    attendance.getOvertime() != null
                            ? attendance.getOvertime()
                            : 0.0;

            boolean holidayPresent =
                    "Holiday Present".equalsIgnoreCase(attendance.getStatus());

            // Ordinary attendance needs Raw OT > 0. HP is allowed through even
            // with Raw OT = 0 because NH x Holiday Multiplier still contributes.
            if (!holidayPresent && overtimeHours <= 0) {
                return;
            }

            Long employeeId = attendance.getEmployee().getId();
            LocalDate date = attendance.getDate();

            Optional<Overtime> existingOvertime =
                    overtimeRepository.findByAttendanceId(attendance.getId());

            if (existingOvertime.isEmpty()) {
                existingOvertime =
                        overtimeRepository.findFirstByEmployeeIdAndDate(
                                employeeId,
                                date
                        );
            }

            Overtime overtime =
                    existingOvertime.orElseGet(Overtime::new);

            overtime.setEmployee(attendance.getEmployee());
            overtime.setAttendance(attendance);
            overtime.setDate(date);
            // Re-evaluate current settings instead of retaining the old multiplier/treatment.
            overtime.setOvertimeMultiplier(null);
            overtime.setMultiplierAppliedToHours(false);

            // Attendance stores RAW overtime only.
            overtime.setRawOvertimeHours(overtimeHours);

            // Required because OvertimeService validates/calculates this field.
            // For HP, OvertimeService replaces this with:
            // (NH x Holiday Multiplier) + (Raw OT x Default Multiplier).
            overtime.setOvertimeHours(overtimeHours);

            double standardHours =
                    getEmployeeStandardHours(attendance.getEmployee());

            int standardMinutes =
                    (int) Math.round(standardHours * 60);

            boolean allWorkedHoursAreOvertime =
                    shouldTreatOffDayAttendanceAsOvertime(attendance);

            if (allWorkedHoursAreOvertime) {
                overtime.setMultiplierAppliedToHours(true);
            }

            LocalTime overtimeStart;

            if (allWorkedHoursAreOvertime
                    && attendance.getCheckIn() != null) {

                overtimeStart = attendance.getCheckIn();

            } else {

                overtimeStart =
                        attendance.getCheckIn() != null
                                ? attendance.getCheckIn().plusMinutes(standardMinutes)
                                : LocalTime.parse(
                                getCategoryWorkStartTime(
                                        attendance.getEmployee()
                                )
                        ).plusMinutes(standardMinutes);

                if (hasJobSchedule(attendance.getEmployee())
                        && attendance.getCheckIn() != null
                        && attendance.getCheckOut() != null) {

                    overtimeStart =
                            JobScheduleHours.calculate(
                                    attendance.getEmployee()
                                            .getJob()
                                            .getWorkStartTime(),
                                    standardHours,
                                    attendance.getCheckIn(),
                                    attendance.getCheckOut()
                            ).overtimeStart();
                }
            }

            overtime.setStartTime(overtimeStart);
            overtime.setEndTime(attendance.getCheckOut());

            Overtime saved =
                    overtimeService.createOvertime(overtime);

            System.out.println(
                    "Overtime synchronized. Attendance ID: "
                            + attendance.getId()
                            + ", Overtime ID: "
                            + saved.getId()
                            + ", Raw OT: "
                            + overtimeHours
                            + ", Final OT: "
                            + saved.getOvertimeHours()
            );

        } catch (Exception e) {
            System.err.println(
                    "Failed to synchronize overtime for attendance ID "
                            + attendance.getId()
                            + ": "
                            + e.getMessage()
            );
            throw new IllegalStateException("Unable to synchronize overtime for attendance " + attendance.getId(), e);
        }
    }

    private long calculateMinutesBetweenTimes(LocalTime start, LocalTime end) {

        if (end.isBefore(start)) {

            // Overnight shift

            return ChronoUnit.MINUTES.between(start, end) + 24 * 60;

        }

        return ChronoUnit.MINUTES.between(start, end);

    }



    public Optional<Attendance> getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date) {

        return attendanceRepository.findByEmployeeIdAndDate(employeeId, date);

    }



    @Transactional

    public int bulkDeleteAttendances(List<Long> attendanceIds) {

        if (attendanceIds == null || attendanceIds.isEmpty()) {

            throw new IllegalArgumentException("No attendance IDs provided");

        }



        List<Long> uniqueIds = attendanceIds.stream()

                .filter(Objects::nonNull)

                .distinct()

                .toList();



        if (uniqueIds.isEmpty()) {

            throw new IllegalArgumentException("No valid attendance IDs provided");

        }



        List<Attendance> attendances =

                attendanceRepository.findAllById(uniqueIds);



        if (attendances.isEmpty()) {

            throw new ResourceNotFoundException(

                    "No attendance records were found"

            );

        }



        for (Attendance attendance : attendances) {

            Long attendanceId = attendance.getId();



            // Delete overtime linked to this attendance

            overtimeRepository.deleteByAttendanceId(attendanceId);



            // Delete attendance

            attendanceRepository.delete(attendance);

        }



        return attendances.size();

    }



    @Transactional

    public Attendance upsertAttendanceByEmployeeAndDate(Long employeeId, LocalDate date, Attendance details) {

        Employee employee = employeeRepository.findById(employeeId)

                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));



        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, date)

                .orElseGet(() -> {

                    Attendance a = new Attendance();

                    a.setEmployee(employee);

                    a.setDate(date);

                    return a;

                });



        // Update fields (only overwrite what you want to allow)

        attendance.setShift(details.getShift());

        attendance.setWorkType(details.getWorkType());

        attendance.setStatus(details.getStatus());



        // Times + derived hours

        attendance.setCheckIn(details.getCheckIn());

        attendance.setCheckOut(details.getCheckOut());

        if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {

            calculateAndSetMinimumHours(attendance);

        }



        applyCheckInStatus(attendance);
        Attendance saved = attendanceRepository.save(attendance);



        // Overtime sync

        createOvertimeIfApplicable(saved);



        return saved;

    }



    @Transactional

    public List<Attendance> createAttendancesForEmployeesInRange(AttendanceRangeCreateRequest req) {

        if (req.getEmployeeIds() == null || req.getEmployeeIds().isEmpty()) {

            throw new IllegalArgumentException("employeeIds is required");

        }

        if (req.getStartDate() == null || req.getEndDate() == null) {

            throw new IllegalArgumentException("startDate and endDate are required");

        }

        if (req.getEndDate().isBefore(req.getStartDate())) {

            throw new IllegalArgumentException("endDate must be on/after startDate");

        }



        List<Attendance> results = new ArrayList<>();



        for (Long employeeId : req.getEmployeeIds()) {

            Employee employee = employeeRepository.findById(employeeId)

                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));



            LocalDate d = req.getStartDate();

            while (!d.isAfter(req.getEndDate())) {



                Optional<Attendance> existingOpt = attendanceRepository.findByEmployeeIdAndDate(employeeId, d);



                if (existingOpt.isPresent() && !req.isOverwriteExisting()) {

                    d = d.plusDays(1);

                    continue;

                }



                Attendance a = existingOpt.orElseGet(Attendance::new);

                a.setEmployee(employee);

                a.setDate(d);



                a.setStatus(req.getStatus() != null ? req.getStatus() : "Present");

                a.setShift(req.getShift() != null ? req.getShift() : "Day");

                a.setWorkType(req.getWorkType() != null ? req.getWorkType() : "Regular");



                a.setCheckIn(req.getCheckIn());

                a.setCheckOut(req.getCheckOut());



                if (a.getCheckIn() != null && a.getCheckOut() != null) {

                    calculateAndSetMinimumHours(a);

                }



                applyCheckInStatus(a);
                Attendance saved = attendanceRepository.save(a);

                createOvertimeIfApplicable(saved);



                results.add(saved);



                d = d.plusDays(1);

            }

        }



        return results;

    }



}

