package com.eacattendance.Controller;

import com.eacattendance.Service.TimesheetService;
import com.eacattendance.entity.Timesheet;
import com.eacattendance.entity.TimesheetStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timesheets")
@CrossOrigin(origins = "http://localhost:5173")
public class TimesheetController {
    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    @GetMapping
    public ResponseEntity<List<Timesheet>> getAllTimesheets() {
        return ResponseEntity.ok(timesheetService.getAllTimesheets());
    }

    @GetMapping("/{id}")
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

    @PutMapping("/{id}")
    public ResponseEntity<Timesheet> updateTimesheet(@PathVariable Long id, @RequestBody Timesheet timesheet) {
        return ResponseEntity.ok(timesheetService.updateTimesheet(id, timesheet));
    }

    @DeleteMapping("/{id}")
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

    @PatchMapping("/{id}/status")
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

    @PostMapping("/generate-from-overview")
    public ResponseEntity<List<Timesheet>> generateFromOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Timesheet> generatedTimesheets = timesheetService.generateFromOverview(startDate, endDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(generatedTimesheets);
    }


    @PostMapping("/generate")
    public ResponseEntity<List<Timesheet>> generateTimesheetsFromPayload(@RequestBody Map<String, Object> payload) {
        // Extract fields from payload (you may want to create a DTO class instead for better practice)
        List<Map<String, Object>> timesheetsData = (List<Map<String, Object>>) payload.get("timesheets");

        List<Timesheet> timesheets = timesheetsData.stream().map(data -> {
            Timesheet t = new Timesheet();
            t.setDate(LocalDate.parse((String) data.get("date")));
            t.setRegularHours(((Number) data.get("regularHours")).intValue());
            t.setOvertimeHours(((Number) data.get("overtimeHours")).intValue());
            t.setStatus(TimesheetStatus.valueOf(((String) data.get("status")).toUpperCase()));

            // You must fetch the employee using ID
            Long empId = ((Number) data.get("employeeId")).longValue();
            t.setEmployee(timesheetService.getEmployeeById(empId)); // create this method in your service if needed

            return t;
        }).collect(Collectors.toList());

        List<Timesheet> saved = timesheetService.createBatchTimesheets(timesheets);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


}