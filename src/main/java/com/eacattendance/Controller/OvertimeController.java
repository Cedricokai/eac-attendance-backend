package com.eacattendance.Controller;

import com.eacattendance.Service.OvertimeService;
import com.eacattendance.entity.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/overtime")
@CrossOrigin(origins = "http://localhost:5173")
public class OvertimeController {
    private final OvertimeService overtimeService;

    public OvertimeController(OvertimeService overtimeService) {
        this.overtimeService = overtimeService;
    }

    @GetMapping
    public List<Overtime> getAllOvertimes() {
        return overtimeService.getAllOvertimes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Overtime> getOvertimeById(@PathVariable Long id) {
        return ResponseEntity.ok(overtimeService.getOvertimeById(id));
    }

    @PostMapping
    public ResponseEntity<Overtime> createOvertime(@Valid @RequestBody Overtime overtime) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(overtimeService.createOvertime(overtime));
    }

    @GetMapping("/available-employees")
    public ResponseEntity<List<Employee>> getAvailableEmployees(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(overtimeService.getAvailableEmployeesForDate(date));
    }


    // Add this at the BOTTOM of your controller class
    @PostMapping("/validate")
    public ResponseEntity<?> validateOvertimes(@RequestBody ValidateOvertimeRequest request) {
        try {
            if (request.getOvertimeIds() == null || request.getOvertimeIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No overtime IDs provided"
                ));
            }

            List<Overview> overviews = overtimeService.validateOvertimes(request.getOvertimeIds());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully validated " + overviews.size() + " records",
                    "data", overviews.stream().map(this::convertToResponse).collect(Collectors.toList())
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace(); // Log the error
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error validating overtimes: " + e.getMessage()
            ));
        }
    }

    private OverviewResponse convertToResponse(Overview overview) {
        OverviewResponse response = new OverviewResponse();
        response.setId(overview.getId());
        if (overview.getEmployee() != null) {
            response.setEmployeeId(overview.getEmployee().getId());
        }
        if (overview.getOvertime() != null) {
            response.setOvertimeId(overview.getOvertime().getId());
        }
        response.setOvertimeRate(overview.getOvertimeRate());
        response.setStatus(overview.getStatus());
        response.setDate(overview.getDate());
        response.setHoursWorked(overview.getHoursWorked());
        return response;
    }
}