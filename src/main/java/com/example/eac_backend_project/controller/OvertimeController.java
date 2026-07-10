package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.OvertimeService;
import com.example.eac_backend_project.dto.OvertimeRequestDTO;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.OvertimeRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/overtime")
public class OvertimeController {
    private final OvertimeService overtimeService;
    private final EmployeeRepository employeeRepository;

    @Autowired
    private OvertimeRepository overtimeRepository;

    public OvertimeController(OvertimeService overtimeService, EmployeeRepository employeeRepository) {
        this.overtimeService = overtimeService;
        this.employeeRepository = employeeRepository;
    }

    // ==================== EXISTING ENDPOINTS ====================

    @GetMapping
    public ResponseEntity<List<OvertimeResponse>> getAllOvertimes() {
        try {
            List<Overtime> overtimes = overtimeService.getAllOvertimes();
            List<OvertimeResponse> responses = overtimes.stream()
                    .map(this::convertToOvertimeResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OvertimeResponse> getOvertimeById(@PathVariable Long id) {
        try {
            Overtime overtime = overtimeService.getOvertimeById(id);
            return ResponseEntity.ok(convertToOvertimeResponse(overtime));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createOvertime(@RequestBody OvertimeRequestDTO overtimeRequestDTO) {
        try {
            // Create Overtime entity
            Overtime overtime = new Overtime();

            // Set employee from ID
            Employee employee = new Employee();
            employee.setId(overtimeRequestDTO.getEmployeeId());
            overtime.setEmployee(employee);

            // Set other fields
            overtime.setDate(overtimeRequestDTO.getDate());
            overtime.setStartTime(overtimeRequestDTO.getStartTime());
            overtime.setEndTime(overtimeRequestDTO.getEndTime());
            overtime.setStatus(overtimeRequestDTO.getStatus() != null ? overtimeRequestDTO.getStatus() : "Pending");
            overtime.setBaseHourlyRate(overtimeRequestDTO.getBaseHourlyRate());
            overtime.setOvertimeMultiplier(overtimeRequestDTO.getOvertimeMultiplier());
            overtime.setOvertimeHours(overtimeRequestDTO.getOvertimeHours());
            overtime.setCalculatedOvertimePay(overtimeRequestDTO.getCalculatedOvertimePay());
            overtime.setMultiplierAppliedToHours(overtimeRequestDTO.getMultiplierAppliedToHours());
            overtime.setRawOvertimeHours(overtimeRequestDTO.getRawOvertimeHours());

            // Save through service
            Overtime createdOvertime = overtimeService.createOvertime(overtime);
            return ResponseEntity.ok(createdOvertime);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to create overtime record: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOvertime(@PathVariable Long id, @Valid @RequestBody OvertimeRequest overtimeRequest) {
        try {
            Overtime overtime = convertToOvertimeEntity(overtimeRequest);
            Overtime updatedOvertime = overtimeService.updateOvertime(id, overtime);
            return ResponseEntity.ok(convertToOvertimeResponse(updatedOvertime));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to update overtime record"
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOvertime(@PathVariable Long id) {
        try {
            overtimeService.deleteOvertime(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Overtime record deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to delete overtime record"
            ));
        }
    }

    @GetMapping("/available-employees")
    public ResponseEntity<List<EmployeeResponse>> getAvailableEmployees(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Employee> employees = overtimeService.getAvailableEmployeesForDate(date);
            List<EmployeeResponse> responses = employees.stream()
                    .map(this::convertToEmployeeResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateOvertimes(@RequestBody ValidateOvertimeRequest request) {
        try {
            if (request.getOvertimeIds() == null || request.getOvertimeIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No overtime IDs provided"
                ));
            }

            // Call the modified service method that only approves overtimes
            List<Overtime> approvedOvertimes = overtimeService.validateOvertimes(request.getOvertimeIds());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully approved " + approvedOvertimes.size() + " overtime records",
                    "data", approvedOvertimes.stream().map(this::convertToOvertimeResponse).collect(Collectors.toList())
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
                    "message", "Error approving overtimes: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getOvertimeSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            OvertimeService.OvertimeSummary summary = overtimeService.getOvertimeSummary(startDate, endDate);
            return ResponseEntity.ok(Map.of(
                    "totalHours", summary.getTotalHours(),
                    "totalPay", summary.getTotalPay(),
                    "recordCount", summary.getRecordCount()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to get overtime summary"
            ));
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<?> rejectOvertimes(@RequestBody ValidateOvertimeRequest request) {
        try {
            if (request.getOvertimeIds() == null || request.getOvertimeIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No overtime IDs provided"
                ));
            }

            List<Overtime> rejectedOvertimes = overtimeService.rejectOvertimes(request.getOvertimeIds());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully rejected " + rejectedOvertimes.size() + " records",
                    "data", rejectedOvertimes.stream().map(this::convertToOvertimeResponse).collect(Collectors.toList())
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
                    "message", "Error rejecting overtimes: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugOvertime() {
        Map<String, Object> response = new HashMap<>();

        // Get all overtimes
        List<Overtime> allOvertimes = overtimeRepository.findAll();

        response.put("totalOvertimes", allOvertimes.size());
        response.put("overtimes", allOvertimes.stream().map(o -> Map.of(
                "id", o.getId(),
                "employeeId", o.getEmployee() != null ? o.getEmployee().getId() : null,
                "attendanceId", o.getAttendance() != null ? o.getAttendance().getId() : null,
                "date", o.getDate(),
                "overtimeHours", o.getOvertimeHours(),
                "status", o.getStatus(),
                "calculatedPay", o.getCalculatedOvertimePay()
        )).collect(Collectors.toList()));

        // Check for biometric-related overtimes
        List<Overtime> biometricOvertimes = allOvertimes.stream()
                .filter(o -> o.getAttendance() != null && o.getAttendance().isBiometric())
                .collect(Collectors.toList());

        response.put("biometricOvertimesCount", biometricOvertimes.size());
        response.put("biometricOvertimes", biometricOvertimes.stream().map(o -> Map.of(
                "id", o.getId(),
                "attendanceId", o.getAttendance().getId(),
                "attendanceBiometric", o.getAttendance().isBiometric(),
                "notes", o.getNotes()
        )).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/toggle-multiplier")
    public ResponseEntity<?> toggleMultiplierAppliedToHours(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean newValue = request.get("multiplierAppliedToHours");
            if (newValue == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "multiplierAppliedToHours value is required"
                ));
            }

            Overtime updated = overtimeService.toggleMultiplierAppliedToHours(id, newValue);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Multiplier setting updated successfully",
                    "id", updated.getId(),
                    "multiplierAppliedToHours", updated.getMultiplierAppliedToHours()
            ));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to toggle multiplier: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/batch/toggle-multiplier")
    public ResponseEntity<?> batchToggleMultiplier(
            @RequestBody Map<String, Object> request) {
        try {
            List<Integer> ids = (List<Integer>) request.get("ids");
            Boolean newValue = (Boolean) request.get("multiplierAppliedToHours");

            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No overtime IDs provided"
                ));
            }

            if (newValue == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "multiplierAppliedToHours value is required"
                ));
            }

            List<Long> longIds = ids.stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            int updatedCount = overtimeService.batchToggleMultiplierAppliedToHours(longIds, newValue);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully updated " + updatedCount + " records",
                    "updatedCount", updatedCount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to batch toggle multiplier: " + e.getMessage()
            ));
        }
    }

    // ==================== NEW BULK CREATION ENDPOINTS ====================

    @PostMapping("/bulk/create")
    public ResponseEntity<?> createBulkOvertime(@RequestBody BulkOvertimeRequest request) {
        try {
            // Validate request
            if (request.getOvertimes() == null || request.getOvertimes().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No overtime records provided"
                ));
            }

            // Convert DTOs to entities
            List<Overtime> overtimes = new ArrayList<>();
            for (BulkOvertimeItem item : request.getOvertimes()) {
                Overtime overtime = new Overtime();

                Employee employee = new Employee();
                employee.setId(item.getEmployeeId());
                overtime.setEmployee(employee);

                overtime.setDate(item.getDate());
                overtime.setStartTime(item.getStartTime());
                overtime.setEndTime(item.getEndTime());
                overtime.setOvertimeMultiplier(item.getOvertimeMultiplier());
                overtime.setMultiplierAppliedToHours(item.getMultiplierAppliedToHours());
                overtime.setStatus("Pending");

                // These will be calculated by the service
                if (item.getOvertimeHours() != null) {
                    overtime.setOvertimeHours(item.getOvertimeHours());
                }
                if (item.getRawOvertimeHours() != null) {
                    overtime.setRawOvertimeHours(item.getRawOvertimeHours());
                }

                overtimes.add(overtime);
            }

            // Create bulk overtime
            List<Overtime> created = request.isSkipDuplicates()
                    ? overtimeService.createBulkOvertimeWithDuplicateCheck(overtimes, true)
                    : overtimeService.createBulkOvertime(overtimes);

            // Convert to response DTOs
            List<OvertimeResponse> responses = created.stream()
                    .map(this::convertToOvertimeResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully created " + created.size() + " overtime records",
                    "data", responses
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to create bulk overtime: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/bulk/create-for-all")
    public ResponseEntity<?> createOvertimeForAllEligible(@RequestBody BulkOvertimeForAllRequest request) {
        try {
            // Get eligible employees
            List<Employee> eligibleEmployees = overtimeService.getEligibleEmployeesForBulkOvertime(
                    request.getDate(),
                    request.getEmployeeIds()
            );

            if (eligibleEmployees.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No eligible employees found for the selected date"
                ));
            }

            // Create overtime records for all eligible employees
            List<Overtime> overtimes = new ArrayList<>();
            for (Employee employee : eligibleEmployees) {
                Overtime overtime = new Overtime();
                overtime.setEmployee(employee);
                overtime.setDate(request.getDate());
                overtime.setStartTime(request.getStartTime());
                overtime.setEndTime(request.getEndTime());
                overtime.setOvertimeMultiplier(request.getOvertimeMultiplier());
                overtime.setMultiplierAppliedToHours(request.getMultiplierAppliedToHours());
                overtime.setStatus("Pending");
                overtimes.add(overtime);
            }

            // Create bulk overtime
            List<Overtime> created = overtimeService.createBulkOvertime(overtimes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully created overtime for " + created.size() + " employees",
                    "totalEmployees", eligibleEmployees.size(),
                    "createdCount", created.size(),
                    "data", created.stream().map(this::convertToOvertimeResponse).collect(Collectors.toList())
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to create bulk overtime: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/bulk/eligible-employees")
    public ResponseEntity<?> getEligibleEmployeesForBulk(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) List<Long> employeeIds) {
        try {
            List<Employee> eligible = overtimeService.getEligibleEmployeesForBulkOvertime(date, employeeIds);

            // Fix: Explicitly type the Map creation
            List<Map<String, Object>> response = new ArrayList<>();
            for (Employee emp : eligible) {
                Map<String, Object> empMap = new HashMap<>();
                empMap.put("id", emp.getId());
                empMap.put("name", emp.getFirstName() + " " + emp.getLastName());
                empMap.put("employeeId", emp.getEmployeeId());
                empMap.put("rate", emp.getMinimumRate());
                response.add(empMap);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", eligible.size(),
                    "data", response
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/bulk/validate-list")
    public ResponseEntity<?> validateBulkEligibility(@RequestBody List<BulkEligibilityCheck> checks) {
        try {
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> validations = new ArrayList<>();

            for (BulkEligibilityCheck check : checks) {
                boolean isEligible = !overtimeRepository.existsByEmployeeIdAndDate(
                        check.getEmployeeId(),
                        check.getDate()
                );

                Map<String, Object> validationMap = new HashMap<>();
                validationMap.put("employeeId", check.getEmployeeId());
                validationMap.put("date", check.getDate());
                validationMap.put("eligible", isEligible);
                validationMap.put("message", isEligible ? "Eligible" : "Overtime already exists for this date");
                validations.add(validationMap);
            }

            result.put("success", true);
            result.put("validations", validations);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ==================== CONVERSION METHODS ====================

    private OvertimeResponse convertToOvertimeResponse(Overtime overtime) {
        OvertimeResponse response = new OvertimeResponse();
        response.setId(overtime.getId());
        response.setEmployeeId(overtime.getEmployee().getId());
        response.setEmployeeName(overtime.getEmployee().getFirstName() + " " + overtime.getEmployee().getLastName());
        response.setDate(overtime.getDate());
        response.setStartTime(overtime.getStartTime());
        response.setEndTime(overtime.getEndTime());
        response.setOvertimeHours(overtime.getOvertimeHours());
        response.setStatus(overtime.getStatus());
        response.setBaseHourlyRate(overtime.getBaseHourlyRate());
        response.setOvertimeMultiplier(overtime.getOvertimeMultiplier());
        response.setCalculatedOvertimePay(overtime.getCalculatedOvertimePay());
        response.setMultiplierAppliedToHours(overtime.getMultiplierAppliedToHours());
        response.setRawOvertimeHours(overtime.getRawOvertimeHours());
        return response;
    }

    private Overtime convertToOvertimeEntity(OvertimeRequest request) {
        Overtime overtime = new Overtime();
        Employee employee = new Employee();
        employee.setId(request.getEmployeeId());
        overtime.setEmployee(employee);
        overtime.setDate(request.getDate());
        overtime.setStartTime(request.getStartTime());
        overtime.setEndTime(request.getEndTime());
        overtime.setStatus(request.getStatus());
        overtime.setOvertimeMultiplier(request.getOvertimeMultiplier());
        return overtime;
    }

    private EmployeeResponse convertToEmployeeResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setFirstName(employee.getFirstName());
        response.setLastName(employee.getLastName());
        response.setEmployeeId(employee.getEmployeeId());
        response.setMinimumRate(employee.getMinimumRate());
        return response;
    }

    private OverviewResponse convertToOverviewResponse(Overview overview) {
        OverviewResponse response = new OverviewResponse();
        response.setId(overview.getId());
        if (overview.getEmployee() != null) {
            response.setEmployeeId(overview.getEmployee().getId());
            response.setEmployeeName(overview.getEmployee().getFirstName() + " " + overview.getEmployee().getLastName());
        }
        if (overview.getOvertime() != null) {
            response.setOvertimeId(overview.getOvertime().getId());
        }
        response.setOvertimeRate(overview.getOvertimeRate());
        response.setStatus(overview.getStatus());
        response.setDate(overview.getDate());
        response.setHoursWorked(overview.getHoursWorked());
        response.setOvertimeHours(overview.getOvertimeHours());

        // Add overtime multiplier info if available
        if (overview.getOvertime() != null) {
            response.setOvertimeMultiplier(overview.getOvertime().getOvertimeMultiplier());
            response.setCalculatedOvertimePay(overview.getOvertime().getCalculatedOvertimePay());
            response.setMultiplierAppliedToHours(overview.getOvertime().getMultiplierAppliedToHours());
            response.setRawOvertimeHours(overview.getOvertime().getRawOvertimeHours());
        }

        return response;
    }

    // ==================== DTO CLASSES ====================

    public static class OvertimeRequest {
        private Long employeeId;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private String status = "Pending";
        private Double overtimeMultiplier = 1.5;

        // Getters and Setters
        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Double getOvertimeMultiplier() { return overtimeMultiplier; }
        public void setOvertimeMultiplier(Double overtimeMultiplier) { this.overtimeMultiplier = overtimeMultiplier; }
    }

    public static class OvertimeResponse {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private Double overtimeHours;
        private String status;
        private Double baseHourlyRate;
        private Double overtimeMultiplier;
        private Double calculatedOvertimePay;
        private Boolean multiplierAppliedToHours;
        private Double rawOvertimeHours;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public Double getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Double getBaseHourlyRate() { return baseHourlyRate; }
        public void setBaseHourlyRate(Double baseHourlyRate) { this.baseHourlyRate = baseHourlyRate; }
        public Double getOvertimeMultiplier() { return overtimeMultiplier; }
        public void setOvertimeMultiplier(Double overtimeMultiplier) { this.overtimeMultiplier = overtimeMultiplier; }
        public Double getCalculatedOvertimePay() { return calculatedOvertimePay; }
        public void setCalculatedOvertimePay(Double calculatedOvertimePay) { this.calculatedOvertimePay = calculatedOvertimePay; }
        public Boolean getMultiplierAppliedToHours() { return multiplierAppliedToHours; }
        public void setMultiplierAppliedToHours(Boolean multiplierAppliedToHours) { this.multiplierAppliedToHours = multiplierAppliedToHours; }
        public Double getRawOvertimeHours() { return rawOvertimeHours; }
        public void setRawOvertimeHours(Double rawOvertimeHours) { this.rawOvertimeHours = rawOvertimeHours; }
    }

    public static class EmployeeResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String employeeId;
        private Double minimumRate;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        public Double getMinimumRate() { return minimumRate; }
        public void setMinimumRate(Double minimumRate) { this.minimumRate = minimumRate; }
    }

    public static class ValidateOvertimeRequest {
        private List<Long> overtimeIds;

        public List<Long> getOvertimeIds() { return overtimeIds; }
        public void setOvertimeIds(List<Long> overtimeIds) { this.overtimeIds = overtimeIds; }
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

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public Long getOvertimeId() { return overtimeId; }
        public void setOvertimeId(Long overtimeId) { this.overtimeId = overtimeId; }
        public Double getOvertimeRate() { return overtimeRate; }
        public void setOvertimeRate(Double overtimeRate) { this.overtimeRate = overtimeRate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public Double getHoursWorked() { return hoursWorked; }
        public void setHoursWorked(Double hoursWorked) { this.hoursWorked = hoursWorked; }
        public Double getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }
        public Double getOvertimeMultiplier() { return overtimeMultiplier; }
        public void setOvertimeMultiplier(Double overtimeMultiplier) { this.overtimeMultiplier = overtimeMultiplier; }
        public Double getCalculatedOvertimePay() { return calculatedOvertimePay; }
        public void setCalculatedOvertimePay(Double calculatedOvertimePay) { this.calculatedOvertimePay = calculatedOvertimePay; }
        public Boolean getMultiplierAppliedToHours() { return multiplierAppliedToHours; }
        public void setMultiplierAppliedToHours(Boolean multiplierAppliedToHours) { this.multiplierAppliedToHours = multiplierAppliedToHours; }
        public Double getRawOvertimeHours() { return rawOvertimeHours; }
        public void setRawOvertimeHours(Double rawOvertimeHours) { this.rawOvertimeHours = rawOvertimeHours; }
    }

    // ==================== BULK OPERATION DTOs ====================

    public static class BulkOvertimeRequest {
        private List<BulkOvertimeItem> overtimes;
        private boolean skipDuplicates = false;

        public List<BulkOvertimeItem> getOvertimes() { return overtimes; }
        public void setOvertimes(List<BulkOvertimeItem> overtimes) { this.overtimes = overtimes; }
        public boolean isSkipDuplicates() { return skipDuplicates; }
        public void setSkipDuplicates(boolean skipDuplicates) { this.skipDuplicates = skipDuplicates; }
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

        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public Double getOvertimeMultiplier() { return overtimeMultiplier; }
        public void setOvertimeMultiplier(Double overtimeMultiplier) { this.overtimeMultiplier = overtimeMultiplier; }
        public Boolean getMultiplierAppliedToHours() { return multiplierAppliedToHours; }
        public void setMultiplierAppliedToHours(Boolean multiplierAppliedToHours) { this.multiplierAppliedToHours = multiplierAppliedToHours; }
        public Double getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }
        public Double getRawOvertimeHours() { return rawOvertimeHours; }
        public void setRawOvertimeHours(Double rawOvertimeHours) { this.rawOvertimeHours = rawOvertimeHours; }
    }

    public static class BulkOvertimeForAllRequest {
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private Double overtimeMultiplier;
        private Boolean multiplierAppliedToHours;
        private List<Long> employeeIds;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public Double getOvertimeMultiplier() { return overtimeMultiplier; }
        public void setOvertimeMultiplier(Double overtimeMultiplier) { this.overtimeMultiplier = overtimeMultiplier; }
        public Boolean getMultiplierAppliedToHours() { return multiplierAppliedToHours; }
        public void setMultiplierAppliedToHours(Boolean multiplierAppliedToHours) { this.multiplierAppliedToHours = multiplierAppliedToHours; }
        public List<Long> getEmployeeIds() { return employeeIds; }
        public void setEmployeeIds(List<Long> employeeIds) { this.employeeIds = employeeIds; }
    }

    public static class BulkEligibilityCheck {
        private Long employeeId;
        private LocalDate date;

        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }
}