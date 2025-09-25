package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.PayrollRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class GhanaPayrollCalculator {

    private static final Logger logger = LoggerFactory.getLogger(GhanaPayrollCalculator.class);

    // PAYE Tax Bands (2025 monthly)
    private static final double TIER_1_LIMIT = 490.0;
    private static final double TIER_2_LIMIT = 600.0;
    private static final double TIER_3_LIMIT = 730.0;
    private static final double TIER_4_LIMIT = 3730.0;
    private static final double TIER_5_LIMIT = 19917.0;
    private static final double TIER_6_LIMIT = Double.MAX_VALUE;

    private static final double TIER_1_RATE = 0.0;
    private static final double TIER_2_RATE = 0.05;
    private static final double TIER_3_RATE = 0.10;
    private static final double TIER_4_RATE = 0.175;
    private static final double TIER_5_RATE = 0.25;
    private static final double TIER_6_RATE = 0.30;

    // SSNIT rates
    private static final double EMPLOYEE_SSNIT_RATE = 0.055;
    private static final double EMPLOYER_SSNIT_RATE = 0.13;
    private static final double SSNIT_CAP = 3500.0;

    public void calculatePayroll(Employee employee,
                                 double totalHours,
                                 int workingDays,
                                 double overtimeHours,
                                 PayrollRecord record) {

        if (employee == null || record == null) {
            logger.warn("Cannot calculate payroll: Employee or PayrollRecord is null");
            return;
        }

        // ===== 1. Calculate salary components =====
        double basicSalary = calculateBasicSalary(employee, totalHours, workingDays);
        double overtimePay = calculateOvertimePay(employee, overtimeHours);
        double allowances = record.getTotalAllowances(); // other allowances

        // Add overtime to allowances for total taxable benefits
        double totalAllowances = allowances + overtimePay;

        double grossSalary = basicSalary + totalAllowances;

        // ===== 2. SSNIT on basic salary only =====
        double ssnitEmployee = calculateEmployeeSSNIT(basicSalary);
        double ssnitEmployer = calculateEmployerSSNIT(basicSalary);

        // ===== 3. Taxable income (basic + overtime + allowances - SSNIT employee) =====
        double chargeableIncome = grossSalary - ssnitEmployee;
        double payeTax = calculatePAYETax(chargeableIncome);

        double netSalary = grossSalary - ssnitEmployee - payeTax;

        // ===== 4. Save all values to PayrollRecord =====
        record.setBasicSalary(round(basicSalary));
        record.setOvertimePay(round(overtimePay));
        record.setGrossSalary(round(grossSalary));
        record.setSsnitEmployee(round(ssnitEmployee));
        record.setSsnitEmployer(round(ssnitEmployer));
        record.setPayeTax(round(payeTax));
        record.setNetSalary(round(netSalary));
        record.setTaxableIncome(round(chargeableIncome));
        record.setTotalAllowances(round(totalAllowances)); // updated total allowances
        record.setTotalHours(totalHours);
        record.setOvertimeHours(overtimeHours);
        record.setWorkingDays(workingDays);

        logger.info("Payroll calculated for Employee ID {} | Basic: {} | Overtime: {} | Allowances: {} | Gross: {} | SSNIT(E): {} | PAYE: {} | Net: {}",
                employee.getId(), basicSalary, overtimePay, totalAllowances, grossSalary, ssnitEmployee, payeTax, netSalary);
    }


    private double calculateBasicSalary(Employee employee, double totalHours, int workingDays) {
        double hourlyRate = employee.getMinimumRate() != null ? employee.getMinimumRate() : 0.0;
        double regularHours = Math.min(totalHours, 8 * workingDays);
        return regularHours * hourlyRate;
    }

    private double calculateOvertimePay(Employee employee, double overtimeHours) {
        double hourlyRate = employee.getMinimumRate() != null ? employee.getMinimumRate() : 0.0;
        return overtimeHours * hourlyRate * 1.5;
    }

    private double calculateEmployeeSSNIT(double grossSalary) {
        return Math.min(grossSalary, SSNIT_CAP) * EMPLOYEE_SSNIT_RATE;
    }

    private double calculateEmployerSSNIT(double grossSalary) {
        return Math.min(grossSalary, SSNIT_CAP) * EMPLOYER_SSNIT_RATE;
    }

    private double calculatePAYETax(double chargeableIncome) {
        double tax = 0.0;

        if (chargeableIncome <= TIER_1_LIMIT) {
            tax = 0;
        } else if (chargeableIncome <= TIER_2_LIMIT) {
            tax = (chargeableIncome - TIER_1_LIMIT) * TIER_2_RATE;
        } else if (chargeableIncome <= TIER_3_LIMIT) {
            tax = (TIER_2_LIMIT - TIER_1_LIMIT) * TIER_2_RATE
                    + (chargeableIncome - TIER_2_LIMIT) * TIER_3_RATE;
        } else if (chargeableIncome <= TIER_4_LIMIT) {
            tax = (TIER_2_LIMIT - TIER_1_LIMIT) * TIER_2_RATE
                    + (TIER_3_LIMIT - TIER_2_LIMIT) * TIER_3_RATE
                    + (chargeableIncome - TIER_3_LIMIT) * TIER_4_RATE;
        } else if (chargeableIncome <= TIER_5_LIMIT) {
            tax = (TIER_2_LIMIT - TIER_1_LIMIT) * TIER_2_RATE
                    + (TIER_3_LIMIT - TIER_2_LIMIT) * TIER_3_RATE
                    + (TIER_4_LIMIT - TIER_3_LIMIT) * TIER_4_RATE
                    + (chargeableIncome - TIER_4_LIMIT) * TIER_5_RATE;
        } else {
            tax = (TIER_2_LIMIT - TIER_1_LIMIT) * TIER_2_RATE
                    + (TIER_3_LIMIT - TIER_2_LIMIT) * TIER_3_RATE
                    + (TIER_4_LIMIT - TIER_3_LIMIT) * TIER_4_RATE
                    + (TIER_5_LIMIT - TIER_4_LIMIT) * TIER_5_RATE
                    + (chargeableIncome - TIER_5_LIMIT) * TIER_6_RATE;
        }

        return round(tax);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}