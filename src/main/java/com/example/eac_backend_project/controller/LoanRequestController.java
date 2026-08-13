package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.LoanRequestService;
import com.example.eac_backend_project.dto.LoanRequestDTO;
import com.example.eac_backend_project.dto.LoanStatusUpdateDTO;
import com.example.eac_backend_project.dto.ToggleActiveStatusDTO;
import com.example.eac_backend_project.entity.attendance.LoanRequest;
import com.example.eac_backend_project.repository.LoanRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loans")
public class LoanRequestController {

    private static final Logger logger = LoggerFactory.getLogger(LoanRequestController.class);

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
            LoanRequest loan = loanRequestRepository.findById(consolidatedLoanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with id: " + consolidatedLoanId));

            if (!Boolean.TRUE.equals(loan.getIsAggregated())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "This loan is not a consolidated loan. Only consolidated loans can be deleted with restoration."
                ));
            }

            List<LoanRequest> originalLoansToRestore = loanRequestRepository.findAll().stream()
                    .filter(l -> consolidatedLoanId.equals(l.getPreviousLoanId()))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Consolidated loan #" + consolidatedLoanId + " has been deleted and " +
                    originalLoansToRestore.size() + " original loan(s) have been restored.");
            response.put("restoredLoanCount", originalLoansToRestore.size());
            response.put("restoredLoanIds", originalLoansToRestore.stream().map(LoanRequest::getId).collect(Collectors.toList()));

            loanRequestService.deleteConsolidatedLoanWithRestoration(consolidatedLoanId);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete consolidated loan: " + e.getMessage()));
        }
    }

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

            List<LoanRequest> originalLoansToRestore = loanRequestRepository.findAll().stream()
                    .filter(loan -> consolidatedLoanId.equals(loan.getPreviousLoanId()))
                    .collect(Collectors.toList());

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

    // ============ BULK OPERATION ENDPOINTS (FIXED) ============

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

            // Use default processedBy if not provided
            if (processedBy == null || processedBy.trim().isEmpty()) {
                processedBy = "System";
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
                    result.put("message", "Toggle successful");
                    results.add(result);
                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    results.add(result);
                    failureCount++;
                    logger.error("Failed to toggle loan {}: {}", loanId, e.getMessage());
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
            logger.error("Error in bulk toggle: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk/approve")
    public ResponseEntity<?> bulkApprove(@RequestBody Map<String, Object> request) {
        try {
            // FIX: Handle both Integer and Long types
            Object loanIdsObj = request.get("loanIds");
            if (loanIdsObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No loan IDs provided"));
            }

            List<Long> loanIds = new ArrayList<>();

            // Handle different possible types
            if (loanIdsObj instanceof List) {
                List<?> rawList = (List<?>) loanIdsObj;
                for (Object item : rawList) {
                    if (item instanceof Number) {
                        loanIds.add(((Number) item).longValue());
                    } else if (item instanceof String) {
                        try {
                            loanIds.add(Long.parseLong((String) item));
                        } catch (NumberFormatException e) {
                            logger.warn("Could not parse loan ID: {}", item);
                        }
                    }
                }
            }

            if (loanIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No valid loan IDs provided"));
            }

            String processedBy = (String) request.get("processedBy");
            String notes = (String) request.get("notes");

            // Use default processedBy if not provided
            if (processedBy == null || processedBy.trim().isEmpty()) {
                processedBy = "System";
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (Long loanId : loanIds) {
                try {
                    LoanStatusUpdateDTO dto = new LoanStatusUpdateDTO();
                    dto.setStatus("Approved");
                    dto.setProcessedBy(processedBy);
                    dto.setNotes(notes != null && !notes.trim().isEmpty() ? notes : "Bulk approved");

                    LoanRequest updated = loanRequestService.updateLoanStatus(loanId, dto);

                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", true);
                    result.put("status", updated.getStatus());
                    result.put("message", "Approved successfully");
                    results.add(result);
                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    results.add(result);
                    failureCount++;
                    logger.error("Failed to approve loan {}: {}", loanId, e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalProcessed", loanIds.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("results", results);
            response.put("message", String.format("Bulk approval completed: %d succeeded, %d failed", successCount, failureCount));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in bulk approve: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/bulk/reject")
    public ResponseEntity<?> bulkReject(@RequestBody Map<String, Object> request) {
        try {
            // FIX: Handle both Integer and Long types
            Object loanIdsObj = request.get("loanIds");
            if (loanIdsObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No loan IDs provided"));
            }

            List<Long> loanIds = new ArrayList<>();

            // Handle different possible types
            if (loanIdsObj instanceof List) {
                List<?> rawList = (List<?>) loanIdsObj;
                for (Object item : rawList) {
                    if (item instanceof Number) {
                        loanIds.add(((Number) item).longValue());
                    } else if (item instanceof String) {
                        try {
                            loanIds.add(Long.parseLong((String) item));
                        } catch (NumberFormatException e) {
                            logger.warn("Could not parse loan ID: {}", item);
                        }
                    }
                }
            }

            if (loanIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No valid loan IDs provided"));
            }

            String processedBy = (String) request.get("processedBy");
            String notes = (String) request.get("notes");

            // Use default processedBy if not provided
            if (processedBy == null || processedBy.trim().isEmpty()) {
                processedBy = "System";
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (Long loanId : loanIds) {
                try {
                    LoanStatusUpdateDTO dto = new LoanStatusUpdateDTO();
                    dto.setStatus("Rejected");
                    dto.setProcessedBy(processedBy);
                    dto.setNotes(notes != null && !notes.trim().isEmpty() ? notes : "Bulk rejected");

                    LoanRequest updated = loanRequestService.updateLoanStatus(loanId, dto);

                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", true);
                    result.put("status", updated.getStatus());
                    result.put("message", "Rejected successfully");
                    results.add(result);
                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("loanId", loanId);
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    results.add(result);
                    failureCount++;
                    logger.error("Failed to reject loan {}: {}", loanId, e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalProcessed", loanIds.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("results", results);
            response.put("message", String.format("Bulk rejection completed: %d succeeded, %d failed", successCount, failureCount));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in bulk reject: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ ADDITIONAL QUERY ENDPOINTS ============

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