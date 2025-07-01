package com.eacattendance.Service;

import com.eacattendance.Exceptions.ResourceNotFoundException;
import com.eacattendance.entity.Employee;
import com.eacattendance.entity.PayrollPeriod;
import com.eacattendance.entity.PayrollRecord;
import com.eacattendance.repository.AttendanceRepository;
import com.eacattendance.repository.EmployeeRepository;
import com.eacattendance.repository.PayrollPeriodRepository;
import com.eacattendance.repository.PayrollRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class PayrollService {

    private final PayrollPeriodRepository payrollPeriodRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public PayrollService(PayrollPeriodRepository payrollPeriodRepository,
                          PayrollRecordRepository payrollRecordRepository,
                          AttendanceRepository attendanceRepository,
                          EmployeeRepository employeeRepository) {
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.payrollRecordRepository = payrollRecordRepository;
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<PayrollPeriod> getAllPeriods() {
        return payrollPeriodRepository.findAll();
    }

    public PayrollPeriod createPeriod(PayrollPeriod period) {
        return payrollPeriodRepository.save(period);
    }

    public List<PayrollRecord> getPayrollForPeriod(Long periodId) {
        return payrollRecordRepository.findByPeriodId(periodId);
    }

    public void generatePayroll(Long periodId) {
        PayrollPeriod period = payrollPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found"));

        // 🧹 Clear existing records for the period
        payrollRecordRepository.deleteByPeriodId(periodId);

        List<Object[]> summaries = attendanceRepository.summarizeAttendanceBetweenDates(
                period.getStartDate(), period.getEndDate());

        for (Object[] row : summaries) {
            Long employeeId = (Long) row[0];
            int workingDays = ((Number) row[1]).intValue();
            double totalHours = ((Number) row[2]).doubleValue();

            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

            PayrollRecord record = new PayrollRecord();
            record.setEmployee(employee);
            record.setPeriod(period);
            record.setWorkingDays(workingDays);
            record.setTotalHours(totalHours);
            record.setStatus("Generated");

            payrollRecordRepository.save(record);
        }
    }

    public void processPayroll(Long periodId) {
        List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(periodId);
        for (PayrollRecord r : records) {
            r.setStatus("Processed");
        }
        payrollRecordRepository.saveAll(records);
    }

    public Map<String, Object> getPayrollSummary() {
        PayrollPeriod latestPeriod = payrollPeriodRepository.findTopByOrderByEndDateDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No payroll periods found"));

        List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(latestPeriod.getId());

        double totalAmount = records.stream()
                .mapToDouble(r -> r.getRegularPay() + r.getOvertimePay())
                .sum();

        return Map.of(
                "totalAmount", totalAmount,
                "employeeCount", records.size(),
                "periodName", latestPeriod.getName(),
                "periodDates", latestPeriod.getStartDate() + " to " + latestPeriod.getEndDate()
        );
    }
}
