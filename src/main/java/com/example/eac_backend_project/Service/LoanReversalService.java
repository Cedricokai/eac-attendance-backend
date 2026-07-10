package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.LoanReversalRequestDTO;
import com.example.eac_backend_project.dto.LoanReversalResultDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.LoanRequest;
import com.example.eac_backend_project.entity.attendance.PayrollRecord;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.LoanRequestRepository;
import com.example.eac_backend_project.repository.PayrollRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class LoanReversalService {

    private static final Logger logger = LoggerFactory.getLogger(LoanReversalService.class);

    @Autowired
    private PayrollRecordRepository payrollRepository;

    @Autowired
    private LoanRequestRepository loanRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public LoanReversalResultDTO reverseLoanDeductions(LoanReversalRequestDTO request) {
        LoanReversalResultDTO result = new LoanReversalResultDTO();
        result.setSuccess(false);

        try {
            logger.info("Starting loan deduction reversal for period ID: {}, Employee ID: {}",
                    request.getPayrollPeriodId(), request.getEmployeeId());

            // Find affected payroll records
            List<PayrollRecord> records = findAffectedRecords(request);

            if (records.isEmpty()) {
                result.setMessage("No payroll records found for the specified criteria");
                result.setSuccess(true);
                return result;
            }

            result.setTotalRecordsProcessed(records.size());

            // Process each record
            for (PayrollRecord record : records) {
                LoanReversalResultDTO.ReversalDetail detail = reverseSingleRecord(record, request);
                if (detail != null) {
                    result.getDetails().add(detail);
                    result.setTotalRecordsReversed(result.getTotalRecordsReversed() + 1);
                }
            }

            result.setSuccess(true);
            result.setMessage(String.format("Successfully reversed loan deductions for %d out of %d employees",
                    result.getTotalRecordsReversed(), result.getTotalRecordsProcessed()));

            logger.info("Loan reversal completed. Processed: {}, Reversed: {}",
                    result.getTotalRecordsProcessed(), result.getTotalRecordsReversed());

        } catch (Exception e) {
            logger.error("Failed to reverse loan deductions", e);
            result.setMessage("Failed to reverse loan deductions: " + e.getMessage());
        }

        return result;
    }

    private List<PayrollRecord> findAffectedRecords(LoanReversalRequestDTO request) {
        if (request.getEmployeeId() != null) {
            // For specific employee
            Optional<PayrollRecord> record = payrollRepository.findByPeriodIdAndEmployeeId(
                    request.getPayrollPeriodId(), request.getEmployeeId());
            return record.map(List::of).orElseGet(List::of);
        } else {
            // For all employees in the period
            return payrollRepository.findByPeriodId(request.getPayrollPeriodId());
        }
    }

    private LoanReversalResultDTO.ReversalDetail reverseSingleRecord(PayrollRecord record,
                                                                     LoanReversalRequestDTO request) {
        LoanReversalResultDTO.ReversalDetail detail = new LoanReversalResultDTO.ReversalDetail();

        try {
            // Get employee info
            Employee employee = record.getEmployee();
            if (employee != null) {
                detail.setEmployeeId(employee.getId());
                detail.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
            }

            // Check if this record has loan deductions to reverse
            if (record.getLoanDeduction() == null || record.getLoanDeduction() <= 0) {
                detail.setStatus("SKIPPED - No loan deduction found");
                detail.setAmountReversed(0.0);
                return detail;
            }

            double amountReversed = record.getLoanDeduction();
            detail.setAmountReversed(amountReversed);
            detail.setOriginalNetSalary(record.getNetSalary());

            // Restore net salary if requested
            if (request.isRestoreOriginalNetSalary()) {
                // Use stored pre-loan net salary if available
                Double restoredNetSalary = record.getPreLoanNetSalary() != null ?
                        record.getPreLoanNetSalary() :
                        record.getNetSalary() + record.getLoanDeduction();

                record.setNetSalary(restoredNetSalary);
                detail.setRestoredNetSalary(restoredNetSalary);
            }

            // Reverse loan balances in LoanRequest table
            if (record.getLoanDetails() != null && !record.getLoanDetails().isEmpty()) {
                reverseLoanBalances(record, amountReversed);
            }

            // Clear loan-related fields on payroll record
            record.setLoanDeduction(0.0);
            record.setHasActiveLoans(false);
            record.setRemainingLoanBalance(0.0);

            // Store reversal information
            record.setReversalReason(request.getReason());
            record.setReversedAt(LocalDateTime.now());
            record.setReversedBy(request.getReversedBy());

            // Keep the original loan details for audit, but clear current
            if (record.getLoanDetailsSnapshot() == null) {
                record.setLoanDetailsSnapshot(record.getLoanDetails());
            }
            record.setLoanDetails(null);
            record.setLoanSummary(null);

            payrollRepository.save(record);

            detail.setStatus("SUCCESS");
            logger.info("Successfully reversed loan deduction of {} for employee {}",
                    amountReversed, detail.getEmployeeId());

        } catch (Exception e) {
            logger.error("Failed to reverse record for employee {}", detail.getEmployeeId(), e);
            detail.setStatus("FAILED - " + e.getMessage());
        }

        return detail;
    }

    private void reverseLoanBalances(PayrollRecord record, double totalAmountReversed) throws Exception {
        // Parse loan details from JSON
        List<Map<String, Object>> loanDetails = objectMapper.readValue(
                record.getLoanDetails(),
                new TypeReference<List<Map<String, Object>>>() {}
        );

        for (Map<String, Object> loanInfo : loanDetails) {
            Long loanId = ((Number) loanInfo.get("id")).longValue();
            Double deductedAmount = ((Number) loanInfo.get("deducted")).doubleValue();

            LoanRequest loan = loanRepository.findById(loanId).orElse(null);
            if (loan != null) {
                // Restore paid amount
                double currentPaid = loan.getPaidAmount() != null ? loan.getPaidAmount() : 0.0;
                double newPaid = Math.max(0, currentPaid - deductedAmount);
                loan.setPaidAmount(newPaid);

                // Recalculate remaining balance
                double approvedAmount = loan.getApprovedAmount() != null ?
                        loan.getApprovedAmount() : loan.getLoanAmount();
                double newRemaining = approvedAmount - newPaid;
                loan.setRemainingBalance(newRemaining);

                // Reactivate loan if it was fully paid and now has balance
                if (!loan.getIsActive() && newRemaining > 0.01) {
                    loan.setIsActive(true);
                    loan.setStatus("Approved");
                    loan.setProcessingNotes("Reactivated after reversal on " + LocalDateTime.now());
                }

                // Handle consolidated loans - mark as no longer aggregated if needed
                if (Boolean.TRUE.equals(loan.getIsAggregated()) && newRemaining <= 0) {
                    // This consolidated loan is now fully paid after reversal?
                    // Keep as is, just log
                    logger.info("Consolidated loan {} remaining after reversal: {}", loanId, newRemaining);
                }

                loanRepository.save(loan);
                logger.info("Restored loan {}: added back {} to paid amount, new remaining: {}",
                        loanId, deductedAmount, newRemaining);
            }
        }
    }

    // Method to get reversal history for a period
    public List<PayrollRecord> getReversalHistory(Long periodId) {
        List<PayrollRecord> records = payrollRepository.findByPeriodId(periodId);
        return records.stream()
                .filter(r -> r.getReversedAt() != null)
                .toList();
    }
}