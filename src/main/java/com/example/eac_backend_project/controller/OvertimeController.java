package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.OvertimeService;
import com.example.eac_backend_project.Service.PageService;
import com.example.eac_backend_project.dto.OvertimeListDTO;
import com.example.eac_backend_project.dto.OvertimeRequestDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.Overtime;
import com.example.eac_backend_project.entity.attendance.Overview;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.OvertimeRepository;
import com.example.eac_backend_project.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/overtime")
public class OvertimeController {

    private final OvertimeService overtimeService;
    private final EmployeeRepository employeeRepository;
    private final OvertimeRepository overtimeRepository;
    private final UserRepository userRepository;
    private final PageService pageService;

    public OvertimeController(
            OvertimeService overtimeService,
            EmployeeRepository employeeRepository,
            OvertimeRepository overtimeRepository,
            UserRepository userRepository,
            PageService pageService
    ) {
        this.overtimeService = overtimeService;
        this.employeeRepository = employeeRepository;
        this.overtimeRepository = overtimeRepository;
        this.userRepository = userRepository;
        this.pageService = pageService;
    }

    // =========================================================
    // LEGACY FULL-LIST ENDPOINT
    // =========================================================

    /**
     * Retained for backward compatibility.
     * The overtime screen should use GET /api/overtime/page.
     */
    @GetMapping
    public ResponseEntity<List<OvertimeResponse>> getAllOvertimes() {
        try {
            List<OvertimeResponse> responses =
                    overtimeService.getAllOvertimes()
                            .stream()
                            .map(this::convertToOvertimeResponse)
                            .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().build();
        }
    }

    // =========================================================
    // PAGINATED OVERTIME ENDPOINT
    // =========================================================

    @GetMapping("/page")
    public ResponseEntity<?> getOvertimePage(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false) Long jobId,
            @RequestParam(defaultValue = "false") boolean unassignedJob,

            @RequestParam(defaultValue = "")
            String status,

            @RequestParam(defaultValue = "")
            String search,

            @RequestParam(required = false)
            Double minHours,

            @RequestParam(required = false)
            Double maxHours,

            @RequestParam(required = false)
            Double minAmount,

            @RequestParam(required = false)
            Double maxAmount,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "100")
            int size
    ) {
        try {
            Page<OvertimeListDTO> result =
                    overtimeService.getOvertimePage(
                            startDate,
                            endDate,
                            categoryId,
                            jobId,
                            unassignedJob,
                            status,
                            search,
                            minHours,
                            maxHours,
                            minAmount,
                            maxAmount,
                            page,
                            size
                    );

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "error", e.getMessage()
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "error",
                            "Failed to load overtime records: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // GET ONE OVERTIME
    // =========================================================

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<?> getOvertimeById(
            @PathVariable Long id
    ) {
        try {
            Overtime overtime =
                    overtimeService.getOvertimeById(id);

            return ResponseEntity.ok(
                    convertToOvertimeResponse(overtime)
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error",
                            "Failed to retrieve overtime record"
                    )
            );
        }
    }

    // =========================================================
    // CREATE OVERTIME
    // =========================================================

    @PostMapping
    public ResponseEntity<?> createOvertime(
            @RequestBody OvertimeRequestDTO request
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Overtime request is required")
                );
            }

            if (request.getEmployeeId() == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Employee ID is required")
                );
            }

            if (request.getDate() == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Overtime date is required")
                );
            }

            Overtime overtime = new Overtime();

            Employee employee = new Employee();
            employee.setId(request.getEmployeeId());

            overtime.setEmployee(employee);
            overtime.setDate(request.getDate());
            overtime.setStartTime(request.getStartTime());
            overtime.setEndTime(request.getEndTime());

            overtime.setStatus(
                    request.getStatus() != null &&
                            !request.getStatus().isBlank()
                            ? request.getStatus()
                            : "Pending"
            );

            overtime.setBaseHourlyRate(
                    request.getBaseHourlyRate()
            );

            overtime.setOvertimeMultiplier(
                    request.getOvertimeMultiplier()
            );

            overtime.setOvertimeHours(
                    request.getOvertimeHours()
            );

            overtime.setRawOvertimeHours(
                    request.getRawOvertimeHours()
            );

            overtime.setCalculatedOvertimePay(
                    request.getCalculatedOvertimePay()
            );

            overtime.setMultiplierAppliedToHours(
                    request.getMultiplierAppliedToHours()
            );

            Overtime created =
                    overtimeService.createOvertime(overtime);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(convertToOvertimeResponse(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error",
                            "Failed to create overtime record: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    @PostMapping("/delegated-request")
    public ResponseEntity<?> createDelegatedOvertimeRequest(
            @RequestBody OvertimeRequestDTO request,
            Authentication authentication
    ) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication is required"));
            }

            boolean hasPageAccess = pageService.getAccessiblePages(authentication).stream()
                    .anyMatch(page -> "/employeeOvertimeRequest".equalsIgnoreCase(page.getPath()));
            if (!hasPageAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not assigned to Overtime Request Entry"));
            }

            if (request == null || request.getEmployeeId() == null || request.getDate() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Employee and overtime date are required"));
            }

            User requestedBy = userRepository.findByUserName(authentication.getName())
                    .or(() -> userRepository.findByEmail(authentication.getName()))
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));

            Overtime overtime = new Overtime();
            Employee employee = new Employee();
            employee.setId(request.getEmployeeId());
            overtime.setEmployee(employee);
            overtime.setDate(request.getDate());
            overtime.setStartTime(request.getStartTime());
            overtime.setEndTime(request.getEndTime());
            overtime.setStatus("Pending");
            overtime.setBaseHourlyRate(request.getBaseHourlyRate());
            overtime.setOvertimeMultiplier(request.getOvertimeMultiplier());
            overtime.setOvertimeHours(request.getOvertimeHours());
            overtime.setRawOvertimeHours(request.getRawOvertimeHours());
            overtime.setCalculatedOvertimePay(request.getCalculatedOvertimePay());
            overtime.setMultiplierAppliedToHours(request.getMultiplierAppliedToHours());
            overtime.setNotes(request.getNotes());
            overtime.setRequestedBy(requestedBy);
            overtime.setRequestSource("DELEGATED_ENTRY");

            Overtime created = overtimeService.createOvertime(overtime);
            try {
                overtimeService.notifySupervisorOfDelegatedRequest(created, requestedBy);
            } catch (Exception notificationError) {
                System.err.println("Overtime request saved but notification failed: " + notificationError.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(convertToOvertimeResponse(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create delegated overtime request: " + safeMessage(e)));
        }
    }

    @PostMapping("/delegated-requests")
    public ResponseEntity<?> createDelegatedOvertimeRequests(
            @RequestBody List<OvertimeRequestDTO> requests,
            Authentication authentication
    ) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication is required"));
            }

            boolean hasPageAccess = pageService.getAccessiblePages(authentication).stream()
                    .anyMatch(page -> "/employeeOvertimeRequest".equalsIgnoreCase(page.getPath()));
            if (!hasPageAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not assigned to Overtime Request Entry"));
            }
            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one overtime request is required"));
            }
            if (requests.stream().anyMatch(request -> request == null
                    || request.getEmployeeId() == null || request.getDate() == null)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Employee and overtime date are required for every request"));
            }

            User requestedBy = userRepository.findByUserName(authentication.getName())
                    .or(() -> userRepository.findByEmail(authentication.getName()))
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));

            List<Overtime> createdRequests = new ArrayList<>();
            for (OvertimeRequestDTO request : requests) {
                Overtime overtime = new Overtime();
                Employee employee = new Employee();
                employee.setId(request.getEmployeeId());
                overtime.setEmployee(employee);
                overtime.setDate(request.getDate());
                overtime.setStartTime(request.getStartTime());
                overtime.setEndTime(request.getEndTime());
                overtime.setStatus("Pending");
                overtime.setBaseHourlyRate(request.getBaseHourlyRate());
                overtime.setOvertimeMultiplier(request.getOvertimeMultiplier());
                overtime.setOvertimeHours(request.getOvertimeHours());
                overtime.setRawOvertimeHours(request.getRawOvertimeHours());
                overtime.setCalculatedOvertimePay(request.getCalculatedOvertimePay());
                overtime.setMultiplierAppliedToHours(request.getMultiplierAppliedToHours());
                overtime.setNotes(request.getNotes());
                overtime.setRequestedBy(requestedBy);
                overtime.setRequestSource("DELEGATED_ENTRY");
                createdRequests.add(overtimeService.createOvertime(overtime));
            }

            try {
                overtimeService.notifySupervisorsOfDelegatedRequests(createdRequests, requestedBy);
            } catch (Exception notificationError) {
                System.err.println("Overtime requests saved but grouped notification failed: " + notificationError.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "createdCount", createdRequests.size(),
                    "requests", createdRequests.stream().map(this::convertToOvertimeResponse).toList()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create delegated overtime requests: " + safeMessage(e)));
        }
    }

    // =========================================================
    // UPDATE OVERTIME
    // =========================================================

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<?> updateOvertime(
            @PathVariable Long id,
            @Valid @RequestBody OvertimeRequest request
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Overtime request is required")
                );
            }

            Overtime overtime =
                    convertToOvertimeEntity(request);

            Overtime updated =
                    overtimeService.updateOvertime(id, overtime);

            return ResponseEntity.ok(
                    convertToOvertimeResponse(updated)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error",
                            "Failed to update overtime record: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // DELETE ONE OVERTIME
    // =========================================================

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<?> deleteOvertime(
            @PathVariable Long id
    ) {
        try {
            overtimeService.deleteOvertime(id);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            "Overtime record deleted successfully"
                    )
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of(
                            "success", false,
                            "error", e.getMessage()
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "error",
                            "Failed to delete overtime record: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // BULK DELETE
    // =========================================================

    @DeleteMapping("/bulk/delete")
    public ResponseEntity<?> bulkDeleteOvertimes(
            @RequestBody BulkDeleteRequest request
    ) {
        try {
            if (request == null ||
                    request.getIds() == null ||
                    request.getIds().isEmpty()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "No overtime IDs provided"
                        )
                );
            }

            List<Long> ids = request.getIds()
                    .stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (ids.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "No valid overtime IDs provided"
                        )
                );
            }

            List<Overtime> existing =
                    overtimeRepository.findAllById(ids);

            if (existing.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of(
                                "success", false,
                                "message",
                                "No matching overtime records were found"
                        )
                );
            }

            List<Long> existingIds = existing.stream()
                    .map(Overtime::getId)
                    .toList();

            int deletedCount =
                    overtimeRepository.deleteAllByIdIn(existingIds);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            "Successfully deleted " +
                                    deletedCount +
                                    " overtime record(s)",
                            "deletedCount", deletedCount
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Failed to delete overtime records: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // AVAILABLE EMPLOYEES
    // =========================================================

    @GetMapping("/available-employees")
    public ResponseEntity<?> getAvailableEmployees(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        try {
            if (date == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Date is required")
                );
            }

            List<EmployeeResponse> responses =
                    overtimeService
                            .getAvailableEmployeesForDate(date)
                            .stream()
                            .map(this::convertToEmployeeResponse)
                            .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error",
                            "Failed to load available employees: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // APPROVE OVERTIME
    // =========================================================

    @PostMapping("/validate")
    public ResponseEntity<?> validateOvertimes(
            @RequestBody ValidateOvertimeRequest request
    ) {
        try {
            if (request == null ||
                    request.getOvertimeIds() == null ||
                    request.getOvertimeIds().isEmpty()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "No overtime IDs provided"
                        )
                );
            }

            List<Overtime> approved =
                    overtimeService.validateOvertimes(
                            request.getOvertimeIds()
                    );

            List<OvertimeResponse> responseData =
                    approved.stream()
                            .map(this::convertToOvertimeResponse)
                            .collect(Collectors.toList());

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            "Successfully approved " +
                                    approved.size() +
                                    " overtime record(s)",
                            "data", responseData
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Error approving overtime records: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // REJECT OVERTIME
    // =========================================================

    @PostMapping("/reject")
    public ResponseEntity<?> rejectOvertimes(
            @RequestBody ValidateOvertimeRequest request
    ) {
        try {
            if (request == null ||
                    request.getOvertimeIds() == null ||
                    request.getOvertimeIds().isEmpty()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "No overtime IDs provided"
                        )
                );
            }

            List<Overtime> rejected =
                    overtimeService.rejectOvertimes(
                            request.getOvertimeIds()
                    );

            List<OvertimeResponse> responseData =
                    rejected.stream()
                            .map(this::convertToOvertimeResponse)
                            .collect(Collectors.toList());

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            "Successfully rejected " +
                                    rejected.size() +
                                    " overtime record(s)",
                            "data", responseData
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Error rejecting overtime records: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // OVERTIME SUMMARY
    // =========================================================

    @GetMapping("/summary")
    public ResponseEntity<?> getOvertimeSummary(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        try {
            if (startDate == null || endDate == null) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "error",
                                "startDate and endDate are required"
                        )
                );
            }

            if (endDate.isBefore(startDate)) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "error",
                                "endDate must be on or after startDate"
                        )
                );
            }

            OvertimeService.OvertimeSummary summary =
                    overtimeService.getOvertimeSummary(
                            startDate,
                            endDate
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "totalHours",
                            summary.getTotalHours(),
                            "totalPay",
                            summary.getTotalPay(),
                            "recordCount",
                            summary.getRecordCount()
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error",
                            "Failed to get overtime summary: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // TOGGLE MULTIPLIER FOR ONE RECORD
    // =========================================================

    @PutMapping("/{id:\\d+}/toggle-multiplier")
    public ResponseEntity<?> toggleMultiplierAppliedToHours(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request
    ) {
        try {
            Boolean newValue =
                    request != null
                            ? request.get(
                            "multiplierAppliedToHours"
                    )
                            : null;

            if (newValue == null) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "error",
                                "multiplierAppliedToHours value is required"
                        )
                );
            }

            Overtime updated =
                    overtimeService
                            .toggleMultiplierAppliedToHours(
                                    id,
                                    newValue
                            );

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            "Multiplier setting updated successfully",
                            "id", updated.getId(),
                            "multiplierAppliedToHours",
                            Boolean.TRUE.equals(
                                    updated.getMultiplierAppliedToHours()
                            )
                    )
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error",
                            "Failed to toggle multiplier: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // TOGGLE MULTIPLIER FOR MULTIPLE RECORDS
    // =========================================================

    @PutMapping("/batch/toggle-multiplier")
    public ResponseEntity<?> batchToggleMultiplier(
            @RequestBody BatchToggleMultiplierRequest request
    ) {
        try {
            if (request == null ||
                    request.getIds() == null ||
                    request.getIds().isEmpty()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "error",
                                "No overtime IDs provided"
                        )
                );
            }

            if (request.getMultiplierAppliedToHours() == null) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "error",
                                "multiplierAppliedToHours value is required"
                        )
                );
            }

            List<Long> ids = request.getIds()
                    .stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            int updatedCount =
                    overtimeService
                            .batchToggleMultiplierAppliedToHours(
                                    ids,
                                    request.getMultiplierAppliedToHours()
                            );

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            "Successfully updated " +
                                    updatedCount +
                                    " overtime record(s)",
                            "updatedCount", updatedCount
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error",
                            "Failed to batch-toggle multiplier: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // BULK CREATE
    // =========================================================

    @PostMapping("/bulk/create")
    public ResponseEntity<?> createBulkOvertime(
            @RequestBody BulkOvertimeRequest request
    ) {
        try {
            if (request == null ||
                    request.getOvertimes() == null ||
                    request.getOvertimes().isEmpty()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "No overtime records provided"
                        )
                );
            }

            List<Overtime> overtimes = new ArrayList<>();

            for (BulkOvertimeItem item :
                    request.getOvertimes()) {

                if (item == null ||
                        item.getEmployeeId() == null ||
                        item.getDate() == null) {
                    continue;
                }

                Overtime overtime = new Overtime();

                Employee employee = new Employee();
                employee.setId(item.getEmployeeId());

                overtime.setEmployee(employee);
                overtime.setDate(item.getDate());
                overtime.setStartTime(item.getStartTime());
                overtime.setEndTime(item.getEndTime());

                overtime.setOvertimeMultiplier(
                        item.getOvertimeMultiplier()
                );

                overtime.setMultiplierAppliedToHours(
                        item.getMultiplierAppliedToHours()
                );

                overtime.setStatus(
                        item.getStatus() != null &&
                                !item.getStatus().isBlank()
                                ? item.getStatus()
                                : "Pending"
                );

                overtime.setNotes(item.getNotes());

                if (item.getOvertimeHours() != null) {
                    overtime.setOvertimeHours(
                            item.getOvertimeHours()
                    );
                }

                if (item.getRawOvertimeHours() != null) {
                    overtime.setRawOvertimeHours(
                            item.getRawOvertimeHours()
                    );
                }

                overtimes.add(overtime);
            }

            if (overtimes.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "No valid overtime records were provided"
                        )
                );
            }

            List<Overtime> created =
                    request.isSkipDuplicates()
                            ? overtimeService
                            .createBulkOvertimeWithDuplicateCheck(
                                    overtimes,
                                    true
                            )
                            : overtimeService
                            .createBulkOvertime(overtimes);

            List<OvertimeResponse> responseData =
                    created.stream()
                            .map(this::convertToOvertimeResponse)
                            .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "success", true,
                            "message",
                            "Successfully created " +
                                    created.size() +
                                    " overtime record(s)",
                            "data", responseData
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Failed to create bulk overtime: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // BULK CREATE FOR ELIGIBLE EMPLOYEES
    // =========================================================

    @PostMapping("/bulk/create-for-all")
    public ResponseEntity<?> createOvertimeForAllEligible(
            @RequestBody BulkOvertimeForAllRequest request
    ) {
        try {
            if (request == null ||
                    request.getDate() == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Overtime date is required"
                        )
                );
            }

            if (request.getStartTime() == null ||
                    request.getEndTime() == null) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "Start time and end time are required"
                        )
                );
            }

            List<Employee> eligibleEmployees =
                    overtimeService
                            .getEligibleEmployeesForBulkOvertime(
                                    request.getDate(),
                                    request.getEmployeeIds()
                            );

            if (eligibleEmployees.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "No eligible employees found for the selected date"
                        )
                );
            }

            List<Overtime> overtimes = new ArrayList<>();

            for (Employee employee :
                    eligibleEmployees) {

                Overtime overtime = new Overtime();

                overtime.setEmployee(employee);
                overtime.setDate(request.getDate());
                overtime.setStartTime(
                        request.getStartTime()
                );
                overtime.setEndTime(
                        request.getEndTime()
                );

                overtime.setOvertimeMultiplier(
                        request.getOvertimeMultiplier()
                );

                overtime.setMultiplierAppliedToHours(
                        request.getMultiplierAppliedToHours()
                );

                overtime.setStatus(
                        request.getStatus() != null &&
                                !request.getStatus().isBlank()
                                ? request.getStatus()
                                : "Pending"
                );

                overtime.setNotes(request.getNotes());

                overtimes.add(overtime);
            }

            List<Overtime> created =
                    overtimeService.createBulkOvertime(
                            overtimes
                    );

            List<OvertimeResponse> responseData =
                    created.stream()
                            .map(this::convertToOvertimeResponse)
                            .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "success", true,
                            "message",
                            "Successfully created overtime for " +
                                    created.size() +
                                    " employee(s)",
                            "totalEmployees",
                            eligibleEmployees.size(),
                            "createdCount",
                            created.size(),
                            "data", responseData
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message",
                            "Failed to create bulk overtime: " +
                                    safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // BULK ELIGIBLE EMPLOYEES
    // =========================================================

    @GetMapping("/bulk/eligible-employees")
    public ResponseEntity<?> getEligibleEmployeesForBulk(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            List<Long> employeeIds
    ) {
        try {
            List<Employee> eligible =
                    overtimeService
                            .getEligibleEmployeesForBulkOvertime(
                                    date,
                                    employeeIds
                            );

            List<Map<String, Object>> response =
                    new ArrayList<>();

            for (Employee employee : eligible) {
                Map<String, Object> employeeMap =
                        new HashMap<>();

                employeeMap.put(
                        "id",
                        employee.getId()
                );

                employeeMap.put(
                        "name",
                        fullName(employee)
                );

                employeeMap.put(
                        "employeeId",
                        employee.getEmployeeId()
                );

                employeeMap.put(
                        "rate",
                        employee.getMinimumRate()
                );

                if (employee.getCategory() != null) {
                    employeeMap.put(
                            "categoryId",
                            employee.getCategory().getId()
                    );

                    employeeMap.put(
                            "categoryName",
                            employee.getCategory().getName()
                    );
                }

                response.add(employeeMap);
            }

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "total", eligible.size(),
                            "data", response
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message", safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // BULK ELIGIBILITY CHECK
    // =========================================================

    @PostMapping("/bulk/validate-list")
    public ResponseEntity<?> validateBulkEligibility(
            @RequestBody List<BulkEligibilityCheck> checks
    ) {
        try {
            if (checks == null || checks.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "message",
                                "No eligibility checks provided"
                        )
                );
            }

            List<Map<String, Object>> validations =
                    new ArrayList<>();

            for (BulkEligibilityCheck check : checks) {
                if (check == null ||
                        check.getEmployeeId() == null ||
                        check.getDate() == null) {
                    continue;
                }

                boolean eligible =
                        !overtimeRepository
                                .existsByEmployeeIdAndDate(
                                        check.getEmployeeId(),
                                        check.getDate()
                                );

                Map<String, Object> validation =
                        new HashMap<>();

                validation.put(
                        "employeeId",
                        check.getEmployeeId()
                );

                validation.put(
                        "date",
                        check.getDate()
                );

                validation.put(
                        "eligible",
                        eligible
                );

                validation.put(
                        "message",
                        eligible
                                ? "Eligible"
                                : "Overtime already exists for this date"
                );

                validations.add(validation);
            }

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "validations", validations
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "message", safeMessage(e)
                    )
            );
        }
    }

    // =========================================================
    // DEBUG ENDPOINT
    // =========================================================

    @GetMapping("/debug")
    public ResponseEntity<?> debugOvertime() {
        try {
            List<Overtime> allOvertimes =
                    overtimeRepository.findAll();

            List<Map<String, Object>> overtimeRows =
                    new ArrayList<>();

            List<Map<String, Object>> biometricRows =
                    new ArrayList<>();

            for (Overtime overtime : allOvertimes) {
                Map<String, Object> row =
                        new HashMap<>();

                row.put("id", overtime.getId());

                row.put(
                        "employeeId",
                        overtime.getEmployee() != null
                                ? overtime.getEmployee().getId()
                                : null
                );

                row.put(
                        "attendanceId",
                        overtime.getAttendance() != null
                                ? overtime.getAttendance().getId()
                                : null
                );

                row.put("date", overtime.getDate());

                row.put(
                        "overtimeHours",
                        overtime.getOvertimeHours()
                );

                row.put("status", overtime.getStatus());

                row.put(
                        "calculatedPay",
                        overtime.getCalculatedOvertimePay()
                );

                overtimeRows.add(row);

                if (overtime.getAttendance() != null &&
                        overtime.getAttendance().isBiometric()) {

                    Map<String, Object> biometricRow =
                            new HashMap<>();

                    biometricRow.put(
                            "id",
                            overtime.getId()
                    );

                    biometricRow.put(
                            "attendanceId",
                            overtime.getAttendance().getId()
                    );

                    biometricRow.put(
                            "attendanceBiometric",
                            true
                    );

                    biometricRow.put(
                            "notes",
                            overtime.getNotes()
                    );

                    biometricRows.add(
                            biometricRow
                    );
                }
            }

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "totalOvertimes",
                    allOvertimes.size()
            );

            response.put(
                    "overtimes",
                    overtimeRows
            );

            response.put(
                    "biometricOvertimesCount",
                    biometricRows.size()
            );

            response.put(
                    "biometricOvertimes",
                    biometricRows
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error",
                            "Failed to load overtime debug information"
                    )
            );
        }
    }

    // =========================================================
    // CONVERSION METHODS
    // =========================================================

    private OvertimeResponse convertToOvertimeResponse(
            Overtime overtime
    ) {
        OvertimeResponse response =
                new OvertimeResponse();

        response.setId(overtime.getId());
        response.setDate(overtime.getDate());
        response.setStartTime(overtime.getStartTime());
        response.setEndTime(overtime.getEndTime());
        response.setOvertimeHours(
                overtime.getOvertimeHours()
        );
        response.setRawOvertimeHours(
                overtime.getRawOvertimeHours()
        );
        response.setStatus(overtime.getStatus());
        response.setBaseHourlyRate(
                overtime.getBaseHourlyRate()
        );
        response.setOvertimeMultiplier(
                overtime.getOvertimeMultiplier()
        );
        response.setCalculatedOvertimePay(
                overtime.getCalculatedOvertimePay()
        );
        response.setMultiplierAppliedToHours(
                overtime.getMultiplierAppliedToHours()
        );
        response.setNotes(overtime.getNotes());
        response.setCategory(overtime.getCategory());
        response.setRequestSource(overtime.getRequestSource());
        if (overtime.getRequestedBy() != null) {
            response.setRequestedByUserId(overtime.getRequestedBy().getId());
            response.setRequestedByName(overtime.getRequestedBy().getName());
        }

        if (overtime.getAttendance() != null) {
            response.setAttendanceId(
                    overtime.getAttendance().getId()
            );
        }

        Employee employee = overtime.getEmployee();

        if (employee != null) {
            response.setEmployeeId(employee.getId());
            response.setEmployeeNumber(
                    employee.getEmployeeId()
            );
            response.setEmployeeName(
                    fullName(employee)
            );
            response.setFirstName(
                    employee.getFirstName()
            );
            response.setLastName(
                    employee.getLastName()
            );
            response.setEmployeeMinimumRate(
                    employee.getMinimumRate()
            );

            if (employee.getCategory() != null) {
                response.setCategoryId(
                        employee.getCategory().getId()
                );
                response.setCategoryName(
                        employee.getCategory().getName()
                );
            }
        }

        return response;
    }

    private Overtime convertToOvertimeEntity(
            OvertimeRequest request
    ) {
        Overtime overtime = new Overtime();

        if (request.getEmployeeId() != null) {
            Employee employee = new Employee();
            employee.setId(request.getEmployeeId());

            overtime.setEmployee(employee);
        }

        overtime.setDate(request.getDate());
        overtime.setStartTime(request.getStartTime());
        overtime.setEndTime(request.getEndTime());
        overtime.setStatus(request.getStatus());
        overtime.setOvertimeMultiplier(
                request.getOvertimeMultiplier()
        );
        overtime.setMultiplierAppliedToHours(
                request.getMultiplierAppliedToHours()
        );
        overtime.setNotes(request.getNotes());

        return overtime;
    }

    private EmployeeResponse convertToEmployeeResponse(
            Employee employee
    ) {
        EmployeeResponse response =
                new EmployeeResponse();

        response.setId(employee.getId());
        response.setFirstName(
                employee.getFirstName()
        );
        response.setLastName(
                employee.getLastName()
        );
        response.setEmployeeId(
                employee.getEmployeeId()
        );
        response.setMinimumRate(
                employee.getMinimumRate()
        );

        if (employee.getCategory() != null) {
            response.setCategoryId(
                    employee.getCategory().getId()
            );

            response.setCategoryName(
                    employee.getCategory().getName()
            );
        }

        if (employee.getJob() != null) {
            Job job = employee.getJob();
            response.setJobId(job.getId());
            response.setJobName(job.getName());
            response.setJobOvertimeMultiplier(job.getOvertimeMultiplier());
            response.setApplyOvertimeMultiplierToHours(job.getApplyOvertimeMultiplierToHours());
            response.setEnforceMinimumOvertime(job.getEnforceMinimumOvertime());
            response.setMinimumOvertimeHours(job.getMinimumOvertimeHours());
            response.setAutoApproveOvertime(job.getAutoApproveOvertime());
        }

        return response;
    }

    @SuppressWarnings("unused")
    private OverviewResponse convertToOverviewResponse(
            Overview overview
    ) {
        OverviewResponse response =
                new OverviewResponse();

        response.setId(overview.getId());

        if (overview.getEmployee() != null) {
            response.setEmployeeId(
                    overview.getEmployee().getId()
            );

            response.setEmployeeName(
                    fullName(overview.getEmployee())
            );
        }

        if (overview.getOvertime() != null) {
            Overtime overtime =
                    overview.getOvertime();

            response.setOvertimeId(
                    overtime.getId()
            );

            response.setOvertimeMultiplier(
                    overtime.getOvertimeMultiplier()
            );

            response.setCalculatedOvertimePay(
                    overtime.getCalculatedOvertimePay()
            );

            response.setMultiplierAppliedToHours(
                    overtime.getMultiplierAppliedToHours()
            );

            response.setRawOvertimeHours(
                    overtime.getRawOvertimeHours()
            );
        }

        response.setOvertimeRate(
                overview.getOvertimeRate()
        );

        response.setStatus(
                overview.getStatus()
        );

        response.setDate(
                overview.getDate()
        );

        response.setHoursWorked(
                overview.getHoursWorked()
        );

        response.setOvertimeHours(
                overview.getOvertimeHours()
        );

        return response;
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    private String fullName(Employee employee) {
        if (employee == null) {
            return "";
        }

        String firstName =
                employee.getFirstName() != null
                        ? employee.getFirstName().trim()
                        : "";

        String lastName =
                employee.getLastName() != null
                        ? employee.getLastName().trim()
                        : "";

        return (firstName + " " + lastName).trim();
    }

    private String safeMessage(Exception exception) {
        if (exception == null ||
                exception.getMessage() == null ||
                exception.getMessage().isBlank()) {
            return "Unexpected server error";
        }

        return exception.getMessage();
    }

    // =========================================================
    // REQUEST / RESPONSE CLASSES
    // =========================================================

    public static class OvertimeRequest {

        private Long employeeId;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private String status = "Pending";
        private Double overtimeMultiplier = 1.5;
        private Boolean multiplierAppliedToHours = false;
        private String notes;

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Double getOvertimeMultiplier() {
            return overtimeMultiplier;
        }

        public void setOvertimeMultiplier(
                Double overtimeMultiplier
        ) {
            this.overtimeMultiplier =
                    overtimeMultiplier;
        }

        public Boolean getMultiplierAppliedToHours() {
            return multiplierAppliedToHours;
        }

        public void setMultiplierAppliedToHours(
                Boolean multiplierAppliedToHours
        ) {
            this.multiplierAppliedToHours =
                    multiplierAppliedToHours;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class OvertimeResponse {

        private Long id;

        private Long employeeId;
        private String employeeNumber;
        private String employeeName;
        private String firstName;
        private String lastName;
        private Double employeeMinimumRate;

        private Long categoryId;
        private String categoryName;
        private Long jobId;
        private String jobName;
        private Double jobOvertimeMultiplier;
        private Boolean applyOvertimeMultiplierToHours;
        private Boolean enforceMinimumOvertime;
        private Double minimumOvertimeHours;
        private Boolean autoApproveOvertime;

        private Long attendanceId;

        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;

        private Double overtimeHours;
        private Double rawOvertimeHours;

        private String status;
        private String notes;
        private String category;

        private Double baseHourlyRate;
        private Double overtimeMultiplier;
        private Double calculatedOvertimePay;
        private Boolean multiplierAppliedToHours;
        private Long requestedByUserId;
        private String requestedByName;
        private String requestSource;

        public Long getId() {
            return id;
        }

        public Long getRequestedByUserId() { return requestedByUserId; }
        public void setRequestedByUserId(Long requestedByUserId) { this.requestedByUserId = requestedByUserId; }
        public String getRequestedByName() { return requestedByName; }
        public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }
        public String getRequestSource() { return requestSource; }
        public void setRequestSource(String requestSource) { this.requestSource = requestSource; }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public String getEmployeeNumber() {
            return employeeNumber;
        }

        public void setEmployeeNumber(
                String employeeNumber
        ) {
            this.employeeNumber =
                    employeeNumber;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public void setEmployeeName(
                String employeeName
        ) {
            this.employeeName =
                    employeeName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(
                String firstName
        ) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(
                String lastName
        ) {
            this.lastName = lastName;
        }

        public Double getEmployeeMinimumRate() {
            return employeeMinimumRate;
        }

        public void setEmployeeMinimumRate(
                Double employeeMinimumRate
        ) {
            this.employeeMinimumRate =
                    employeeMinimumRate;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(
                Long categoryId
        ) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(
                String categoryName
        ) {
            this.categoryName =
                    categoryName;
        }

        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }
        public Double getJobOvertimeMultiplier() { return jobOvertimeMultiplier; }
        public void setJobOvertimeMultiplier(Double value) { this.jobOvertimeMultiplier = value; }
        public Boolean getApplyOvertimeMultiplierToHours() { return applyOvertimeMultiplierToHours; }
        public void setApplyOvertimeMultiplierToHours(Boolean value) { this.applyOvertimeMultiplierToHours = value; }
        public Boolean getEnforceMinimumOvertime() { return enforceMinimumOvertime; }
        public void setEnforceMinimumOvertime(Boolean value) { this.enforceMinimumOvertime = value; }
        public Double getMinimumOvertimeHours() { return minimumOvertimeHours; }
        public void setMinimumOvertimeHours(Double value) { this.minimumOvertimeHours = value; }
        public Boolean getAutoApproveOvertime() { return autoApproveOvertime; }
        public void setAutoApproveOvertime(Boolean value) { this.autoApproveOvertime = value; }

        public Long getAttendanceId() {
            return attendanceId;
        }

        public void setAttendanceId(
                Long attendanceId
        ) {
            this.attendanceId =
                    attendanceId;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(
                LocalTime startTime
        ) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(
                LocalTime endTime
        ) {
            this.endTime = endTime;
        }

        public Double getOvertimeHours() {
            return overtimeHours;
        }

        public void setOvertimeHours(
                Double overtimeHours
        ) {
            this.overtimeHours =
                    overtimeHours;
        }

        public Double getRawOvertimeHours() {
            return rawOvertimeHours;
        }

        public void setRawOvertimeHours(
                Double rawOvertimeHours
        ) {
            this.rawOvertimeHours =
                    rawOvertimeHours;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(
                String category
        ) {
            this.category = category;
        }

        public Double getBaseHourlyRate() {
            return baseHourlyRate;
        }

        public void setBaseHourlyRate(
                Double baseHourlyRate
        ) {
            this.baseHourlyRate =
                    baseHourlyRate;
        }

        public Double getOvertimeMultiplier() {
            return overtimeMultiplier;
        }

        public void setOvertimeMultiplier(
                Double overtimeMultiplier
        ) {
            this.overtimeMultiplier =
                    overtimeMultiplier;
        }

        public Double getCalculatedOvertimePay() {
            return calculatedOvertimePay;
        }

        public void setCalculatedOvertimePay(
                Double calculatedOvertimePay
        ) {
            this.calculatedOvertimePay =
                    calculatedOvertimePay;
        }

        public Boolean getMultiplierAppliedToHours() {
            return multiplierAppliedToHours;
        }

        public void setMultiplierAppliedToHours(
                Boolean multiplierAppliedToHours
        ) {
            this.multiplierAppliedToHours =
                    multiplierAppliedToHours;
        }
    }

    public static class EmployeeResponse {

        private Long id;
        private String firstName;
        private String lastName;
        private String employeeId;
        private Double minimumRate;
        private Long categoryId;
        private String categoryName;
        private Long jobId;
        private String jobName;
        private Double jobOvertimeMultiplier;
        private Boolean applyOvertimeMultiplierToHours;
        private Boolean enforceMinimumOvertime;
        private Double minimumOvertimeHours;
        private Boolean autoApproveOvertime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(
                String firstName
        ) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(
                String lastName
        ) {
            this.lastName = lastName;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(
                String employeeId
        ) {
            this.employeeId = employeeId;
        }

        public Double getMinimumRate() {
            return minimumRate;
        }

        public void setMinimumRate(
                Double minimumRate
        ) {
            this.minimumRate = minimumRate;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(
                Long categoryId
        ) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(
                String categoryName
        ) {
            this.categoryName =
                    categoryName;
        }

        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }
        public Double getJobOvertimeMultiplier() { return jobOvertimeMultiplier; }
        public void setJobOvertimeMultiplier(Double value) { this.jobOvertimeMultiplier = value; }
        public Boolean getApplyOvertimeMultiplierToHours() { return applyOvertimeMultiplierToHours; }
        public void setApplyOvertimeMultiplierToHours(Boolean value) { this.applyOvertimeMultiplierToHours = value; }
        public Boolean getEnforceMinimumOvertime() { return enforceMinimumOvertime; }
        public void setEnforceMinimumOvertime(Boolean value) { this.enforceMinimumOvertime = value; }
        public Double getMinimumOvertimeHours() { return minimumOvertimeHours; }
        public void setMinimumOvertimeHours(Double value) { this.minimumOvertimeHours = value; }
        public Boolean getAutoApproveOvertime() { return autoApproveOvertime; }
        public void setAutoApproveOvertime(Boolean value) { this.autoApproveOvertime = value; }
    }

    public static class ValidateOvertimeRequest {

        private List<Long> overtimeIds;

        public List<Long> getOvertimeIds() {
            return overtimeIds;
        }

        public void setOvertimeIds(
                List<Long> overtimeIds
        ) {
            this.overtimeIds = overtimeIds;
        }
    }

    public static class BatchToggleMultiplierRequest {

        private List<Long> ids;
        private Boolean multiplierAppliedToHours;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }

        public Boolean getMultiplierAppliedToHours() {
            return multiplierAppliedToHours;
        }

        public void setMultiplierAppliedToHours(
                Boolean multiplierAppliedToHours
        ) {
            this.multiplierAppliedToHours =
                    multiplierAppliedToHours;
        }
    }

    public static class BulkDeleteRequest {

        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }

    public static class BulkOvertimeRequest {

        private List<BulkOvertimeItem> overtimes;
        private boolean skipDuplicates;

        public List<BulkOvertimeItem> getOvertimes() {
            return overtimes;
        }

        public void setOvertimes(
                List<BulkOvertimeItem> overtimes
        ) {
            this.overtimes = overtimes;
        }

        public boolean isSkipDuplicates() {
            return skipDuplicates;
        }

        public void setSkipDuplicates(
                boolean skipDuplicates
        ) {
            this.skipDuplicates =
                    skipDuplicates;
        }
    }

    public static class BulkOvertimeItem {

        private Long employeeId;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;

        private Double overtimeMultiplier;
        private Boolean multiplierAppliedToHours;

        private Double overtimeHours;
        private Double rawOvertimeHours;

        private String status;
        private String notes;

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(
                Long employeeId
        ) {
            this.employeeId = employeeId;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(
                LocalTime startTime
        ) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(
                LocalTime endTime
        ) {
            this.endTime = endTime;
        }

        public Double getOvertimeMultiplier() {
            return overtimeMultiplier;
        }

        public void setOvertimeMultiplier(
                Double overtimeMultiplier
        ) {
            this.overtimeMultiplier =
                    overtimeMultiplier;
        }

        public Boolean getMultiplierAppliedToHours() {
            return multiplierAppliedToHours;
        }

        public void setMultiplierAppliedToHours(
                Boolean multiplierAppliedToHours
        ) {
            this.multiplierAppliedToHours =
                    multiplierAppliedToHours;
        }

        public Double getOvertimeHours() {
            return overtimeHours;
        }

        public void setOvertimeHours(
                Double overtimeHours
        ) {
            this.overtimeHours =
                    overtimeHours;
        }

        public Double getRawOvertimeHours() {
            return rawOvertimeHours;
        }

        public void setRawOvertimeHours(
                Double rawOvertimeHours
        ) {
            this.rawOvertimeHours =
                    rawOvertimeHours;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class BulkOvertimeForAllRequest {

        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;

        private Double overtimeMultiplier;
        private Boolean multiplierAppliedToHours;

        private List<Long> employeeIds;

        private String status;
        private String notes;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(
                LocalTime startTime
        ) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(
                LocalTime endTime
        ) {
            this.endTime = endTime;
        }

        public Double getOvertimeMultiplier() {
            return overtimeMultiplier;
        }

        public void setOvertimeMultiplier(
                Double overtimeMultiplier
        ) {
            this.overtimeMultiplier =
                    overtimeMultiplier;
        }

        public Boolean getMultiplierAppliedToHours() {
            return multiplierAppliedToHours;
        }

        public void setMultiplierAppliedToHours(
                Boolean multiplierAppliedToHours
        ) {
            this.multiplierAppliedToHours =
                    multiplierAppliedToHours;
        }

        public List<Long> getEmployeeIds() {
            return employeeIds;
        }

        public void setEmployeeIds(
                List<Long> employeeIds
        ) {
            this.employeeIds = employeeIds;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class BulkEligibilityCheck {

        private Long employeeId;
        private LocalDate date;

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(
                Long employeeId
        ) {
            this.employeeId = employeeId;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }
    }

    public static class OverviewResponse {

        private Long id;
        private Long employeeId;
        private String employeeName;
        private Long overtimeId;

        private Double overtimeRate;
        private String status;
        private LocalDate date;

        private Double hoursWorked;
        private Double overtimeHours;

        private Double overtimeMultiplier;
        private Double calculatedOvertimePay;
        private Boolean multiplierAppliedToHours;
        private Double rawOvertimeHours;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(
                Long employeeId
        ) {
            this.employeeId = employeeId;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public void setEmployeeName(
                String employeeName
        ) {
            this.employeeName =
                    employeeName;
        }

        public Long getOvertimeId() {
            return overtimeId;
        }

        public void setOvertimeId(
                Long overtimeId
        ) {
            this.overtimeId = overtimeId;
        }

        public Double getOvertimeRate() {
            return overtimeRate;
        }

        public void setOvertimeRate(
                Double overtimeRate
        ) {
            this.overtimeRate = overtimeRate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public Double getHoursWorked() {
            return hoursWorked;
        }

        public void setHoursWorked(
                Double hoursWorked
        ) {
            this.hoursWorked = hoursWorked;
        }

        public Double getOvertimeHours() {
            return overtimeHours;
        }

        public void setOvertimeHours(
                Double overtimeHours
        ) {
            this.overtimeHours =
                    overtimeHours;
        }

        public Double getOvertimeMultiplier() {
            return overtimeMultiplier;
        }

        public void setOvertimeMultiplier(
                Double overtimeMultiplier
        ) {
            this.overtimeMultiplier =
                    overtimeMultiplier;
        }

        public Double getCalculatedOvertimePay() {
            return calculatedOvertimePay;
        }

        public void setCalculatedOvertimePay(
                Double calculatedOvertimePay
        ) {
            this.calculatedOvertimePay =
                    calculatedOvertimePay;
        }

        public Boolean getMultiplierAppliedToHours() {
            return multiplierAppliedToHours;
        }

        public void setMultiplierAppliedToHours(
                Boolean multiplierAppliedToHours
        ) {
            this.multiplierAppliedToHours =
                    multiplierAppliedToHours;
        }

        public Double getRawOvertimeHours() {
            return rawOvertimeHours;
        }

        public void setRawOvertimeHours(
                Double rawOvertimeHours
        ) {
            this.rawOvertimeHours =
                    rawOvertimeHours;
        }
    }
}
