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



    public Attendance createAttendance(Attendance attendance) {

        // Validate employee exists

        Employee employee = employeeRepository.findById(attendance.getEmployee().getId())

                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));



        // Calculate minimum hours if check-in and check-out are provided

        if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {

            calculateAndSetMinimumHours(attendance);

        }



        attendance.setEmployee(employee);

        Attendance savedAttendance = attendanceRepository.save(attendance);



        // Create overtime record if applicable

        createOvertimeIfApplicable(savedAttendance);

        return savedAttendance;

    }



    public Attendance updateAttendance(Long id, Attendance attendanceDetails) {

        Attendance attendance = getAttendanceById(id);



        // Update basic fields

        attendance.setShift(attendanceDetails.getShift());

        attendance.setWorkType(attendanceDetails.getWorkType());

        attendance.setDate(attendanceDetails.getDate());

        attendance.setStatus(attendanceDetails.getStatus());



        // Handle time changes and recalculate hours if times changed

        if (attendanceDetails.getCheckIn() != null && attendanceDetails.getCheckOut() != null) {

            attendance.setCheckIn(attendanceDetails.getCheckIn());

            attendance.setCheckOut(attendanceDetails.getCheckOut());

            calculateAndSetMinimumHours(attendance);

        }



        Attendance savedAttendance = attendanceRepository.save(attendance);



        // Update overtime record if applicable

        createOvertimeIfApplicable(savedAttendance);

        return savedAttendance;

    }



    private void calculateAndSetMinimumHours(Attendance attendance) {

        LocalTime checkIn = attendance.getCheckIn();

        LocalTime checkOut = attendance.getCheckOut();



        Employee employee = attendance.getEmployee();



        // Get fixed work start time

        String workStartTimeStr = getCategoryWorkStartTime(employee);

        LocalTime workStartTime = LocalTime.parse(workStartTimeStr);



        // Get standard hours

        double standardHours = getEmployeeStandardHours(employee);



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



    private double getEmployeeStandardHours(Employee employee) {

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



        // Check for existing record

        Optional<Attendance> existingRecord = attendanceRepository.findByEmployeeIdAndDate(

                employee.getId(),

                mobileAttendance.getDate()

        );



        Attendance attendance;

        if (existingRecord.isPresent()) {

            attendance = existingRecord.get();

            boolean hadCheckIn = attendance.getCheckIn() != null;

            boolean hadCheckOut = attendance.getCheckOut() != null;



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



        Attendance savedAttendance = attendanceRepository.save(attendance);

        createOvertimeIfApplicable(savedAttendance);

        return savedAttendance;

    }



    private void calculateWorkingHours(Attendance attendance) {

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

            attendance.setShift(dto.getShift());

            attendance.setStatus(dto.getStatus());

            attendance.setWorkType(dto.getWorkType());



            Attendance savedAttendance = attendanceRepository.save(attendance);

            // Create overtime if applicable

            if (savedAttendance.getCheckIn() != null && savedAttendance.getCheckOut() != null) {

                double standardHours = getEmployeeStandardHours(employee);

                if (savedAttendance.getMinimumHour() > standardHours) {

                    savedAttendance.setOvertime(savedAttendance.getMinimumHour() - standardHours);

                    createOvertimeIfApplicable(savedAttendance);

                }

            }

            return savedAttendance;

        }).collect(Collectors.toList());

    }



    @Transactional

    public List<Attendance> processBiometricRecords(List<BiometricRecordDTO> records) {

        return records.stream()

                .collect(Collectors.groupingBy(

                        r -> new EmployeeDateKey(r.getEmployeeId(), r.getTimestamp().toLocalDate())

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

                                a.setStatus("Half Day");

                                return a;

                            });



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

                    if (hasCheckIn && hasCheckOut) {

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



    @Scheduled(cron = "0 0 18 * * *")

    @Transactional

    public void createAbsentRecordsForPreviousDay() {

        LocalDate previousDay = LocalDate.now().minusDays(1);



        // Get schedule settings

        AttendanceReportScheduleSettings settings = scheduleRepository.findFirstByOrderByIdAsc()

                .orElse(null);



        // Check if auto-absent marking is enabled

        if (settings == null || !settings.isAutoAbsentEnabled()) {

            System.out.println("Auto-absent marking is disabled. No records created.");

            return;

        }



        // Get all active employees

        List<Employee> activeEmployees = employeeRepository.findByActiveTrue();



        // Get employees who already have attendance for previous day

        List<Attendance> existingAttendances = attendanceRepository.findByDate(previousDay);

        Set<Long> employeesWithAttendance = existingAttendances.stream()

                .map(a -> a.getEmployee().getId())

                .collect(Collectors.toSet());



        // Get employees on approved leave for previous day

        List<Leave> activeLeaves = leaveRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(

                "Approved", previousDay);

        Set<Long> employeesOnLeave = activeLeaves.stream()

                .map(l -> l.getEmployee().getId())

                .collect(Collectors.toSet());



        // Get categories to skip (from both category flag AND schedule settings)

        Set<Long> skipCategoryIds = getAllCategoryIdsToSkipForAutoAbsent(settings);



        List<Attendance> absentRecords = new ArrayList<>();

        List<String> skippedEmployees = new ArrayList<>();



        for (Employee employee : activeEmployees) {

            // Skip if already has attendance

            if (employeesWithAttendance.contains(employee.getId())) {

                continue;

            }



            // Skip if on approved leave

            if (employeesOnLeave.contains(employee.getId())) {

                continue;

            }



            // Skip if employee's category should be skipped

            if (employee.getCategory() != null &&

                    skipCategoryIds.contains(employee.getCategory().getId())) {

                skippedEmployees.add(employee.getFirstName() + " " + employee.getLastName() +

                        " (" + employee.getCategory().getName() + ")");

                continue;

            }



            // Create absent record

            Attendance absent = new Attendance();

            absent.setEmployee(employee);

            absent.setDate(previousDay);

            absent.setStatus("Absent");

            absent.setShift("Day");

            absent.setWorkType("Regular");

            absent.setMinimumHour(0.0);

            absent.setOvertime(0.0);

            absent.setStandardHours(getEmployeeStandardHours(employee));

            absent.setBiometric(false);



            absentRecords.add(absent);

        }



        if (!absentRecords.isEmpty()) {

            attendanceRepository.saveAll(absentRecords);

            System.out.println("✅ Created " + absentRecords.size() + " absent records for " + previousDay);

        }



        if (!skippedEmployees.isEmpty()) {

            System.out.println("⏭️ Skipped auto-absent for " + skippedEmployees.size() + " employees: " +

                    String.join(", ", skippedEmployees));

        }

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



            if (exists) continue;



            Attendance savedAttendance = attendanceRepository.save(attendance);

            saved.add(savedAttendance);

            createOvertimeIfApplicable(savedAttendance);

        }



        return saved;

    }







    private void createOvertimeIfApplicable(Attendance attendance) {

        if (attendance.getOvertime() != null && attendance.getOvertime() > 0) {

            createOvertimeFromAttendance(attendance);

        }

    }



    private void createOvertimeFromAttendance(Attendance attendance) {

        try {

            if (attendance == null ||

                    attendance.getId() == null ||

                    attendance.getEmployee() == null ||

                    attendance.getEmployee().getId() == null ||

                    attendance.getDate() == null) {

                return;

            }



            Double overtimeHours = attendance.getOvertime();



            if (overtimeHours == null || overtimeHours <= 0) {

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



            Overtime overtime = existingOvertime.orElseGet(Overtime::new);



            overtime.setEmployee(attendance.getEmployee());

            overtime.setAttendance(attendance);

            overtime.setDate(date);

            overtime.setOvertimeHours(overtimeHours);

            overtime.setRawOvertimeHours(overtimeHours);

            overtime.setStatus(

                    overtime.getStatus() != null

                            ? overtime.getStatus()

                            : "Pending"

            );



            String workStartTimeStr =

                    getCategoryWorkStartTime(attendance.getEmployee());



            LocalTime workStartTime =

                    LocalTime.parse(workStartTimeStr);



            double standardHours =

                    getEmployeeStandardHours(attendance.getEmployee());



            int windowEndMinutes =

                    workStartTime.getHour() * 60

                            + workStartTime.getMinute()

                            + (int) Math.round(standardHours * 60);



            LocalTime windowEndTime = LocalTime.of(

                    (windowEndMinutes / 60) % 24,

                    windowEndMinutes % 60

            );



            overtime.setStartTime(windowEndTime);

            overtime.setEndTime(attendance.getCheckOut());



            SystemSettings systemSettings =

                    systemSettingsRepository.findFirstByOrderByIdAsc()

                            .orElse(new SystemSettings());



            double multiplier =

                    systemSettings.getDefaultOvertimeMultiplier() != null

                            ? systemSettings.getDefaultOvertimeMultiplier()

                            : 1.5;



            if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY &&

                    Boolean.TRUE.equals(

                            systemSettings.getDoubleTimeOnSunday()

                    )) {



                multiplier =

                        systemSettings.getSundayOvertimeMultiplier() != null

                                ? systemSettings.getSundayOvertimeMultiplier()

                                : 2.0;

            }



            double hourlyRate =

                    attendance.getEmployee().getMinimumRate() != null

                            ? attendance.getEmployee().getMinimumRate()

                            : systemSettings.getHourlyRate() != null

                            ? systemSettings.getHourlyRate()

                            : 10.0;



            overtime.setBaseHourlyRate(hourlyRate);

            overtime.setOvertimeMultiplier(multiplier);

            overtime.setMultiplierAppliedToHours(false);

            overtime.setCalculatedOvertimePay(

                    hourlyRate * multiplier * overtimeHours

            );



            Overtime saved = overtimeRepository.save(overtime);



            System.out.println(

                    "Overtime synchronized. Attendance ID: "

                            + attendance.getId()

                            + ", Overtime ID: "

                            + saved.getId()

            );



        } catch (Exception e) {

            System.err.println(

                    "Failed to synchronize overtime for attendance ID "

                            + attendance.getId()

                            + ": "

                            + e.getMessage()

            );



            e.printStackTrace();

        }

    }

    private long calculateMinutesBetweenTimes(LocalTime start, LocalTime end) {

        if (end.isBefore(start)) {

            // Overnight shift

            return ChronoUnit.MINUTES.between(start, LocalTime.MIDNIGHT) +

                    ChronoUnit.MINUTES.between(LocalTime.MIDNIGHT, end);

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



                Attendance saved = attendanceRepository.save(a);

                createOvertimeIfApplicable(saved);



                results.add(saved);



                d = d.plusDays(1);

            }

        }



        return results;

    }



}
