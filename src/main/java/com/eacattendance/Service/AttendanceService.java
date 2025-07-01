package com.eacattendance.Service;

import com.eacattendance.Exceptions.ResourceNotFoundException;
import com.eacattendance.dto.AttendanceImportDTO;
import com.eacattendance.dto.BiometricRecordDTO;
import com.eacattendance.entity.Attendance;
import com.eacattendance.entity.Employee;
import com.eacattendance.entity.Overview;
import com.eacattendance.repository.AttendanceRepository;
import com.eacattendance.repository.EmployeeRepository;
import com.eacattendance.repository.OverviewRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final OverviewRepository overviewRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository,
                             OverviewRepository overviewRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.overviewRepository = overviewRepository;
    }

    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
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
        return attendanceRepository.save(attendance);
    }

    public List<Attendance> createBatchAttendances(List<Attendance> attendances) {
        return attendances.stream()
                .map(this::createAttendance)
                .collect(Collectors.toList());
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

        return attendanceRepository.save(attendance);
    }

    private void calculateAndSetMinimumHours(Attendance attendance) {
        LocalTime checkIn = attendance.getCheckIn();
        LocalTime checkOut = attendance.getCheckOut();

        if (checkOut.isBefore(checkIn)) {
            throw new IllegalArgumentException("Check-out time must be after check-in time");
        }

        // Calculate difference in hours (with minute precision)
        double hours = ChronoUnit.MINUTES.between(checkIn, checkOut) / 60.0;
        attendance.setMinimumHour(hours);
    }

    public void deleteAttendance(Long id) {
        Attendance attendance = getAttendanceById(id);
        attendanceRepository.delete(attendance);
    }

    public List<Employee> getAvailableEmployeesForDate(LocalDate date) {
        // Get all employees
        List<Employee> allEmployees = employeeRepository.findAll();

        // Get employees with attendance on this date
        List<Long> employeeIdsWithAttendance = attendanceRepository.findByDate(date)
                .stream()
                .map(a -> a.getEmployee().getId())
                .collect(Collectors.toList());

        // Filter out employees with existing attendance
        return allEmployees.stream()
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
                Overview overview = new Overview();
                overview.setAttendance(attendance);
                overview.setEmployee(attendance.getEmployee());
                overview.setDate(attendance.getDate());
                overview.setHoursWorked(attendance.getMinimumHour());
                overview.setMinimumHour(attendance.getMinimumHour());
                overview.setStatus(attendance.getStatus());
                overview.setShift(attendance.getShift());
                overview.setVersion(0);
                return overviewRepository.save(overview);
            }
        }).collect(Collectors.toList());
    }

    public List<Attendance> getAttendancesByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    // Add these methods to your existing AttendanceService class

    @Transactional
    public Attendance processMobileAttendance(Attendance mobileAttendance) {
        // Get the full employee object from the ID
        Employee employee = employeeRepository.findById(mobileAttendance.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + mobileAttendance.getEmployee().getId()));

        // Check for existing record
        Optional<Attendance> existingRecord = attendanceRepository.findByEmployeeIdAndDate(
                employee.getId(),
                mobileAttendance.getDate()
        );

        Attendance attendance;
        if (existingRecord.isPresent()) {
            attendance = existingRecord.get();
            // Update check-out if provided
            if (mobileAttendance.getCheckOut() != null) {
                attendance.setCheckOut(mobileAttendance.getCheckOut());
                calculateWorkingHours(attendance);
            }
            // You might want to handle check-in updates here if needed
        } else {
            // Create new record
            if (mobileAttendance.getCheckIn() == null) {
                throw new IllegalStateException("First record must be CHECK_IN");
            }
            attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setDate(mobileAttendance.getDate());
            attendance.setCheckIn(mobileAttendance.getCheckIn());
            attendance.setStatus("Present");
            attendance.setShift("Day"); // Default shift
        }

        return attendanceRepository.save(attendance);
    }

    private void calculateWorkingHours(Attendance attendance) {
        if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {
            long minutes = ChronoUnit.MINUTES.between(attendance.getCheckIn(), attendance.getCheckOut());
            double hours = minutes / 60.0;
            attendance.setMinimumHour(hours);

            // Calculate overtime if applicable
            if (hours > 8.0) {
                attendance.setOvertime(hours - 8.0);
            }
        }
    }
    public Optional<Attendance> getMostRecentBiometric(Long employeeId) {
        return attendanceRepository.findFirstByEmployeeIdAndBiometricTrueOrderByTimestampDesc(employeeId);
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

            return attendanceRepository.save(attendance);
        }).collect(Collectors.toList());
    }

    @Transactional
    public List<Attendance> processBiometricRecords(List<BiometricRecordDTO> records) {
        return records.stream()
                .collect(Collectors.groupingBy(
                        record -> new EmployeeDateKey(record.getEmployeeId(), record.getTimestamp().toLocalDate())
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    EmployeeDateKey key = entry.getKey();
                    List<BiometricRecordDTO> dayRecords = entry.getValue();

                    // Find or create attendance record
                    Attendance attendance = attendanceRepository
                            .findByEmployeeIdAndDate(key.employeeId(), key.date())
                            .orElseGet(() -> {
                                Attendance newAttendance = new Attendance();
                                newAttendance.setEmployee(employeeRepository.findById(key.employeeId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found")));
                                newAttendance.setDate(key.date());
                                newAttendance.setBiometric(true);
                                newAttendance.setStatus("Present");
                                return newAttendance;
                            });

                    // Process all records for this employee/date
                    dayRecords.forEach(record -> {
                        if ("SIGN ON".equalsIgnoreCase(record.getAction())) {
                            attendance.setCheckIn(record.getTimestamp().toLocalTime());
                        } else if ("SIGN OFF".equalsIgnoreCase(record.getAction())) {
                            attendance.setCheckOut(record.getTimestamp().toLocalTime());
                        }
                    });

                    // Calculate hours if both times are present
                    if (attendance.getCheckIn() != null && attendance.getCheckOut() != null) {
                        long minutes = ChronoUnit.MINUTES.between(
                                attendance.getCheckIn(),
                                attendance.getCheckOut()
                        );
                        attendance.setMinimumHour(minutes / 60.0);
                    }

                    return attendanceRepository.save(attendance);
                })
                .collect(Collectors.toList());
    }

    // Helper record for grouping
    record EmployeeDateKey(Long employeeId, LocalDate date) {}



    public List<Attendance> getTodaysIncompleteAttendances() {
        return attendanceRepository.findIncompleteAttendances(LocalDate.now());
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

    // In AttendanceService.java
    public List<Attendance> getRecentAttendances(LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByDateBetweenOrderByDateDesc(startDate, endDate);
    }


}