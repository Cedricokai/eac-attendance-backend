package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.dto.LoanRequestDTO;
import com.example.eac_backend_project.dto.LoanStatusUpdateDTO;
import com.example.eac_backend_project.dto.ToggleActiveStatusDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.LoanRequest;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.LoanRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LoanRequestService {

    private static final Logger logger = LoggerFactory.getLogger(LoanRequestService.class);

    @Autowired
    private LoanRequestRepository loanRequestRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailService emailService;

    public List<LoanRequest> getAllLoanRequest() {
        return loanRequestRepository.findAll();
    }

    public List<LoanRequest> getActiveDisplayLoans() {
        return loanRequestRepository.findAll().stream()
                .filter(loan -> Boolean.TRUE.equals(loan.getIsActive()) &&
                        !Boolean.TRUE.equals(loan.getIsAggregated()) &&
                        "Approved".equals(loan.getStatus()))
                .collect(Collectors.toList());
    }

    public List<LoanRequest> getActiveDisplayLoansForEmployee(Long employeeId) {
        return loanRequestRepository.findActiveNonAggregatedLoansByEmployee(employeeId);
    }

    public Map<String, Object> getEmployeeLoanHistory(Long employeeId) {
        Map<String, Object> history = new HashMap<>();
        List<LoanRequest> allLoans = loanRequestRepository.findAllLoansByEmployee(employeeId);
        List<LoanRequest> activeLoans = loanRequestRepository.findActiveNonAggregatedLoansByEmployee(employeeId);
        List<LoanRequest> consolidatedLoans = loanRequestRepository.findAggregatedLoansByEmployee(employeeId);
        List<LoanRequest> completedLoans = loanRequestRepository.findCompletedLoansByEmployee(employeeId);
        history.put("allLoans", allLoans);
        history.put("activeLoans", activeLoans);
        history.put("consolidatedLoans", consolidatedLoans);
        history.put("completedLoans", completedLoans);
        history.put("hasActiveLoans", !activeLoans.isEmpty());
        history.put("hasConsolidatedHistory", !consolidatedLoans.isEmpty());
        history.put("hasCompletedLoans", !completedLoans.isEmpty());
        double totalBorrowed = allLoans.stream()
                .mapToDouble(loan -> loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getLoanAmount())
                .sum();
        double totalPaid = allLoans.stream()
                .mapToDouble(loan -> loan.getPaidAmount() != null ? loan.getPaidAmount() : 0.0)
                .sum();
        double totalOutstanding = activeLoans.stream()
                .mapToDouble(LoanRequest::calculateRemainingBalance)
                .sum();
        history.put("totalBorrowed", totalBorrowed);
        history.put("totalPaid", totalPaid);
        history.put("totalOutstanding", totalOutstanding);
        return history;
    }

    @Transactional
    public LoanRequest createLoanRequest(LoanRequestDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        List<LoanRequest> existingActiveLoans = loanRequestRepository.findActiveNonAggregatedLoansByEmployee(employee.getId());

        LoanRequest loanRequest = new LoanRequest();
        loanRequest.setEmployee(employee);
        loanRequest.setLoanAmount(dto.getAmount());
        loanRequest.setMonthlyPaymentAmount(dto.getMonthlyPaymentAmount());
        loanRequest.setPurpose(dto.getPurpose());
        loanRequest.setRepaymentPeriod(dto.getRepaymentPeriod());
        loanRequest.setOriginalLoanAmount(dto.getAmount());

        if (!existingActiveLoans.isEmpty()) {
            double totalOutstanding = existingActiveLoans.stream()
                    .mapToDouble(LoanRequest::calculateRemainingBalance)
                    .sum();
            logger.info("Employee {} has existing active loans totaling {}", employee.getId(), totalOutstanding);
            loanRequest.setProcessingNotes("Employee has " + existingActiveLoans.size() + " existing active loan(s) totaling " + totalOutstanding);
        }

        LoanRequest savedLoan = loanRequestRepository.save(loanRequest);

        emailService.sendLoanRequestNotificationToAll(savedLoan);
        emailService.sendLoanRequestSubmittedConfirmation(savedLoan);

        return savedLoan;
    }

    public Map<String, Object> getEmployeeLoanStatus(Long employeeId) {
        Map<String, Object> status = new HashMap<>();
        List<LoanRequest> activeLoans = loanRequestRepository.findActiveNonAggregatedLoansByEmployee(employeeId);
        List<LoanRequest> allLoans = loanRequestRepository.findAllLoansByEmployee(employeeId);
        List<LoanRequest> consolidatedLoans = loanRequestRepository.findAggregatedLoansByEmployee(employeeId);
        double totalOutstanding = activeLoans.stream()
                .mapToDouble(LoanRequest::calculateRemainingBalance)
                .sum();
        double totalMonthlyPayment = activeLoans.stream()
                .mapToDouble(loan -> loan.getApprovedMonthlyPayment() != null ?
                        loan.getApprovedMonthlyPayment() : loan.getMonthlyPaymentAmount())
                .sum();
        status.put("hasActiveLoans", !activeLoans.isEmpty());
        status.put("activeLoanCount", activeLoans.size());
        status.put("totalOutstanding", totalOutstanding);
        status.put("totalMonthlyPayment", totalMonthlyPayment);
        status.put("activeLoans", activeLoans);
        status.put("loanHistory", allLoans);
        status.put("consolidatedLoanCount", consolidatedLoans.size());
        return status;
    }

    @Transactional
    public LoanRequest updateLoanStatus(Long id, LoanStatusUpdateDTO dto) {
        LoanRequest loan = loanRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        boolean shouldConsolidate = dto.getConsolidateWithExisting() != null && dto.getConsolidateWithExisting();

        if ("Approved".equals(dto.getStatus()) && shouldConsolidate) {
            return approveWithConsolidation(loan, dto);
        } else {
            LoanRequest updatedLoan = updateLoanStatusStandard(loan, dto);
            if ("Approved".equals(dto.getStatus())) {
                emailService.sendLoanApprovalNotification(updatedLoan, dto.getNotes());
            } else if ("Rejected".equals(dto.getStatus())) {
                emailService.sendLoanRejectionNotification(updatedLoan, dto.getNotes());
            }
            return updatedLoan;
        }
    }

    @Transactional
    private LoanRequest approveWithConsolidation(LoanRequest newLoan, LoanStatusUpdateDTO dto) {
        Employee employee = newLoan.getEmployee();
        List<LoanRequest> existingActiveLoans = loanRequestRepository.findActiveNonAggregatedLoansByEmployee(employee.getId());

        if (existingActiveLoans.isEmpty()) {
            logger.info("No existing active loans found for employee {}, proceeding with standard approval", employee.getId());
            LoanRequest updatedLoan = updateLoanStatusStandard(newLoan, dto);
            emailService.sendLoanApprovalNotification(updatedLoan, dto.getNotes());
            return updatedLoan;
        }

        logger.info("Consolidating new loan with {} existing active loans for employee {}",
                existingActiveLoans.size(), employee.getId());

        double existingTotalOutstanding = existingActiveLoans.stream()
                .mapToDouble(LoanRequest::calculateRemainingBalance)
                .sum();

        double newLoanAmount = dto.getApprovedAmount() != null ? dto.getApprovedAmount() : newLoan.getLoanAmount();
        double consolidatedTotal = existingTotalOutstanding + newLoanAmount;

        // ===== FIX: First, mark the NEW LOAN as consolidated/inactive =====
        newLoan.setStatus("Consolidated");  // Change from "Pending" to "Consolidated"
        newLoan.setIsActive(false);
        newLoan.setIsAggregated(true);
        newLoan.setProcessedBy(dto.getProcessedBy());
        newLoan.setProcessedDate(LocalDateTime.now());
        newLoan.setProcessingNotes((newLoan.getProcessingNotes() != null ? newLoan.getProcessingNotes() + "\n" : "") +
                String.format("Consolidated into loan on %s with existing active loans. Amount: %.2f",
                        LocalDateTime.now(), newLoanAmount));
        loanRequestRepository.save(newLoan);
        logger.info("Marked new loan {} as consolidated", newLoan.getId());

        List<Map<String, Object>> aggregatedLoanDetails = new ArrayList<>();

        // Add new loan to the aggregation details
        Map<String, Object> newLoanDetail = new HashMap<>();
        newLoanDetail.put("id", newLoan.getId());
        newLoanDetail.put("type", "NEW_REQUEST");
        newLoanDetail.put("amount", newLoanAmount);
        newLoanDetail.put("originalAmount", newLoan.getLoanAmount());
        aggregatedLoanDetails.add(newLoanDetail);

        for (LoanRequest existingLoan : existingActiveLoans) {
            Map<String, Object> loanDetail = new HashMap<>();
            loanDetail.put("id", existingLoan.getId());
            loanDetail.put("type", "EXISTING_LOAN");
            loanDetail.put("remaining", existingLoan.calculateRemainingBalance());
            loanDetail.put("paid", existingLoan.getPaidAmount() != null ? existingLoan.getPaidAmount() : 0.0);
            aggregatedLoanDetails.add(loanDetail);

            existingLoan.setStatus("Consolidated");
            existingLoan.setIsActive(false);
            existingLoan.setIsAggregated(true);
            existingLoan.setCompletedDate(LocalDateTime.now());
            existingLoan.setProcessingNotes((existingLoan.getProcessingNotes() != null ? existingLoan.getProcessingNotes() + "\n" : "") +
                    String.format("Consolidated into new loan #%s on %s. Outstanding: %.2f",
                            newLoan.getId(), LocalDateTime.now(), existingLoan.calculateRemainingBalance()));

            loanRequestRepository.save(existingLoan);
            logger.info("Marked existing loan {} as consolidated", existingLoan.getId());
        }

        // Create consolidated loan record
        LoanRequest consolidatedLoan = new LoanRequest();
        consolidatedLoan.setEmployee(employee);
        consolidatedLoan.setLoanAmount(newLoan.getLoanAmount());
        consolidatedLoan.setOriginalLoanAmount(newLoan.getOriginalLoanAmount());
        consolidatedLoan.setAggregatedTotal(consolidatedTotal);
        consolidatedLoan.setApprovedAmount(consolidatedTotal);

        String purpose = "CONSOLIDATED: " + newLoan.getPurpose();
        if (purpose.length() > 255) {
            purpose = purpose.substring(0, 252) + "...";
        }
        consolidatedLoan.setPurpose(purpose);

        consolidatedLoan.setRepaymentPeriod(dto.getApprovedRepaymentPeriod() != null ?
                dto.getApprovedRepaymentPeriod() : newLoan.getRepaymentPeriod());
        consolidatedLoan.setApprovedRepaymentPeriod(dto.getApprovedRepaymentPeriod());
        consolidatedLoan.setMonthlyPaymentAmount(newLoan.getMonthlyPaymentAmount());
        consolidatedLoan.setApprovedMonthlyPayment(dto.getApprovedMonthlyPayment());
        consolidatedLoan.setStatus("Approved");
        consolidatedLoan.setProcessedBy(dto.getProcessedBy());
        consolidatedLoan.setProcessedDate(LocalDateTime.now());

        String notes = dto.getNotes() != null ? dto.getNotes() : "";
        String processingNotes = String.format(
                "Consolidated %d existing loans + 1 new request. Total: %.2f. Original new amount: %.2f. %s",
                existingActiveLoans.size(), existingTotalOutstanding, newLoanAmount,
                notes.length() > 200 ? notes.substring(0, 197) + "..." : notes);
        if (processingNotes.length() > 500) {
            processingNotes = processingNotes.substring(0, 497) + "...";
        }
        consolidatedLoan.setProcessingNotes(processingNotes);

        consolidatedLoan.setIsActive(true);
        consolidatedLoan.setIsAggregated(true);
        consolidatedLoan.setConsolidationDate(LocalDateTime.now());
        consolidatedLoan.setConsolidatedBy(dto.getProcessedBy());

        consolidatedLoan.setPaidAmount(0.0);
        consolidatedLoan.setRemainingBalance(consolidatedTotal);

        try {
            Map<String, Object> aggregationData = new LinkedHashMap<>();
            aggregationData.put("originalRequestId", newLoan.getId()); // Track original request
            aggregationData.put("ids", existingActiveLoans.stream().map(LoanRequest::getId).collect(Collectors.toList()));
            aggregationData.put("count", existingActiveLoans.size());
            aggregationData.put("outstanding", existingTotalOutstanding);
            aggregationData.put("newAmount", newLoanAmount);
            aggregationData.put("total", consolidatedTotal);
            aggregationData.put("date", LocalDateTime.now().toString());
            aggregationData.put("by", dto.getProcessedBy());
            aggregationData.put("details", aggregatedLoanDetails);

            String jsonData = objectMapper.writeValueAsString(aggregationData);
            logger.info("Aggregated JSON data size: {} characters", jsonData.length());
            consolidatedLoan.setAggregatedFromLoanIds(jsonData);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize aggregated loan data", e);
            String ids = existingActiveLoans.stream()
                    .map(l -> String.valueOf(l.getId()))
                    .collect(Collectors.joining(","));
            consolidatedLoan.setAggregatedFromLoanIds(ids + "," + newLoan.getId());
        }

        if (!existingActiveLoans.isEmpty()) {
            consolidatedLoan.setPreviousLoanId(existingActiveLoans.get(0).getId());
        }

        LoanRequest savedConsolidated = loanRequestRepository.save(consolidatedLoan);

        // Update both new and existing loans to point to the consolidated loan
        newLoan.setPreviousLoanId(savedConsolidated.getId());
        loanRequestRepository.save(newLoan);

        for (LoanRequest existingLoan : existingActiveLoans) {
            existingLoan.setPreviousLoanId(savedConsolidated.getId());
            loanRequestRepository.save(existingLoan);
        }

        logger.info("Successfully consolidated new loan {} and {} existing loans into consolidated loan {} for employee {}",
                newLoan.getId(), existingActiveLoans.size(), savedConsolidated.getId(), employee.getId());

        emailService.sendLoanApprovalNotification(savedConsolidated, dto.getNotes());
        emailService.sendLoanConsolidationNotification(savedConsolidated, existingActiveLoans);

        // Return the consolidated loan (the active one), not the original request
        return savedConsolidated;
    }

    @Transactional
    public void deleteConsolidatedLoanWithRestoration(Long consolidatedLoanId) {
        logger.info("Starting hard delete of consolidated loan: {}", consolidatedLoanId);

        LoanRequest consolidatedLoan = loanRequestRepository.findById(consolidatedLoanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with id: " + consolidatedLoanId));

        if (!Boolean.TRUE.equals(consolidatedLoan.getIsAggregated())) {
            throw new RuntimeException("This loan is not a consolidated loan. Only consolidated loans can be deleted with restoration.");
        }

        List<LoanRequest> originalLoans = loanRequestRepository.findAll().stream()
                .filter(loan -> consolidatedLoanId.equals(loan.getPreviousLoanId()))
                .collect(Collectors.toList());

        logger.info("Found {} original loans to restore", originalLoans.size());

        for (LoanRequest original : originalLoans) {
            original.setStatus("Approved");
            original.setIsActive(true);
            original.setIsAggregated(false);
            original.setPreviousLoanId(null);
            original.setProcessingNotes((original.getProcessingNotes() != null ? original.getProcessingNotes() + "\n" : "") +
                    "Restored after consolidated loan #" + consolidatedLoanId + " was deleted on " + LocalDateTime.now());
            loanRequestRepository.save(original);
            logger.info("Restored original loan: {}", original.getId());
        }

        loanRequestRepository.delete(consolidatedLoan);
        loanRequestRepository.flush();

        logger.info("Successfully hard deleted consolidated loan {} and restored {} original loans",
                consolidatedLoanId, originalLoans.size());
    }

    @Transactional
    private LoanRequest updateLoanStatusStandard(LoanRequest loan, LoanStatusUpdateDTO dto) {
        loan.setStatus(dto.getStatus());
        loan.setProcessedBy(dto.getProcessedBy());
        loan.setProcessingNotes(dto.getNotes());
        loan.setProcessedDate(LocalDateTime.now());

        if ("Approved".equals(dto.getStatus())) {
            if (dto.getApprovedAmount() != null) {
                loan.setApprovedAmount(dto.getApprovedAmount());
                loan.setApprovedRepaymentPeriod(dto.getApprovedRepaymentPeriod());
                loan.setApprovedMonthlyPayment(dto.getApprovedMonthlyPayment());
            } else {
                loan.setApprovedAmount(loan.getLoanAmount());
                loan.setApprovedRepaymentPeriod(loan.getRepaymentPeriod());
                loan.setApprovedMonthlyPayment(loan.getMonthlyPaymentAmount());
            }
            loan.setIsActive(true);
            loan.setRemainingBalance(loan.getApprovedAmount());
            loan.setPaidAmount(0.0);
            logger.info("Loan {} approved with amount {}", loan.getId(), loan.getApprovedAmount());
        } else if ("Rejected".equals(dto.getStatus())) {
            loan.setIsActive(false);
            loan.setRemainingBalance(0.0);
            logger.info("Loan {} rejected", loan.getId());
        }

        return loanRequestRepository.save(loan);
    }

    public List<LoanRequest> getActiveLoansForEmployee(Long employeeId) {
        return loanRequestRepository.findActiveNonAggregatedLoansByEmployee(employeeId);
    }

    public Map<String, Object> getConsolidatedLoanDetails(Long consolidatedLoanId) {
        LoanRequest consolidatedLoan = loanRequestRepository.findById(consolidatedLoanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        Map<String, Object> details = new HashMap<>();
        details.put("consolidatedLoan", consolidatedLoan);

        if (consolidatedLoan.getAggregatedFromLoanIds() != null) {
            try {
                try {
                    Map<String, Object> aggregationData = objectMapper.readValue(
                            consolidatedLoan.getAggregatedFromLoanIds(),
                            new TypeReference<Map<String, Object>>() {}
                    );
                    details.put("aggregationData", aggregationData);

                    @SuppressWarnings("unchecked")
                    List<Long> loanIds = (List<Long>) aggregationData.get("ids");
                    if (loanIds != null && !loanIds.isEmpty()) {
                        List<LoanRequest> originalLoans = loanRequestRepository.findAllById(loanIds);
                        details.put("originalLoans", originalLoans);
                    }
                } catch (Exception e) {
                    List<Long> loanIds = objectMapper.readValue(
                            consolidatedLoan.getAggregatedFromLoanIds(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
                    List<LoanRequest> originalLoans = loanRequestRepository.findAllById(loanIds);
                    details.put("originalLoans", originalLoans);
                }
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse aggregated loan IDs", e);
            }
        }

        return details;
    }

    @Transactional
    public LoanRequest toggleLoanActiveStatus(Long loanId, ToggleActiveStatusDTO dto) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with id: " + loanId));

        Boolean targetStatus = dto.getActive();
        String currentStatus = loan.getStatus();
        Boolean currentActive = loan.getIsActive();
        Double remainingBalance = loan.calculateRemainingBalance();

        if (targetStatus != null && targetStatus.equals(currentActive)) {
            throw new RuntimeException("Loan is already " + (currentActive ? "active" : "inactive"));
        }

        if (targetStatus && !"Approved".equals(currentStatus)) {
            throw new RuntimeException("Cannot activate a loan that is not approved. Current status: " + currentStatus);
        }

        if (targetStatus && remainingBalance <= 0) {
            throw new RuntimeException("Cannot activate a loan with zero remaining balance. Loan is already completed.");
        }

        if (targetStatus) {
            if (loan.getPreviousLoanId() != null && loan.getPreviousLoanId() > 0) {
                Optional<LoanRequest> parentLoan = loanRequestRepository.findById(loan.getPreviousLoanId());
                if (parentLoan.isPresent() && Boolean.TRUE.equals(parentLoan.get().getIsActive())) {
                    throw new RuntimeException("Cannot activate this loan because it has been consolidated into loan #" +
                            loan.getPreviousLoanId() + ". Please deactivate the consolidated loan first if you want to revert.");
                }
            }
        }

        if (!targetStatus && (dto.getReason() == null || dto.getReason().trim().isEmpty())) {
            throw new RuntimeException("A reason is required to deactivate a loan.");
        }

        loan.setIsActive(targetStatus);

        if (targetStatus) {
            loan.setReactivatedBy(dto.getProcessedBy());
            loan.setReactivationDate(LocalDateTime.now());
            loan.setProcessingNotes((loan.getProcessingNotes() != null ? loan.getProcessingNotes() + "\n" : "") +
                    "Loan reactivated by " + dto.getProcessedBy() + " on " + LocalDateTime.now());
            logger.info("Loan {} activated by {}", loanId, dto.getProcessedBy());
        } else {
            loan.setDeactivatedBy(dto.getProcessedBy());
            loan.setDeactivationReason(dto.getReason());
            loan.setDeactivationDate(LocalDateTime.now());
            loan.setProcessingNotes((loan.getProcessingNotes() != null ? loan.getProcessingNotes() + "\n" : "") +
                    "Loan deactivated by " + dto.getProcessedBy() + " on " + LocalDateTime.now() +
                    ". Reason: " + dto.getReason());
            logger.info("Loan {} deactivated by {}. Reason: {}", loanId, dto.getProcessedBy(), dto.getReason());
        }

        return loanRequestRepository.save(loan);
    }

    public Map<String, Object> canActivateLoan(Long loanId) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("canActivate", false);
        result.put("canDeactivate", false);
        result.put("currentStatus", loan.getIsActive());
        result.put("loanStatus", loan.getStatus());
        result.put("remainingBalance", loan.calculateRemainingBalance());
        result.put("isAggregated", loan.getIsAggregated());

        List<String> reasons = new ArrayList<>();

        if (!Boolean.TRUE.equals(loan.getIsActive())) {
            if (!"Approved".equals(loan.getStatus())) {
                reasons.add("Loan status must be 'Approved' to activate. Current status: " + loan.getStatus());
            }
            if (loan.calculateRemainingBalance() <= 0) {
                reasons.add("Loan has zero remaining balance. Cannot activate a completed loan.");
            }
            if (loan.getPreviousLoanId() != null && loan.getPreviousLoanId() > 0) {
                Optional<LoanRequest> parentLoan = loanRequestRepository.findById(loan.getPreviousLoanId());
                if (parentLoan.isPresent() && Boolean.TRUE.equals(parentLoan.get().getIsActive())) {
                    reasons.add("This loan has been consolidated into loan #" + loan.getPreviousLoanId() +
                            " and cannot be activated while that loan is active.");
                }
            }
            if (reasons.isEmpty()) {
                result.put("canActivate", true);
            }
        }

        if (Boolean.TRUE.equals(loan.getIsActive())) {
            result.put("canDeactivate", true);
        }

        result.put("reasons", reasons);
        return result;
    }

    @Transactional
    public void checkAndCompleteLoan(LoanRequest loan) {
        double remainingBalance = loan.calculateRemainingBalance();

        if (remainingBalance <= 0.01 && !"Completed".equals(loan.getStatus()) && !"Consolidated".equals(loan.getStatus())) {
            loan.setStatus("Completed");
            loan.setIsActive(false);
            loan.setCompletedDate(LocalDateTime.now());
            loan.setProcessingNotes((loan.getProcessingNotes() != null ? loan.getProcessingNotes() + "\n" : "") +
                    "Loan completed on " + LocalDateTime.now() + " - fully paid");

            loanRequestRepository.save(loan);
            logger.info("Loan {} automatically completed as balance reached zero", loan.getId());
        }
    }

    public List<LoanRequest> getActiveLoansForDeduction() {
        return loanRequestRepository.findActiveLoansForDeduction();
    }

    public List<LoanRequest> getConsolidatedLoansForEmployee(Long employeeId) {
        return loanRequestRepository.findAggregatedLoansByEmployee(employeeId);
    }

    public List<LoanRequest> getCompletedLoansForEmployee(Long employeeId) {
        return loanRequestRepository.findCompletedLoansByEmployee(employeeId);
    }

    public Map<String, Object> getLoanSummaryStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<LoanRequest> allLoans = loanRequestRepository.findAll();
        List<LoanRequest> activeLoans = allLoans.stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsActive()) && !Boolean.TRUE.equals(l.getIsAggregated()))
                .collect(Collectors.toList());
        List<LoanRequest> consolidatedLoans = allLoans.stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsAggregated()))
                .collect(Collectors.toList());
        List<LoanRequest> completedLoans = allLoans.stream()
                .filter(l -> "Completed".equals(l.getStatus()))
                .collect(Collectors.toList());

        double totalActiveOutstanding = activeLoans.stream()
                .mapToDouble(LoanRequest::calculateRemainingBalance)
                .sum();

        double totalApprovedAmount = allLoans.stream()
                .mapToDouble(l -> l.getApprovedAmount() != null ? l.getApprovedAmount() : l.getLoanAmount())
                .sum();

        double totalPaidAmount = allLoans.stream()
                .mapToDouble(l -> l.getPaidAmount() != null ? l.getPaidAmount() : 0.0)
                .sum();

        stats.put("totalLoans", allLoans.size());
        stats.put("activeLoans", activeLoans.size());
        stats.put("consolidatedLoans", consolidatedLoans.size());
        stats.put("completedLoans", completedLoans.size());
        stats.put("totalActiveOutstanding", totalActiveOutstanding);
        stats.put("totalApprovedAmount", totalApprovedAmount);
        stats.put("totalPaidAmount", totalPaidAmount);
        stats.put("totalRemaining", totalApprovedAmount - totalPaidAmount);

        return stats;
    }
}