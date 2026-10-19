package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.dto.*;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.Category;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private static final Logger logger = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollPeriodRepository payrollPeriodRepository;
    @Autowired
    private JobRepository projectPayrollJobs;
    @Autowired
    private PayrollWorkScopeService payrollWorkScopeService;
    private final PayrollRecordRepository payrollRecordRepository;
    private final OvertimeRepository overtimeRepository;
    private final AttendanceRepository attendanceRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private JobEmployeeRateRepository jobEmployeeRateRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private PayrollProjectSnapshotService payrollProjectSnapshotService;
    private final EmployeeRepository employeeRepository;
    private final LeaveRepository leaveRepository;
    private final PendingCreditRepository pendingCreditRepository;
    private final GhanaPayrollCalculator ghanaPayrollCalculator;
    private final PayrollCalculator payrollCalculator;
    private final SettingsService settingsService;
    private final LoanDeductionService loanDeductionService;
    private final LoanDeductionHistoryRepository loanDeductionHistoryRepository;
    private final LoanRequestRepository loanRequestRepository;
// in constructor

    public PayrollService(PayrollPeriodRepository payrollPeriodRepository,
                          PayrollRecordRepository payrollRecordRepository,
                          AttendanceRepository attendanceRepository,
                          EmployeeRepository employeeRepository,
                          GhanaPayrollCalculator ghanaPayrollCalculator,
                          OvertimeRepository overtimeRepository,
                          LeaveRepository leaveRepository,
                          PayrollCalculator payrollCalculator,
                          SettingsService settingsService,
                          PendingCreditRepository pendingCreditRepository,
                          LoanDeductionService loanDeductionService,
                          LoanDeductionHistoryRepository loanDeductionHistoryRepository,
                          LoanRequestRepository loanRequestRepository) {
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.payrollRecordRepository = payrollRecordRepository;
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.ghanaPayrollCalculator = ghanaPayrollCalculator;
        this.overtimeRepository = overtimeRepository;
        this.leaveRepository = leaveRepository;
        this.payrollCalculator = payrollCalculator;
        this.settingsService = settingsService;
        this.pendingCreditRepository = pendingCreditRepository;
        this.loanDeductionService = loanDeductionService;
        this.loanDeductionHistoryRepository = loanDeductionHistoryRepository;
        this.loanRequestRepository = loanRequestRepository;
    }

    // ==================== PENDING CREDITS METHODS ====================

    @Transactional
    public PendingCredit addPendingCredit(Long employeeId, Double amount, String reason, String createdBy) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        PendingCredit credit = new PendingCredit();
        credit.setEmployee(employee);
        credit.setAmount(amount);
        credit.setReason(reason);
        credit.setCreatedDate(LocalDateTime.now());
        credit.setCreatedBy(createdBy);
        credit.setStatus("PENDING");

        return pendingCreditRepository.save(credit);
    }

    @Transactional
    public List<PendingCredit> addMultiplePendingCredits(Map<Long, Double> credits, String reason, String createdBy) {
        List<PendingCredit> savedCredits = new ArrayList<>();

        for (Map.Entry<Long, Double> entry : credits.entrySet()) {
            try {
                PendingCredit credit = addPendingCredit(entry.getKey(), entry.getValue(), reason, createdBy);
                savedCredits.add(credit);
            } catch (Exception e) {
                logger.error("Failed to add credit for employee {}: {}", entry.getKey(), e.getMessage());
            }
        }

        return savedCredits;
    }

    public List<PendingCreditDTO> getPendingCreditsForEmployees(List<Long> employeeIds) {
        List<PendingCredit> credits = pendingCreditRepository.findPendingCreditsByEmployeeIds(employeeIds);
        return credits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PendingCreditDTO> getPendingCreditsForPeriod(Long periodId) {
        PayrollPeriod period = payrollPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + periodId));

        List<Object[]> attendanceData = attendanceRepository.findEmployeeIdsInPeriod(
                period.getStartDate(), period.getEndDate());

        List<Long> employeeIds = attendanceData.stream()
                .map(row -> ((Number) row[0]).longValue())
                .distinct()
                .collect(Collectors.toList());
        if(period.isProjectPayroll()) employeeIds = new ArrayList<>(payrollWorkScopeService.prepare(period).workDates.keySet());

        if (employeeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<PendingCredit> credits = pendingCreditRepository.findUnappliedPendingCreditsByEmployeeIds(employeeIds);
        return credits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public boolean hasPendingCreditsForPeriod(Long periodId) {
        List<PendingCreditDTO> pendingCredits = getPendingCreditsForPeriod(periodId);
        return !pendingCredits.isEmpty();
    }

    @Transactional
    public Map<String, Object> generatePayrollWithSelectedCredits(GeneratePayrollWithCreditsRequest request) {
        Long periodId = request.getPeriodId();
        payrollPeriodRepository.lockForPayrollGeneration();
        PayrollPeriod scopePeriod = payrollPeriodRepository.findById(periodId).orElseThrow(() -> new IllegalArgumentException("Payroll period not found"));
        if (scopePeriod.isProjectPayroll()) {
            var eligible = payrollWorkScopeService.prepare(scopePeriod).workDates.keySet();
            if (request.getEmployeeId()!=null && !eligible.contains(request.getEmployeeId())) throw new IllegalArgumentException("Selected employee has no unpaid approved, synced work for this project");
            if (request.getSelectedCreditIds()!=null && pendingCreditRepository.findAllById(request.getSelectedCreditIds()).stream().anyMatch(c->c.getEmployee()==null || !eligible.contains(c.getEmployee().getId())))
                throw new IllegalArgumentException("Selected credits must belong to eligible project employees");
            if(request.getDirectCreditAmounts()!=null && !eligible.containsAll(request.getDirectCreditAmounts().keySet())) throw new IllegalArgumentException("Direct credits must belong to eligible project employees");
        }
        if (request.getSelectedCreditIds() != null && request.getExcludedEmployeeIds() != null) {
            boolean excludedCredit = pendingCreditRepository.findAllById(request.getSelectedCreditIds()).stream()
                    .anyMatch(credit -> credit.getEmployee() != null && request.getExcludedEmployeeIds().contains(credit.getEmployee().getId()));
            if (excludedCredit) throw new IllegalArgumentException("Cannot apply credits for excluded employees");
        }
        List<PayrollRecord> scopedRecords = payrollRecordRepository.findByPeriodId(periodId).stream()
                .filter(row -> request.getEmployeeId() == null || (row.getEmployee() != null
                        && request.getEmployeeId().equals(row.getEmployee().getId())))
                .collect(Collectors.toList());
        PayrollRateSelection.validate(request.getRateChoice(), scopedRecords);
        if (request.getEmployeeId() != null) {
            Employee selected = employeeRepository.findByIdAndActiveTrue(request.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Selected employee is not active or does not exist"));
            PayrollPeriod selectedPeriod = payrollPeriodRepository.findById(periodId)
                    .orElseThrow(() -> new IllegalArgumentException("Payroll period not found"));
            if (!selectedPeriod.isProjectPayroll() && (selected.getCategory() == null || !Objects.equals(selected.getCategory().getName(), selectedPeriod.getCategory()))) {
                throw new IllegalArgumentException("Selected employee does not belong to this period's category");
            }
        }
        List<Long> selectedCreditIds = request.getSelectedCreditIds();

        logger.info("Generating payroll for period {} with {} selected credits", periodId,
                selectedCreditIds != null ? selectedCreditIds.size() : 0);

        Map<Long, Double> appliedCreditAmounts = new HashMap<>();
        String appliedCreditIdsStr = null;

        if (selectedCreditIds != null && !selectedCreditIds.isEmpty()) {
            if (request.getEmployeeId() != null) {
                List<PendingCredit> selected = pendingCreditRepository.findAllById(selectedCreditIds);
                if (selected.size() != new HashSet<>(selectedCreditIds).size() || selected.stream().anyMatch(c ->
                        c.getEmployee() == null || !request.getEmployeeId().equals(c.getEmployee().getId())
                        || !"PENDING".equals(c.getStatus()))) {
                    throw new IllegalArgumentException("Credits must be pending and belong to the selected employee");
                }
            }
            int updated = pendingCreditRepository.markCreditsAsApplied(selectedCreditIds, periodId);
            logger.info("Marked {} credits as applied to period {}", updated, periodId);

            List<PendingCredit> appliedCredits = pendingCreditRepository.findAllById(selectedCreditIds);
            for (PendingCredit credit : appliedCredits) {
                Long empId = credit.getEmployee().getId();
                appliedCreditAmounts.merge(empId, credit.getAmount(), Double::sum);
            }

            appliedCreditIdsStr = selectedCreditIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }

        if (request.getDirectCreditAmounts() != null && !request.getDirectCreditAmounts().isEmpty()) {
            logger.info("Adding direct credits from request: {}", request.getDirectCreditAmounts().size());
            for (Map.Entry<Long, Double> entry : request.getDirectCreditAmounts().entrySet()) {
                appliedCreditAmounts.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        if (request.getEmployeeId() != null) {
            Long target = request.getEmployeeId();
            appliedCreditAmounts.keySet().removeIf(id -> !target.equals(id));
            PayrollRecord previous = scopedRecords.stream().findFirst().orElse(null);
            if (previous != null) {
                if (request.getDirectCreditAmounts() == null || !request.getDirectCreditAmounts().containsKey(target)) {
                    appliedCreditAmounts.merge(target, previous.getCreditAmount() == null ? 0.0 : previous.getCreditAmount(), Double::sum);
                }
                if (previous.getAppliedCreditIds() != null && !previous.getAppliedCreditIds().isBlank()) {
                    appliedCreditIdsStr = previous.getAppliedCreditIds() + (appliedCreditIdsStr == null ? "" : "," + appliedCreditIdsStr);
                }
            }
        }
        return generatePayrollWithCredits(periodId, appliedCreditAmounts, appliedCreditIdsStr, request);
    }



    @Transactional
    public Map<String, Object> generatePayrollWithCredits(Long periodId, Map<Long, Double> creditAmounts,
                                                          String appliedCreditIds, GeneratePayrollWithCreditsRequest request) {
        payrollPeriodRepository.lockForPayrollGeneration();
        SystemSettings sys = settingsService.getSystemSettings();
        Map<Long, Double> allCreditAmounts = new HashMap<>();
        if (creditAmounts != null) {
            allCreditAmounts.putAll(creditAmounts);
        }
        if (request != null && request.getEmployeeId() == null && request.getDirectCreditAmounts() != null) {
            allCreditAmounts.putAll(request.getDirectCreditAmounts());
            logger.info("Added direct credits from request in generatePayrollWithCredits: {}", request.getDirectCreditAmounts().size());
        }

        boolean includeBasicSalary = request.getIncludeBasicSalary() == null
                || request.getIncludeBasicSalary();
        Map<Long, Map<String, Double>> tempAllowances = request.getTemporaryAllowances();
        SettingsDTO settings = request.getSettings();

        logger.info("Generating payroll for period ID: {} using ONLY approved overtime records", periodId);
        logger.info("Total credits to apply: {}", allCreditAmounts.size());

        PayrollPeriod period = payrollPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found for id: " + periodId));

        payrollPeriodRepository.lockForPayrollGeneration();
        PayrollWorkScopeService.Scope workScope = payrollWorkScopeService == null ? null : payrollWorkScopeService.prepare(period);
        if(period.isProjectPayroll() && workScope==null) throw new IllegalStateException("Project payroll work selection unavailable");
        String periodCategory = period.getCategory();
        logger.info("Period category: {}", periodCategory);

        // ==================== CRITICAL FIX: Check if loan deductions already processed ====================
        boolean loanDeductionsAlreadyProcessed = loanDeductionService.hasLoanDeductionsForEmployee(periodId, request.getEmployeeId());

        List<PayrollRecord> existingRecords = payrollRecordRepository.findByPeriodId(periodId).stream()
                .filter(row -> request.getEmployeeId() == null || (row.getEmployee() != null
                        && request.getEmployeeId().equals(row.getEmployee().getId())))
                .collect(Collectors.toList());

        Set<Long> excludedIds = request.getExcludedEmployeeIds() == null ? Collections.emptySet() : new HashSet<>(request.getExcludedEmployeeIds());
        if (request.getEmployeeId() != null && !excludedIds.isEmpty()) throw new IllegalArgumentException("Choose either an employee or exclusions");
        List<PayrollRecord> excludedRecords = existingRecords.stream()
                .filter(row -> row.getEmployee() != null && excludedIds.contains(row.getEmployee().getId())).collect(Collectors.toList());
        if (excludedRecords.stream().anyMatch(row -> "Processed".equals(row.getStatus())
                || (row.getCreditAmount() != null && row.getCreditAmount() != 0)
                || loanDeductionService.hasLoanDeductionsForEmployee(periodId, row.getEmployee().getId()))) {
            throw new IllegalStateException("Clear existing payroll records and reverse credits/loan deductions before excluding these employees");
        }
        payrollRecordRepository.deleteAll(excludedRecords);
        existingRecords.removeAll(excludedRecords);
        allCreditAmounts.keySet().removeAll(excludedIds);
        PayrollRateSelection.validate(request.getRateChoice(), existingRecords);
        Map<Long, PayrollRecord> previousRates = existingRecords.stream()
                .filter(row -> row.getEmployee() != null)
                .collect(Collectors.toMap(row -> row.getEmployee().getId(), row -> row, (first, second) -> first));

        // If loan deductions were already processed, we need to PRESERVE them
        Map<Long, Double> existingLoanDeductions = new HashMap<>();
        Map<Long, Double> existingNetSalaries = new HashMap<>();
        Map<Long, Double> existingRemainingBalances = new HashMap<>();

        if (loanDeductionsAlreadyProcessed && !existingRecords.isEmpty()) {
            logger.info("Loan deductions already processed for period {}. Will preserve existing loan deduction values.", periodId);

            for (PayrollRecord existingRecord : existingRecords) {
                if (existingRecord.getEmployee() != null) {
                    Long empId = existingRecord.getEmployee().getId();
                    if (existingRecord.getLoanDeduction() != null && existingRecord.getLoanDeduction() > 0) {
                        existingLoanDeductions.put(empId, existingRecord.getLoanDeduction());
                        existingNetSalaries.put(empId, existingRecord.getNetSalary());
                        existingRemainingBalances.put(empId, existingRecord.getRemainingLoanBalance());
                        logger.debug("Will preserve loan deduction of {} for employee {} from existing record",
                                existingRecord.getLoanDeduction(), empId);
                    }
                }
            }

            // If all records have loan deductions preserved, we can skip full recalculation
            if (!existingLoanDeductions.isEmpty()) {
                logger.info("Preserved {} existing loan deductions for period {}", existingLoanDeductions.size(), periodId);
            }
        }

        if (settings == null) {
            settings = getDefaultSettings();
            logger.warn("No settings provided, using defaults");
        }

        // Check for existing processed records
        if (!existingRecords.isEmpty() && existingRecords.stream().anyMatch(r -> "Processed".equals(r.getStatus()))) {
            throw new IllegalStateException("Cannot generate payroll for already processed period id: " + periodId);
        }

        // Delete existing records BUT preserve loan deduction values if they were already processed
        if (loanDeductionsAlreadyProcessed) {
            logger.info("Loan deductions already processed - will preserve deduction values when recreating records");
            // Don't delete if we're just updating - we'll update existing records instead
            // OR we'll recreate them with preserved values
        } else if (request.getEmployeeId() == null) {
            payrollRecordRepository.deleteByPeriodId(periodId);
        }

        List<Object[]> attendanceSummaries = workScope == null ? attendanceRepository.summarizeAllPresentAttendanceBetweenDatesForCategory(
                period.getStartDate(), period.getEndDate(), periodCategory) : workScope.normalSummary();

        List<Object[]> approvedOvertimeSummaries = workScope == null ? overtimeRepository.summarizeApprovedOvertimeBetweenDatesForCategory(
                period.getStartDate(), period.getEndDate(), periodCategory) : workScope.overtimeSummary();

        Map<Long, Double> approvedOvertimeHoursMap = new HashMap<>();
        Map<Long, Double> preCalculatedOvertimePayMap = new HashMap<>();
        Map<Long, Map<LocalDate, Double>> approvedOvertimeByEmployeeAndDate = new HashMap<>();

        (workScope == null ? overtimeRepository.findApprovedOvertimeForPayrollPeriod(period.getStartDate(), period.getEndDate()) : workScope.approvedOvertime)
                .forEach(overtime -> {
                    if (overtime.getEmployee() != null && overtime.getDate() != null && overtime.getOvertimeHours() != null) {
                        approvedOvertimeByEmployeeAndDate
                                .computeIfAbsent(overtime.getEmployee().getId(), ignored -> new HashMap<>())
                                .merge(overtime.getDate(), overtime.getOvertimeHours(), Double::sum);
                    }
                });

        if (approvedOvertimeSummaries != null) {
            for (Object[] row : approvedOvertimeSummaries) {
                if (row != null && row.length >= 4 && row[0] != null && row[1] != null) {
                    Long empId = ((Number) row[0]).longValue();
                    Double hours = ((Number) row[1]).doubleValue();
                    Double calculatedPay = ((Number) row[2]).doubleValue();
                    Boolean appliedToHours = (Boolean) row[3];

                    approvedOvertimeHoursMap.merge(empId, hours, Double::sum);

                    if (Boolean.TRUE.equals(appliedToHours)) {
                        preCalculatedOvertimePayMap.merge(empId, calculatedPay, Double::sum);
                        logger.info("Employee {}: Multiplier applied to HOURS - storing pre-calculated pay: {}",
                                empId, calculatedPay);
                    } else {
                        logger.info("Employee {}: Multiplier applied to RATE - will calculate in GhanaPayrollCalculator", empId);
                    }
                }
            }
        }

        List<Leave> approvedLeaves = period.isProjectPayroll() ? Collections.emptyList() : leaveRepository.findApprovedLeavesInPeriodForCategory(
                period.getStartDate(), period.getEndDate(), periodCategory);

        List<Object[]> detailedAttendance = attendanceRepository.findValidAttendanceForPayroll(
                period.getStartDate(), period.getEndDate());

        List<Object[]> leaveAttendanceRecords = period.isProjectPayroll() ? Collections.emptyList() : attendanceRepository.findAttendanceWithLeaveStatusForCategory(
                period.getStartDate(), period.getEndDate(), periodCategory);

        Set<Long> employeeIds = new HashSet<>();

        if (attendanceSummaries != null) {
            attendanceSummaries.forEach(r -> {
                if (r != null && r[0] != null) {
                    Long empId = ((Number) r[0]).longValue();
                    employeeRepository.findByIdAndActiveTrue(empId).ifPresent(e -> employeeIds.add(empId));
                }
            });
        }

        if (approvedOvertimeHoursMap != null) {
            approvedOvertimeHoursMap.keySet().forEach(empId -> {
                employeeRepository.findByIdAndActiveTrue(empId).ifPresent(e -> employeeIds.add(empId));
            });
        }

        if (approvedLeaves != null) {
            approvedLeaves.forEach(leave -> {
                if (leave.getEmployee() != null && leave.getEmployee().getId() != null) {
                    Long empId = leave.getEmployee().getId();
                    employeeRepository.findByIdAndActiveTrue(empId).ifPresent(e -> employeeIds.add(empId));
                }
            });
        }

        if (leaveAttendanceRecords != null) {
            leaveAttendanceRecords.forEach(r -> {
                if (r != null && r[0] != null) {
                    Long empId = ((Number) r[0]).longValue();
                    employeeRepository.findByIdAndActiveTrue(empId).ifPresent(e -> employeeIds.add(empId));
                }
            });
        }

        // Temporary allowances can be the sole source of pay for an employee.
        // Include active employees selected here even if they have no attendance,
        // approved overtime, or leave in this payroll period.
        if (tempAllowances != null) {
            tempAllowances.keySet().forEach(empId ->
                    employeeRepository.findByIdAndActiveTrue(empId)
                            .filter(employee -> employee.getCategory() != null
                                    && Objects.equals(employee.getCategory().getName(), periodCategory))
                            .ifPresent(employee -> employeeIds.add(empId)));
        }

        if (period.isProjectPayroll()) employeeIds.retainAll(workScope.workDates.keySet());
        if (request.getEmployeeId() != null) {
            employeeIds.retainAll(Collections.singleton(request.getEmployeeId()));
        }
        employeeIds.removeAll(excludedIds);
        if (employeeIds.isEmpty()) {
            throw new IllegalStateException("No active employees with attendance, approved overtime, leave, or temporary allowances found for the period in category: " + periodCategory);
        }

        Map<Long, Object[]> attendanceMap = (attendanceSummaries == null) ? Collections.emptyMap()
                : attendanceSummaries.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.length >= 3 && r[0] != null)
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> r, (a, b) -> a));

        Map<Long, Set<LocalDate>> leaveDates = PayrollLeaveDays.collect(approvedLeaves, leaveAttendanceRecords, period);
        if(workScope!=null) leaveDates.forEach((employee,dates)->dates.removeIf(date->workScope.reserved(employee,date)));
        Map<Long, Integer> finalLeaveDaysMap = new HashMap<>();
        leaveDates.forEach((employeeId, dates) -> finalLeaveDaysMap.put(employeeId, dates.size()));

        Map<Long, List<LocalDate>> employeeAttendanceDates = workScope == null ? buildEmployeeAttendanceDates(period) : workScope.dates;
        Map<Long, Map<LocalDate, Double>> employeeDailyHours = workScope == null ? buildEmployeeDailyHours(period) : workScope.daily;
        // Leave attendance may carry standard hours. Pay these once as leave,
        // without counting them as worked days or automatic weekend overtime.
        PayrollLeaveDays.excludeFromWorkedHours(leaveDates, employeeAttendanceDates, employeeDailyHours);
        Map<Long, Set<LocalDate>> unpaidAttendanceDates = PayrollLeaveDays.collectUnpaid(approvedLeaves, period);
        Map<Long, Set<LocalDate>> recordedLeaveDates = PayrollLeaveDays.collect(null, leaveAttendanceRecords, period);
        unpaidAttendanceDates.forEach((id, dates) -> dates.retainAll(recordedLeaveDates.getOrDefault(id, Collections.emptySet())));
        PayrollLeaveDays.excludeFromWorkedHours(unpaidAttendanceDates, employeeAttendanceDates, employeeDailyHours);

        int processedCount = 0;
        List<PayrollRecord> recordsToSave = new ArrayList<>();

        for (Long employeeId : employeeIds) {
            try {
                Employee employee = employeeRepository.findByIdAndActiveTrue(employeeId)
                        .orElseThrow(() -> new IllegalArgumentException("Active employee not found: " + employeeId));

                List<LocalDate> attendanceDates = employeeAttendanceDates.getOrDefault(employeeId, new ArrayList<>());
                Map<LocalDate, Double> dailyHours = employeeDailyHours.getOrDefault(employeeId, new HashMap<>());

                double standardHoursPerDay = period.isProjectPayroll()
                        ? projectPayrollJobs.findById(period.getProjectId()).map(j->j.getStandardWorkHours()==null?getStandardHoursPerDay(employee):j.getStandardWorkHours().doubleValue()).orElseThrow(()->new IllegalStateException("Project not found"))
                        : getStandardHoursPerDay(employee);

                MultiplierAdjustedHours adjustedHours = calculateMultiplierAdjustedHours(
                        attendanceDates, dailyHours, settings, period, standardHoursPerDay);

                boolean weekendAttendanceAsOvertime = employee.getCategory() != null
                        && Boolean.TRUE.equals(employee.getCategory().getWeekendAttendanceAsOvertime());
                double rawAutomaticWeekendOvertimeHours = 0.0;
                double automaticWeekendOvertimeHours = 0.0;
                if (weekendAttendanceAsOvertime) {
                    Map<LocalDate, Double> approvedByDate = approvedOvertimeByEmployeeAndDate
                            .getOrDefault(employeeId, Collections.emptyMap());
                    for (Map.Entry<LocalDate, Double> entry : dailyHours.entrySet()) {
                        Double recordedHours = entry.getValue();
                        if (recordedHours != null && recordedHours > 0
                                && payrollCalculator.isWeekend(entry.getKey(), settings.getWeekendDays())
                                && !payrollCalculator.isHoliday(entry.getKey(), settings.getHolidays())) {
                            rawAutomaticWeekendOvertimeHours += Math.max(0.0,
                                    recordedHours - approvedByDate.getOrDefault(entry.getKey(), 0.0));
                        }
                    }
                    double weekendOvertimeMultiplier = sys.getDefaultOvertimeMultiplier() != null
                            ? sys.getDefaultOvertimeMultiplier()
                            : 1.0;
                    automaticWeekendOvertimeHours = rawAutomaticWeekendOvertimeHours
                            * Math.max(0.0, weekendOvertimeMultiplier);

                    double regularWeekendHours = adjustedHours.getWeekendHours();
                    double weekendRate = settings.getWeekendRate() != null ? settings.getWeekendRate() : 1.0;
                    double weekendMultiplierBonus = regularWeekendHours * Math.max(0.0, weekendRate - 1.0);
                    adjustedHours = new MultiplierAdjustedHours(
                            Math.max(0.0, adjustedHours.getTotalHours() - regularWeekendHours),
                            Math.max(0, adjustedHours.getWorkingDays() - adjustedHours.getWeekendDays()),
                            adjustedHours.getHolidayHours(),
                            automaticWeekendOvertimeHours,
                            adjustedHours.getHolidayDays(),
                            adjustedHours.getWeekendDays(),
                            Math.max(0.0, adjustedHours.getMultiplierBonus() - weekendMultiplierBonus)
                    );
                    logger.info("Employee {} category {}: converted {} uncovered weekend hours to {} overtime hours using multiplier {}",
                            employeeId, employee.getCategory().getName(), rawAutomaticWeekendOvertimeHours,
                            automaticWeekendOvertimeHours, weekendOvertimeMultiplier);
                }

                int leaveDays = finalLeaveDaysMap.getOrDefault(employeeId, 0);

                double leaveHours = leaveDays * standardHoursPerDay;

                int actualWorkingDays = adjustedHours.getWorkingDays();
                double actualTotalHours = adjustedHours.getTotalHours();

                int totalDaysInPeriod = PayrollAttendanceHours.weekdays(period.getStartDate(), period.getEndDate());
                PayrollAttendanceHours.Split hours = PayrollAttendanceHours.split(
                        actualTotalHours, totalDaysInPeriod, standardHoursPerDay, period.isConvertExcessToOvertime());
                double extraOvertimeHours = hours.overtime();
                double normalRatio = actualTotalHours > 0 ? hours.normal() / actualTotalHours : 1.0;
                actualTotalHours = hours.normal();
                if (period.isConvertExcessToOvertime()) actualWorkingDays = Math.min(actualWorkingDays, totalDaysInPeriod);
                // Leave is paid separately; only worked attendance hours can become overtime.
                adjustedHours = new MultiplierAdjustedHours(actualTotalHours, actualWorkingDays,
                        adjustedHours.getHolidayHours() * normalRatio,
                        adjustedHours.getWeekendHours() * normalRatio,
                        adjustedHours.getHolidayDays(), adjustedHours.getWeekendDays(),
                        adjustedHours.getMultiplierBonus() * normalRatio);

                double approvedOvertimeHours = approvedOvertimeHoursMap.getOrDefault(employeeId, 0.0);
                double preCalculatedOvertimePay = preCalculatedOvertimePayMap.getOrDefault(employeeId, 0.0);

                if (automaticWeekendOvertimeHours > 0) {
                    approvedOvertimeHours += automaticWeekendOvertimeHours;
                }

                if (extraOvertimeHours > 0) {
                    approvedOvertimeHours += extraOvertimeHours;
                    logger.info("  - Added {} overtime hours from excess days. Total Overtime Hours: {}",
                            extraOvertimeHours, approvedOvertimeHours);
                }

                // ==================== CRITICAL FIX: Check if we have an existing record with loan deduction ====================
                PayrollRecord record;
                Double existingLoanDeduction = existingLoanDeductions.get(employeeId);
                Double existingNetSalary = existingNetSalaries.get(employeeId);
                Double existingRemainingBalance = existingRemainingBalances.get(employeeId);

                if (existingLoanDeduction != null && existingLoanDeduction > 0 && loanDeductionsAlreadyProcessed) {
                    // Use existing record or create new one with preserved loan deduction
                    Optional<PayrollRecord> existingRecordOpt = existingRecords.stream()
                            .filter(r -> r.getEmployee() != null && r.getEmployee().getId().equals(employeeId))
                            .findFirst();

                    if (existingRecordOpt.isPresent()) {
                        record = existingRecordOpt.get();
                        logger.info("Using existing payroll record for employee {} with preserved loan deduction of {}",
                                employeeId, existingLoanDeduction);
                    } else {
                        record = new PayrollRecord();
                        record.setEmployee(employee);
                        record.setPeriod(period);
                        logger.info("Creating new payroll record for employee {} with preserved loan deduction of {}",
                                employeeId, existingLoanDeduction);
                    }

                    // Set the preserved loan deduction values
                    record.setLoanDeduction(existingLoanDeduction);
                    record.setRemainingLoanBalance(existingRemainingBalance);

                    // We'll still recalculate the base salary but preserve the loan deduction in net calculation
                } else {
                    record = request.getEmployeeId() == null && !loanDeductionsAlreadyProcessed ? new PayrollRecord()
                            : previousRates.getOrDefault(employeeId, new PayrollRecord());
                    record.setEmployee(employee);
                    record.setPeriod(period);
                }

                record.setStatus("Generated");
                record.setIncludeBasicSalary(includeBasicSalary);
                record.setWorkingDays(actualWorkingDays);
                record.setTotalHours(actualTotalHours);
                record.setOvertimeHours(approvedOvertimeHours);
                record.setLeaveDays(leaveDays);
                record.setLeaveHours(leaveHours);
                record.setTotalDaysInPeriod(totalDaysInPeriod);
                record.setAppliedCreditIds(appliedCreditIds);

                double holidayHours = adjustedHours.getHolidayHours();
                double weekendHours = adjustedHours.getWeekendHours();
                double multiplierBonus = adjustedHours.getMultiplierBonus();

                logger.info("Processing Employee {}: {}", employeeId, employee.getFirstName() + " " + employee.getLastName());
                logger.info("  - Working Days: {}, Total Hours: {}", actualWorkingDays, actualTotalHours);
                logger.info("  - Leave Days: {}, Leave Hours: {}", leaveDays, leaveHours);
                logger.info("  - Approved Overtime Hours: {}", approvedOvertimeHours);
                logger.info("  - Pre-calculated Overtime Pay: {}", preCalculatedOvertimePay);
                logger.info("  - Holiday Days: {}, Weekend Days: {}", adjustedHours.getHolidayDays(), adjustedHours.getWeekendDays());
                logger.info("  - Multiplier Bonus: {}", multiplierBonus);

                if (existingLoanDeduction != null && existingLoanDeduction > 0) {
                    logger.info("  - PRESERVED Loan Deduction from previous run: {}", existingLoanDeduction);
                }

                record.setHolidayDays(adjustedHours.getHolidayDays());
                record.setWeekendDays(adjustedHours.getWeekendDays());
                record.setHolidayHours(holidayHours);
                record.setWeekendHours(weekendHours);
                record.setMultiplierBonus(multiplierBonus);

                setAllowances(record, employee, tempAllowances);

                double creditAmount = allCreditAmounts.getOrDefault(employeeId, 0.0);
                record.setCreditAmount(creditAmount);
                if (creditAmount > 0) {
                    logger.info("Applying credit of {} to employee {}", creditAmount, employeeId);
                    record.setCreditAmount(creditAmount);
                }

                Boolean excludeFromSsnit = employee.getExcludeFromSsnit() != null ? employee.getExcludeFromSsnit() : false;
                logger.info("  - Exclude from SSNIT: {}", excludeFromSsnit ? "YES" : "NO");

                // ==================== CALCULATE PAYROLL ====================
                PayrollRecord previousRate = previousRates.get(employeeId);
                Employee calculationEmployee = PayrollRateSelection.employeeForCalculation(employee, previousRate, request.getRateChoice());
                double appliedOvertimeRate = PayrollRateSelection.effectiveRate(calculationEmployee);
                if (preCalculatedOvertimePay > 0 && approvedOvertimeHours > 0) {
                    // The OT multiplier is already reflected in payable hours.
                    // Re-price those hours using the employee payroll rate, never a client billing rate.
                    preCalculatedOvertimePay = approvedOvertimeHours * appliedOvertimeRate;
                }
                JobEmployeeRate projectRate = null;
                if ("SAVED".equals(request.getRateChoice()) && previousRate != null && Boolean.TRUE.equals(previousRate.getRateSnapshotSaved())) {
                    projectRate = new JobEmployeeRate();
                    projectRate.setEffectivePayrollRate(previousRate.getAppliedEffectivePayrollRate());
                    projectRate.setEffectivePayrollRateFrom(previousRate.getAppliedEffectivePayrollRateFrom());
                    projectRate.setEffectivePayrollRateTo(previousRate.getAppliedEffectivePayrollRateTo());
                } else if (period.isProjectPayroll() || employee.getJob() != null) {
                    projectRate = jobEmployeeRateRepository.findByJobIdAndEmployeeId(period.isProjectPayroll() ? period.getProjectId() : employee.getJob().getId(), employeeId).orElse(null);
                }
                Long rateJobId = "SAVED".equals(request.getRateChoice()) && previousRate != null
                        ? previousRate.getAppliedEffectivePayrollJobId() : period.isProjectPayroll() ? period.getProjectId() : employee.getJob() == null ? null : employee.getJob().getId();
                record.setAppliedEffectivePayrollRate(null);
                record.setAppliedEffectivePayrollRateFrom(null);
                record.setAppliedEffectivePayrollRateTo(null);
                record.setAppliedEffectivePayrollJobId(null);
                record.setNormalRateHours(null);
                record.setEffectiveRateHours(null);
                record.setPrecalculatedHourlyBasicPay(null);
                boolean hourlyBasic = calculationEmployee.getMinimumRate() != null && calculationEmployee.getMinimumRate() > 0
                        && !(includeBasicSalary && calculationEmployee.getBasicSalary() != null && calculationEmployee.getBasicSalary() > 0)
                        && !(employee.getCategory() != null && Boolean.TRUE.equals(employee.getCategory().getUseNonTaxableAllowances()));
                if (hourlyBasic && projectRate != null && projectRate.getEffectivePayrollRate() != null && projectRate.getEffectivePayrollRateFrom() != null) {
                    double remainingNormalHours = actualTotalHours;
                    double effectiveHours = 0.0;
                    // Allocate the existing period cap chronologically to actual worked dates.
                    for (LocalDate date : new TreeSet<>(attendanceDates)) {
                        if (weekendAttendanceAsOvertime && payrollCalculator.isWeekend(date, settings.getWeekendDays())
                                && !payrollCalculator.isHoliday(date, settings.getHolidays())) continue;
                        double payable = Math.min(remainingNormalHours, Math.min(standardHoursPerDay, Math.max(0.0, dailyHours.getOrDefault(date, 0.0))));
                        if (projectRate.appliesOn(date)) effectiveHours += payable;
                        remainingNormalHours -= payable;
                    }
                    record.setAppliedEffectivePayrollRate(projectRate.getEffectivePayrollRate());
                    record.setAppliedEffectivePayrollRateFrom(projectRate.getEffectivePayrollRateFrom());
                    record.setAppliedEffectivePayrollRateTo(projectRate.getEffectivePayrollRateTo());
                    record.setAppliedEffectivePayrollJobId(rateJobId);
                    record.setNormalRateHours(actualTotalHours - effectiveHours);
                    record.setEffectiveRateHours(effectiveHours);
                    if (effectiveHours > 0) record.setPrecalculatedHourlyBasicPay(
                            (actualTotalHours - effectiveHours) * PayrollRateSelection.effectiveRate(calculationEmployee)
                                    + effectiveHours * projectRate.getEffectivePayrollRate());
                }
                SettingsDTO overtimeSplitSettings = settings;
                PayrollOvertimeRateHours.Split overtimeSplit = PayrollOvertimeRateHours.split(
                        approvedOvertimeByEmployeeAndDate.getOrDefault(employeeId, Collections.emptyMap()),
                        dailyHours, actualTotalHours, standardHoursPerDay, period.isConvertExcessToOvertime(),
                        date -> weekendAttendanceAsOvertime
                                && payrollCalculator.isWeekend(date, overtimeSplitSettings.getWeekendDays())
                                && !payrollCalculator.isHoliday(date, overtimeSplitSettings.getHolidays()),
                        sys.getDefaultOvertimeMultiplier() == null ? 1.0 : sys.getDefaultOvertimeMultiplier(),
                        record.getAppliedEffectivePayrollRateFrom(), record.getAppliedEffectivePayrollRateTo());
                record.setNormalRateOvertimeHours(overtimeSplit.oldRate());
                record.setEffectiveRateOvertimeHours(overtimeSplit.newRate());
                PayrollRateSelection.capture(record, calculationEmployee, appliedOvertimeRate);
                ghanaPayrollCalculator.calculatePayrollWithMultipliers(
                        calculationEmployee,
                        period.getStartDate(),
                        period.getEndDate(),
                        actualTotalHours,
                        actualWorkingDays,
                        approvedOvertimeHours,
                        leaveDays,
                        leaveHours,
                        holidayHours,
                        weekendHours,
                        multiplierBonus,
                        settings,
                        includeBasicSalary,
                        record,
                        sys,
                        preCalculatedOvertimePay,
                        creditAmount,
                        excludeFromSsnit,
                        record.getPrecalculatedHourlyBasicPay()
                );

                // ==================== CRITICAL FIX: Re-apply preserved loan deduction to net salary ====================
                if (existingLoanDeduction != null && existingLoanDeduction > 0 && loanDeductionsAlreadyProcessed) {
                    // The calculator already computed net without loan deduction
                    // We need to subtract the preserved loan deduction from net
                    Double calculatedNet = record.getNetSalary();
                    Double correctedNet = calculatedNet - existingLoanDeduction;
                    record.setNetSalary(correctedNet);
                    record.setLoanDeduction(existingLoanDeduction);

                    logger.info("  - Re-applied preserved loan deduction of {} to net salary. Net changed from {} to {}",
                            existingLoanDeduction, calculatedNet, correctedNet);
                }

                if (excludeFromSsnit) {
                    record.setSsnitEmployee(0.0);
                    record.setSsnitEmployer(0.0);
                    logger.info("  - SSNIT deductions ZEROED for employee {} (Excluded from SSNIT)", employee.getEmployeeId());
                }

                if (payrollProjectSnapshotService != null) {
                    payrollProjectSnapshotService.capture(record, dailyHours,
                            approvedOvertimeByEmployeeAndDate.getOrDefault(employeeId, Collections.emptyMap()),
                            actualTotalHours, standardHoursPerDay, period.isConvertExcessToOvertime(),
                            date -> weekendAttendanceAsOvertime
                                    && payrollCalculator.isWeekend(date, overtimeSplitSettings.getWeekendDays())
                                    && !payrollCalculator.isHoliday(date, overtimeSplitSettings.getHolidays()),
                            sys.getDefaultOvertimeMultiplier() == null ? 1.0 : sys.getDefaultOvertimeMultiplier());
                }
                if (payrollWorkScopeService != null) {
                    Set<LocalDate> paidDates = new TreeSet<>(attendanceDates);
                    paidDates.addAll(approvedOvertimeByEmployeeAndDate.getOrDefault(employeeId,Collections.emptyMap()).keySet());
                    paidDates.addAll(leaveDates.getOrDefault(employeeId,Collections.emptySet()));
                    record.setPayrollWorkDates(payrollWorkScopeService.snapshot(paidDates));
                }
                validatePayrollRecord(record, employeeId);
                recordsToSave.add(record);
                processedCount++;

            } catch (Exception ex) {
                if (workScope != null || request.getEmployeeId() != null) throw new IllegalStateException("Could not generate payroll; no records were saved", ex);
                logger.error("Failed to process employee {}: {}", employeeId, ex.getMessage(), ex);
                employeeRepository.findByIdAndActiveTrue(employeeId).ifPresent(e ->
                        createErrorPayrollRecord(employeeId, period, ex.getMessage())
                );
            }
        }

        // ==================== SAVE ALL RECORDS ====================
        if (!recordsToSave.isEmpty()) {
            if (loanDeductionsAlreadyProcessed) {
                // Update existing records instead of deleting and recreating
                for (PayrollRecord record : recordsToSave) {
                    payrollRecordRepository.save(record);
                }
                logger.info("Updated {} existing payroll records for period {}", recordsToSave.size(), periodId);
            } else {
                payrollRecordRepository.saveAll(recordsToSave);
                logger.info("Saved {} new payroll records for period {}", recordsToSave.size(), periodId);
            }
        }

        logger.info("Payroll generation completed. {} active employees processed for period {} in category {}",
                processedCount, periodId, periodCategory);

        // ==================== PROCESS LOAN DEDUCTIONS (ONLY IF NOT ALREADY PROCESSED) ====================
        boolean loanDeductionsProcessed = false;
        int loanUpdatesCount = 0;

        if (!loanDeductionsAlreadyProcessed || request.getEmployeeId() == null) {
            try {
                logger.info("Processing loan deductions for period {} (first time)", periodId);
                loanDeductionService.processLoanDeductions(periodId, request.getEmployeeId());
                loanDeductionsProcessed = true;
                loanUpdatesCount = payrollRecordRepository.findByPeriodId(periodId).stream()
                        .filter(r -> r.getLoanDeduction() != null && r.getLoanDeduction() > 0)
                        .collect(Collectors.toList())
                        .size();
                logger.info("Processed loan deductions for period {}. Updated {} employees with loans.",
                        periodId, loanUpdatesCount);
            } catch (Exception e) {
                if (period.isProjectPayroll() || request.getEmployeeId() != null) throw new IllegalStateException("Could not process payroll loans", e);
                logger.error("Loan processing failed for period {}: {}", periodId, e.getMessage(), e);
            }
        } else {
            logger.info("Skipping loan deductions for period {} - already processed in previous run", periodId);

            // Update the loanUpdatesCount from existing records
            loanUpdatesCount = (int) existingLoanDeductions.values().stream()
                    .filter(d -> d > 0)
                    .count();
            loanDeductionsProcessed = true; // They were processed before
            logger.info("Using existing loan deductions - {} employees have loan deductions", loanUpdatesCount);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("periodId", periodId);
        result.put("creditsApplied", allCreditAmounts.size());
        result.put("appliedCreditIds", appliedCreditIds);
        result.put("employeeCount", processedCount);
        result.put("excludedAlreadyAllocatedWorkDays", workScope == null ? 0 : workScope.excludedDays);
        result.put("periodType", period.isProjectPayroll() ? "PROJECT" : "CATEGORY");
        result.put("loanDeductionsProcessed", loanDeductionsProcessed);
        result.put("loanUpdatesCount", loanUpdatesCount);
        result.put("loanDeductionsPreserved", loanDeductionsAlreadyProcessed);

        if (loanDeductionsAlreadyProcessed) {
            result.put("message", "Payroll regenerated successfully. Loan deductions were preserved from previous run.");
        } else {
            result.put("message", "Payroll generated successfully with new loan deductions.");
        }

        return result;
    }
    // ==================== ADD THIS METHOD TO CHECK IF LOANS ALREADY PROCESSED ====================

    /**
     * Check if loan deductions have already been processed for a period
     * This method will be called by LoanDeductionService
     */
    public boolean hasLoanDeductionsForPeriod(Long periodId) {
        // Check the HISTORY table - this survives payroll record deletions!
        boolean hasHistory = loanDeductionHistoryRepository.existsByPayrollPeriodId(periodId);

        if (hasHistory) {
            logger.info("Period {} has existing loan deduction history - will skip duplicate deductions", periodId);
        } else {
            logger.info("Period {} has NO loan deduction history - will process deductions", periodId);
        }

        return hasHistory;
    }

    @Transactional
    public void cancelPendingCredit(Long creditId) {
        PendingCredit credit = pendingCreditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending credit not found: " + creditId));

        if (!"PENDING".equals(credit.getStatus())) {
            throw new IllegalStateException("Cannot cancel credit that is already " + credit.getStatus());
        }

        credit.setStatus("CANCELLED");
        pendingCreditRepository.save(credit);
    }

    public List<PendingCreditDTO> getAllPendingCredits() {
        List<PendingCredit> credits = pendingCreditRepository.findAllPendingCredits();
        return credits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private PendingCreditDTO convertToDTO(PendingCredit credit) {
        String periodName = null;
        Long periodId = null;

        if (credit.getAppliedToPeriod() != null) {
            periodName = credit.getAppliedToPeriod().getName();
            periodId = credit.getAppliedToPeriod().getId();
        }

        return new PendingCreditDTO(
                credit.getId(),
                credit.getEmployee().getId(),
                credit.getEmployee().getFirstName() + " " + credit.getEmployee().getLastName(),
                credit.getAmount(),
                credit.getReason(),
                credit.getCreatedDate(),
                credit.getCreatedBy(),
                credit.getStatus(),
                periodName,
                periodId,
                credit.getAppliedDate()
        );
    }

    // ==================== CATEGORY-BASED PERIOD METHODS ====================

    @Transactional
    public PayrollPeriod createPeriodWithCategory(String name, LocalDate startDate, LocalDate endDate,
                                                  String category, boolean convertExcessToOvertime) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }

        List<PayrollPeriod> overlappingPeriods = payrollPeriodRepository
                .findOverlappingPeriodsForCategory(startDate, endDate, category);

        if (!overlappingPeriods.isEmpty()) {
            throw new IllegalArgumentException(
                    "Payroll period overlaps with existing period in category: " + category
            );
        }

        PayrollPeriod period = new PayrollPeriod();
        period.setName(name);
        period.setStartDate(startDate);
        period.setEndDate(endDate);
        period.setCategory(category);
        period.setStatus("Open");
        period.setConvertExcessToOvertime(convertExcessToOvertime);

        return payrollPeriodRepository.save(period);
    }

    public List<PayrollPeriod> getPeriodsByCategory(String category) {
        return payrollPeriodRepository.findByCategoryOrderByStartDateDesc(category);
    }

    public List<String> getAllPeriodCategories() {
        return payrollPeriodRepository.findDistinctCategories();
    }

    public Map<String, Object> getPayrollSummaryByCategory(String category) {
        List<PayrollPeriod> periods = payrollPeriodRepository.findByCategoryOrderByStartDateDesc(category);

        if (periods.isEmpty()) {
            throw new ResourceNotFoundException("No payroll periods found for category: " + category);
        }

        PayrollPeriod latestPeriod = periods.get(0);
        List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(latestPeriod.getId());

        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No payroll records found for the latest period in category: " + category);
        }

        double totalNetAmount = records.stream().mapToDouble(PayrollRecord::getNetSalary).sum();
        double totalTax = records.stream().mapToDouble(PayrollRecord::getPayeTax).sum();
        double totalSsnit = records.stream().mapToDouble(PayrollRecord::getSsnitEmployee).sum();
        double totalGross = records.stream().mapToDouble(PayrollRecord::getGrossSalary).sum();
        int totalLeaveDays = records.stream().mapToInt(PayrollRecord::getLeaveDays).sum();
        double totalLeaveHours = records.stream().mapToDouble(r -> r.getLeaveHours() != null ? r.getLeaveHours() : 0.0).sum();
        long employeesOnLeave = records.stream().filter(r -> r.getLeaveDays() > 0).count();
        double totalMultiplierBonus = records.stream().mapToDouble(r -> r.getMultiplierBonus() != null ? r.getMultiplierBonus() : 0.0).sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("category", category);
        summary.put("totalNetAmount", Math.round(totalNetAmount * 100.0) / 100.0);
        summary.put("totalTax", Math.round(totalTax * 100.0) / 100.0);
        summary.put("totalSsnit", Math.round(totalSsnit * 100.0) / 100.0);
        summary.put("totalGross", Math.round(totalGross * 100.0) / 100.0);
        summary.put("employeeCount", records.size());
        summary.put("totalLeaveDays", totalLeaveDays);
        summary.put("totalLeaveHours", Math.round(totalLeaveHours * 100.0) / 100.0);
        summary.put("employeesOnLeave", employeesOnLeave);
        summary.put("totalMultiplierBonus", Math.round(totalMultiplierBonus * 100.0) / 100.0);
        summary.put("periodName", latestPeriod.getName());
        summary.put("periodId", latestPeriod.getId());
        summary.put("startDate", latestPeriod.getStartDate().toString());
        summary.put("endDate", latestPeriod.getEndDate().toString());

        return summary;
    }

    // ==================== UPDATED WORKING DAYS CALCULATION METHODS ====================

    /**
     * Calculate working days in a period considering employee's category setting for weekend exclusion
     * @param startDate Period start date
     * @param endDate Period end date
     * @param employee Employee whose category determines weekend handling
     * @return Number of working days
     */
    private int calculateWorkingDaysInPeriod(LocalDate startDate, LocalDate endDate, Employee employee) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }

        // Get setting from employee's category
        boolean excludeWeekends = true;
        if (employee != null && employee.getCategory() != null) {
            Boolean setting = employee.getCategory().getExcludeWeekendsFromLeave();
            excludeWeekends = setting == null ? true : setting;
            logger.debug("Employee {}: excludeWeekendsFromLeave = {}", employee.getId(), excludeWeekends);
        }

        int workingDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

            if (excludeWeekends) {
                if (!isWeekend) {
                    workingDays++;
                }
            } else {
                workingDays++; // Count ALL days including weekends
            }
            current = current.plusDays(1);
        }

        logger.debug("Working days from {} to {} for employee {}: {} (excludeWeekends={})",
                startDate, endDate, employee != null ? employee.getId() : "null", workingDays, excludeWeekends);
        return workingDays;
    }

    /**
     * Legacy method - kept for backward compatibility
     * @deprecated Use calculateWorkingDaysInPeriod(startDate, endDate, employee) instead
     */
    @Deprecated
    private int calculateWorkingDaysInPeriod(LocalDate startDate, LocalDate endDate) {
        return calculateWorkingDaysInPeriod(startDate, endDate, null);
    }

    private void validatePayrollRecord(PayrollRecord record, Long employeeId) {
        if (record.getBasicSalary() == 0 && record.getTotalHours() > 0) {
            logger.warn("ZERO basic salary for employee {} with {} working hours",
                    employeeId, record.getTotalHours());

            Employee employee = record.getEmployee();
            if (employee != null) {
                double dailyRate = employee.getBasicSalary() / 22;
                double expectedSalary = dailyRate * record.getWorkingDays();
                logger.warn("Expected basic salary: {} (daily rate: {}, working days: {})",
                        expectedSalary, dailyRate, record.getWorkingDays());
            }
        }

        if (record.getNetSalary() < 0) {
            logger.error("Negative net salary for employee {}: {}", employeeId, record.getNetSalary());
        }

        if (record.getLeaveDays() > 0 && record.getLeaveHours() == 0) {
            logger.warn("Employee {} has {} leave days but 0 leave hours",
                    employeeId, record.getLeaveDays());
        }
    }

    private Map<Long, List<LocalDate>> buildEmployeeAttendanceDates(PayrollPeriod period) {
        Map<Long, List<LocalDate>> attendanceDates = new HashMap<>();

        List<Object[]> attendanceData = attendanceRepository.findAttendanceDatesByPeriod(
                period.getStartDate(), period.getEndDate());

        for (Object[] row : attendanceData) {
            if (row.length >= 2 && row[0] != null && row[1] != null) {
                Long employeeId = ((Number) row[0]).longValue();

                LocalDate attendanceDate;
                if (row[1] instanceof java.sql.Date) {
                    attendanceDate = ((java.sql.Date) row[1]).toLocalDate();
                } else if (row[1] instanceof LocalDate) {
                    attendanceDate = (LocalDate) row[1];
                } else {
                    logger.warn("Unexpected date type: {}", row[1].getClass().getName());
                    continue;
                }

                attendanceDates.computeIfAbsent(employeeId, k -> new ArrayList<>()).add(attendanceDate);
            }
        }

        return attendanceDates;
    }

    private Map<Long, Map<LocalDate, Double>> buildEmployeeDailyHours(PayrollPeriod period) {
        Map<Long, Map<LocalDate, Double>> dailyHours = new HashMap<>();

        List<Object[]> dailyHoursData = attendanceRepository.findDailyHoursByPeriod(
                period.getStartDate(), period.getEndDate());

        for (Object[] row : dailyHoursData) {
            if (row.length >= 3 && row[0] != null && row[1] != null && row[2] != null) {
                Long employeeId = ((Number) row[0]).longValue();

                LocalDate attendanceDate;
                if (row[1] instanceof java.sql.Date) {
                    attendanceDate = ((java.sql.Date) row[1]).toLocalDate();
                } else if (row[1] instanceof LocalDate) {
                    attendanceDate = (LocalDate) row[1];
                } else {
                    logger.warn("Unexpected date type: {}", row[1].getClass().getName());
                    continue;
                }

                Double hours = ((Number) row[2]).doubleValue();

                dailyHours.computeIfAbsent(employeeId, k -> new HashMap<>()).put(attendanceDate, hours);
            }
        }

        return dailyHours;
    }

    private MultiplierAdjustedHours calculateMultiplierAdjustedHours(List<LocalDate> attendanceDates,
                                                                     Map<LocalDate, Double> dailyHours,
                                                                     SettingsDTO settings,
                                                                     PayrollPeriod period,
                                                                     double standardHoursPerDay) {
        double totalHours = 0.0;
        double holidayHours = 0.0;
        double weekendHours = 0.0;
        double multiplierBonus = 0.0;
        int holidayDays = 0;
        int weekendDays = 0;
        int workingDays = 0;

        for (LocalDate date : attendanceDates) {
            double recordedHours = dailyHours.getOrDefault(date, 0.0);
            if (recordedHours > 0) {
                // Regular payroll hours are capped for each attendance day before
                // they are summed. This preserves partial days instead of allowing
                // excess hours on one day to offset a short day elsewhere.
                double hours = Math.min(recordedHours, standardHoursPerDay);
                totalHours += hours;
                workingDays++;

                boolean isHoliday = payrollCalculator.isHoliday(date, settings.getHolidays());
                boolean isWeekend = payrollCalculator.isWeekend(date, settings.getWeekendDays());

                double multiplier = 1.0;

                if (isHoliday) {
                    multiplier = settings.getHolidayRate() != null ? settings.getHolidayRate() : 1.5;
                    holidayHours += hours;
                    holidayDays++;
                } else if (isWeekend) {
                    multiplier = settings.getWeekendRate() != null ? settings.getWeekendRate() : 1.25;
                    weekendHours += hours;
                    weekendDays++;
                }

                if (multiplier > 1.0) {
                    double basePay = hours * (settings.getHourlyRate() != null ? settings.getHourlyRate() : 15.0);
                    double adjustedPay = basePay * multiplier;
                    double bonus = adjustedPay - basePay;
                    multiplierBonus += bonus;
                }
            }
        }

        return new MultiplierAdjustedHours(totalHours, workingDays, holidayHours, weekendHours,
                holidayDays, weekendDays, multiplierBonus);
    }

    private SettingsDTO getDefaultSettings() {
        return new SettingsDTO(
                15.0,
                22.5,
                1.25,
                1.5,
                Arrays.asList(0, 6),
                new ArrayList<>(),
                8.0
        );
    }

    private void createErrorPayrollRecord(Long employeeId, PayrollPeriod period, String error) {
        try {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) return;

            PayrollRecord errorRecord = new PayrollRecord();
            errorRecord.setEmployee(employee);
            errorRecord.setPeriod(period);
            errorRecord.setStatus("Error");
            errorRecord.setWorkingDays(0);
            errorRecord.setTotalHours(0.0);
            errorRecord.setOvertimeHours(0.0);
            errorRecord.setLeaveDays(0);
            errorRecord.setLeaveHours(0.0);
            errorRecord.setBasicSalary(0.0);
            errorRecord.setOvertimePay(0.0);
            errorRecord.setGrossSalary(0.0);
            errorRecord.setSsnitEmployee(0.0);
            errorRecord.setSsnitEmployer(0.0);
            errorRecord.setPayeTax(0.0);
            errorRecord.setNetSalary(0.0);
            errorRecord.setTaxableIncome(0.0);
            errorRecord.setTotalAllowances(0.0);
            errorRecord.setHolidayDays(0);
            errorRecord.setWeekendDays(0);
            errorRecord.setHolidayHours(0.0);
            errorRecord.setWeekendHours(0.0);
            errorRecord.setMultiplierBonus(0.0);

            logger.error("Created error record for employee {}: {}", employeeId, error);
            payrollRecordRepository.save(errorRecord);
        } catch (Exception e) {
            logger.error("Failed to create error record for employee {}: {}", employeeId, e.getMessage());
        }
    }

    // ==================== UPDATED LEAVE HOURS CALCULATION ====================

    private Map<Long, Double> calculateLeaveHoursPerEmployee(List<Leave> approvedLeaves, PayrollPeriod period) {
        Map<Long, Double> leaveHoursMap = new HashMap<>();

        if (approvedLeaves == null || approvedLeaves.isEmpty()) {
            logger.info("No approved leaves found for the period");
            return leaveHoursMap;
        }

        for (Leave leave : approvedLeaves) {
            if (leave.getEmployee() == null || leave.getEmployee().getId() == null) {
                logger.warn("Skipping leave with null employee or employee ID");
                continue;
            }

            Long employeeId = leave.getEmployee().getId();

            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                logger.warn("Employee not found for ID: {}", employeeId);
                continue;
            }

            // Pass employee to calculateOverlappingHours so it respects category settings
            double leaveHours = calculateOverlappingHours(leave, period, employee);

            if (leaveHours > 0) {
                leaveHoursMap.merge(employeeId, leaveHours, Double::sum);
                logger.info("Leave hours for employee {}: {} hours (from {} to {}, total leave hours: {})",
                        employeeId, leaveHours, leave.getStartDate(), leave.getEndDate(),
                        leave.getTotalLeaveHours() != null ? leave.getTotalLeaveHours() : 0.0);
            } else {
                logger.debug("No overlapping hours for employee {}: leave {} to {}, period {} to {}",
                        employeeId, leave.getStartDate(), leave.getEndDate(),
                        period.getStartDate(), period.getEndDate());
            }
        }

        logger.info("Total employees with leave hours: {}", leaveHoursMap.size());
        return leaveHoursMap;
    }

    /**
     * Calculate overlapping leave hours between a leave request and a payroll period
     * Now respects employee's category setting for weekend exclusion
     */
    private double calculateOverlappingHours(Leave leave, PayrollPeriod period, Employee employee) {
        if (leave.isUnpaid()) return 0.0;
        LocalDate leaveStart = leave.getStartDate();
        LocalDate leaveEnd = leave.getEndDate();
        LocalDate periodStart = period.getStartDate();
        LocalDate periodEnd = period.getEndDate();

        if (leaveEnd.isBefore(periodStart) || leaveStart.isAfter(periodEnd)) {
            return 0.0;
        }

        LocalDate overlapStart = leaveStart.isAfter(periodStart) ? leaveStart : periodStart;
        LocalDate overlapEnd = leaveEnd.isBefore(periodEnd) ? leaveEnd : periodEnd;

        if (overlapStart.isAfter(overlapEnd)) {
            return 0.0;
        }

        // FIXED: Pass employee to calculateWorkingDaysInPeriod to respect category setting
        int overlappingDays = calculateWorkingDaysInPeriod(overlapStart, overlapEnd, employee);

        int totalLeaveDays = calculateWorkingDaysInPeriod(leaveStart, leaveEnd, employee);

        if (totalLeaveDays == 0) {
            logger.warn("No working days in leave period {} to {} for employee {}",
                    leaveStart, leaveEnd, employee.getId());
            return 0.0;
        }

        double standardHoursPerDay = getStandardHoursPerDay(employee);
        Double totalLeaveHours = leave.getTotalLeaveHours();
        if (totalLeaveHours == null || totalLeaveHours == 0.0) {
            totalLeaveHours = totalLeaveDays * standardHoursPerDay;
            logger.info("Calculated total leave hours for leave {}: {} hours", leave.getId(), totalLeaveHours);
        }

        double proportionalHours = ((double) overlappingDays / totalLeaveDays) * totalLeaveHours;
        double roundedHours = Math.round(proportionalHours * 100.0) / 100.0;

        logger.debug("Overlap calculation for employee {}: {} overlapping days, {} total leave days, {} total leave hours, {} proportional hours",
                employee.getId(), overlappingDays, totalLeaveDays, totalLeaveHours, roundedHours);

        return roundedHours;
    }

    private void setAllowances(PayrollRecord record, Employee employee,
                               Map<Long, Map<String, Double>> tempAllowances) {
        double rentAllowance = employee.getRentAllowance() != null ? employee.getRentAllowance() : 0.0;
        double transportAllowance = employee.getTransportAllowance() != null ? employee.getTransportAllowance() : 0.0;
        double clothingAllowance = employee.getClothingAllowance() != null ? employee.getClothingAllowance() : 0.0;
        double otherAllowance = employee.getOtherAllowance() != null ? employee.getOtherAllowance() : 0.0;
        double nssAllowance = employee.getNssAllowance() != null ? employee.getNssAllowance() : 0.0;

        boolean hasTemporaryAllowances = false;

        if (tempAllowances != null && tempAllowances.containsKey(employee.getId())) {
            Map<String, Double> empTemp = tempAllowances.get(employee.getId());

            if (empTemp != null) {
                if (empTemp.get("housingAllowance") != null) rentAllowance = empTemp.get("housingAllowance");
                if (empTemp.get("tntAllowance") != null) transportAllowance = empTemp.get("tntAllowance");
                if (empTemp.get("clothsAllowances") != null) clothingAllowance = empTemp.get("clothsAllowances");
                if (empTemp.get("otherAllowances") != null) otherAllowance = empTemp.get("otherAllowances");
                if (empTemp.get("nssAllowance") != null) nssAllowance = empTemp.get("nssAllowance");

                hasTemporaryAllowances = empTemp.keySet().stream().anyMatch(key ->
                        key.equals("housingAllowance") || key.equals("tntAllowance")
                                || key.equals("clothsAllowances") || key.equals("otherAllowances")
                                || key.equals("nssAllowance"));
            }

            logger.info("Using temporary allowances for employee {}: Housing={}, Transport={}, Clothing={}, Other={}, NSS={}",
                    employee.getId(), rentAllowance, transportAllowance, clothingAllowance, otherAllowance, nssAllowance);
        }

        record.setRentAllowance(rentAllowance);
        record.setTransportAllowance(transportAllowance);
        record.setClothingAllowance(clothingAllowance);
        record.setOtherAllowance(otherAllowance);
        record.setNssAllowance(nssAllowance);

        double totalAllowances = rentAllowance + transportAllowance + clothingAllowance + otherAllowance + nssAllowance;
        record.setTotalAllowances(totalAllowances);

        record.setUsesTemporaryAllowances(hasTemporaryAllowances);

        logger.info("Final allowances for employee {}: Total={}, NSS={}, Temporary={}",
                employee.getId(), totalAllowances, nssAllowance, hasTemporaryAllowances);
    }

    // ==================== EXISTING PUBLIC METHODS ====================

    public List<PayrollPeriod> getAllPeriods() {
        return payrollPeriodRepository.findAllByOrderByStartDateDesc();
    }

    @Transactional
    public PayrollPeriod createPeriod(PayrollPeriod period) {
        if (period.getStartDate() == null || period.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (period.getStartDate().isAfter(period.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        if (period.isProjectPayroll()) {
            if (period.getProjectId() == null) throw new IllegalArgumentException("Select a project");
            Job project = projectPayrollJobs.findById(period.getProjectId()).orElseThrow(() -> new IllegalArgumentException("Project not found"));
            if (!payrollPeriodRepository.findOverlappingProjectPeriods(period.getStartDate(),period.getEndDate(),period.getProjectId()).isEmpty())
                throw new IllegalArgumentException("Payroll period overlaps another period for this project");
            period.setProjectName(project.getName()); period.setCategory(null); period.setStatus("Open");
            return payrollPeriodRepository.save(period);
        }
        if (period.getPeriodType() != null && !"CATEGORY".equals(period.getPeriodType())) throw new IllegalArgumentException("Invalid payroll period type");
        period.setPeriodType("CATEGORY"); period.setProjectId(null); period.setProjectName(null);
        if (period.getCategory() == null || period.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (!payrollPeriodRepository.findOverlappingPeriodsForCategory(
                period.getStartDate(), period.getEndDate(), period.getCategory()).isEmpty()) {
            throw new IllegalArgumentException(
                    "Payroll period overlaps with existing period in category: " + period.getCategory());
        }
        return payrollPeriodRepository.save(period);
    }

    @Transactional(readOnly = true)
    public List<PayrollRecord> getPayrollForPeriod(Long periodId) {
        List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(periodId);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No payroll records found for period ID: " + periodId);
        }
        if (records.stream().anyMatch(r -> r.getNormalRateOvertimeHours() == null || r.getEffectiveRateOvertimeHours() == null)) {
            PayrollPeriod period = records.get(0).getPeriod();
            Map<Long, Map<LocalDate, Double>> datedOvertime = new HashMap<>();
            overtimeRepository.findApprovedOvertimeForPayrollPeriod(period.getStartDate(), period.getEndDate())
                    .forEach(o -> {
                        if (o.getEmployee() != null && o.getDate() != null && o.getOvertimeHours() != null)
                            datedOvertime.computeIfAbsent(o.getEmployee().getId(), ignored -> new HashMap<>())
                                    .merge(o.getDate(), o.getOvertimeHours(), Double::sum);
                    });
            for (PayrollRecord record : records) {
                if (record.getNormalRateOvertimeHours() != null && record.getEffectiveRateOvertimeHours() != null) continue;
                double savedTotal = record.getOvertimeHours() == null ? 0 : record.getOvertimeHours();
                Map<LocalDate, Double> dated = datedOvertime.getOrDefault(record.getEmployee().getId(), Collections.emptyMap());
                double datedTotal = dated.values().stream().mapToDouble(Double::doubleValue).sum();
                if (Math.abs(savedTotal - datedTotal) < 0.000001) {
                    PayrollOvertimeRateHours.Split split = PayrollOvertimeRateHours.split(
                            dated, Collections.emptyMap(), 0, 0, false, date -> false, 1,
                            record.getAppliedEffectivePayrollRateFrom(), record.getAppliedEffectivePayrollRateTo());
                    record.setNormalRateOvertimeHours(split.oldRate());
                    record.setEffectiveRateOvertimeHours(split.newRate());
                }
            }
        }
        return records;
    }

    @Transactional
    public void processPayroll(Long periodId) {
        payrollPeriodRepository.lockForPayrollGeneration();
        logger.info("Processing payroll for period ID: {}", periodId);
        List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(periodId);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No payroll records found for period ID: " + periodId);
        }
        if (records.stream().allMatch(r -> "Processed".equals(r.getStatus()))) {
            throw new IllegalStateException("Payroll is already processed for period ID: " + periodId);
        }
        for (PayrollRecord record : records) {
            record.setStatus("Processed");
            payrollRecordRepository.save(record);
        }
        logger.info("Payroll processing completed for period ID: {}", periodId);
    }

    public Map<String, Object> getPayrollSummary() {
        PayrollPeriod latestPeriod = payrollPeriodRepository.findTopByOrderByEndDateDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No payroll periods found"));

        List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(latestPeriod.getId());
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No payroll records found for the latest period");
        }

        double totalNetAmount = records.stream().mapToDouble(PayrollRecord::getNetSalary).sum();
        double totalTax = records.stream().mapToDouble(PayrollRecord::getPayeTax).sum();
        double totalSsnit = records.stream().mapToDouble(PayrollRecord::getSsnitEmployee).sum();
        double totalGross = records.stream().mapToDouble(PayrollRecord::getGrossSalary).sum();
        int totalLeaveDays = records.stream().mapToInt(PayrollRecord::getLeaveDays).sum();
        double totalLeaveHours = records.stream().mapToDouble(r -> r.getLeaveHours() != null ? r.getLeaveHours() : 0.0).sum();
        long employeesOnLeave = records.stream().filter(r -> r.getLeaveDays() > 0).count();
        double totalMultiplierBonus = records.stream().mapToDouble(r -> r.getMultiplierBonus() != null ? r.getMultiplierBonus() : 0.0).sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalNetAmount", Math.round(totalNetAmount * 100.0) / 100.0);
        summary.put("totalTax", Math.round(totalTax * 100.0) / 100.0);
        summary.put("totalSsnit", Math.round(totalSsnit * 100.0) / 100.0);
        summary.put("totalGross", Math.round(totalGross * 100.0) / 100.0);
        summary.put("employeeCount", records.size());
        summary.put("totalLeaveDays", totalLeaveDays);
        summary.put("totalLeaveHours", Math.round(totalLeaveHours * 100.0) / 100.0);
        summary.put("employeesOnLeave", employeesOnLeave);
        summary.put("totalMultiplierBonus", Math.round(totalMultiplierBonus * 100.0) / 100.0);
        summary.put("periodName", latestPeriod.getName());
        summary.put("periodId", latestPeriod.getId());
        summary.put("startDate", latestPeriod.getStartDate().toString());
        summary.put("endDate", latestPeriod.getEndDate().toString());
        summary.put("periodDates", latestPeriod.getStartDate() + " to " + latestPeriod.getEndDate());

        return summary;
    }

    public List<PayrollRecord> getEmployeePayrollHistory(Long employeeId) {
        return payrollRecordRepository.findByEmployeeIdOrderByPeriodStartDateDesc(employeeId);
    }

    public Map<String, Object> getPayrollDashboardStats() {
        PayrollPeriod latestPeriod = payrollPeriodRepository.findTopByOrderByEndDateDesc()
                .orElse(null);

        if (latestPeriod == null) {
            return Map.of(
                    "totalEmployees", 0,
                    "totalPayrollAmount", 0.0,
                    "pendingPayrolls", 0,
                    "recentPeriod", "No periods found"
            );
        }

        List<PayrollRecord> latestRecords = payrollRecordRepository.findByPeriodId(latestPeriod.getId());
        List<PayrollPeriod> allPeriods = payrollPeriodRepository.findAll();
        long totalEmployees = employeeRepository.count();

        double totalPayrollAmount = latestRecords.stream()
                .mapToDouble(PayrollRecord::getNetSalary)
                .sum();

        long pendingPayrolls = allPeriods.stream()
                .filter(period -> {
                    List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(period.getId());
                    return records.isEmpty() || records.stream().anyMatch(r -> "Pending".equals(r.getStatus()));
                })
                .count();

        return Map.of(
                "totalEmployees", totalEmployees,
                "totalPayrollAmount", Math.round(totalPayrollAmount * 100.0) / 100.0,
                "pendingPayrolls", pendingPayrolls,
                "recentPeriod", latestPeriod.getName(),
                "periodStatus", latestRecords.isEmpty() ? "No records" :
                        latestRecords.stream().allMatch(r -> "Processed".equals(r.getStatus())) ? "Processed" : "Pending"
        );
    }

    public Map<String, Object> getPayrollSummaryByPeriod(Long periodId) {
        List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(periodId);

        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No payroll records found for period: " + periodId);
        }

        double totalNetAmount = records.stream()
                .mapToDouble(r -> r.getNetSalary() != null ? r.getNetSalary() : 0.0)
                .sum();

        double totalTax = records.stream()
                .mapToDouble(r -> r.getPayeTax() != null ? r.getPayeTax() : 0.0)
                .sum();

        double totalSsnitEmployee = records.stream()
                .mapToDouble(r -> r.getSsnitEmployee() != null ? r.getSsnitEmployee() : 0.0)
                .sum();

        double totalSsnitEmployer = records.stream()
                .mapToDouble(r -> r.getSsnitEmployer() != null ? r.getSsnitEmployer() : 0.0)
                .sum();

        double totalGrossSalary = records.stream()
                .mapToDouble(r -> r.getGrossSalary() != null ? r.getGrossSalary() : 0.0)
                .sum();

        long employeeCount = records.size();
        long employeesOnLeave = records.stream()
                .filter(r -> r.getLeaveDays() != null && r.getLeaveDays() > 0)
                .count();

        double totalLeaveDays = records.stream()
                .mapToDouble(r -> r.getLeaveDays() != null ? r.getLeaveDays() : 0)
                .sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalNetAmount", Math.round(totalNetAmount * 100.0) / 100.0);
        summary.put("totalTax", Math.round(totalTax * 100.0) / 100.0);
        summary.put("totalSsnit", Math.round((totalSsnitEmployee + totalSsnitEmployer) * 100.0) / 100.0);
        summary.put("totalSsnitEmployee", Math.round(totalSsnitEmployee * 100.0) / 100.0);
        summary.put("totalSsnitEmployer", Math.round(totalSsnitEmployer * 100.0) / 100.0);
        summary.put("employeeCount", employeeCount);
        summary.put("totalLeaveDays", totalLeaveDays);
        summary.put("employeesOnLeave", employeesOnLeave);
        summary.put("totalGrossSalary", Math.round(totalGrossSalary * 100.0) / 100.0);
        summary.put("periodId", periodId);

        PayrollPeriod period = payrollPeriodRepository.findById(periodId)
                .orElse(null);
        if (period != null) {
            summary.put("periodName", period.getName());
            summary.put("periodStart", period.getStartDate().toString());
            summary.put("periodEnd", period.getEndDate().toString());
            summary.put("periodCategory", period.getCategory());
        }

        return summary;
    }

    @Transactional
    public void recalculatePayroll(Long periodId) {
        SystemSettings sys = settingsService.getSystemSettings();
        logger.info("Recalculating payroll for period ID: {}", periodId);

        PayrollPeriod period = payrollPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found for id: " + periodId));

        List<PayrollRecord> existingRecords = payrollRecordRepository.findByPeriodId(periodId);
        if (existingRecords.isEmpty()) {
            throw new ResourceNotFoundException("No payroll records found for period ID: " + periodId);
        }

        for (PayrollRecord record : existingRecords) {
            try {
                Employee employee = record.getEmployee();
                if (employee == null) continue;

                double creditAmount = record.getCreditAmount() != null ? record.getCreditAmount() : 0.0;
                double preCalculatedOvertimePay = 0.0;

                Boolean excludeFromSsnit = employee.getExcludeFromSsnit() != null ? employee.getExcludeFromSsnit() : false;

                ghanaPayrollCalculator.calculatePayrollWithLeave(
                        employee,
                        period.getStartDate(),
                        period.getEndDate(),
                        record.getTotalHours(),
                        record.getWorkingDays(),
                        record.getOvertimeHours(),
                        record.getLeaveDays(),
                        record.getLeaveHours() != null ? record.getLeaveHours() : 0.0,
                        record,
                        sys,
                        preCalculatedOvertimePay,
                        creditAmount,
                        excludeFromSsnit
                );

                if (excludeFromSsnit) {
                    record.setSsnitEmployee(0.0);
                    record.setSsnitEmployer(0.0);
                    logger.info("  - SSNIT deductions ZEROED for employee {} (Excluded from SSNIT)", employee.getEmployeeId());
                }

                Job employeeJob = employee.getJob();
                double overtimeMultiplier = employeeJob != null
                        && employeeJob.getOvertimeMultiplier() != null
                        && employeeJob.getOvertimeMultiplier() > 0
                        ? employeeJob.getOvertimeMultiplier()
                        : (sys != null && sys.getDefaultOvertimeMultiplier() != null
                            ? sys.getDefaultOvertimeMultiplier()
                            : 1.5);

                double rawOvertime = record.getOvertimeHours() != null ? record.getOvertimeHours() : 0.0;
                double payableOvertime = employeeJob != null
                        && Boolean.TRUE.equals(employeeJob.getEnforceMinimumOvertime())
                        && employeeJob.getMinimumOvertimeHours() != null
                        ? Math.max(rawOvertime, employeeJob.getMinimumOvertimeHours())
                        : rawOvertime;
                record.setWeightedOvertimeHours(payableOvertime * overtimeMultiplier);

                payrollRecordRepository.save(record);
            } catch (Exception e) {
                logger.error("Failed to recalculate payroll for employee {}: {}",
                        record.getEmployee() != null ? record.getEmployee().getId() : "unknown",
                        e.getMessage(), e);
            }
        }

        logger.info("Payroll recalculation completed for period ID: {}", periodId);
    }

    @Transactional
    public PayrollRecord removeTemporaryAllowances(Long periodId, Long employeeId) {
        PayrollRecord record = payrollRecordRepository.findByPeriodIdAndEmployeeId(periodId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payroll record found for employee " + employeeId + " in period " + periodId));

        if ("Processed".equalsIgnoreCase(record.getStatus())) {
            throw new IllegalStateException("Temporary allowances cannot be removed from processed payroll");
        }

        Employee employee = record.getEmployee();
        PayrollPeriod period = record.getPeriod();
        SystemSettings systemSettings = settingsService.getSystemSettings();

        double loanDeduction = record.getLoanDeduction() != null ? record.getLoanDeduction() : 0.0;
        double creditAmount = record.getCreditAmount() != null ? record.getCreditAmount() : 0.0;
        double leaveHours = record.getLeaveHours() != null ? record.getLeaveHours() : 0.0;
        double totalHours = record.getTotalHours() != null ? record.getTotalHours() : 0.0;
        double normalHours = Math.max(0.0, totalHours - leaveHours);

        setAllowances(record, employee, null);

        ghanaPayrollCalculator.calculatePayrollWithMultipliers(
                employee,
                period.getStartDate(),
                period.getEndDate(),
                normalHours,
                record.getWorkingDays(),
                record.getOvertimeHours() != null ? record.getOvertimeHours() : 0.0,
                record.getLeaveDays() != null ? record.getLeaveDays() : 0,
                leaveHours,
                record.getHolidayHours() != null ? record.getHolidayHours() : 0.0,
                record.getWeekendHours() != null ? record.getWeekendHours() : 0.0,
                record.getMultiplierBonus() != null ? record.getMultiplierBonus() : 0.0,
                getDefaultSettings(),
                true,
                record,
                systemSettings,
                record.getOvertimePay() != null ? record.getOvertimePay() : 0.0,
                creditAmount,
                employee.getExcludeFromSsnit(),
                record.getPrecalculatedHourlyBasicPay()
        );

        if (loanDeduction > 0) {
            record.setNetSalary(record.getNetSalary() - loanDeduction);
            record.setLoanDeduction(loanDeduction);
        }
        // This path recalculates pay outside generation; do not expose a stale project allocation.
        record.setProjectEarningsSnapshot(null);
        record.setUsesTemporaryAllowances(false);
        record.setIncludeBasicSalary(true);
        return payrollRecordRepository.save(record);
    }

    @Transactional
    public void deletePayrollPeriod(Long periodId) {
        payrollPeriodRepository.lockForPayrollGeneration();
        if(!payrollRecordRepository.findByPeriodId(periodId).isEmpty()) throw new IllegalStateException("Clear unprocessed payroll records before deleting the period; processed payment history must be retained");
        logger.info("Deleting payroll period ID: {}", periodId);

        PayrollPeriod period = payrollPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll period not found for id: " + periodId));

        payrollRecordRepository.deleteByPeriodId(periodId);
        payrollPeriodRepository.delete(period);

        logger.info("Payroll period ID: {} deleted successfully", periodId);
    }

    public List<PayrollRecord> getFilteredPayrollRecords(Long periodId, String category, String status) {
        List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(periodId);

        if (category != null && !category.isEmpty()) {
            records = records.stream()
                    .filter(r -> r.getEmployee() != null &&
                            r.getEmployee().getCategory() != null &&
                            r.getEmployee().getCategory().toLowerCase().contains(category.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isEmpty()) {
            records = records.stream()
                    .filter(r -> status.equalsIgnoreCase(r.getStatus()))
                    .collect(Collectors.toList());
        }

        return records;
    }

    public List<Map<String, Object>> exportPayrollData(Long periodId) {
        List<PayrollRecord> records = getPayrollForPeriod(periodId);

        return records.stream().map(record -> {
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("employeeId", record.getEmployee().getId());
            exportData.put("employeeName", record.getEmployee().getFirstName() + " " + record.getEmployee().getLastName());
            exportData.put("category", record.getEmployee().getCategory());
            exportData.put("basicSalary", record.getBasicSalary());
            exportData.put("overtimePay", record.getOvertimePay());
            exportData.put("weightedOvertimeHours",
                    record.getWeightedOvertimeHours() != null ? record.getWeightedOvertimeHours() : 0.0);
            exportData.put("rentAllowance", record.getRentAllowance());
            exportData.put("transportAllowance", record.getTransportAllowance());
            exportData.put("clothingAllowance", record.getClothingAllowance());
            exportData.put("otherAllowance", record.getOtherAllowance());
            exportData.put("nssAllowance", record.getNssAllowance());
            exportData.put("grossSalary", record.getGrossSalary());
            exportData.put("nonTaxableIncome", record.getNonTaxableIncome() != null ? record.getNonTaxableIncome() : 0.0);
            exportData.put("ssnitEmployee", record.getSsnitEmployee());
            exportData.put("ssnitEmployer", record.getSsnitEmployer());
            exportData.put("payeTax", record.getPayeTax());
            exportData.put("netSalary", record.getNetSalary());
            exportData.put("workingDays", record.getWorkingDays());
            exportData.put("leaveDays", record.getLeaveDays());
            exportData.put("leaveHours", record.getLeaveHours() != null ? record.getLeaveHours() : 0.0);
            exportData.put("totalHours", record.getTotalHours());
            exportData.put("holidayDays", record.getHolidayDays() != null ? record.getHolidayDays() : 0);
            exportData.put("weekendDays", record.getWeekendDays() != null ? record.getWeekendDays() : 0);
            exportData.put("holidayHours", record.getHolidayHours() != null ? record.getHolidayHours() : 0.0);
            exportData.put("weekendHours", record.getWeekendHours() != null ? record.getWeekendHours() : 0.0);
            exportData.put("multiplierBonus", record.getMultiplierBonus() != null ? record.getMultiplierBonus() : 0.0);
            exportData.put("status", record.getStatus());
            exportData.put("creditAmount", record.getCreditAmount() != null ? record.getCreditAmount() : 0.0);
            exportData.put("appliedCreditIds", record.getAppliedCreditIds());
            exportData.put("excludeFromSsnit", record.getEmployee().getExcludeFromSsnit() != null && record.getEmployee().getExcludeFromSsnit());

            return exportData;
        }).collect(Collectors.toList());
    }

    public PayrollRecord getEmployeePayslip(Long periodId, Long employeeId) {
        return payrollRecordRepository.findByPeriodIdAndEmployeeId(periodId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payslip found for employee " + employeeId + " in period " + periodId));
    }

    public byte[] generatePayslipPDF(Long periodId, Long employeeId) {
        logger.info("Generating PDF payslip for employee {} in period {}", employeeId, periodId);

        try {
            PayrollRecord payslip = getEmployeePayslip(periodId, employeeId);
            return new byte[0];
        } catch (Exception e) {
            logger.error("Failed to generate PDF payslip for employee {}: {}", employeeId, e.getMessage());
            throw new RuntimeException("Failed to generate payslip PDF", e);
        }
    }

    public List<PayrollRecord> getEmployeePayrollByDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return payrollRecordRepository.findByEmployeeIdAndPeriodStartDateBetween(employeeId, startDate, endDate);
    }

    public Map<String, Object> getEmployeePayrollSummary(Long employeeId) {
        List<PayrollRecord> records = getEmployeePayrollHistory(employeeId);

        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No payroll records found for employee ID: " + employeeId);
        }

        double totalNetAmount = records.stream().mapToDouble(PayrollRecord::getNetSalary).sum();
        double totalTax = records.stream().mapToDouble(PayrollRecord::getPayeTax).sum();
        double totalSsnit = records.stream().mapToDouble(PayrollRecord::getSsnitEmployee).sum();
        double totalGross = records.stream().mapToDouble(PayrollRecord::getGrossSalary).sum();
        int totalLeaveDays = records.stream().mapToInt(PayrollRecord::getLeaveDays).sum();
        double totalLeaveHours = records.stream().mapToDouble(r -> r.getLeaveHours() != null ? r.getLeaveHours() : 0.0).sum();
        double totalMultiplierBonus = records.stream().mapToDouble(r -> r.getMultiplierBonus() != null ? r.getMultiplierBonus() : 0.0).sum();

        Employee employee = records.get(0).getEmployee();

        Map<String, Object> summary = new HashMap<>();
        summary.put("employeeId", employeeId);
        summary.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
        summary.put("totalNetAmount", Math.round(totalNetAmount * 100.0) / 100.0);
        summary.put("totalTax", Math.round(totalTax * 100.0) / 100.0);
        summary.put("totalSsnit", Math.round(totalSsnit * 100.0) / 100.0);
        summary.put("totalGross", Math.round(totalGross * 100.0) / 100.0);
        summary.put("totalLeaveDays", totalLeaveDays);
        summary.put("totalLeaveHours", Math.round(totalLeaveHours * 100.0) / 100.0);
        summary.put("totalMultiplierBonus", Math.round(totalMultiplierBonus * 100.0) / 100.0);
        summary.put("periodCount", records.size());
        summary.put("latestPeriod", records.get(0).getPeriod().getName());

        return summary;
    }

    private double getStandardHoursPerDay(Employee employee) {
        if (employee == null) {
            return 8.0;
        }

        Category category = employee.getCategory();
        if (category == null) {
            return 8.0;
        }

        Integer hours = category.getStandardRateHours();
        if (hours == null || hours <= 0) {
            return 8.0;
        }

        return hours.doubleValue();
    }

    public MonthlyReportResponseDTO getPayrollReportByCategoryAndMonth() {
        MonthlyReportResponseDTO response = new MonthlyReportResponseDTO();

        List<String> categories = payrollRecordRepository.findAllCategoriesWithRecords();
        response.setAvailableCategories(categories);

        List<String> availableMonths = payrollRecordRepository.findAllMonthsWithRecords();
        response.setAvailableMonths(availableMonths);

        Map<String, Map<String, CategoryMonthReportDTO.MonthlyTotals>> reportData = new LinkedHashMap<>();

        for (String category : categories) {
            Map<String, CategoryMonthReportDTO.MonthlyTotals> monthlyMap = new LinkedHashMap<>();

            List<Object[]> aggregates = payrollRecordRepository.getMonthlyAggregatesByCategory(category);

            for (Object[] row : aggregates) {
                CategoryMonthReportDTO.MonthlyTotals totals = new CategoryMonthReportDTO.MonthlyTotals();
                totals.setTotalNetSalary(roundToTwoDecimals(((Number) row[0]).doubleValue()));
                totals.setTotalTax(roundToTwoDecimals(((Number) row[1]).doubleValue()));
                totals.setTotalSsnitEmployee(roundToTwoDecimals(((Number) row[2]).doubleValue()));
                totals.setTotalSsnitEmployer(roundToTwoDecimals(((Number) row[3]).doubleValue()));
                totals.setTotalGrossSalary(roundToTwoDecimals(((Number) row[4]).doubleValue()));
                totals.setEmployeeCount(((Number) row[5]).intValue());

                String yearMonth = (String) row[6];
                totals.setPeriodName((String) row[7]);
                totals.setPeriodId(String.valueOf(row[8]));

                monthlyMap.put(yearMonth, totals);
            }

            if (!monthlyMap.isEmpty()) {
                reportData.put(category, monthlyMap);
            }
        }

        response.setReportData(reportData);

        Map<String, Double> grandTotals = new HashMap<>();
        double totalNetAll = 0;
        double totalTaxAll = 0;
        double totalSsnitAll = 0;
        double totalGrossAll = 0;
        int totalEmployeesAll = 0;

        for (Map<String, CategoryMonthReportDTO.MonthlyTotals> categoryMap : reportData.values()) {
            for (CategoryMonthReportDTO.MonthlyTotals totals : categoryMap.values()) {
                totalNetAll += totals.getTotalNetSalary();
                totalTaxAll += totals.getTotalTax();
                totalSsnitAll += totals.getTotalSsnit();
                totalGrossAll += totals.getTotalGrossSalary();
                totalEmployeesAll += totals.getEmployeeCount();
            }
        }

        grandTotals.put("totalNetSalary", roundToTwoDecimals(totalNetAll));
        grandTotals.put("totalTax", roundToTwoDecimals(totalTaxAll));
        grandTotals.put("totalSsnit", roundToTwoDecimals(totalSsnitAll));
        grandTotals.put("totalGrossSalary", roundToTwoDecimals(totalGrossAll));
        grandTotals.put("totalEmployees", (double) totalEmployeesAll);

        response.setGrandTotals(grandTotals);

        return response;
    }

    public MonthlyReportResponseDTO getAllPayrollReports() {
        MonthlyReportResponseDTO response = new MonthlyReportResponseDTO();

        List<String> categories = payrollRecordRepository.findAllCategoriesWithRecords();
        response.setAvailableCategories(categories);

        List<String> availableMonths = payrollRecordRepository.findAllMonthsWithRecords();
        response.setAvailableMonths(availableMonths);

        Map<String, Map<String, CategoryMonthReportDTO.MonthlyTotals>> reportData = new LinkedHashMap<>();

        for (String category : categories) {
            Map<String, CategoryMonthReportDTO.MonthlyTotals> monthlyMap = new LinkedHashMap<>();

            List<PayrollRecord> records = payrollRecordRepository.findByEmployeeCategory(category);

            for (PayrollRecord record : records) {
                if (record.getPeriod() != null && record.getPeriod().getEndDate() != null) {
                    String yearMonth = String.format("%d-%02d",
                            record.getPeriod().getEndDate().getYear(),
                            record.getPeriod().getEndDate().getMonthValue());

                    CategoryMonthReportDTO.MonthlyTotals totals = monthlyMap.getOrDefault(yearMonth, new CategoryMonthReportDTO.MonthlyTotals());

                    totals.setTotalNetSalary(totals.getTotalNetSalary() + (record.getNetSalary() != null ? record.getNetSalary() : 0));
                    totals.setTotalTax(totals.getTotalTax() + (record.getPayeTax() != null ? record.getPayeTax() : 0));
                    totals.setTotalSsnitEmployee(totals.getTotalSsnitEmployee() + (record.getSsnitEmployee() != null ? record.getSsnitEmployee() : 0));
                    totals.setTotalSsnitEmployer(totals.getTotalSsnitEmployer() + (record.getSsnitEmployer() != null ? record.getSsnitEmployer() : 0));
                    totals.setTotalGrossSalary(totals.getTotalGrossSalary() + (record.getGrossSalary() != null ? record.getGrossSalary() : 0));
                    totals.setEmployeeCount(totals.getEmployeeCount() + 1);
                    totals.setPeriodName(record.getPeriod().getName());
                    totals.setPeriodId(String.valueOf(record.getPeriod().getId()));

                    monthlyMap.put(yearMonth, totals);
                }
            }

            if (!monthlyMap.isEmpty()) {
                Map<String, CategoryMonthReportDTO.MonthlyTotals> sortedMap = new LinkedHashMap<>();
                monthlyMap.entrySet().stream()
                        .sorted(Map.Entry.<String, CategoryMonthReportDTO.MonthlyTotals>comparingByKey().reversed())
                        .forEachOrdered(entry -> sortedMap.put(entry.getKey(), entry.getValue()));
                reportData.put(category, sortedMap);
            }
        }

        response.setReportData(reportData);

        Map<String, Double> grandTotals = new HashMap<>();
        double totalNetAll = 0;
        double totalTaxAll = 0;
        double totalSsnitAll = 0;
        double totalGrossAll = 0;
        int totalEmployeesAll = 0;

        for (Map<String, CategoryMonthReportDTO.MonthlyTotals> categoryMap : reportData.values()) {
            for (CategoryMonthReportDTO.MonthlyTotals totals : categoryMap.values()) {
                totalNetAll += totals.getTotalNetSalary();
                totalTaxAll += totals.getTotalTax();
                totalSsnitAll += totals.getTotalSsnit();
                totalGrossAll += totals.getTotalGrossSalary();
                totalEmployeesAll += totals.getEmployeeCount();
            }
        }

        grandTotals.put("totalNetSalary", roundToTwoDecimals(totalNetAll));
        grandTotals.put("totalTax", roundToTwoDecimals(totalTaxAll));
        grandTotals.put("totalSsnit", roundToTwoDecimals(totalSsnitAll));
        grandTotals.put("totalGrossSalary", roundToTwoDecimals(totalGrossAll));
        grandTotals.put("totalEmployees", (double) totalEmployeesAll);

        response.setGrandTotals(grandTotals);

        return response;
    }



    public MonthlyReportResponseDTO getPayrollReportForSpecificMonth(String yearMonth) {
        MonthlyReportResponseDTO response = new MonthlyReportResponseDTO();

        List<String> categories = payrollRecordRepository.findAllCategoriesWithRecords();
        response.setAvailableCategories(categories);

        Map<String, Map<String, CategoryMonthReportDTO.MonthlyTotals>> reportData = new LinkedHashMap<>();

        for (String category : categories) {
            List<Object[]> aggregates = payrollRecordRepository.getMonthlyAggregatesByCategory(category);
            Map<String, CategoryMonthReportDTO.MonthlyTotals> monthlyMap = new LinkedHashMap<>();

            for (Object[] row : aggregates) {
                String currentMonth = (String) row[6];
                if (currentMonth.equals(yearMonth)) {
                    CategoryMonthReportDTO.MonthlyTotals totals = new CategoryMonthReportDTO.MonthlyTotals();
                    totals.setTotalNetSalary(roundToTwoDecimals(((Number) row[0]).doubleValue()));
                    totals.setTotalTax(roundToTwoDecimals(((Number) row[1]).doubleValue()));
                    totals.setTotalSsnitEmployee(roundToTwoDecimals(((Number) row[2]).doubleValue()));
                    totals.setTotalSsnitEmployer(roundToTwoDecimals(((Number) row[3]).doubleValue()));
                    totals.setTotalGrossSalary(roundToTwoDecimals(((Number) row[4]).doubleValue()));
                    totals.setEmployeeCount(((Number) row[5]).intValue());
                    totals.setPeriodName((String) row[7]);
                    totals.setPeriodId(String.valueOf(row[8]));

                    monthlyMap.put(currentMonth, totals);
                    break;
                }
            }

            if (!monthlyMap.isEmpty()) {
                reportData.put(category, monthlyMap);
            }
        }

        response.setReportData(reportData);
        response.setAvailableMonths(List.of(yearMonth));

        Map<String, Double> monthlyTotals = new HashMap<>();
        double netSum = 0, taxSum = 0, ssnitSum = 0, grossSum = 0;
        int empCount = 0;

        for (Map<String, CategoryMonthReportDTO.MonthlyTotals> categoryMap : reportData.values()) {
            CategoryMonthReportDTO.MonthlyTotals totals = categoryMap.get(yearMonth);
            if (totals != null) {
                netSum += totals.getTotalNetSalary();
                taxSum += totals.getTotalTax();
                ssnitSum += totals.getTotalSsnit();
                grossSum += totals.getTotalGrossSalary();
                empCount += totals.getEmployeeCount();
            }
        }

        monthlyTotals.put("totalNetSalary", roundToTwoDecimals(netSum));
        monthlyTotals.put("totalTax", roundToTwoDecimals(taxSum));
        monthlyTotals.put("totalSsnit", roundToTwoDecimals(ssnitSum));
        monthlyTotals.put("totalGrossSalary", roundToTwoDecimals(grossSum));
        monthlyTotals.put("totalEmployees", (double) empCount);

        response.setGrandTotals(monthlyTotals);

        return response;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public Map<String, Object> validatePayrollData(Long periodId) {
        List<PayrollRecord> records = getPayrollForPeriod(periodId);

        int errorCount = 0;
        int warningCount = 0;
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (PayrollRecord record : records) {
            if (record.getBasicSalary() == 0 && record.getTotalHours() > 0) {
                errors.add("Employee " + record.getEmployee().getFirstName() + " " +
                        record.getEmployee().getLastName() + " has zero basic salary but worked " +
                        record.getTotalHours() + " hours");
                errorCount++;
            }

            if (record.getNetSalary() < 0) {
                errors.add("Employee " + record.getEmployee().getFirstName() + " " +
                        record.getEmployee().getLastName() + " has negative net salary: " +
                        record.getNetSalary());
                errorCount++;
            }

            if (record.getOvertimeHours() > 40) {
                warnings.add("Employee " + record.getEmployee().getFirstName() + " " +
                        record.getEmployee().getLastName() + " has high overtime: " +
                        record.getOvertimeHours() + " hours");
                warningCount++;
            }

            if (record.getLeaveDays() > 0 && (record.getLeaveHours() == null || record.getLeaveHours() == 0)) {
                warnings.add("Employee " + record.getEmployee().getFirstName() + " " +
                        record.getEmployee().getLastName() + " has " + record.getLeaveDays() +
                        " leave days but no leave hours calculated");
                warningCount++;
            }
        }

        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("periodId", periodId);
        validationResult.put("totalRecords", records.size());
        validationResult.put("errorCount", errorCount);
        validationResult.put("warningCount", warningCount);
        validationResult.put("errors", errors);
        validationResult.put("warnings", warnings);
        validationResult.put("isValid", errorCount == 0);

        return validationResult;
    }

    @Transactional
    public void generatePayroll(GeneratePayrollRequest request) {
        GeneratePayrollWithCreditsRequest newRequest = new GeneratePayrollWithCreditsRequest();
        newRequest.setPeriodId(request.getPeriodId());
        newRequest.setEmployeeId(request.getEmployeeId());
        newRequest.setExcludedEmployeeIds(request.getExcludedEmployeeIds());
        newRequest.setRateChoice(request.getRateChoice());
        newRequest.setSelectedCreditIds(new ArrayList<>());
        newRequest.setTemporaryAllowances(request.getTemporaryAllowances());
        newRequest.setSettings(request.getSettings());
        newRequest.setIncludeBasicSalary(request.getIncludeBasicSalary());
        newRequest.setDirectCreditAmounts(request.getCreditAmounts());

        generatePayrollWithSelectedCredits(newRequest);
    }

    // ==================== PAYROLL PERIOD MANAGEMENT ====================

    /**
     * Get a single payroll period by ID
     */
    public PayrollPeriod getPayrollPeriodById(Long id) {
        return payrollPeriodRepository.findById(id).orElse(null);
    }

    /**
     * Update an existing payroll period
     */
    @Transactional
    public PayrollPeriod updatePayrollPeriod(Long id, PayrollPeriod periodDetails) {
        PayrollPeriod existingPeriod = payrollPeriodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found with id: " + id));

        if (!payrollRecordRepository.findByPeriodId(id).isEmpty()) throw new IllegalStateException("Clear unprocessed payroll records before editing the period");
        if (existingPeriod.isProjectPayroll()) {
            LocalDate start = periodDetails.getStartDate() == null ? existingPeriod.getStartDate() : periodDetails.getStartDate();
            LocalDate end = periodDetails.getEndDate() == null ? existingPeriod.getEndDate() : periodDetails.getEndDate();
            if (start.isAfter(end)) throw new IllegalArgumentException("Start date cannot be after end date");
            if (payrollPeriodRepository.findOverlappingProjectPeriods(start,end,existingPeriod.getProjectId()).stream().anyMatch(p -> !id.equals(p.getId())))
                throw new IllegalArgumentException("Project payroll periods cannot overlap");
            existingPeriod.setStartDate(start); existingPeriod.setEndDate(end);
            if (periodDetails.getName() != null && !periodDetails.getName().isBlank()) existingPeriod.setName(periodDetails.getName());
            existingPeriod.setConvertExcessToOvertime(periodDetails.isConvertExcessToOvertime());
            return payrollPeriodRepository.save(existingPeriod);
        }
        if (periodDetails.getName() != null && !periodDetails.getName().trim().isEmpty()) {
            existingPeriod.setName(periodDetails.getName());
        }

        if (periodDetails.getStartDate() != null) {
            existingPeriod.setStartDate(periodDetails.getStartDate());
        }

        if (periodDetails.getEndDate() != null) {
            existingPeriod.setEndDate(periodDetails.getEndDate());
        }

        if (periodDetails.getCategory() != null && !periodDetails.getCategory().trim().isEmpty()) {
            existingPeriod.setCategory(periodDetails.getCategory());
        }

        existingPeriod.setConvertExcessToOvertime(periodDetails.isConvertExcessToOvertime());

        if (existingPeriod.getStartDate().isAfter(existingPeriod.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        List<PayrollPeriod> overlappingPeriods = payrollPeriodRepository
                .findOverlappingPeriodsForCategory(existingPeriod.getStartDate(),
                        existingPeriod.getEndDate(),
                        existingPeriod.getCategory());

        for (PayrollPeriod overlap : overlappingPeriods) {
            if (!overlap.getId().equals(id)) {
                throw new IllegalArgumentException("Period overlaps with existing period: " + overlap.getName());
            }
        }

        logger.info("Updated payroll period ID: {} - Name: {}, Category: {}",
                id, existingPeriod.getName(), existingPeriod.getCategory());

        return payrollPeriodRepository.save(existingPeriod);
    }

    @Transactional
    public int deletePayrollRecordsByPeriodId(Long periodId) {
        payrollPeriodRepository.lockForPayrollGeneration();
        if(payrollRecordRepository.findByPeriodId(periodId).stream().anyMatch(r->"Processed".equals(r.getStatus())))
            throw new IllegalStateException("Processed payroll cannot be cleared because its paid work dates must remain protected");
        PayrollPeriod period = payrollPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found with id: " + periodId));

        logger.info("Starting full reset for payroll period: {} (ID: {})", period.getName(), periodId);

        // 1. Revert loan deductions (restore balances, delete history)
        logger.info("Reverting loan deductions for period: {}", periodId);
        loanDeductionService.revertLoanDeductionsForPeriod(periodId);

        // 2. Revert applied pending credits (back to PENDING status)
        logger.info("Reverting applied pending credits for period: {}", periodId);
        int creditsReverted = pendingCreditRepository.revertAppliedCreditsByPeriod(periodId);
        if (creditsReverted > 0) {
            logger.info("Reverted {} pending credit(s) for period {}", creditsReverted, periodId);
        } else {
            logger.info("No applied pending credits found for period {}", periodId);
        }

        // 3. Delete payroll records
        int recordCount = payrollRecordRepository.countByPeriodId(periodId);
        if (recordCount > 0) {
            payrollRecordRepository.deleteByPeriodId(periodId);
            logger.info("Deleted {} payroll record(s) for period: {} (ID: {})",
                    recordCount, period.getName(), periodId);
        } else {
            logger.info("No payroll records to delete for period: {} (ID: {})",
                    period.getName(), periodId);
        }

        logger.info("Full reset completed for period: {} (ID: {}). Deleted {} records, reverted {} credits.",
                period.getName(), periodId, recordCount, creditsReverted);

        return recordCount;
    }

    // In PayrollService.java

    public List<LoanPreviewDTO> getActiveLoansForPeriod(Long periodId) {
        PayrollPeriod period = payrollPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + periodId));

        // Get all employee IDs that appear in attendance/overtime/leave for this period
        Set<Long> employeeIds = new HashSet<>();
        List<Object[]> attendanceData = attendanceRepository.findEmployeeIdsInPeriod(period.getStartDate(), period.getEndDate());
        attendanceData.forEach(row -> employeeIds.add(((Number) row[0]).longValue()));
        if(period.isProjectPayroll()) { employeeIds.clear(); employeeIds.addAll(payrollWorkScopeService.prepare(period).workDates.keySet()); }

        if (employeeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<LoanPreviewDTO> result = new ArrayList<>();
        for (Long empId : employeeIds) {
            Employee employee = employeeRepository.findById(empId).orElse(null);
            if (employee == null) continue;

            // Fetch active loans for this employee (Approved, isActive = true, remaining > 0)
            List<LoanRequest> activeLoans = loanRequestRepository.findActiveLoansByEmployee(empId);
            if (activeLoans.isEmpty()) continue;

            LoanPreviewDTO dto = new LoanPreviewDTO();
            dto.setEmployeeId(empId);
            dto.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());

            List<LoanPreviewDTO.LoanDetail> details = new ArrayList<>();
            double totalMonthly = 0.0;
            double totalRemaining = 0.0;

            for (LoanRequest loan : activeLoans) {
                LoanPreviewDTO.LoanDetail detail = new LoanPreviewDTO.LoanDetail();
                detail.setLoanId(loan.getId());
                double monthly = loan.getApprovedMonthlyPayment() != null ? loan.getApprovedMonthlyPayment() : loan.getMonthlyPaymentAmount();
                detail.setMonthlyPayment(monthly);
                double remaining = loan.calculateRemainingBalance();
                detail.setRemainingBalance(remaining);
                detail.setConsolidated(Boolean.TRUE.equals(loan.getIsAggregated()));
                detail.setOriginalAmount(loan.getApprovedAmount() != null ? loan.getApprovedAmount() : loan.getLoanAmount());
                detail.setPaidToDate(loan.getPaidAmount() != null ? loan.getPaidAmount() : 0.0);
                details.add(detail);

                totalMonthly += monthly;
                totalRemaining += remaining;
            }
            dto.setLoans(details);
            dto.setTotalMonthlyDeduction(totalMonthly);
            dto.setTotalRemainingBalance(totalRemaining);
            result.add(dto);
        }
        return result;
    }


    public static class MultiplierAdjustedHours {
        private double totalHours;
        private int workingDays;
        private double holidayHours;
        private double weekendHours;
        private int holidayDays;
        private int weekendDays;
        private double multiplierBonus;

        public MultiplierAdjustedHours(double totalHours, int workingDays, double holidayHours,
                                       double weekendHours, int holidayDays, int weekendDays,
                                       double multiplierBonus) {
            this.totalHours = totalHours;
            this.workingDays = workingDays;
            this.holidayHours = holidayHours;
            this.weekendHours = weekendHours;
            this.holidayDays = holidayDays;
            this.weekendDays = weekendDays;
            this.multiplierBonus = multiplierBonus;
        }

        public double getTotalHours() { return totalHours; }
        public int getWorkingDays() { return workingDays; }
        public double getHolidayHours() { return holidayHours; }
        public double getWeekendHours() { return weekendHours; }
        public int getHolidayDays() { return holidayDays; }
        public int getWeekendDays() { return weekendDays; }
        public double getMultiplierBonus() { return multiplierBonus; }
    }


}
