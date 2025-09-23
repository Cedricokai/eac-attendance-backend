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

    // Ghana PAYE Tax Bands (2025 monthly)
    private static final double TIER_1_LIMIT = 490.0;      // 0–490 → 0%
    private static final double TIER_2_LIMIT = 600.0;      // 491–600 → 5% (110)
    private static final double TIER_3_LIMIT = 730.0;      // 601–730 → 10% (130)
    private static final double TIER_4_LIMIT = 3730.0;     // 731–3730 → 17.5% (3000)
    private static final double TIER_5_LIMIT = 19917.0;    // 3731–19917 → 25% (16187)
    private static final double TIER_6_LIMIT = Double.MAX_VALUE; // Above 19917 → 30%

    private static final double TIER_1_RATE = 0.0;
    private static final double TIER_2_RATE = 0.05;
    private static final double TIER_3_RATE = 0.10;
    private static final double TIER_4_RATE = 0.175;
    private static final double TIER_5_RATE = 0.25;
    private static final double TIER_6_RATE = 0.30;

    // SSNIT rates
    private static final double EMPLOYEE_SSNIT_RATE = 0.055; // 5.5%
    private static final double EMPLOYER_SSNIT_RATE = 0.13;  // 13%
    private static final double SSNIT_CAP = 3500.0;          // Max contributory salary for SSNIT

    public void calculatePayroll(Employee employee,
                                 double totalHours,
                                 int workingDays,
                                 double overtimeHours,
                                 PayrollRecord record) {

        // 1. Calculate basic + overtime + gross
        double basicSalary = calculateBasicSalary(employee, totalHours, workingDays);
        double overtimePay = calculateOvertimePay(employee, overtimeHours);
        double grossSalary = calculateGrossSalary(employee, basicSalary, overtimePay);

        // 2. Deductions
        double ssnitEmployee = calculateEmployeeSSNIT(grossSalary); // SSNIT based on gross salary (capped)
        double chargeableIncome = grossSalary - ssnitEmployee;
        double payeTax = calculatePAYETax(chargeableIncome);

        // 3. Net
        double netSalary = grossSalary - ssnitEmployee - payeTax;

        // 4. Employer reporting
        double ssnitEmployer = calculateEmployerSSNIT(grossSalary);

        // 5. Save all values
        record.setTaxableIncome(round(chargeableIncome));
        record.setBasicSalary(round(basicSalary));
        record.setOvertimePay(round(overtimePay));
        record.setGrossSalary(round(grossSalary));
        record.setSsnitEmployee(round(ssnitEmployee));
        record.setSsnitEmployer(round(ssnitEmployer));
        record.setPayeTax(round(payeTax));
        record.setNetSalary(round(netSalary));
        record.setTotalHours(totalHours);
        record.setOvertimeHours(overtimeHours);
        record.setWorkingDays(workingDays);

        logger.info("Payroll calculated for Employee ID {} => Basic: {}, Overtime: {}, Gross: {}, SSNIT(5.5%): {}, PAYE: {}, Net: {}",
                employee.getId(), basicSalary, overtimePay, grossSalary, ssnitEmployee, payeTax, netSalary);
    }

    private double calculateBasicSalary(Employee employee, double totalHours, int workingDays) {
        double hourlyRate = employee.getMinimumRate();
        double regularHours = Math.min(totalHours, 8 * workingDays);
        return regularHours * hourlyRate;
    }

    private double calculateOvertimePay(Employee employee, double overtimeHours) {
        double hourlyRate = employee.getMinimumRate();
        double overtimeRate = hourlyRate * 1.5;
        return overtimeHours * overtimeRate;
    }

    private double calculateGrossSalary(Employee employee, double basicSalary, double overtimePay) {
        double allowances = employee.getAllowances() != null ? employee.getAllowances() : 0.0;
        return basicSalary + overtimePay + allowances;
    }

    private double calculateEmployeeSSNIT(double grossSalary) {
        double ssnitBase = Math.min(grossSalary, SSNIT_CAP);
        double ssnit = ssnitBase * EMPLOYEE_SSNIT_RATE;

        logger.debug("SSNIT Calculation -> Gross: {}, SSNIT Base: {}, Rate: 5.5%, Deduction: {}",
                grossSalary, ssnitBase, ssnit);

        return ssnit;
    }

    private double calculateEmployerSSNIT(double grossSalary) {
        double ssnitBase = Math.min(grossSalary, SSNIT_CAP);
        return ssnitBase * EMPLOYER_SSNIT_RATE;
    }

    private double calculatePAYETax(double chargeableIncome) {
        double tax = 0.0;

        // Tier 1: 0 – 490 (0%)
        if (chargeableIncome <= TIER_1_LIMIT) {
            return 0.0;
        }

        // Tier 2: 491 – 600 (5%)
        if (chargeableIncome > TIER_1_LIMIT) {
            double amountInTier = Math.min(chargeableIncome, TIER_2_LIMIT) - TIER_1_LIMIT;
            tax += amountInTier * TIER_2_RATE;
        }

        // Tier 3: 601 – 730 (10%)
        if (chargeableIncome > TIER_2_LIMIT) {
            double amountInTier = Math.min(chargeableIncome, TIER_3_LIMIT) - TIER_2_LIMIT;
            tax += amountInTier * TIER_3_RATE;
        }

        // Tier 4: 731 – 3730 (17.5%)
        if (chargeableIncome > TIER_3_LIMIT) {
            double amountInTier = Math.min(chargeableIncome, TIER_4_LIMIT) - TIER_3_LIMIT;
            tax += amountInTier * TIER_4_RATE;
        }

        // Tier 5: 3731 – 19917 (25%)
        if (chargeableIncome > TIER_4_LIMIT) {
            double amountInTier = Math.min(chargeableIncome, TIER_5_LIMIT) - TIER_4_LIMIT;
            tax += amountInTier * TIER_5_RATE;
        }

        // Tier 6: Above 19917 (30%)
        if (chargeableIncome > TIER_5_LIMIT) {
            double amountInTier = chargeableIncome - TIER_5_LIMIT;
            tax += amountInTier * TIER_6_RATE;
        }

        return round(tax);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
