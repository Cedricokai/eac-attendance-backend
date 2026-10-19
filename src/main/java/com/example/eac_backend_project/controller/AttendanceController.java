package com.example.eac_backend_project.controller;



import com.example.eac_backend_project.Service.AttendanceService;

import com.example.eac_backend_project.Service.TimesheetService;

import com.example.eac_backend_project.component.BiometricExcelParser;

import com.example.eac_backend_project.dto.AttendanceListDTO;
import com.example.eac_backend_project.dto.AttendanceRangeCreateRequest;
import com.example.eac_backend_project.Service.RawBiometricEventService;
import com.example.eac_backend_project.dto.BiometricRecordDTO;

import com.example.eac_backend_project.entity.attendance.*;

import com.example.eac_backend_project.repository.AttendanceRepository;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;



import java.time.LocalDate;

import java.util.ArrayList;

import java.util.List;

import java.util.Map;

import java.util.stream.Collectors;



@RestController

@RequestMapping("/api/attendance")

@CrossOrigin(origins = "http://localhost:5173")

public class AttendanceController {



    private final AttendanceService attendanceService;

    private final BiometricExcelParser biometricExcelParser;

    private final AttendanceRepository attendanceRepository;

    private final TimesheetService timesheetService;

    private final RawBiometricEventService rawBiometricEventService;

    public AttendanceController(AttendanceService attendanceService, BiometricExcelParser biometricExcelParser, AttendanceRepository attendanceRepository, TimesheetService timesheetService, RawBiometricEventService rawBiometricEventService) {

        this.attendanceService = attendanceService;

        this.biometricExcelParser = biometricExcelParser;

        this.attendanceRepository = attendanceRepository;

        this.timesheetService = timesheetService;

        this.rawBiometricEventService = rawBiometricEventService;

    }

    /** Legacy endpoint retained for compatibility. */
    @GetMapping
    public List<Attendance> getAllAttendances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null && endDate == null) return attendanceService.getAllAttendances();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide a valid startDate and endDate");
        }
        return attendanceService.getRecentAttendances(startDate, endDate);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<AttendanceListDTO>> getAttendancePage(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(defaultValue = "")
            String search,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "100")
            int size
    ) {
        Page<AttendanceListDTO> result =
                attendanceService.getAttendancePage(
                        startDate,
                        endDate,
                        categoryId,
                        search,
                        page,
                        size
                );

        return ResponseEntity.ok(result);
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





    @PutMapping("/{id}")

    public ResponseEntity<Attendance> updateAttendance(@PathVariable Long id, @RequestBody Attendance attendance) {

        return ResponseEntity.ok(attendanceService.updateAttendance(id, attendance));

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
             records = biometricExcelParser.parseExcel(file);
            String rawBatchId = "excel-" + java.util.UUID.randomUUID();
            records.forEach(record -> rawBiometricEventService.captureExcel(record, rawBatchId));
             savedAttendances = attendanceService.processBiometricRecords(records);

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



    @PostMapping("/batch")

    public ResponseEntity<?> createBatchAttendances(@RequestBody List<Attendance> attendances,
            @RequestParam(defaultValue = "false") boolean overwriteExisting) {

        try {

            return ResponseEntity

                    .status(HttpStatus.CREATED)

                    .body(attendanceService.createBatchAttendances(attendances, overwriteExisting));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity

                    .badRequest()

                    .body(Map.of("error", e.getMessage()));

        }

    }





    @PostMapping("/bulk")

    public ResponseEntity<?> createBulkAttendance(@RequestBody Map<String, Object> payload) {

        try {

            // Extract attendance data from payload

            Map<String, Map<String, String>> attendanceData =

                    (Map<String, Map<String, String>>) payload.get("attendance");



            // Extract timesheet data from payload

            List<Map<String, Object>> timesheetsData =

                    (List<Map<String, Object>>) payload.get("timesheets");



            // Process attendance data (you'll need to implement this logic)

            List<Attendance> savedAttendances = processAttendanceData(attendanceData);



            // Process timesheet data (you'll need to implement this logic)

            List<Timesheet> savedTimesheets = processTimesheetData(timesheetsData);



            return ResponseEntity.ok(Map.of(

                    "success", true,

                    "message", "Successfully saved attendance and timesheets",

                    "attendanceCount", savedAttendances.size(),

                    "timesheetCount", savedTimesheets.size()

            ));



        } catch (Exception e) {

            return ResponseEntity.badRequest().body(Map.of(

                    "success", false,

                    "message", "Error processing bulk data: " + e.getMessage()

            ));

        }

    }



    // Helper method to process attendance data

    private List<Attendance> processAttendanceData(Map<String, Map<String, String>> attendanceData) {

        List<Attendance> attendances = new ArrayList<>();



        for (Map.Entry<String, Map<String, String>> employeeEntry : attendanceData.entrySet()) {

            Long employeeId = Long.parseLong(employeeEntry.getKey());

            Map<String, String> dateStatusMap = employeeEntry.getValue();



            for (Map.Entry<String, String> dateEntry : dateStatusMap.entrySet()) {

                LocalDate date = LocalDate.parse(dateEntry.getKey());

                String status = "P".equals(dateEntry.getValue()) ? "Present" : "Absent";



                Attendance attendance = new Attendance();

                attendance.setEmployee(attendanceService.getEmployeeById(employeeId)); // You'll need to add this method

                attendance.setDate(date);

                attendance.setStatus(status);

                attendance.setShift("Day"); // Default shift



                attendances.add(attendance);

            }

        }



        return attendanceService.createBatchAttendances(attendances);

    }



    private List<Timesheet> processTimesheetData(List<Map<String, Object>> timesheetsData) {

        List<Timesheet> timesheets = timesheetsData.stream().map(data -> {

            Timesheet timesheet = new Timesheet();



            Long employeeId = ((Number) data.get("employeeId")).longValue();

            timesheet.setEmployee(attendanceService.getEmployeeById(employeeId));

            timesheet.setDate(LocalDate.parse((String) data.get("date")));

            timesheet.setRegularHours(((Number) data.get("regularHours")).doubleValue());

            timesheet.setOvertimeHours(((Number) data.get("overtimeHours")).doubleValue());

            timesheet.setTotalHours(((Number) data.get("totalHours")).doubleValue());

            timesheet.setEarnings(((Number) data.get("earnings")).doubleValue());

            timesheet.setStatus(TimesheetStatus.PENDING);



            return timesheet;

        }).collect(Collectors.toList());



        return timesheetService.createBatchTimesheets(timesheets);

    }



    public record EmployeeAttendanceRow(Long id, Long employeeId, LocalDate date, java.time.LocalTime checkIn,
            java.time.LocalTime checkOut, double minimumHour, Double overtime, String status, String shift) {}

    @GetMapping("/by-employee-range")
    public ResponseEntity<?> getByEmployeeAndRange(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().body(Map.of("message", "End date must be on or after start date"));
        }
        List<EmployeeAttendanceRow> records = attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate)
                .stream().map(a -> new EmployeeAttendanceRow(a.getId(), employeeId, a.getDate(), a.getCheckIn(),
                        a.getCheckOut(), a.getMinimumHour(), a.getOvertime(), a.getStatus(), a.getShift()))
                .toList();
        return ResponseEntity.ok(records);
    }

    @GetMapping("/by-employee-date")

    public ResponseEntity<Attendance> getByEmployeeAndDate(

            @RequestParam Long employeeId,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date

    ) {

        return attendanceService.getAttendanceByEmployeeAndDate(employeeId, date)

                .map(ResponseEntity::ok)

                .orElse(ResponseEntity.notFound().build());

    }



    @PutMapping("/by-employee-date")

    public ResponseEntity<Attendance> upsertByEmployeeAndDate(

            @RequestParam Long employeeId,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @RequestBody Attendance attendanceDetails

    ) {

        return ResponseEntity.ok(attendanceService.upsertAttendanceByEmployeeAndDate(employeeId, date, attendanceDetails));

    }



    @PostMapping("/range")

    public ResponseEntity<?> createAttendanceRange(@RequestBody AttendanceRangeCreateRequest request) {

        try {

            return ResponseEntity.status(HttpStatus.CREATED)

                    .body(attendanceService.createAttendancesForEmployeesInRange(request));

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        }

    }



    @DeleteMapping("/bulk/delete")

    public ResponseEntity<?> bulkDeleteAttendances(

            @RequestBody BulkDeleteAttendanceRequest request

    ) {

        try {

            if (request == null ||

                    request.getIds() == null ||

                    request.getIds().isEmpty()) {



                return ResponseEntity.badRequest().body(Map.of(

                        "success", false,

                        "message", "No attendance IDs provided"

                ));

            }



            int deletedCount =

                    attendanceService.bulkDeleteAttendances(

                            request.getIds()

                    );



            return ResponseEntity.ok(Map.of(

                    "success", true,

                    "message",

                    "Successfully deleted " +

                            deletedCount +

                            " attendance record(s)",

                    "deletedCount", deletedCount

            ));



        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(Map.of(

                    "success", false,

                    "message", e.getMessage()

            ));



        } catch (Exception e) {

            e.printStackTrace();



            return ResponseEntity.internalServerError().body(Map.of(

                    "success", false,

                    "message",

                    "Error deleting attendance records: " +

                            e.getMessage()

            ));

        }

    }



    @DeleteMapping("/{id:\\d+}")

    public ResponseEntity<Void> deleteAttendance(

            @PathVariable Long id

    ) {

        attendanceService.deleteAttendance(id);

        return ResponseEntity.noContent().build();

    }



    static class BulkDeleteAttendanceRequest {



        private List<Long> ids;



        public List<Long> getIds() {

            return ids;

        }



        public void setIds(List<Long> ids) {

            this.ids = ids;

        }

    }







}
