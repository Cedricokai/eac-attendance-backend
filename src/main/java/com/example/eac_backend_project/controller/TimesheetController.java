package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.JobService;
import com.example.eac_backend_project.Service.TimesheetService;
import com.example.eac_backend_project.dto.TimesheetListDTO;
import com.example.eac_backend_project.dto.TimesheetUpdateDTO;
import com.example.eac_backend_project.entity.attendance.Timesheet;
import com.example.eac_backend_project.entity.attendance.TimesheetStatus;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timesheets")
public class TimesheetController {


    private final TimesheetService timesheetService;

    @Autowired
    private JobService jobService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    private static final Logger logger = LoggerFactory.getLogger(TimesheetController.class);

    @GetMapping
    public ResponseEntity<List<Timesheet>> getAllTimesheets() {
        return ResponseEntity.ok(timesheetService.getAllTimesheets());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Timesheet> getTimesheetById(@PathVariable Long id) {
        return ResponseEntity.ok(timesheetService.getTimesheetById(id));
    }

    @PostMapping
    public ResponseEntity<Timesheet> createTimesheet(@RequestBody Timesheet timesheet) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timesheetService.createTimesheet(timesheet));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Timesheet>> createBatchTimesheets(@RequestBody List<Timesheet> timesheets) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timesheetService.createBatchTimesheets(timesheets));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Timesheet> updateTimesheet(@PathVariable Long id, @RequestBody TimesheetUpdateDTO dto) {
        Timesheet updated = timesheetService.updateTimesheetPartial(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteTimesheet(@PathVariable Long id) {
        timesheetService.deleteTimesheet(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/range")
    public ResponseEntity<List<Timesheet>> getTimesheetsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(timesheetService.getTimesheetsByDateRange(startDate, endDate));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Timesheet>> getEmployeeTimesheets(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(timesheetService.getEmployeeTimesheets(employeeId, startDate, endDate));
    }

    @PatchMapping("/{id:\\d+}/status")
    public ResponseEntity<Timesheet> updateTimesheetStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(timesheetService.updateTimesheetStatus(id, status));
    }

    @PostMapping("/bulk-status")
    public ResponseEntity<List<Timesheet>> bulkUpdateStatus(
            @RequestBody List<Long> ids,
            @RequestParam String status) {
        return ResponseEntity.ok(timesheetService.bulkUpdateStatus(ids, status));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<Timesheet>> getTimesheetsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(timesheetService.getTimesheetsByDateRange(date, date));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateTimesheetsFromPayload(@RequestBody Map<String, Object> payload) {
        try {
            List<Map<String, Object>> timesheetsData = (List<Map<String, Object>>) payload.get("timesheets");
            String saveMode = payload.get("saveMode") == null ? "skip" : payload.get("saveMode").toString();

            if (timesheetsData == null || timesheetsData.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No timesheet data provided"
                ));
            }

            Map<String, Object> result = timesheetService.upsertTimesheetsFromPayload(timesheetsData, saveMode);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error creating/updating timesheets: " + e.getMessage()
            ));
        }
    }


    @PutMapping("/range-upsert")
    public ResponseEntity<?> upsertRange(@RequestBody Map<String, Object> payload) {
        try {
            List<Number> employeeIdsRaw = (List<Number>) payload.get("employeeIds");
            LocalDate startDate = LocalDate.parse((String) payload.get("startDate"));
            LocalDate endDate = LocalDate.parse((String) payload.get("endDate"));

            String attendanceCode = payload.get("attendanceCode") == null ? "" : payload.get("attendanceCode").toString();
            double totalHours = payload.get("totalHours") == null ? 0.0 : ((Number) payload.get("totalHours")).doubleValue();
            double overtimeHours = payload.get("overtimeHours") == null ? 0.0 : ((Number) payload.get("overtimeHours")).doubleValue();
            double regularHours = payload.get("regularHours") == null ? Math.max(0.0, totalHours - overtimeHours)
                    : ((Number) payload.get("regularHours")).doubleValue();

            String saveMode = payload.get("saveMode") == null ? "override" : payload.get("saveMode").toString();

            List<Long> employeeIds = employeeIdsRaw.stream().map(Number::longValue).toList();

            List<Map<String, Object>> rows = new ArrayList<>();
            LocalDate d = startDate;
            while (!d.isAfter(endDate)) {
                for (Long empId : employeeIds) {
                    rows.add(Map.of(
                            "employeeId", empId,
                            "date", d.toString(),
                            "attendanceCode", attendanceCode,
                            "regularHours", regularHours,
                            "overtimeHours", overtimeHours,
                            "breakHours", 0.0,
                            "totalHours", totalHours,
                            "earnings", 0.0
                    ));
                }
                d = d.plusDays(1);
            }

            Map<String, Object> result = timesheetService.upsertTimesheetsFromPayload(rows, saveMode);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Range upsert failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Timesheet>> getTimesheetsByStatus(@PathVariable TimesheetStatus status) {
        return ResponseEntity.ok(timesheetService.getTimesheetsByStatus(status));
    }

    @GetMapping("/employee/{employeeId}/all")
    public ResponseEntity<List<Timesheet>> getTimesheetsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(timesheetService.getTimesheetsByEmployeeId(employeeId));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<Timesheet>> getTimesheetsByJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(timesheetService.getTimesheetsByJobId(jobId));
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkTimesheetExists(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(timesheetService.existsByEmployeeAndDate(employeeId, date));
    }

    // Add this method to TimesheetController.java
    @PostMapping("/import")
    public ResponseEntity<?> importTimesheets(@RequestBody Map<String, Object> importRequest) {
        try {
            System.out.println("Received import request: " + importRequest.keySet());

            List<Map<String, Object>> timesheetsData = (List<Map<String, Object>>) importRequest.get("timesheets");

            if (timesheetsData == null || timesheetsData.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No timesheet data provided for import"
                ));
            }

            System.out.println("Processing import for " + timesheetsData.size() + " timesheets");

            // Use your existing createTimesheetsFromPayload method
            List<Timesheet> savedTimesheets = timesheetService.createTimesheetsFromPayload(timesheetsData);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Successfully imported " + savedTimesheets.size() + " timesheets",
                    "data", savedTimesheets
            ));

        } catch (Exception e) {
            System.err.println("Error in importTimesheets: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error importing timesheets: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteTimesheetRecords(@RequestBody DeleteRequest request) {
        try {
            int deletedCount = 0;

            // SINGLE DAY - Delete for one employee on one specific date
            if ("singleDay".equals(request.getScope())) {
                if (request.getEmployeeId() != null && request.getDate() != null) {
                    // Delete single day for specific employee
                    List<Long> singleId = Collections.singletonList(request.getEmployeeId());
                    deletedCount = timesheetService.deleteByEmployeeIdsAndDate(
                            singleId,
                            LocalDate.parse(request.getDate())
                    );
                    logger.info("Deleting single day for employee {} on date {}: {} records deleted",
                            request.getEmployeeId(), request.getDate(), deletedCount);
                } else {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "error", "Both employeeId and date are required for singleDay scope"
                    ));
                }
            }
            // SELECTED - Delete for multiple selected employees
            else if ("selected".equals(request.getScope())) {
                if (request.getEmployeeIds() != null && !request.getEmployeeIds().isEmpty()) {
                    if (request.getDate() != null) {
                        // Delete specific date for selected employees
                        deletedCount = timesheetService.deleteByEmployeeIdsAndDate(
                                request.getEmployeeIds(),
                                LocalDate.parse(request.getDate())
                        );
                    } else if (request.getStartDate() != null && request.getEndDate() != null) {
                        // Delete date range for selected employees
                        deletedCount = timesheetService.deleteByEmployeeIdsAndDateRange(
                                request.getEmployeeIds(),
                                LocalDate.parse(request.getStartDate()),
                                LocalDate.parse(request.getEndDate())
                        );
                    } else {
                        // Delete all dates for selected employees
                        deletedCount = timesheetService.deleteByEmployeeIds(request.getEmployeeIds());
                    }
                }
            }
            // ALL - Delete for all employees
            else if ("all".equals(request.getScope())) {
                if (request.getDate() != null) {
                    // Delete specific date for all employees
                    deletedCount = timesheetService.deleteByDate(LocalDate.parse(request.getDate()));
                } else if (request.getStartDate() != null && request.getEndDate() != null) {
                    // Delete date range for all employees
                    deletedCount = timesheetService.deleteByDateRange(
                            LocalDate.parse(request.getStartDate()),
                            LocalDate.parse(request.getEndDate())
                    );
                } else {
                    // Delete all timesheets (be careful!)
                    deletedCount = timesheetService.deleteAll();
                }
            }
            // DATE - Delete by date only (all employees)
            else if ("date".equals(request.getScope())) {
                if (request.getDate() != null) {
                    deletedCount = timesheetService.deleteByDate(LocalDate.parse(request.getDate()));
                }
            }
            // DATE RANGE - Delete by date range only (all employees)
            else if ("dateRange".equals(request.getScope())) {
                if (request.getStartDate() != null && request.getEndDate() != null) {
                    deletedCount = timesheetService.deleteByDateRange(
                            LocalDate.parse(request.getStartDate()),
                            LocalDate.parse(request.getEndDate())
                    );
                }
            }
            // Unknown scope
            else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Unknown scope: " + request.getScope()
                ));
            }


            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "deletedCount", deletedCount,
                    "message", "Successfully deleted " + deletedCount + " records"
            ));
        } catch (Exception e) {
            logger.error("Error deleting timesheet records: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/rows")
    public ResponseEntity<?> getTimesheetRows(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            Long employeeId,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            Long jobId,

            @RequestParam(defaultValue = "false")
            boolean withoutJob,

            @RequestParam(required = false)
            TimesheetStatus status,

            @RequestParam(required = false)
            Boolean sourceMissing,

            @RequestParam(defaultValue = "")
            String search
    ) {
        try {
            List<TimesheetListDTO> result =
                    timesheetService.getTimesheetRows(
                            startDate,
                            endDate,
                            employeeId,
                            categoryId,
                            jobId,
                            withoutJob,
                            status,
                            sourceMissing,
                            search
                    );

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        } catch (Exception e) {
            logger.error(
                    "Failed to load timesheet rows",
                    e
            );

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Failed to load timesheets: " +
                                    e.getMessage()
                    )
            );
        }
    }

    @GetMapping("/invoice/export")
    public ResponseEntity<byte[]> exportJobInvoice(
            @RequestParam Long jobId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            byte[] excelData = timesheetService.generateJobInvoiceExcel(jobId, start, end);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "invoice_job_" + jobId + "_" + startDate + "_to_" + endDate + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/invoice/export-with-attachments")
    public ResponseEntity<byte[]> exportJobInvoiceWithAttachments(
            @RequestParam Long jobId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            byte[] zipData = timesheetService.generateJobInvoiceZip(jobId, start, end);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = "invoice_" + jobId + "_" + startDate + "_to_" + endDate + ".zip";
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }


    // DeleteRequest DTO class
    @Data
    static class DeleteRequest {
        private String scope;
        private List<Long> employeeIds;
        private Long employeeId;
        private String date;
        private String startDate;
        private String endDate;

        // getters and setters
    }
}