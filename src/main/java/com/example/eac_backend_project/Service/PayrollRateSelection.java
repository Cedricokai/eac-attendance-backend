package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.PayrollRecord;
import org.springframework.beans.BeanUtils;
import java.util.List;

/** Payroll rate snapshots never modify the employee's current profile. */
public final class PayrollRateSelection {
    private PayrollRateSelection() {}
    public static void validate(String choice, List<PayrollRecord> existing) {
        if (existing.stream().anyMatch(row -> "Processed".equals(row.getStatus())))
            throw new IllegalStateException("Cannot generate payroll for an already processed period");
        if (choice != null && !choice.equals("SAVED") && !choice.equals("CURRENT"))
            throw new IllegalArgumentException("Choose SAVED or CURRENT payroll rates");
        if (!existing.isEmpty() && choice == null)
            throw new IllegalArgumentException("Choose whether to keep saved rates or use updated rates before regenerating payroll");
        if ("SAVED".equals(choice) && existing.stream().anyMatch(row -> !Boolean.TRUE.equals(row.getRateSnapshotSaved())))
            throw new IllegalArgumentException("Some previous payroll rates were not saved. Choose updated rates to establish a rate snapshot.");
    }
    public static Employee employeeForCalculation(Employee current, PayrollRecord previous, String choice) {
        Employee copy = new Employee();
        BeanUtils.copyProperties(current, copy);
        if ("SAVED".equals(choice) && previous != null) {
            if (!Boolean.TRUE.equals(previous.getRateSnapshotSaved())) throw new IllegalArgumentException("Saved payroll rates are unavailable");
            copy.setMinimumRate(previous.getAppliedMinimumRate());
            copy.setHourlyRate(previous.getAppliedHourlyRate());
            copy.setBasicSalary(previous.getAppliedMonthlySalary());
        }
        return copy;
    }
    public static double effectiveRate(Employee employee) {
        if (employee.getMinimumRate() != null && employee.getMinimumRate() > 0) return employee.getMinimumRate();
        if (employee.getHourlyRate() != null && employee.getHourlyRate() > 0) return employee.getHourlyRate();
        return 0.0;
    }
    public static void capture(PayrollRecord record, Employee employee, double overtimeRate) {
        record.setRateSnapshotSaved(true);
        record.setAppliedMinimumRate(employee.getMinimumRate());
        record.setAppliedHourlyRate(employee.getHourlyRate());
        record.setAppliedEmployeeRate(effectiveRate(employee));
        record.setAppliedMonthlySalary(employee.getBasicSalary());
        record.setAppliedOvertimeRate(overtimeRate);
    }
}
