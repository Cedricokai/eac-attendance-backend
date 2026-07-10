package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.LoanRequestService;
import com.example.eac_backend_project.dto.LoanRequestDTO;
import com.example.eac_backend_project.dto.LoanStatusUpdateDTO;
import com.example.eac_backend_project.dto.ToggleActiveStatusDTO;
import com.example.eac_backend_project.entity.attendance.LoanRequest;
import com.example.eac_backend_project.repository.LoanRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loans")
public class LoanRequestController {

    private final LoanRequestService loanRequestService;

    @Autowired
    private LoanRequestRepository loanRequestRepository;

    public LoanRequestController(LoanRequestService loanRequestService) {
        this.loanRequestService = loanRequestService;
    }

    // ============ EXISTING ENDPOINTS ============

    @GetMapping
    public ResponseEntity<List<LoanRequest>> getAllLoanRequest() {
        return ResponseEntity.ok(loanRequestService.getAllLoanRequest());
    }

    @GetMapping("/active-display")
    public ResponseEntity<List<LoanRequest>> getActiveDisplayLoans() {
        return ResponseEntity.ok(loanRequestService.getActiveDisplayLoans());
    }

    @GetMapping("/employee/{employeeId}/active")
    public ResponseEntity<?> getActiveLoansByEmployee(@PathVariable Long employeeId) {
        try {
            List<LoanRequest> activeLoans = loanRequestService.getActiveLoansForEmployee(employeeId);
            return ResponseEntity.ok(activeLoans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // NEW: Get active display loans for employee (only non-consolidated)
    @GetMapping("/employee/{employeeId}/active-display")
    public ResponseEntity<?> getActiveDisplayLoansForEmployee(@PathVariable Long employeeId) {
        try {
            List<LoanRequest> activeLoans = loanRequestService.getActiveDisplayLoansForEmployee(employeeId);
            return ResponseEntity.ok(activeLoans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // NEW: Get complete loan history for an employee (FIXES THE 401 ERROR)
    @GetMapping("/employee/{employeeId}/history")
    public ResponseEntity<?> getEmployeeLoanHistory(@PathVariable Long employeeId) {
        try {
            Map<String, Object> history = loanRequestService.getEmployeeLoanHistory(employeeId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // NEW: Get consolidated loans for an employee
    @GetMapping("/employee/{employeeId}/consolidated")
    public ResponseEntity<?> getConsolidatedLoansForEmployee(@PathVariable Long employeeId) {
        try {
            List<LoanRequest> consolidatedLoans = loanRequestService.getConsolidatedLoansForEmployee(employeeId);
            return ResponseEntity.ok(consolidatedLoans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // NEW: Get completed loans for an employee
    @GetMapping("/employee/{employeeId}/completed")
    public ResponseEntity<?> getCompletedLoansForEmployee(@PathVariable Long employeeId) {
        try {
            List<LoanRequest> completedLoans = loanRequestService.getCompletedLoansForEmployee(employeeId);
            return ResponseEntity.ok(completedLoans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // NEW: Get employee loan status summary
    @GetMapping("/employee/{employeeId}/status")
    public ResponseEntity<?> getEmployeeLoanStatus(@PathVariable Long employeeId) {
        try {
            Map<String, Object> status = loanRequestService.getEmployeeLoanStatus(employeeId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // NEW: Get consolidated loan details
    @GetMapping("/consolidated/{consolidatedLoanId}")
    public ResponseEntity<?> getConsolidatedLoanDetails(@PathVariable Long consolidatedLoanId) {
        try {
            Map<String, Object> details = loanRequestService.getConsolidatedLoanDetails(consolidatedLoanId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // NEW: Get loan summary statistics
    @GetMapping("/statistics/summary")
    public ResponseEntity<?> getLoanSummaryStatistics() {
        try {
            Map<String, Object> stats = loanRequestService.getLoanSummaryStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createLoanRequest(@RequestBody LoanRequestDTO dto) {
        try {
            LoanRequest savedLoanRequest = loanRequestService.createLoanRequest(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedLoanRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateLoanStatus(
            @PathVariable Long id,
            @RequestBody LoanStatusUpdateDTO dto) {
        try {
            LoanRequest updatedLoan = loanRequestService.updateLoanStatus(id, dto);
            return ResponseEntity.ok(updatedLoan);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleLoanActiveStatus(
            @PathVariable Long id,
            @RequestBody ToggleActiveStatusDTO dto) {
        try {
            LoanRequest updatedLoan = loanRequestService.toggleLoanActiveStatus(id, dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Loan " + (updatedLoan.getIsActive() ? "activated" : "deactivated") + " successfully");
            response.put("loan", updatedLoan);
            response.put("isActive", updatedLoan.getIsActive());
            response.put("remainingBalance", updatedLoan.calculateRemainingBalance());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/can-toggle")
    public ResponseEntity<?> canToggleLoan(@PathVariable Long id) {
        try {
            Map<String, Object> result = loanRequestService.canActivateLoan(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ CONSOLIDATED LOAN DELETION ENDPOINTS ============

    @DeleteMapping("/consolidated/{consolidatedLoanId}/delete-with-restoration")
    public ResponseEntity<?> deleteConsolidatedLoanWithRestoration(@PathVariable Long consolidatedLoanId) {
        try {
            // First check if the loan exists and is a consolidated loan
            LoanRequest loan = loanRequestRepository.findById(consolidatedLoanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with id: " + consolidatedLoanId));

            if (!Boolean.TRUE.equals(loan.getIsAggregated())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "This loan is not a consolidated loan. Only consolidated loans can be deleted with restoration."
                ));
            }

            // Find original loans that would be restored (for response) - DO THIS BEFORE DELETION
            List<LoanRequest> originalLoansToRestore = loanRequestRepository.findAll().stream()
                    .filter(l -> consolidatedLoanId.equals(l.getPreviousLoanId()))
                    .collect(Collectors.toList());

            // Build response data BEFORE deletion (capture all needed data)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Consolidated loan #" + consolidatedLoanId + " has been deleted and " +
                    originalLoansToRestore.size() + " original loan(s) have been restored.");
            response.put("restoredLoanCount", originalLoansToRestore.size());
            response.put("restoredLoanIds", originalLoansToRestore.stream().map(LoanRequest::getId).collect(Collectors.toList()));

            // Execute deletion with restoration (this will delete the entity from database)
            loanRequestService.deleteConsolidatedLoanWithRestoration(consolidatedLoanId);

            // Return the pre-built response (no reference to deleted entity)
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete consolidated loan: " + e.getMessage()));
        }
    }

    // Preview what would happen when deleting a consolidated loan (dry run)
    @GetMapping("/consolidated/{consolidatedLoanId}/preview-deletion")
    public ResponseEntity<?> previewConsolidatedLoanDeletion(@PathVariable Long consolidatedLoanId) {
        try {
            LoanRequest consolidatedLoan = loanRequestRepository.findById(consolidatedLoanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with id: " + consolidatedLoanId));

            if (!Boolean.TRUE.equals(consolidatedLoan.getIsAggregated())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "This loan is not a consolidated loan."
                ));
            }

            // Find original loans that would be restored
            List<LoanRequest> originalLoansToRestore = loanRequestRepository.findAll().stream()
                    .filter(loan -> consolidatedLoanId.equals(loan.getPreviousLoanId()))
                    .collect(Collectors.toList());

            // Calculate totals that would be restored
            double totalOriginalAmount = originalLoansToRestore.stream()
                    .mapToDouble(loan -> loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getLoanAmount())
                    .sum();

            double totalOriginalOutstanding = originalLoansToRestore.stream()
                    .mapToDouble(LoanRequest::calculateRemainingBalance)
                    .sum();

            Map<String, Object> preview = new HashMap<>();
            preview.put("consolidatedLoan", Map.of(
                    "id", consolidatedLoan.getId(),
                    "amount", consolidatedLoan.getApprovedAmount(),
                    "totalAmount", consolidatedLoan.getAggregatedTotal(),
                    "consolidationDate", consolidatedLoan.getConsolidationDate(),
                    "consolidatedBy", consolidatedLoan.getConsolidatedBy()
            ));
            preview.put("willRestoreCount", originalLoansToRestore.size());
            preview.put("willRestoreLoans", originalLoansToRestore.stream().map(loan -> Map.of(
                    "id", loan.getId(),
                    "originalAmount", loan.getOriginalLoanAmount() != null ? loan.getOriginalLoanAmount() : loan.getLoanAmount(),
                    "approvedAmount", loan.getApprovedAmount(),
                    "remainingBalance", loan.calculateRemainingBalance(),
                    "paidAmount", loan.getPaidAmount(),
                    "status", loan.getStatus(),
                    "wasAggregated", loan.getIsAggregated()
            )).collect(Collectors.toList()));
            preview.put("totalRestoredAmount", totalOriginalAmount);
            preview.put("totalRestoredOutstanding", totalOriginalOutstanding);
            preview.put("action", "DELETE_AND_RESTORE");
            preview.put("warning", "This action cannot be undone. The consolidated loan will be permanently deleted and original loans will become active again.");

            return ResponseEntity.ok(preview);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Soft delete consolidated loan (mark as deleted without restoration)
    @PutMapping("/consolidated/{consolidatedLoanId}/soft-delete")
    public ResponseEntity<?> softDeleteConsolidatedLoan(
            @PathVariable Long consolidatedLoanId,
            @RequestBody Map<String, String> requestBody) {
        try {
            String deletedBy = requestBody.get("deletedBy");
            String reason = requestBody.get("reason");

            if (deletedBy == null || deletedBy.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "deletedBy is required"));
            }

            LoanRequest consolidatedLoan = loanRequestRepository.findById(consolidatedLoanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with id: " + consolidatedLoanId));

            if (!Boolean.TRUE.equals(consolidatedLoan.getIsAggregated())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "This loan is not a consolidated loan."
                ));
            }

            // Soft delete - mark as deleted but keep in database
            consolidatedLoan.setStatus("Deleted");
            consolidatedLoan.setIsActive(false);
            consolidatedLoan.setDeactivatedBy(deletedBy);
            consolidatedLoan.setDeactivationReason(reason != null ? reason : "Soft deleted by " + deletedBy);
            consolidatedLoan.setDeactivationDate(LocalDateTime.now());
            consolidatedLoan.setProcessingNotes((consolidatedLoan.getProcessingNotes() != null ? consolidatedLoan.getProcessingNotes() + "\n" : "") +
                    "Consolidated loan soft deleted by " + deletedBy + " on " + LocalDateTime.now() +
                    (reason != null ? ". Reason: " + reason : ""));

            LoanRequest saved = loanRequestRepository.save(consolidatedLoan);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Consolidated loan #" + consolidatedLoanId + " has been soft deleted.");
            response.put("loan", saved);
            response.put("note", "Original loans remain in consolidated state. Use restoration endpoint to fully restore.");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Restore a soft-deleted consolidated loan
    @PutMapping("/consolidated/{consolidatedLoanId}/restore")
    public ResponseEntity<?> restoreConsolidatedLoan(@PathVariable Long consolidatedLoanId) {
        try {
            LoanRequest consolidatedLoan = loanRequestRepository.findById(consolidatedLoanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with id: " + consolidatedLoanId));

            if (!"Deleted".equals(consolidatedLoan.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "This loan is not in deleted state. Current status: " + consolidatedLoan.getStatus()
                ));
            }

            // Restore the consolidated loan
            consolidatedLoan.setStatus("Approved");
            consolidatedLoan.setIsActive(true);
            consolidatedLoan.setDeactivatedBy(null);
            consolidatedLoan.setDeactivationReason(null);
            consolidatedLoan.setDeactivationDate(null);
            consolidatedLoan.setProcessingNotes((consolidatedLoan.getProcessingNotes() != null ? consolidatedLoan.getProcessingNotes() + "\n" : "") +
                    "Consolidated loan restored on " + LocalDateTime.now());

            LoanRequest saved = loanRequestRepository.save(consolidatedLoan);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Consolidated loan #" + consolidatedLoanId + " has been restored.");
            response.put("loan", saved);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Get all deletable consolidated loans
    @GetMapping("/consolidated/deletable")
    public ResponseEntity<?> getDeletableConsolidatedLoans() {
        try {
            List<LoanRequest> deletableLoans = loanRequestRepository.findAll().stream()
                    .filter(loan -> Boolean.TRUE.equals(loan.getIsAggregated()) && !"Deleted".equals(loan.getStatus()))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("count", deletableLoans.size());
            response.put("loans", deletableLoans.stream().map(loan -> {
                Map<String, Object> loanInfo = new HashMap<>();
                loanInfo.put("id", loan.getId());
                loanInfo.put("employee", loan.getEmployee().getFirstName() + " " + loan.getEmployee().getLastName());
                loanInfo.put("employeeId", loan.getEmployee().getEmployeeId());
                loanInfo.put("totalAmount", loan.getAggregatedTotal());
                loanInfo.put("consolidationDate", loan.getConsolidationDate());
                loanInfo.put("consolidatedBy", loan.getConsolidatedBy());

                // Count original loans that would be restored
                long originalCount = loanRequestRepository.findAll().stream()
                        .filter(l -> loan.getId().equals(l.getPreviousLoanId()))
                        .count();
                loanInfo.put("originalLoanCount", originalCount);

                return loanInfo;
            }).collect(Collectors.toList()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk/toggle-active")
    public ResponseEntity<?> bulkToggleActiveStatus(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> loanIds = (List<Long>) request.get("loanIds");
            Boolean activate = (Boolean) request.get("activate");
            String processedBy = (String) request.get("processedBy");
            String reason = (String) request.get("reason");

            if (loanIds == null || loanIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No loan IDs provided"));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (Long loanId : loanIds) {
                try {
                    ToggleActiveStatusDTO dto = new ToggleActiveStatusDTO();
                    dto.setActive(activate);
                    dto.setProcessedBy(processedBy);
                    dto.setReason(reason);

                    LoanRequest updated = loanRequestService.toggleLoanActiveStatus(loanId, dto);

                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", true);
                    result.put("isActive", updated.getIsActive());
                    results.add(result);
                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    results.add(result);
                    failureCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalProcessed", loanIds.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("results", results);
            response.put("message", "Bulk toggle completed: " + successCount + " succeeded, " + failureCount + " failed");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk/approve")
    public ResponseEntity<?> bulkApprove(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> loanIds = (List<Long>) request.get("loanIds");
            String processedBy = (String) request.get("processedBy");
            String notes = (String) request.get("notes");

            if (loanIds == null || loanIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No loan IDs provided"));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (Long loanId : loanIds) {
                try {
                    LoanStatusUpdateDTO dto = new LoanStatusUpdateDTO();
                    dto.setStatus("Approved");
                    dto.setProcessedBy(processedBy);
                    dto.setNotes(notes);

                    LoanRequest updated = loanRequestService.updateLoanStatus(loanId, dto);

                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", true);
                    result.put("status", updated.getStatus());
                    results.add(result);
                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    results.add(result);
                    failureCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalProcessed", loanIds.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("results", results);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk/reject")
    public ResponseEntity<?> bulkReject(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> loanIds = (List<Long>) request.get("loanIds");
            String processedBy = (String) request.get("processedBy");
            String notes = (String) request.get("notes");

            if (loanIds == null || loanIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No loan IDs provided"));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (Long loanId : loanIds) {
                try {
                    LoanStatusUpdateDTO dto = new LoanStatusUpdateDTO();
                    dto.setStatus("Rejected");
                    dto.setProcessedBy(processedBy);
                    dto.setNotes(notes);

                    LoanRequest updated = loanRequestService.updateLoanStatus(loanId, dto);

                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", true);
                    result.put("status", updated.getStatus());
                    results.add(result);
                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    results.add(result);
                    failureCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalProcessed", loanIds.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("results", results);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get pending loans
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingLoans() {
        try {
            List<LoanRequest> pendingLoans = loanRequestRepository.findByStatus("Pending");
            return ResponseEntity.ok(pendingLoans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Get loans by status
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getLoansByStatus(@PathVariable String status) {
        try {
            List<LoanRequest> loans = loanRequestRepository.findByStatus(status);
            return ResponseEntity.ok(loans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Get loans processed by a specific user
    @GetMapping("/processed-by/{processedBy}")
    public ResponseEntity<?> getLoansProcessedBy(@PathVariable String processedBy) {
        try {
            List<LoanRequest> loans = loanRequestRepository.findByProcessedBy(processedBy);
            return ResponseEntity.ok(loans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}