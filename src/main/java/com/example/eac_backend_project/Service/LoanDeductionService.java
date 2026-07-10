package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.LoanDeductionHistory;
import com.example.eac_backend_project.entity.attendance.LoanRequest;
import com.example.eac_backend_project.entity.attendance.PayrollPeriod;
import com.example.eac_backend_project.entity.attendance.PayrollRecord;
import com.example.eac_backend_project.repository.LoanDeductionHistoryRepository;
import com.example.eac_backend_project.repository.LoanRequestRepository;
import com.example.eac_backend_project.repository.PayrollPeriodRepository;
import com.example.eac_backend_project.repository.PayrollRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class LoanDeductionService {

    private static final Logger logger = LoggerFactory.getLogger(LoanDeductionService.class);

    @Autowired
    private LoanRequestRepository loanRepository;

    @Autowired
    private PayrollRecordRepository payrollRepository;

    @Autowired
    private LoanDeductionHistoryRepository loanDeductionHistoryRepository;

    @Autowired
    private PayrollPeriodRepository payrollPeriodRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Process loan deductions for a specific payroll period
     * This method is idempotent - running it multiple times for the same period
     * will only apply deductions once
     */
    @Transactional
    public void processLoanDeductions(Long periodId) {
        logger.info("Processing loan deductions for period ID: {}", periodId);

        // ==================== CHECK IF ALREADY PROCESSED ====================
        if (hasLoanDeductionsForPeriod(periodId)) {
            logger.warn("Loan deductions already processed for period {}. Skipping duplicate processing.", periodId);
            return;
        }

        // Get period name for history records
        String periodName = getPeriodName(periodId);
        logger.info("Processing loan deductions for period: {} ({})", periodName, periodId);

        List<PayrollRecord> records = payrollRepository.findByPeriodId(periodId);

        if (records.isEmpty()) {
            logger.warn("No payroll records found for period ID: {}", periodId);
            return;
        }

        int updatedCount = 0;
        int totalDeductionsApplied = 0;
        List<LoanDeductionHistory> historyRecords = new ArrayList<>();

        for (PayrollRecord record : records) {
            if (record.getEmployee() == null) {
                logger.warn("Payroll record {} has no associated employee, skipping", record.getId());
                continue;
            }

            Long employeeId = record.getEmployee().getId();
            logger.debug("Processing loan deductions for employee: {}", employeeId);

            try {
                // Find ALL active loans for this employee
                List<LoanRequest> activeLoans = loanRepository.findActiveLoansByEmployee(employeeId);

                if (activeLoans.isEmpty()) {
                    // Clear any stale loan flags on the payroll record
                    if (record.getHasActiveLoans() != null && record.getHasActiveLoans()) {
                        record.setHasActiveLoans(false);
                        record.setLoanDeduction(0.0);
                        record.setRemainingLoanBalance(0.0);
                        record.setLoanDetails(null);
                        payrollRepository.save(record);
                        logger.debug("Cleared stale loan flags for employee {}", employeeId);
                    }
                    continue;
                }

                // Save snapshot before applying deductions (for potential reversal)
                record.setPreLoanNetSalary(record.getNetSalary());
                record.setPreLoanLoanDeduction(record.getLoanDeduction());
                if (record.getLoanDetails() != null) {
                    record.setLoanDetailsSnapshot(record.getLoanDetails());
                }
                record.setReversalReason(null);
                record.setReversedAt(null);
                record.setReversedBy(null);

                double totalDeduction = 0.0;
                List<Map<String, Object>> loanDetails = new ArrayList<>();
                double totalRemaining = 0.0;
                double totalPaidThisMonth = 0.0;
                boolean anyLoanDeducted = false;

                // Process each active loan
                for (LoanRequest loan : activeLoans) {
                    // ==================== CRITICAL FIX: Check if this specific loan was already deducted for this period ====================
                    if (loanDeductionHistoryRepository.existsByLoanIdAndPayrollPeriodId(loan.getId(), periodId)) {
                        logger.warn("Loan {} for employee {} was already deducted for period {}. Skipping this loan.",
                                loan.getId(), employeeId, periodId);
                        continue;
                    }

                    double monthlyPayment = loan.getApprovedMonthlyPayment() != null ?
                            loan.getApprovedMonthlyPayment() :
                            loan.getMonthlyPaymentAmount();

                    double currentPaid = loan.getPaidAmount() != null ? loan.getPaidAmount() : 0.0;
                    double approvedAmount = loan.getApprovedAmount() != null ?
                            loan.getApprovedAmount() :
                            loan.getLoanAmount();
                    double remaining = approvedAmount - currentPaid;

                    boolean isConsolidated = Boolean.TRUE.equals(loan.getIsAggregated());

                    totalRemaining += remaining;

                    if (remaining > 0.01) { // Only deduct if there's actually money left
                        double deduction = Math.min(monthlyPayment, remaining);
                        totalDeduction += deduction;
                        totalPaidThisMonth += deduction;
                        anyLoanDeducted = true;

                        // Store before values for history
                        double remainingBefore = remaining;

                        // Update loan paid amount
                        double newPaidAmount = currentPaid + deduction;
                        loan.setPaidAmount(newPaidAmount);

                        // Update remaining balance
                        double newRemaining = remaining - deduction;
                        loan.setRemainingBalance(newRemaining);

                        // Check if loan is now fully paid
                        if (newRemaining <= 0.01) {
                            loan.setIsActive(false);
                            loan.setStatus("Completed");
                            loan.setCompletedDate(LocalDateTime.now());
                            logger.info("Loan {} for employee {} fully paid and completed",
                                    loan.getId(), employeeId);
                        }

                        loanRepository.save(loan);

                        // Store details for payslip and history
                        Map<String, Object> loanInfo = new HashMap<>();
                        loanInfo.put("id", loan.getId());
                        loanInfo.put("monthlyPayment", monthlyPayment);
                        loanInfo.put("deducted", deduction);
                        loanInfo.put("remaining", newRemaining);
                        loanInfo.put("remainingBefore", remainingBefore);
                        loanInfo.put("originalAmount", approvedAmount);
                        loanInfo.put("isConsolidated", isConsolidated);
                        loanInfo.put("paidToDate", newPaidAmount);
                        loanInfo.put("loanStatus", newRemaining <= 0.01 ? "COMPLETED" : "ACTIVE");

                        if (isConsolidated && loan.getAggregatedFromLoanIds() != null) {
                            loanInfo.put("consolidatedFrom", loan.getAggregatedFromLoanIds());
                            loanInfo.put("consolidationDate", loan.getConsolidationDate());
                        }

                        loanDetails.add(loanInfo);

                        // ==================== CREATE HISTORY RECORD FOR THIS DEDUCTION ====================
                        LoanDeductionHistory history = new LoanDeductionHistory();
                        history.setLoanId(loan.getId());
                        history.setEmployeeId(employeeId);
                        history.setPayrollPeriodId(periodId);
                        history.setPayrollPeriodName(periodName);
                        history.setDeductionAmount(deduction);
                        history.setLoanDeductionAmount(deduction);
                        history.setRemainingBalanceBefore(remainingBefore);
                        history.setRemainingBalanceAfter(newRemaining);
                        history.setAppliedBy("SYSTEM");
                        history.setAppliedDate(LocalDateTime.now());
                        history.setPayrollRecordId(record.getId());
                        history.setProcessedDate(LocalDateTime.now());
                        history.setProcessedBy("SYSTEM");
                        history.setIsReversed(false);

                        // Store snapshots as JSON for audit trail
                        try {
                            history.setDeductionSnapshot(objectMapper.writeValueAsString(loanInfo));
                        } catch (Exception e) {
                            logger.error("Failed to serialize deduction snapshot for loan {}", loan.getId(), e);
                        }

                        historyRecords.add(history);

                        logger.debug("Applied deduction of {} to loan {} for employee {}. New remaining: {}",
                                deduction, loan.getId(), employeeId, newRemaining);
                    } else {
                        logger.debug("Loan {} for employee {} has zero or negative remaining balance ({}). Skipping deduction.",
                                loan.getId(), employeeId, remaining);

                        // If loan has no remaining balance but is still marked active, fix it
                        if (Boolean.TRUE.equals(loan.getIsActive())) {
                            loan.setIsActive(false);
                            loan.setStatus("Completed");
                            loan.setCompletedDate(LocalDateTime.now());
                            loanRepository.save(loan);
                            logger.info("Fixed loan {} - marked as completed due to zero balance", loan.getId());
                        }
                    }
                }

                // Update payroll record with deduction information
                if (totalDeduction > 0 || !loanDetails.isEmpty()) {
                    record.setLoanDeduction(totalDeduction);
                    record.setHasActiveLoans(anyLoanDeducted);

                    // Calculate total remaining balance after all deductions
                    double totalRemainingAfter = 0.0;
                    for (LoanRequest loan : activeLoans) {
                        double approved = loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getLoanAmount();
                        double paid = loan.getPaidAmount() != null ? loan.getPaidAmount() : 0.0;
                        totalRemainingAfter += (approved - paid);
                    }
                    record.setRemainingLoanBalance(totalRemainingAfter);

                    // Serialize loan details to JSON
                    try {
                        record.setLoanDetails(objectMapper.writeValueAsString(loanDetails));
                    } catch (Exception e) {
                        logger.error("Failed to serialize loan details for employee {}: {}",
                                employeeId, e.getMessage());
                    }

                    // Create loan summary
                    Map<String, Object> loanSummary = new HashMap<>();
                    loanSummary.put("totalMonthlyPayment", totalDeduction);
                    loanSummary.put("totalRemaining", totalRemainingAfter);
                    loanSummary.put("activeLoanCount", activeLoans.size());
                    loanSummary.put("loansDeductedThisPeriod", loanDetails.size());
                    loanSummary.put("consolidatedLoanCount",
                            activeLoans.stream().filter(l -> Boolean.TRUE.equals(l.getIsAggregated())).count());
                    loanSummary.put("periodId", periodId);
                    loanSummary.put("periodName", periodName);
                    loanSummary.put("processedDate", LocalDateTime.now().toString());

                    try {
                        record.setLoanSummary(objectMapper.writeValueAsString(loanSummary));
                    } catch (Exception e) {
                        logger.error("Failed to serialize loan summary", e);
                    }

                    // Update net salary (subtract loan deduction)
                    double originalNet = record.getNetSalary() != null ? record.getNetSalary() : 0.0;
                    double newNetSalary = originalNet - totalDeduction;
                    record.setNetSalary(newNetSalary);

                    payrollRepository.save(record);
                    updatedCount++;
                    totalDeductionsApplied += loanDetails.size();

                    logger.info("Applied total loan deduction of {} for employee {} across {} loans. Net changed from {} to {}",
                            totalDeduction, employeeId, loanDetails.size(), originalNet, newNetSalary);
                } else if (record.getHasActiveLoans() != null && record.getHasActiveLoans()) {
                    // No deductions were applied but record still has active loan flag - clear it
                    record.setHasActiveLoans(false);
                    record.setLoanDeduction(0.0);
                    record.setRemainingLoanBalance(0.0);
                    record.setLoanDetails(null);
                    payrollRepository.save(record);
                    logger.debug("Cleared active loan flags for employee {} (no deductions applied)", employeeId);
                }
            } catch (Exception e) {
                logger.error("Failed to process loans for employee {}: {}",
                        record.getEmployee() != null ? record.getEmployee().getId() : "unknown",
                        e.getMessage(), e);
            }
        }

        // ==================== SAVE ALL HISTORY RECORDS ====================
        if (!historyRecords.isEmpty()) {
            try {
                loanDeductionHistoryRepository.saveAll(historyRecords);
                logger.info("Saved {} loan deduction history records for period {}", historyRecords.size(), periodId);
            } catch (Exception e) {
                logger.error("Failed to save loan deduction history records for period {}", periodId, e);
                throw new RuntimeException("Failed to save loan deduction history", e);
            }
        }

        logger.info("Loan deductions completed for period {}. Updated {} payroll records, applied {} loan deductions, saved {} history records.",
                periodId, updatedCount, totalDeductionsApplied, historyRecords.size());
    }

    /**
     * Check if loan deductions have already been processed for a period
     * This provides idempotency - prevents duplicate deductions for the same period
     */
    public boolean hasLoanDeductionsForPeriod(Long periodId) {
        boolean hasHistory = loanDeductionHistoryRepository.existsByPayrollPeriodId(periodId);
        logger.debug("Period {} has existing loan deduction history: {}", periodId, hasHistory);
        return hasHistory;
    }

    /**
     * Check if a specific loan has been deducted for a specific period
     */
    public boolean hasLoanBeenDeductedForPeriod(Long loanId, Long periodId) {
        return loanDeductionHistoryRepository.existsByLoanIdAndPayrollPeriodId(loanId, periodId);
    }

    /**
     * Get the total amount deducted for a specific loan across all periods
     */
    public Double getTotalDeductedForLoan(Long loanId) {
        Double total = loanDeductionHistoryRepository.getTotalDeductedForLoan(loanId);
        return total != null ? total : 0.0;
    }

    /**
     * Get all deduction history for a specific loan
     */
    public List<LoanDeductionHistory> getLoanDeductionHistory(Long loanId) {
        return loanDeductionHistoryRepository.findByLoanId(loanId);
    }

    /**
     * Get all deduction history for a specific payroll period
     */
    public List<LoanDeductionHistory> getPeriodDeductionHistory(Long periodId) {
        return loanDeductionHistoryRepository.findByPayrollPeriodId(periodId);
    }

    /**
     * Get period name by ID
     */
    private String getPeriodName(Long periodId) {
        try {
            return payrollPeriodRepository.findById(periodId)
                    .map(PayrollPeriod::getName)
                    .orElse("Period " + periodId);
        } catch (Exception e) {
            logger.warn("Could not fetch period name for ID {}: {}", periodId, e.getMessage());
            return "Period " + periodId;
        }
    }

    /**
     * Get employee loan snapshot for display (does not modify data)
     */
    public Map<String, Object> getEmployeeLoanSnapshot(Long employeeId) {
        List<LoanRequest> activeLoans = loanRepository.findActiveLoansByEmployee(employeeId);

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("hasActiveLoans", !activeLoans.isEmpty());
        snapshot.put("activeLoanCount", activeLoans.size());

        double totalRemaining = 0;
        double totalMonthlyPayment = 0;
        List<Map<String, Object>> loanDetails = new ArrayList<>();

        for (LoanRequest loan : activeLoans) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", loan.getId());
            detail.put("amount", loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getLoanAmount());
            detail.put("remaining", loan.calculateRemainingBalance());
            detail.put("monthlyPayment", loan.getApprovedMonthlyPayment() != null ?
                    loan.getApprovedMonthlyPayment() : loan.getMonthlyPaymentAmount());
            detail.put("isConsolidated", loan.getIsAggregated());
            detail.put("originalAmount", loan.getOriginalLoanAmount());
            detail.put("paidToDate", loan.getPaidAmount() != null ? loan.getPaidAmount() : 0.0);

            totalRemaining += loan.calculateRemainingBalance();
            totalMonthlyPayment += (loan.getApprovedMonthlyPayment() != null ?
                    loan.getApprovedMonthlyPayment() : loan.getMonthlyPaymentAmount());

            loanDetails.add(detail);
        }

        snapshot.put("totalRemaining", totalRemaining);
        snapshot.put("totalMonthlyPayment", totalMonthlyPayment);
        snapshot.put("loans", loanDetails);

        // Add total paid across all loans for this employee
        List<LoanRequest> allLoans = loanRepository.findAllLoansByEmployee(employeeId);
        double totalPaid = allLoans.stream()
                .mapToDouble(l -> l.getPaidAmount() != null ? l.getPaidAmount() : 0.0)
                .sum();
        snapshot.put("totalPaidToDate", totalPaid);

        return snapshot;
    }

    /**
     * Verify loan deduction integrity for a period
     * This can be used to audit whether deductions were applied correctly
     */
    public Map<String, Object> verifyPeriodDeductions(Long periodId) {
        Map<String, Object> verification = new HashMap<>();
        verification.put("periodId", periodId);
        verification.put("hasHistory", hasLoanDeductionsForPeriod(periodId));

        List<LoanDeductionHistory> history = getPeriodDeductionHistory(periodId);
        verification.put("historyRecordCount", history.size());

        List<PayrollRecord> records = payrollRepository.findByPeriodId(periodId);
        verification.put("payrollRecordCount", records.size());

        long recordsWithLoanDeductions = records.stream()
                .filter(r -> r.getLoanDeduction() != null && r.getLoanDeduction() > 0)
                .count();
        verification.put("recordsWithLoanDeductions", recordsWithLoanDeductions);

        double totalDeductedFromHistory = history.stream()
                .mapToDouble(LoanDeductionHistory::getDeductionAmount)
                .sum();
        verification.put("totalDeductedFromHistory", totalDeductedFromHistory);

        double totalDeductedFromPayroll = records.stream()
                .mapToDouble(r -> r.getLoanDeduction() != null ? r.getLoanDeduction() : 0.0)
                .sum();
        verification.put("totalDeductedFromPayroll", totalDeductedFromPayroll);

        boolean isConsistent = Math.abs(totalDeductedFromHistory - totalDeductedFromPayroll) < 0.01;
        verification.put("isConsistent", isConsistent);

        if (!isConsistent) {
            verification.put("warning", "Mismatch between history total and payroll total!");
        }

        return verification;
    }

    /**
     * Reverts all loan deductions that were applied for a given payroll period.
     * This restores loan balances and deletes the associated history entries.
     */
    @Transactional
    public void revertLoanDeductionsForPeriod(Long periodId) {
        logger.info("Reverting loan deductions for period ID: {}", periodId);

        List<LoanDeductionHistory> historyList = loanDeductionHistoryRepository.findByPayrollPeriodId(periodId);
        if (historyList.isEmpty()) {
            logger.info("No loan deduction history found for period {}", periodId);
            return;
        }

        int revertedLoans = 0;
        int reactivatedLoans = 0;

        for (LoanDeductionHistory history : historyList) {
            LoanRequest loan = loanRepository.findById(history.getLoanId()).orElse(null);
            if (loan == null) {
                logger.warn("Loan {} not found, skipping revert", history.getLoanId());
                continue;
            }

            double deduction = history.getDeductionAmount();
            double currentPaid = loan.getPaidAmount() != null ? loan.getPaidAmount() : 0.0;
            double currentRemaining = loan.getRemainingBalance() != null ? loan.getRemainingBalance() : 0.0;

            // Restore previous values
            double restoredPaid = currentPaid - deduction;
            double restoredRemaining = currentRemaining + deduction;

            loan.setPaidAmount(restoredPaid);
            loan.setRemainingBalance(restoredRemaining);

            // If the loan was completed due to this deduction, reactivate it
            if ("Completed".equals(loan.getStatus()) && restoredRemaining > 0.01) {
                loan.setStatus("Approved");
                loan.setIsActive(true);
                loan.setCompletedDate(null);
                reactivatedLoans++;
                logger.info("Reactivated loan {} (remaining balance restored to {})", loan.getId(), restoredRemaining);
            } else {
                logger.debug("Reverted deduction of {} from loan {}: paid={}, remaining={}",
                        deduction, loan.getId(), restoredPaid, restoredRemaining);
            }

            loanRepository.save(loan);
            revertedLoans++;
        }

        // Delete all history entries for this period
        loanDeductionHistoryRepository.deleteByPayrollPeriodId(periodId);
        logger.info("Deleted {} loan deduction history records for period {}", historyList.size(), periodId);

        logger.info("Loan deduction revert completed: reverted {} loans, reactivated {} loans for period {}",
                revertedLoans, reactivatedLoans, periodId);
    }
}



