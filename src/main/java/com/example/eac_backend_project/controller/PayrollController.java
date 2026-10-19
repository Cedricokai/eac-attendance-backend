package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.LoanDeductionService;
import com.example.eac_backend_project.Service.LoanReversalService;
import com.example.eac_backend_project.Service.PayrollService;
import com.example.eac_backend_project.dto.*;
import com.example.eac_backend_project.entity.attendance.PayrollPeriod;
import com.example.eac_backend_project.entity.attendance.PayrollRecord;
import com.example.eac_backend_project.entity.attendance.PendingCredit;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.PayrollPeriodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private static final Logger logger = LoggerFactory.getLogger(PayrollController.class);

    private final PayrollService payrollService;
    private final AttendanceRepository attendanceRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final LoanDeductionService loanDeductionService;

    @Autowired
    private LoanReversalService loanReversalService;

    public PayrollController(PayrollService payrollService,
                             AttendanceRepository attendanceRepository,
                             PayrollPeriodRepository payrollPeriodRepository,
                             LoanDeductionService loanDeductionService) {
        this.payrollService = payrollService;
        this.attendanceRepository = attendanceRepository;
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.loanDeductionService = loanDeductionService;
    }

    @Autowired
    private com.example.eac_backend_project.Service.TimesheetPayrollApprovalService timesheetPayrollApproval;
    @GetMapping("/generation-review")
    public Map<String,Object> generationReview(@RequestParam Long periodId, @RequestParam(required=false) Long employeeId,
            @RequestParam(required=false) List<Long> excludedEmployeeIds) {
        return timesheetPayrollApproval.review(periodId,employeeId,excludedEmployeeIds);
    }

    // ==================== EXISTING ENDPOINTS ====================

    @GetMapping("/periods")
    public List<PayrollPeriod> getAllPeriods() {
        return payrollService.getAllPeriods();
    }

    @PostMapping("/periods")
    public ResponseEntity<PayrollPeriod> createPeriod(@RequestBody PayrollPeriod period) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createPeriod(period));
    }

    @GetMapping
    public ResponseEntity<List<PayrollRecord>> getPayroll(@RequestParam Long periodId) {
        try {
            return ResponseEntity.ok(payrollService.getPayrollForPeriod(periodId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/employee-payslip")
    public ResponseEntity<PayrollRecord> getEmployeePayslip(
            @RequestParam Long periodId,
            @RequestParam Long employeeId) {
        try {
            PayrollRecord record = payrollService.getEmployeePayslip(periodId, employeeId);
            return ResponseEntity.ok(record);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(
            value = "/generate-payslip-pdf",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generatePayslipPDF(@RequestBody Map<String, Long> request) {
        Long periodId = request.get("periodId");
        Long employeeId = request.get("employeeId");

        if (periodId == null || employeeId == null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"periodId and employeeId are required\"}").getBytes());
        }

        byte[] pdfBytes = payrollService.generatePayslipPDF(periodId, employeeId);

        if (pdfBytes == null || pdfBytes.length == 0) {
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"PDF generation returned empty content\"}").getBytes());
        }

        String filename = "payslip-" + employeeId + "-period-" + periodId + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // Separate routes prevent older servers from silently treating a scoped request as a full run.
    @PostMapping("/generate-employee")
    public ResponseEntity<?> generateEmployeePayroll(@RequestBody GeneratePayrollRequest request) {
        if (request.getEmployeeId() == null) return ResponseEntity.badRequest().body(Map.of("error", "Select an employee"));
        return generatePayroll(request);
    }

    @PostMapping("/generate-employee-with-credits")
    public ResponseEntity<?> generateEmployeePayrollWithCredits(@RequestBody GeneratePayrollWithCreditsRequest request) {
        if (request.getEmployeeId() == null) return ResponseEntity.badRequest().body(Map.of("error", "Select an employee"));
        try {
            return ResponseEntity.ok(payrollService.generatePayrollWithSelectedCredits(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generatePayroll(@RequestBody GeneratePayrollRequest request) {
        try {
            payrollService.generatePayroll(request);
            return ResponseEntity.ok(Map.of("message", "Payroll generated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate payroll: " + e.getMessage()));
        }
    }

    @PostMapping("/process")
    public ResponseEntity<?> processPayroll(@RequestParam Long periodId) {
        try {
            payrollService.processPayroll(periodId);
            return ResponseEntity.ok(Map.of("message", "Payroll processed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process payroll: " + e.getMessage()));
        }
    }

    @PostMapping("/{periodId}/process-loans")
    public ResponseEntity<?> processLoanDeductions(@PathVariable Long periodId) {
        try {
            loanDeductionService.processLoanDeductions(periodId);

            List<PayrollRecord> updatedRecords = payrollService.getPayrollForPeriod(periodId);
            long employeesWithLoans = updatedRecords.stream()
                    .filter(r -> r.getHasActiveLoans() != null && r.getHasActiveLoans())
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Loan deductions processed successfully");
            response.put("updatedCount", employeesWithLoans);
            response.put("periodId", periodId);

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process loan deductions: " + e.getMessage()));
        }
    }

    // ==================== PENDING CREDITS ENDPOINTS ====================

    /**
     * Get pending credits for a specific payroll period
     */
    @GetMapping("/pending-credits/period/{periodId}")
    public ResponseEntity<?> getPendingCreditsForPeriod(@PathVariable Long periodId) {
        try {
            List<PendingCreditDTO> pendingCredits = payrollService.getPendingCreditsForPeriod(periodId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasPendingCredits", !pendingCredits.isEmpty(),
                    "pendingCredits", pendingCredits,
                    "periodId", periodId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add a pending credit for an employee
     */
    @PostMapping("/pending-credits")
    public ResponseEntity<?> addPendingCredit(@RequestBody Map<String, Object> request) {
        try {
            Long employeeId = Long.valueOf(request.get("employeeId").toString());
            Double amount = Double.valueOf(request.get("amount").toString());
            String reason = (String) request.get("reason");
            String createdBy = (String) request.get("createdBy");

            PendingCredit credit = payrollService.addPendingCredit(employeeId, amount, reason, createdBy);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Credit added successfully",
                    "creditId", credit.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== REPORT ENDPOINTS ====================

    /**
     * Get payroll report by category and month (all data)
     */
    @GetMapping("/reports/category-month")
    public ResponseEntity<?> getCategoryMonthReport() {
        try {
            MonthlyReportResponseDTO report = payrollService.getPayrollReportByCategoryAndMonth();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Failed to generate category-month report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate report: " + e.getMessage()));
        }
    }

    /**
     * Apply direct credits to employees (without pending credits system)
     */
    @PostMapping("/apply-direct-credits")
    public ResponseEntity<?> applyDirectCredits(@RequestBody Map<String, Object> request) {
        try {
            Long periodId = Long.valueOf(request.get("periodId").toString());

            @SuppressWarnings("unchecked")
            Map<String, Double> creditsMap = (Map<String, Double>) request.get("credits");

            Map<Long, Double> creditAmounts = new HashMap<>();
            for (Map.Entry<String, Double> entry : creditsMap.entrySet()) {
                creditAmounts.put(Long.valueOf(entry.getKey()), entry.getValue());
            }

            // Store credits in session or temporary storage for the payroll generation
            // You can use a cache or simply return them to be used in the generate call

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Direct credits prepared for " + creditAmounts.size() + " employees",
                    "creditAmounts", creditAmounts
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get payroll report for specific month
     */
    @GetMapping("/reports/category-month/{yearMonth}")
    public ResponseEntity<?> getCategoryMonthReportForMonth(@PathVariable String yearMonth) {
        try {
            MonthlyReportResponseDTO report = payrollService.getPayrollReportForSpecificMonth(yearMonth);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Failed to generate category-month report for month: {}", yearMonth, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate report: " + e.getMessage()));
        }
    }

    /**
     * Add multiple pending credits at once
     */
    @PostMapping("/pending-credits/batch")
    public ResponseEntity<?> addMultiplePendingCredits(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Double> creditsData = (Map<String, Double>) request.get("credits");
            String reason = (String) request.get("reason");
            String createdBy = (String) request.get("createdBy");

            Map<Long, Double> credits = new HashMap<>();
            for (Map.Entry<String, Double> entry : creditsData.entrySet()) {
                credits.put(Long.valueOf(entry.getKey()), entry.getValue());
            }

            List<PendingCredit> savedCredits = payrollService.addMultiplePendingCredits(credits, reason, createdBy);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", savedCredits.size() + " credits added successfully",
                    "count", savedCredits.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate payroll with selected credits
     */
    @PostMapping("/generate-with-credits")
    public ResponseEntity<?> generatePayrollWithSelectedCredits(@RequestBody GeneratePayrollWithCreditsRequest request) {
        try {
            Map<String, Object> result = payrollService.generatePayrollWithSelectedCredits(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to generate payroll with credits: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Cancel a pending credit
     */
    @DeleteMapping("/pending-credits/{creditId}")
    public ResponseEntity<?> cancelPendingCredit(@PathVariable Long creditId) {
        try {
            payrollService.cancelPendingCredit(creditId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Credit cancelled successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all pending credits
     */
    @GetMapping("/pending-credits/all")
    public ResponseEntity<?> getAllPendingCredits() {
        try {
            List<PendingCreditDTO> credits = payrollService.getAllPendingCredits();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "pendingCredits", credits,
                    "count", credits.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if period has pending credits (used before showing popup)
     */
    @GetMapping("/pending-credits/check/{periodId}")
    public ResponseEntity<?> checkPendingCreditsForPeriod(@PathVariable Long periodId) {
        try {
            boolean hasPending = payrollService.hasPendingCreditsForPeriod(periodId);
            List<PendingCreditDTO> pendingCredits = payrollService.getPendingCreditsForPeriod(periodId);

            return ResponseEntity.ok(Map.of(
                    "hasPendingCredits", hasPending,
                    "pendingCredits", pendingCredits,
                    "count", pendingCredits.size(),
                    "periodId", periodId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPayrollSummary() {
        try {
            return ResponseEntity.ok(payrollService.getPayrollSummary());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get payroll summary: " + e.getMessage()));
        }
    }

    @GetMapping("/period-summary/{periodId}")
    public ResponseEntity<Map<String, Object>> getPayrollSummaryByPeriod(@PathVariable Long periodId) {
        try {
            Map<String, Object> summary = payrollService.getPayrollSummaryByPeriod(periodId);
            return ResponseEntity.ok(summary);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get payroll summary for period: " + e.getMessage()));
        }
    }

    @GetMapping("/preview/{periodId}")
    public ResponseEntity<Map<String, Object>> getPayrollPreview(@PathVariable Long periodId) {
        Map<String, Object> preview = new HashMap<>();
        // pending credits
        List<PendingCreditDTO> credits = payrollService.getPendingCreditsForPeriod(periodId);
        preview.put("pendingCredits", credits);
        preview.put("hasPendingCredits", !credits.isEmpty());
        // active loans
        List<LoanPreviewDTO> loans = payrollService.getActiveLoansForPeriod(periodId);
        preview.put("activeLoans", loans);
        preview.put("hasActiveLoans", !loans.isEmpty());
        return ResponseEntity.ok(preview);
    }
    // Add to your PayrollController.java

    @GetMapping("/reports/all")
    public ResponseEntity<MonthlyReportResponseDTO> getAllPayrollReports() {
        MonthlyReportResponseDTO reports = payrollService.getAllPayrollReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            return ResponseEntity.ok(payrollService.getPayrollDashboardStats());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get dashboard stats: " + e.getMessage()));
        }
    }

    @GetMapping("/employee-history/{employeeId}")
    public ResponseEntity<List<PayrollRecord>> getEmployeePayrollHistory(@PathVariable Long employeeId) {
        try {
            return ResponseEntity.ok(payrollService.getEmployeePayrollHistory(employeeId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/employee-summary/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeePayrollSummary(@PathVariable Long employeeId) {
        try {
            return ResponseEntity.ok(payrollService.getEmployeePayrollSummary(employeeId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No payroll records found for employee"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get employee summary: " + e.getMessage()));
        }
    }

    @PostMapping("/recalculate/{periodId}")
    public ResponseEntity<?> recalculatePayroll(@PathVariable Long periodId) {
        try {
            payrollService.recalculatePayroll(periodId);
            return ResponseEntity.ok(Map.of("message", "Payroll recalculated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to recalculate payroll: " + e.getMessage()));
        }
    }

    @DeleteMapping("/periods/{periodId}/employees/{employeeId}/temporary-allowances")
    public ResponseEntity<?> removeTemporaryAllowances(
            @PathVariable Long periodId,
            @PathVariable Long employeeId) {
        try {
            return ResponseEntity.ok(payrollService.removeTemporaryAllowances(periodId, employeeId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to remove temporary allowances: " + e.getMessage()));
        }
    }

    @DeleteMapping("/periods/{periodId}")
    public ResponseEntity<?> deletePayrollPeriod(@PathVariable Long periodId) {
        try {
            payrollService.deletePayrollPeriod(periodId);
            return ResponseEntity.ok(Map.of("message", "Payroll period deleted successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete payroll period: " + e.getMessage()));
        }
    }

    @GetMapping("/export/{periodId}")
    public ResponseEntity<List<Map<String, Object>>> exportPayrollData(@PathVariable Long periodId) {
        try {
            return ResponseEntity.ok(payrollService.exportPayrollData(periodId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/validate/{periodId}")
    public ResponseEntity<Map<String, Object>> validatePayrollData(@PathVariable Long periodId) {
        try {
            return ResponseEntity.ok(payrollService.validatePayrollData(periodId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to validate payroll data: " + e.getMessage()));
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<List<PayrollRecord>> getFilteredPayrollRecords(
            @RequestParam Long periodId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(payrollService.getFilteredPayrollRecords(periodId, category, status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/debug/attendance/{periodId}")
    public ResponseEntity<?> debugAttendance(@PathVariable Long periodId) {
        PayrollPeriod period = payrollPeriodRepository.findById(periodId).orElse(null);
        if (period == null) return ResponseEntity.badRequest().build();

        List<Object[]> results = attendanceRepository.findAllAttendanceWithStatusBetweenDates(
                period.getStartDate(), period.getEndDate());

        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("totalRecords", results.size());

        Map<String, Long> statusCount = results.stream()
                .collect(Collectors.groupingBy(
                        r -> (String) r[3],
                        Collectors.counting()
                ));

        debugInfo.put("statusBreakdown", statusCount);
        debugInfo.put("records", results.stream().limit(10).collect(Collectors.toList()));

        return ResponseEntity.ok(debugInfo);
    }

    // ==================== NEW CATEGORY-BASED ENDPOINTS ====================

    @PostMapping("/periods/category")
    public ResponseEntity<PayrollPeriod> createPeriodWithCategory(@RequestBody CreatePayrollPeriodRequest request) {
        PayrollPeriod period = payrollService.createPeriodWithCategory(
                request.getName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCategory(),
                request.isConvertExcessToOvertime()

        );
        return ResponseEntity.status(HttpStatus.CREATED).body(period);
    }

    @GetMapping("/periods/category/{category}")
    public ResponseEntity<List<PayrollPeriod>> getPeriodsByCategory(@PathVariable String category) {
        List<PayrollPeriod> periods = payrollService.getPeriodsByCategory(category);
        return ResponseEntity.ok(periods);
    }

    // ==================== LOAN REVERSAL ENDPOINTS (FIXED) ====================

    @PostMapping("/{periodId}/reverse-loans")
    public ResponseEntity<?> reverseLoanDeductions(
            @PathVariable Long periodId,
            @RequestBody LoanReversalRequestDTO request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        try {
            // Validate token
            String token = authHeader.substring(7);

            request.setPayrollPeriodId(periodId);
            request.setReversedBy("System User");

            LoanReversalResultDTO result = loanReversalService.reverseLoanDeductions(request);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("Error reversing loan deductions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reverse loan deductions: " + e.getMessage()));
        }
    }

    @GetMapping("/{periodId}/reversal-history")
    public ResponseEntity<?> getReversalHistory(@PathVariable Long periodId) {
        List<PayrollRecord> history = loanReversalService.getReversalHistory(periodId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllPeriodCategories() {
        List<String> categories = payrollService.getAllPeriodCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/summary/category/{category}")
    public ResponseEntity<Map<String, Object>> getPayrollSummaryByCategory(@PathVariable String category) {
        try {
            Map<String, Object> summary = payrollService.getPayrollSummaryByCategory(category);
            return ResponseEntity.ok(summary);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get payroll summary for category: " + e.getMessage()));
        }
    }

    // ==================== PAYROLL PERIOD MANAGEMENT ====================

    /**
     * Update an existing payroll period
     */
    @PutMapping("/periods/{id}")
    public ResponseEntity<?> updatePayrollPeriod(@PathVariable Long id, @RequestBody PayrollPeriod periodDetails) {
        try {
            // Validate the period exists
            PayrollPeriod existingPeriod = payrollService.getPayrollPeriodById(id);
            if (existingPeriod == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Payroll period not found with id: " + id));
            }

            // Validate dates
            if (periodDetails.getStartDate() != null && periodDetails.getEndDate() != null) {
                if (periodDetails.getStartDate().isAfter(periodDetails.getEndDate())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Start date cannot be after end date"));
                }
            }

            // Update the period
            PayrollPeriod updatedPeriod = payrollService.updatePayrollPeriod(id, periodDetails);

            return ResponseEntity.ok(updatedPeriod);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating payroll period: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update payroll period: " + e.getMessage()));
        }
    }

    /**
     * Delete all payroll records for a specific period (clear records)
     */
    @DeleteMapping("/periods/{id}/records")
    public ResponseEntity<?> clearPayrollRecords(@PathVariable Long id) {
        try {
            // Verify period exists
            PayrollPeriod period = payrollService.getPayrollPeriodById(id);
            if (period == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Payroll period not found with id: " + id));
            }

            // Get count of records before deletion
            int deletedCount = payrollService.deletePayrollRecordsByPeriodId(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("message", "Successfully cleared " + deletedCount + " payroll record(s) for period: " + period.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error clearing payroll records for period {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear payroll records: " + e.getMessage()));
        }
    }

    /**
     * Get a single payroll period by ID
     */
    @GetMapping("/periods/{id}")
    public ResponseEntity<?> getPayrollPeriodById(@PathVariable Long id) {
        try {
            PayrollPeriod period = payrollService.getPayrollPeriodById(id);
            if (period == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Payroll period not found with id: " + id));
            }
            return ResponseEntity.ok(period);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch payroll period: " + e.getMessage()));
        }
    }

}
