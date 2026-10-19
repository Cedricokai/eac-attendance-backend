package com.example.eac_backend_project.controller.lunch;

import com.example.eac_backend_project.Service.UserService;
import com.example.eac_backend_project.Service.lunch.LunchServingService;
import com.example.eac_backend_project.dto.lunch.LunchServingDTO;
import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.lunch.LunchServing;
import com.example.eac_backend_project.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/lunch/serve")
@RequiredArgsConstructor
public class LunchServingController {

    private final LunchServingService servingService;
    private final AttendanceRepository attendanceRepository;
    private final UserService userService;

    /**
     * Serve lunch to an employee.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<?> serveLunch(@RequestBody LunchServingDTO dto) {
        try {
            System.out.println("📥 Serving lunch request: " + dto);

            LocalDate date = dto.getLunchDate() != null
                    ? dto.getLunchDate()
                    : LocalDate.now();

            if (dto.getEmployeeId() == null) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "success", false,
                                "error", "Employee ID is required"
                        ));
            }

            User currentUser;

            try {
                currentUser = userService.getCurrentUser();

                System.out.println(
                        "👤 Current user: "
                                + (currentUser != null
                                ? currentUser.getId()
                                : "null")
                );
            } catch (Exception exception) {
                System.err.println(
                        "❌ Error getting current user: "
                                + exception.getMessage()
                );

                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "success", false,
                                "error",
                                "User not authenticated: "
                                        + exception.getMessage()
                        ));
            }

            if (currentUser == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "success", false,
                                "error", "User not authenticated"
                        ));
            }

            Optional<Attendance> attendance =
                    attendanceRepository.findByEmployeeIdAndDate(
                            dto.getEmployeeId(),
                            date
                    );

            boolean present =
                    attendance.filter(com.example.eac_backend_project.Service.lunch.LunchAttendancePolicy::isPresent).isPresent();

            if (!present) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "success", false,
                                "error",
                                "Employee is not present on " + date
                        ));
            }

            String location =
                    dto.getLocation() != null
                            && !dto.getLocation().trim().isEmpty()
                            ? dto.getLocation().trim()
                            : "MKV";

            LunchServing serving = servingService.serveLunch(
                    dto.getEmployeeId(),
                    date,
                    currentUser,
                    dto.getDevice(),
                    dto.getRemarks(),
                    present,
                    location
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", serving);
            response.put(
                    "message",
                    "Lunch served successfully at " + location
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException exception) {
            System.err.println(
                    "❌ Runtime error: " + exception.getMessage()
            );
            exception.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "success", false,
                            "error",
                            exception.getMessage() != null
                                    ? exception.getMessage()
                                    : "Unable to serve lunch"
                    ));

        } catch (Exception exception) {
            System.err.println(
                    "❌ Unexpected error: " + exception.getMessage()
            );
            exception.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error",
                            "An unexpected error occurred: "
                                    + exception.getMessage()
                    ));
        }
    }

    /**
     * Get all lunch-serving records for a selected date.
     */
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<List<LunchServing>> getTodayServed(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate requestedDate =
                date != null ? date : LocalDate.now();

        return ResponseEntity.ok(
                servingService.getServedForDate(requestedDate)
        );
    }

    /**
     * Get the daily serving statistics.
     *
     * Frontend URL:
     * GET /api/lunch/serve/stats/daily?date=2026-08-03
     */
    @GetMapping("/stats/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getDailyServingStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate requestedDate =
                date != null ? date : LocalDate.now();

        List<LunchServing> servedRecords =
                servingService.getServedForDate(requestedDate);

        Map<String, Long> locationTotals = new HashMap<>();

        for (LunchServing serving : servedRecords) {
            String location =
                    serving.getLocation() != null
                            && !serving.getLocation().trim().isEmpty()
                            ? serving.getLocation().trim()
                            : "Unknown";

            locationTotals.put(
                    location,
                    locationTotals.getOrDefault(location, 0L) + 1L
            );
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("date", requestedDate);
        statistics.put("totalServed", servedRecords.size());
        statistics.put("locationTotals", locationTotals);
        statistics.put("records", servedRecords);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get an employee's lunch-serving record for a selected date.
     */
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<LunchServing> getEmployeeServing(
            @PathVariable Long employeeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate requestedDate =
                date != null ? date : LocalDate.now();

        return ResponseEntity.of(
                servingService.getServingForEmployee(
                        employeeId,
                        requestedDate
                )
        );
    }

    /**
     * Get serving records for a selected location and date.
     */
    @GetMapping("/by-location")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<List<LunchServing>> getServedByLocation(
            @RequestParam String location,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate requestedDate =
                date != null ? date : LocalDate.now();

        return ResponseEntity.ok(
                servingService.getServedByLocation(
                        location,
                        requestedDate
                )
        );
    }

    /**
     * Get totals grouped by serving location.
     */
    @GetMapping("/location-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'KITCHEN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getLocationStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate requestedDate =
                date != null ? date : LocalDate.now();

        return ResponseEntity.ok(
                servingService.getLocationStats(requestedDate)
        );
    }
}
