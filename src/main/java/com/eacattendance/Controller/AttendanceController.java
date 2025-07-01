package com.eacattendance.Controller;

import com.eacattendance.Service.AttendanceService;
import com.eacattendance.component.BiometricExcelParser;
import com.eacattendance.dto.BiometricRecordDTO;
import com.eacattendance.entity.Attendance;
import com.eacattendance.entity.Employee;
import com.eacattendance.entity.Overview;
import com.eacattendance.repository.AttendanceRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:5173")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final BiometricExcelParser biometricExcelParser;
    private final AttendanceRepository attendanceRepository;

    public AttendanceController(AttendanceService attendanceService, BiometricExcelParser biometricExcelParser, AttendanceRepository attendanceRepository) {
        this.attendanceService = attendanceService;
        this.biometricExcelParser = biometricExcelParser;
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping
    public List<Attendance> getAllAttendances() {
        return attendanceService.getAllAttendances();
    }

    // Only match numeric IDs to avoid conflicts with other routes
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Attendance> getAttendanceById(@PathVariable Long id) {
        return ResponseEntity.ok(attendanceService.getAttendanceById(id));
    }

    @PostMapping
    public ResponseEntity<Attendance> createAttendance(@RequestBody Attendance attendance) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createAttendance(attendance));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Attendance>> createBatchAttendances(@RequestBody List<Attendance> attendances) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createBatchAttendances(attendances));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Attendance> updateAttendance(@PathVariable Long id, @RequestBody Attendance attendance) {
        return ResponseEntity.ok(attendanceService.updateAttendance(id, attendance));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available-employees")
    public ResponseEntity<List<Employee>> getAvailableEmployees(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAvailableEmployeesForDate(date));
    }

    @PostMapping("/insertOverview")
    public ResponseEntity<?> validateAttendances(@RequestBody ValidateAttendanceRequest request) {
        try {
            if (request.getAttendanceIds() == null || request.getAttendanceIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No attendance IDs provided"
                ));
            }

            List<Overview> overviews = attendanceService.validateAttendances(request.getAttendanceIds());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully validated " + overviews.size() + " records",
                    "data", overviews
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error validating attendances: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<Attendance>> getAttendancesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAttendancesByDate(date));
    }

    @GetMapping("/recent-biometric")
    public ResponseEntity<Attendance> getMostRecentBiometricRecord(@RequestParam Long employeeId) {
        return attendanceService.getMostRecentBiometric(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ NEW: For fetching all biometric records (used by BiometricAttendanceFeed.jsx)
    @GetMapping("/biometric-records")
    public ResponseEntity<List<Attendance>> getAllBiometricRecords() {
        return ResponseEntity.ok(attendanceService.getAllBiometricRecords());
    }

    @PostMapping(value = "/import/biometric-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importBiometricFile(@RequestParam("file") MultipartFile file) {
        try {
            List<BiometricRecordDTO> records = biometricExcelParser.parseExcel(file);
            List<Attendance> savedAttendances = attendanceService.processBiometricRecords(records);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", savedAttendances.size(),
                    "data", savedAttendances
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // Request DTO
    static class ValidateAttendanceRequest {
        private List<Long> attendanceIds;

        public List<Long> getAttendanceIds() {
            return attendanceIds;
        }

        public void setAttendanceIds(List<Long> attendanceIds) {
            this.attendanceIds = attendanceIds;
        }
    }

    // Add to AttendanceController.java

    @GetMapping("/recent")
    public ResponseEntity<List<Attendance>> getRecentAttendances() {
        // Get attendances from the last 7 days
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);

        List<Attendance> recentAttendances = attendanceRepository.findByDateBetweenOrderByDateDesc(
                startDate, endDate);

        return ResponseEntity.ok(recentAttendances);
    }
}
