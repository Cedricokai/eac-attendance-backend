package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.PayrollPeriod;
import com.example.eac_backend_project.entity.attendance.PayrollRecord;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.OvertimeRepository;
import com.example.eac_backend_project.repository.PayrollPeriodRepository;
import com.example.eac_backend_project.repository.PayrollRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private static final Logger logger = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollPeriodRepository payrollPeriodRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final OvertimeRepository overtimeRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final GhanaPayrollCalculator ghanaPayrollCalculator;

    public PayrollService(PayrollPeriodRepository payrollPeriodRepository,
                          PayrollRecordRepository payrollRecordRepository,
                          AttendanceRepository attendanceRepository,
                          EmployeeRepository employeeRepository,
                          GhanaPayrollCalculator ghanaPayrollCalculator,
                          OvertimeRepository overtimeRepository) {
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.payrollRecordRepository = payrollRecordRepository;
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.ghanaPayrollCalculator = ghanaPayrollCalculator;
        this.overtimeRepository = overtimeRepository;
    }

    /**
     * Generate payroll for the given period.
     */
    @Transactional
    public void generatePayroll(Long periodId) {
        logger.info("Generating payroll for period ID: {}", periodId);

        PayrollPeriod period = payrollPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found for id: " + periodId));

        // Prevent regenerating processed payroll
        List<PayrollRecord> existingRecords = payrollRecordRepository.findByPeriodId(periodId);
        if (!existingRecords.isEmpty() && existingRecords.stream().anyMatch(r -> "Processed".equals(r.getStatus()))) {
            throw new IllegalStateException("Cannot generate payroll for already processed period id: " + periodId);
        }

        // Clear existing pending records for the period
        payrollRecordRepository.deleteByPeriodId(periodId);

        // Fetch attendance and overtime summaries
        List<Object[]> attendanceSummaries = attendanceRepository.summarizeAttendanceBetweenDates(
                period.getStartDate(), period.getEndDate());

        List<Object[]> overtimeSummaries = overtimeRepository.summarizeOvertimeBetweenDates(
                period.getStartDate(), period.getEndDate());

        logger.info("Attendance summaries found: {}", attendanceSummaries == null ? 0 : attendanceSummaries.size());
        logger.info("Overtime summaries found: {}", overtimeSummaries == null ? 0 : overtimeSummaries.size());

        // Build set of all employee IDs present
        Set<Long> employeeIds = new HashSet<>();
        if (attendanceSummaries != null) {
            for (Object[] row : attendanceSummaries) {
                if (row != null && row.length > 0 && row[0] != null) {
                    employeeIds.add(((Number) row[0]).longValue());
                }
            }
        }
        if (overtimeSummaries != null) {
            for (Object[] row : overtimeSummaries) {
                if (row != null && row.length > 0 && row[0] != null) {
                    employeeIds.add(((Number) row[0]).longValue());
                }
            }
        }

        if (employeeIds.isEmpty()) {
            throw new IllegalStateException("No attendance or overtime records found for the selected period: "
                    + period.getStartDate() + " to " + period.getEndDate());
        }

        // Build maps for quick lookups
        Map<Long, Object[]> attendanceMap = (attendanceSummaries == null) ? Collections.emptyMap()
                : attendanceSummaries.stream()
                .filter(Objects::nonNull)
                .filter(row -> row.length >= 3 && row[0] != null)
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> row,
                        (existing, replacement) -> existing
                ));

        Map<Long, Double> overtimeMap = (overtimeSummaries == null) ? Collections.emptyMap()
                : overtimeSummaries.stream()
                .filter(Objects::nonNull)
                .filter(row -> row.length >= 2 && row[0] != null && row[1] != null)
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).doubleValue(),
                        (existing, replacement) -> existing
                ));

        int processedCount = 0;
        for (Long employeeId : employeeIds) {
            try {
                int workingDays = 0;
                double totalHours = 0.0;
                double overtimeHours = 0.0;

                if (attendanceMap.containsKey(employeeId)) {
                    Object[] aRow = attendanceMap.get(employeeId);
                    try {
                        workingDays = aRow[1] == null ? 0 : ((Number) aRow[1]).intValue();
                    } catch (Exception e) {
                        logger.warn("Invalid workingDays for employee {} in attendance row: {}", employeeId, Arrays.toString(aRow));
                    }
                    try {
                        totalHours = aRow[2] == null ? 0.0 : ((Number) aRow[2]).doubleValue();
                    } catch (Exception e) {
                        logger.warn("Invalid totalHours for employee {} in attendance row: {}", employeeId, Arrays.toString(aRow));
                    }
                }

                overtimeHours = overtimeMap.getOrDefault(employeeId, 0.0);

                Employee employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

                if (employee.getMinimumRate() == null || employee.getMinimumRate() <= 0) {
                    throw new IllegalArgumentException("Invalid minimum rate for employee " + employeeId + ": " + employee.getMinimumRate());
                }

                PayrollRecord record = new PayrollRecord();
                record.setEmployee(employee);
                record.setPeriod(period);
                record.setStatus("Pending");
                record.setWorkingDays(workingDays);
                record.setTotalHours(totalHours);
                record.setOvertimeHours(overtimeHours);

                // === Set allowances from Employee (or from any source you store them) ===
                record.setRentAllowance(employee.getRentAllowance() != null ? employee.getRentAllowance() : 0.0);
                record.setTransportAllowance(employee.getTransportAllowance() != null ? employee.getTransportAllowance() : 0.0);
                record.setClothingAllowance(employee.getClothingAllowance() != null ? employee.getClothingAllowance() : 0.0);
                record.setOtherAllowance(employee.getOtherAllowance() != null ? employee.getOtherAllowance() : 0.0);

// At this point, totalAllowances, taxableAllowances, and nonTaxableAllowances are automatically recalculated


                // Debug before calculation
                logger.debug("Employee {} -> WorkingDays: {}, TotalHours: {}, OvertimeHours: {}, MinRate: {}",
                        employeeId, workingDays, totalHours, overtimeHours, employee.getMinimumRate());

                // Calculate payroll
                ghanaPayrollCalculator.calculatePayroll(employee, totalHours, workingDays, overtimeHours, record);

                // Debug after calculation
                logger.debug("Employee {} Payroll -> Basic: {}, Gross: {}, SSNIT (5.5%): {}, PAYE: {}, Net: {}",
                        employeeId,
                        record.getBasicSalary(),
                        record.getGrossSalary(),
                        record.getSsnitEmployee(),
                        record.getPayeTax(),
                        record.getNetSalary());

                payrollRecordRepository.save(record);
                processedCount++;
            } catch (Exception e) {
                logger.error("Failed to process payroll for employee id {}: {}", employeeId, e.getMessage(), e);
            }
        }

        logger.info("Payroll generation completed. Processed {} employees for period ID: {}", processedCount, periodId);
    }

    public List<PayrollPeriod> getAllPeriods() {
        return payrollPeriodRepository.findAllByOrderByStartDateDesc();
    }

    public PayrollPeriod createPeriod(PayrollPeriod period) {
        if (period.getStartDate().isAfter(period.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        List<PayrollPeriod> overlappingPeriods = payrollPeriodRepository.findOverlappingPeriods(
                period.getStartDate(), period.getEndDate());
        if (!overlappingPeriods.isEmpty()) {
            throw new IllegalArgumentException("Payroll period overlaps with existing period");
        }
        return payrollPeriodRepository.save(period);
    }

    public List<PayrollRecord> getPayrollForPeriod(Long periodId) {
        List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(periodId);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("No payroll records found for period ID: " + periodId);
        }
        return records;
    }

    @Transactional
    public void processPayroll(Long periodId) {
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

        return Map.of(
                "totalNetAmount", Math.round(totalNetAmount * 100.0) / 100.0,
                "totalTax", Math.round(totalTax * 100.0) / 100.0,
                "totalSsnit", Math.round(totalSsnit * 100.0) / 100.0,
                "totalGross", Math.round(totalGross * 100.0) / 100.0,
                "employeeCount", records.size(),
                "periodName", latestPeriod.getName(),
                "periodId", latestPeriod.getId(),
                "startDate", latestPeriod.getStartDate().toString(),
                "endDate", latestPeriod.getEndDate().toString(),
                "periodDates", latestPeriod.getStartDate() + " to " + latestPeriod.getEndDate()
        );
    }

    public PayrollRecord getEmployeePayslip(Long periodId, Long employeeId) {
        return payrollRecordRepository.findByPeriodIdAndEmployeeId(periodId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payslip found for employee " + employeeId + " in period " + periodId));
    }

    public byte[] generatePayslipPDF(Long periodId, Long employeeId) {
        // Implementation for PDF generation using libraries like Apache PDFBox or iText
        // This would create a formatted PDF version of the payslip
        // For now, return empty array - you'll need to implement PDF generation
        return new byte[0];
    }
}
