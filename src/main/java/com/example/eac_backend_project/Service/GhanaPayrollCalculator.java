package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.SettingsDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.PayrollRecord;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
public class GhanaPayrollCalculator {

    private static final Logger logger = LoggerFactory.getLogger(GhanaPayrollCalculator.class);

    // cumulative thresholds based on the table you posted
    private static final double TIER_1_LIMIT = 490.0;        // 0%
    private static final double TIER_2_LIMIT = 600.0;        // 5%
    private static final double TIER_3_LIMIT = 730.0;        // 10%
    private static final double TIER_4_LIMIT = 3896.67;      // 17.5%
    private static final double TIER_5_LIMIT = 19896.67;     // 25%
    private static final double TIER_6_LIMIT = 50416.67;     // 30%

    private static final double TIER_1_RATE = 0.0;
    private static final double TIER_2_RATE = 0.05;
    private static final double TIER_3_RATE = 0.10;
    private static final double TIER_4_RATE = 0.175;
    private static final double TIER_5_RATE = 0.25;
    private static final double TIER_6_RATE = 0.30;
    private static final double TIER_7_RATE = 0.35;

    private static final double EMPLOYEE_SSNIT_RATE = 0.055;
    private static final double EMPLOYER_SSNIT_RATE = 0.13;

    public void calculatePayrollWithLeave(Employee employee,
                                          LocalDate periodStart,
                                          LocalDate periodEnd,
                                          double normalAttendanceHours,
                                          int actualWorkingDays,
                                          double overtimeHours,
                                          int leaveDays,
                                          double totalLeaveHours,
                                          PayrollRecord record,
                                          SystemSettings sys,
                                          double preCalculatedOvertimePay,
                                          double creditAmount,
                                          Boolean excludeFromSsnit) {

        if (employee == null || record == null) {
            return;
        }

        int totalDaysInPeriod = calculateWorkingDaysInPeriod(periodStart, periodEnd, employee);
        int totalPaidDays = actualWorkingDays + leaveDays;
        double totalHours = normalAttendanceHours + totalLeaveHours;

        logger.info("Payroll calculation for Employee {}: Period: {} to {}, Total Days: {}, Actual Working Days: {}, Leave Days: {}, Total Paid Days: {}, Normal Hours: {}, Leave Hours: {}, Total Hours: {}",
                employee.getId(), periodStart, periodEnd, totalDaysInPeriod, actualWorkingDays,
                leaveDays, totalPaidDays, normalAttendanceHours, totalLeaveHours, totalHours);

        boolean hasHourlyRate = employee.getMinimumRate() != null && employee.getMinimumRate() > 0;
        boolean useNonTaxableAllowances = false;

        if (employee.getCategory() != null && employee.getCategory().getUseNonTaxableAllowances() != null) {
            useNonTaxableAllowances = employee.getCategory().getUseNonTaxableAllowances();
        }

        double basicSalary;
        double overtimePay;

        double ssnitEmployee = 0.0;
        double ssnitEmployer = 0.0;

        boolean isExcludedFromSsnit = (excludeFromSsnit != null && excludeFromSsnit);

        if (isExcludedFromSsnit) {
            logger.info("Employee {} is EXCLUDED from SSNIT - all SSNIT deductions will be ZERO",
                    employee.getEmployeeId() != null ? employee.getEmployeeId() : employee.getId());
        }

        double tier2Amount = 0.0;

        if (useNonTaxableAllowances) {
            if (hasHourlyRate) {
                basicSalary = calculateBasicSalaryFromTotalHours(employee, totalHours);
            } else {
                basicSalary = calculateBasicSalaryWithProration(employee, totalDaysInPeriod, totalPaidDays);
            }

            ssnitEmployee = 0.0;
            ssnitEmployer = 0.0;

            overtimePay = calculateBasicOvertimePay(employee, overtimeHours);

            logger.info("NSS MODE: Basic Salary = {} (EXEMPT from SSNIT and PAYE)", basicSalary);
        } else {
            if (hasHourlyRate) {
                basicSalary = calculateBasicSalaryFromTotalHours(employee, totalHours);
                overtimePay = calculateBasicOvertimePay(employee, overtimeHours);
            } else {
                basicSalary = record.getBasicSalary() != null ? record.getBasicSalary() : 0.0;
                overtimePay = 0.0;
            }

            if (isExcludedFromSsnit) {
                ssnitEmployee = 0.0;
                ssnitEmployer = 0.0;
                logger.info("REGULAR MODE: Employee {} is EXCLUDED from SSNIT - SSNIT deductions set to 0",
                        employee.getEmployeeId());
            } else {
                ssnitEmployee = calculateEmployeeSSNIT(basicSalary);
                ssnitEmployer = calculateEmployerSSNIT(basicSalary);
            }
        }

        tier2Amount = basicSalary * 0.05;
        logger.info("Tier 2 amount (store only) for employee {}: {}", employee.getId(), tier2Amount);

        double rentAllowance = record.getRentAllowance() != null ? record.getRentAllowance() : 0.0;
        double transportAllowance = record.getTransportAllowance() != null ? record.getTransportAllowance() : 0.0;
        double clothingAllowance = record.getClothingAllowance() != null ? record.getClothingAllowance() : 0.0;
        double otherAllowance = record.getOtherAllowance() != null ? record.getOtherAllowance() : 0.0;
        double nssAllowance = record.getNssAllowance() != null ? record.getNssAllowance() : 0.0;

        double totalAllowances = rentAllowance + transportAllowance + clothingAllowance + otherAllowance + nssAllowance + overtimePay;
        double grossSalary = basicSalary + totalAllowances;

        double chargeableIncome;
        double taxAmount;

        boolean useWithholdingTax = false;
        double withholdingTaxRate = 0.0;
        logger.info("=== WITHHOLDING TAX DEBUG ===");
        logger.info("sys object is: {}", sys);
        if (sys != null && Boolean.TRUE.equals(sys.getWithholdingTaxEnabled())) {
            if (employee.getApplyWithholdingTax() != null && employee.getApplyWithholdingTax()) {
                useWithholdingTax = true;
            } else if (employee.getCategory() != null &&
                    employee.getCategory().getApplyWithholdingTax() != null &&
                    employee.getCategory().getApplyWithholdingTax()) {
                useWithholdingTax = true;
            }

            if (useWithholdingTax) {
                withholdingTaxRate = sys.getWithholdingTaxRate() != null ? sys.getWithholdingTaxRate() : 15.0;
            }
        }

        if (useNonTaxableAllowances) {
            chargeableIncome = totalAllowances;

            if (useWithholdingTax) {
                taxAmount = calculateWithholdingTax(chargeableIncome, withholdingTaxRate);
                logger.info("NSS MODE WITHHOLDING TAX: Taxable Income = Allowances Only = {}, Rate {}%, Tax = {}",
                        chargeableIncome, withholdingTaxRate, taxAmount);
            } else {
                taxAmount = calculatePAYETax(chargeableIncome);
                logger.info("NSS MODE: Taxable Income = Allowances Only = {}, PAYE = {}",
                        chargeableIncome, taxAmount);
            }
        } else {
            chargeableIncome = grossSalary - ssnitEmployee;

            if (useWithholdingTax) {
                taxAmount = calculateWithholdingTax(chargeableIncome, withholdingTaxRate);
                logger.info("REGULAR MODE WITHHOLDING TAX: Taxable Income = Gross - SSNIT = {} - {} = {}, Rate {}%, Tax = {}",
                        grossSalary, ssnitEmployee, chargeableIncome, withholdingTaxRate, taxAmount);
            } else {
                taxAmount = calculatePAYETax(chargeableIncome);
            }
        }

        double netSalary;
        if (useNonTaxableAllowances) {
            netSalary = basicSalary + (totalAllowances - taxAmount);
            logger.info("NSS MODE: Net = {} (Basic) + ({} - {}) = {}",
                    basicSalary, totalAllowances, taxAmount, netSalary);
        } else {
            netSalary = grossSalary - ssnitEmployee - taxAmount;
        }

        double netSalaryWithCredit = applyCredits(netSalary, creditAmount);

        double employerCost = grossSalary + ssnitEmployer;

        record.setBasicSalary(round(basicSalary));
        record.setOvertimePay(round(overtimePay));
        record.setGrossSalary(round(grossSalary));
        record.setSsnitEmployee(round(ssnitEmployee));
        record.setSsnitEmployer(round(ssnitEmployer));
        record.setPayeTax(round(taxAmount));
        record.setNetSalary(round(netSalaryWithCredit));
        record.setCreditAmount(creditAmount);
        record.setTaxableIncome(round(chargeableIncome));
        record.setTotalAllowances(round(totalAllowances));
        record.setTotalHours(totalHours);
        record.setOvertimeHours(overtimeHours);
        record.setWorkingDays(actualWorkingDays);
        record.setLeaveDays(leaveDays);
        record.setTotalDaysInPeriod(totalDaysInPeriod);
        record.setTotalWorkingDays(totalPaidDays);
        record.setUseNonTaxableAllowances(useNonTaxableAllowances);
        record.setTier2Amount(round(tier2Amount));

        logger.info("FINAL Payroll - Employee ID {} | Mode: {} | Basic: {} | Allowances: {} | Taxable: {} | Tax: {} | Net: {} | SSNIT Exempt: {} | Withholding Tax: {} | Tier2: {}",
                employee.getId(),
                useNonTaxableAllowances ? "NSS (Basic Exempt)" : "Regular",
                basicSalary, totalAllowances, chargeableIncome, taxAmount, netSalary,
                isExcludedFromSsnit ? "YES" : "NO",
                useWithholdingTax ? "YES (" + withholdingTaxRate + "%)" : "NO",
                tier2Amount);
    }

    public void calculatePayrollWithMultipliers(Employee employee,
                                                LocalDate periodStart,
                                                LocalDate periodEnd,
                                                double normalAttendanceHours,
                                                int actualWorkingDays,
                                                double overtimeHours,
                                                int leaveDays,
                                                double totalLeaveHours,
                                                double holidayHours,
                                                double weekendHours,
                                                double multiplierBonus,
                                                SettingsDTO settings,
                                                boolean includeBasicSalary,
                                                PayrollRecord record,
                                                SystemSettings sys,
                                                double preCalculatedOvertimePay,
                                                double creditAmount,
                                                Boolean excludeFromSsnit) {

        if (employee == null || record == null) {
            logger.warn("Cannot calculate payroll: Employee or PayrollRecord is null");
            return;
        }

        int totalDaysInPeriod = calculateWorkingDaysInPeriod(periodStart, periodEnd, employee);
        int totalPaidDays = actualWorkingDays + leaveDays;
        double totalHours = normalAttendanceHours + totalLeaveHours;

        logger.info("Payroll calculation with multipliers for Employee {}:", employee.getId());
        logger.info("Period: {} to {}, Total Working Days in Period: {}", periodStart, periodEnd, totalDaysInPeriod);
        logger.info("Actual Working Days: {}, Leave Days: {}, Total Paid Days: {}",
                actualWorkingDays, leaveDays, totalPaidDays);
        logger.info("Normal Hours: {}, Leave Hours: {}, Total Hours: {}",
                normalAttendanceHours, totalLeaveHours, totalHours);
        logger.info("Holiday Hours: {}, Weekend Hours: {}, Multiplier Bonus: {}",
                holidayHours, weekendHours, multiplierBonus);
        logger.info("Overtime Hours: {}, Pre-calculated Overtime Pay: {}", overtimeHours, preCalculatedOvertimePay);
        logger.info("Include Basic Salary flag: {}", includeBasicSalary);

        boolean hasHourlyRate = employee.getMinimumRate() != null && employee.getMinimumRate() > 0;
        boolean useNonTaxableAllowances = false;

        if (employee.getCategory() != null && employee.getCategory().getUseNonTaxableAllowances() != null) {
            useNonTaxableAllowances = employee.getCategory().getUseNonTaxableAllowances();
        }

        logger.info("Non-Taxable Allowances Flag: {} for employee {}", useNonTaxableAllowances, employee.getId());

        double basicSalary = 0.0;
        double overtimePay = 0.0;
        double ssnitEmployee = 0.0;
        double ssnitEmployer = 0.0;

        boolean isExcludedFromSsnit = (excludeFromSsnit != null && excludeFromSsnit);

        if (isExcludedFromSsnit) {
            logger.info("Employee {} is EXCLUDED from SSNIT - all SSNIT deductions will be ZERO",
                    employee.getEmployeeId() != null ? employee.getEmployeeId() : employee.getId());
        }

        boolean useWithholdingTax = false;
        double withholdingTaxRate = 0.0;

        if (sys != null && Boolean.TRUE.equals(sys.getWithholdingTaxEnabled())) {
            if (employee.getApplyWithholdingTax() != null && employee.getApplyWithholdingTax()) {
                useWithholdingTax = true;
                logger.info("Employee {} has WITHHOLDING TAX enabled via individual override", employee.getEmployeeId());
            } else if (employee.getCategory() != null &&
                    employee.getCategory().getApplyWithholdingTax() != null &&
                    employee.getCategory().getApplyWithholdingTax()) {
                useWithholdingTax = true;
                logger.info("Employee {} has WITHHOLDING TAX enabled via category", employee.getEmployeeId());
            }

            if (useWithholdingTax) {
                withholdingTaxRate = sys.getWithholdingTaxRate() != null ? sys.getWithholdingTaxRate() : 15.0;
                logger.info("Withholding Tax Rate: {}%", withholdingTaxRate);
            }
        }

        double tier2Amount = 0.0;  // stored only, not used in calculations

        if (useNonTaxableAllowances) {
            double totalHoursWorked = normalAttendanceHours + overtimeHours;
            double hourlyRate = getEffectiveHourlyRate(employee);

            if (preCalculatedOvertimePay > 0) {
                overtimePay = preCalculatedOvertimePay;
                logger.info("NSS MODE: Using pre-calculated overtime pay: {}", overtimePay);
            } else {
                overtimePay = totalHoursWorked * hourlyRate;
                logger.info("NSS MODE: Calculated overtime pay from {} total hours × {} rate = {}",
                        totalHoursWorked, hourlyRate, overtimePay);
            }

            double nssAllowance = record.getNssAllowance() != null ? record.getNssAllowance() : 0.0;
            double rentAllowance = record.getRentAllowance() != null ? record.getRentAllowance() : 0.0;
            double transportAllowance = record.getTransportAllowance() != null ? record.getTransportAllowance() : 0.0;
            double clothingAllowance = record.getClothingAllowance() != null ? record.getClothingAllowance() : 0.0;
            double otherAllowance = record.getOtherAllowance() != null ? record.getOtherAllowance() : 0.0;

            double taxableAllowances = rentAllowance + transportAllowance + clothingAllowance + otherAllowance + multiplierBonus;
            double taxableIncome = overtimePay + taxableAllowances;
            double nonTaxableIncome = nssAllowance;

            logger.info("NSS MODE BREAKDOWN for Employee {}:", employee.getId());
            logger.info("  - Total Hours Worked: {} (Normal: {} + Overtime: {})",
                    totalHoursWorked, normalAttendanceHours, overtimeHours);
            logger.info("  - Hourly Rate: {}", hourlyRate);
            logger.info("  - Overtime Pay (Taxable): {}", overtimePay);
            logger.info("  - Other Allowances (Taxable): {}", taxableAllowances);
            logger.info("  - TOTAL TAXABLE INCOME: {}", taxableIncome);
            logger.info("  - NSS Allowance (NON-Taxable): {}", nonTaxableIncome);

            double taxAmount;
            if (useWithholdingTax) {
                taxAmount = calculateWithholdingTax(taxableIncome, withholdingTaxRate);
                logger.info("NSS MODE: WITHHOLDING TAX applied to taxable income {} at {}% = {}",
                        taxableIncome, withholdingTaxRate, taxAmount);
            } else {
                taxAmount = calculatePAYETax(taxableIncome);
                logger.info("NSS MODE: Regular PAYE applied to taxable income {} = {}", taxableIncome, taxAmount);
            }

            double netFromTaxable = taxableIncome - taxAmount;
            double netSalary = netFromTaxable + nonTaxableIncome;
            double netSalaryWithCredit = applyCredits(netSalary, creditAmount);

            double grossSalary = taxableIncome;
            record.setBasicSalary(0.0);
            record.setOvertimePay(round(overtimePay));
            record.setGrossSalary(round(grossSalary));
            record.setSsnitEmployee(0.0);
            record.setSsnitEmployer(0.0);
            record.setPayeTax(round(taxAmount));
            record.setNetSalary(round(netSalaryWithCredit));
            record.setCreditAmount(creditAmount);
            record.setTaxableIncome(round(taxableIncome));
            record.setNonTaxableIncome(round(nonTaxableIncome));
            record.setTotalAllowances(round(taxableAllowances + nonTaxableIncome));
            record.setOvertimeHours(totalHoursWorked);
            record.setTier2Amount(0.0);  // basicSalary is 0, so tier2 is 0

            logger.info("NSS MODE FINAL - Employee ID {}:", employee.getId());
            logger.info("  - Taxable Income: {}", taxableIncome);
            logger.info("  - Tax Amount ({}): {}", useWithholdingTax ? "Withholding Tax" : "PAYE", taxAmount);
            logger.info("  - Non-Taxable Income: {}", nonTaxableIncome);
            logger.info("  - FINAL NET SALARY: {}", netSalary);
            logger.info("  - SSNIT Exempt: {}", isExcludedFromSsnit ? "YES" : "NO");

            record.setTotalHours(totalHours);
            record.setOvertimeHours(overtimeHours);
            record.setWorkingDays(actualWorkingDays);
            record.setLeaveDays(leaveDays);
            record.setTotalDaysInPeriod(totalDaysInPeriod);
            record.setTotalWorkingDays(totalPaidDays);
            record.setHolidayHours(holidayHours);
            record.setWeekendHours(weekendHours);
            record.setMultiplierBonus(multiplierBonus);
            record.setUseNonTaxableAllowances(useNonTaxableAllowances);
            return;
        }

        // ================ REGULAR EMPLOYEE ================
        if (includeBasicSalary) {
            Double monthlySalary = employee.getBasicSalary();

            if (monthlySalary != null && monthlySalary > 0) {
                // Use monthly salary
                basicSalary = monthlySalary;
                logger.info("Using monthly salary: {} (includeBasicSalary=true)", basicSalary);

                // Overtime pay from hourly rate if available
                if (hasHourlyRate) {
                    overtimePay = calculateBasicOvertimePay(employee, overtimeHours);
                    logger.info("Overtime pay from hourly rate: {}", overtimePay);
                } else {
                    overtimePay = 0.0;
                    logger.info("No hourly rate – overtime pay set to 0");
                }
            } else {
                // Monthly salary is 0 or null → fallback to hourly rate if exists
                if (hasHourlyRate) {
                    basicSalary = calculateBasicSalaryFromTotalHoursWithLeave(employee, normalAttendanceHours, totalLeaveHours);
                    overtimePay = calculateBasicOvertimePay(employee, overtimeHours);
                    logger.info("Monthly salary is 0, using hourly rate: Basic Salary = {} from {} total hours, Overtime Pay = {}",
                            basicSalary, totalHours, overtimePay);
                } else {
                    basicSalary = 0.0;
                    overtimePay = 0.0;
                    logger.info("Monthly salary is 0 and no hourly rate – set to 0");
                }
            }
        } else {
            // Checkbox OFF: use hourly rate if available, otherwise 0
            if (hasHourlyRate) {
                basicSalary = calculateBasicSalaryFromTotalHoursWithLeave(employee, normalAttendanceHours, totalLeaveHours);
                overtimePay = calculateBasicOvertimePay(employee, overtimeHours);
                logger.info("Hourly rate calcuaclation (checkbox off): Basic Salary = {} from {} total hours, Overtime Pay = {}",
                        basicSalary, totalHours, overtimePay);
            } else {
                basicSalary = 0.0;
                overtimePay = 0.0;
                logger.info("Checkbox off and no hourly rate – set to 0");
            }
        }

        tier2Amount = basicSalary * 0.05;
        logger.info("Tier 2 amount (store only) for employee {}: {}", employee.getId(), tier2Amount);

        double rentAllowance = record.getRentAllowance() != null ? record.getRentAllowance() : 0.0;
        double transportAllowance = record.getTransportAllowance() != null ? record.getTransportAllowance() : 0.0;
        double clothingAllowance = record.getClothingAllowance() != null ? record.getClothingAllowance() : 0.0;
        double otherAllowance = record.getOtherAllowance() != null ? record.getOtherAllowance() : 0.0;
        double nssAllowance = record.getNssAllowance() != null ? record.getNssAllowance() : 0.0;

        double totalAllowances = rentAllowance + transportAllowance + clothingAllowance + otherAllowance + nssAllowance + overtimePay + multiplierBonus;
        double grossSalary = basicSalary + totalAllowances;

        if (isExcludedFromSsnit) {
            ssnitEmployee = 0.0;
            ssnitEmployer = 0.0;
            logger.info("REGULAR MODE: Employee {} is EXCLUDED from SSNIT - SSNIT deductions set to 0",
                    employee.getEmployeeId());
        } else {
            ssnitEmployee = calculateEmployeeSSNIT(basicSalary);
            ssnitEmployer = calculateEmployerSSNIT(basicSalary);
        }

        double chargeableIncome = grossSalary - ssnitEmployee;
        double taxAmount;
        if (useWithholdingTax) {
            taxAmount = calculateWithholdingTax(grossSalary, withholdingTaxRate);
            logger.info("REGULAR MODE WITHHOLDING TAX: Taxable = Gross Salary {} × {}% = {}",
                    grossSalary, withholdingTaxRate, taxAmount);
            logger.info("NOTE: SSNIT is {} but NOT deducted from taxable income for withholding tax", ssnitEmployee);
        } else {
            taxAmount = calculatePAYETax(chargeableIncome);
            logger.info("REGULAR MODE PAYE: Taxable = Gross - SSNIT = {} - {} = {}, PAYE = {}",
                    grossSalary, ssnitEmployee, chargeableIncome, taxAmount);
        }

        double netSalary = grossSalary - ssnitEmployee - taxAmount;
        double netSalaryWithCredit = applyCredits(netSalary, creditAmount);

        record.setBasicSalary(round(basicSalary));
        record.setOvertimePay(round(overtimePay));
        record.setGrossSalary(round(grossSalary));
        record.setSsnitEmployee(round(ssnitEmployee));
        record.setSsnitEmployer(round(ssnitEmployer));
        record.setPayeTax(round(taxAmount));
        record.setNetSalary(round(netSalaryWithCredit));
        record.setCreditAmount(creditAmount);
        record.setTaxableIncome(round(useWithholdingTax ? grossSalary : chargeableIncome));
        record.setTotalAllowances(round(totalAllowances));
        record.setTier2Amount(round(tier2Amount));

        logger.info("REGULAR MODE FINAL - Employee ID {}:", employee.getId());
        logger.info("  - Basic Salary: {}", basicSalary);
        logger.info("  - Allowances: {}", totalAllowances);
        logger.info("  - Gross Salary: {}", grossSalary);
        logger.info("  - SSNIT Deducted: {}", ssnitEmployee);
        logger.info("  - Tax Type: {}", useWithholdingTax ? "Withholding Tax" : "PAYE");
        logger.info("  - Tax Amount: {}", taxAmount);
        logger.info("  - Net Salary: {}", netSalary);
        logger.info("  - SSNIT Exempt: {}", isExcludedFromSsnit ? "YES" : "NO");
        logger.info("  - Tier 2 (stored): {}", tier2Amount);

        record.setTotalHours(totalHours);
        record.setOvertimeHours(overtimeHours);
        record.setWorkingDays(actualWorkingDays);
        record.setLeaveDays(leaveDays);
        record.setTotalDaysInPeriod(totalDaysInPeriod);
        record.setTotalWorkingDays(totalPaidDays);
        record.setHolidayHours(holidayHours);
        record.setWeekendHours(weekendHours);
        record.setMultiplierBonus(multiplierBonus);
        record.setUseNonTaxableAllowances(useNonTaxableAllowances);
    }


    /**
     * Calculate basic salary with proration based on paid days vs total days in period
     * This is used when includeBasicSalary is true
     */
    private double calculateBasicSalaryWithProration(Employee employee, int totalDaysInPeriod, int totalPaidDays) {
        Double monthlySalary = employee.getBasicSalary();

        if (monthlySalary == null || monthlySalary <= 0) {
            logger.warn("Monthly salary is 0 or not set for employee {} - cannot prorate", employee.getId());
            return 0.0;
        }

        if (totalDaysInPeriod <= 0) {
            logger.warn("Total days in period is 0 for employee {} - returning full monthly salary", employee.getId());
            return monthlySalary;
        }

        // Prorate the salary: (Monthly Salary × Paid Days) ÷ Total Working Days in Period
        double proratedSalary = (monthlySalary * totalPaidDays) / totalDaysInPeriod;

        logger.info("Proration calculation for employee {}: Monthly Salary: {}, Paid Days: {}, Total Days: {}, Prorated: {}",
                employee.getId(), monthlySalary, totalPaidDays, totalDaysInPeriod, proratedSalary);

        return proratedSalary;
    }

    private double calculateBasicSalaryFromTotalHours(Employee employee, double totalHours) {
        double hourlyRate = getEffectiveHourlyRate(employee);

        if (hourlyRate <= 0) {
            logger.error("No valid hourly rate found for employee {}", employee.getId());
            return 0.0;
        }

        double basicSalary = totalHours * hourlyRate;

        logger.info("Total hours calculation - Employee: {}, Hourly Rate: {}, Total Hours: {}, Basic Salary: {}",
                employee.getId(), hourlyRate, totalHours, basicSalary);

        return basicSalary;
    }

    private double calculateBasicSalaryFromTotalHoursWithLeave(Employee employee, double normalHours, double leaveHours) {
        double hourlyRate = getEffectiveHourlyRate(employee);

        if (hourlyRate <= 0) {
            logger.error("No valid hourly rate found for employee {}", employee.getId());
            return 0.0;
        }

        double totalHours = normalHours + leaveHours;
        double basicSalary = totalHours * hourlyRate;

        logger.info("Basic salary with leave - Employee: {}, Hourly Rate: {}, Normal Hours: {}, Leave Hours: {}, Total Hours: {}, Basic Salary: {}",
                employee.getId(), hourlyRate, normalHours, leaveHours, totalHours, basicSalary);

        return basicSalary;
    }

    private double getEffectiveHourlyRate(Employee employee) {
        if (employee.getMinimumRate() != null && employee.getMinimumRate() > 0) {
            logger.info("Using minimumRate for employee {}: {}", employee.getId(), employee.getMinimumRate());
            return employee.getMinimumRate();
        }

        if (employee.getHourlyRate() != null && employee.getHourlyRate() > 0) {
            logger.info("Using hourlyRate for employee {}: {}", employee.getId(), employee.getHourlyRate());
            return employee.getHourlyRate();
        }

        logger.warn("No hourly rate (minimumRate or hourlyRate) set for employee {}. Hourly calculations will be skipped.", employee.getId());
        return 0.0;
    }

    private double calculateBasicOvertimePay(Employee employee, double overtimeHours) {
        if (overtimeHours <= 0) return 0.0;

        double hourlyRate = getEffectiveHourlyRate(employee);
        if (hourlyRate <= 0) return 0.0;

        return overtimeHours * hourlyRate;
    }

    private double calculateEmployeeSSNIT(double basicSalary) {
        if (basicSalary <= 0) return 0.0;
        return basicSalary * EMPLOYEE_SSNIT_RATE;
    }

    private double calculateEmployerSSNIT(double basicSalary) {
        if (basicSalary <= 0) return 0.0;
        return basicSalary * EMPLOYER_SSNIT_RATE;
    }

    private double calculatePAYETax(double chargeableIncome) {
        if (chargeableIncome <= 0) {
            logger.info("[PAYE] Chargeable income <= 0. PAYE = 0.00 (income={})", chargeableIncome);
            return 0.0;
        }

        logger.info("[PAYE] START | chargeableIncome={}", round(chargeableIncome));
        logger.info("[PAYE] Tiers: t1={} ({}%), t2={} ({}%), t3={} ({}%), t4={} ({}%), t5={} ({}%), t6={} ({}%), t7Rate={}% (exceeding)",
                TIER_1_LIMIT, (TIER_1_RATE * 100),
                TIER_2_LIMIT, (TIER_2_RATE * 100),
                TIER_3_LIMIT, (TIER_3_RATE * 100),
                TIER_4_LIMIT, (TIER_4_RATE * 100),
                TIER_5_LIMIT, (TIER_5_RATE * 100),
                TIER_6_LIMIT, (TIER_6_RATE * 100),
                (TIER_7_RATE * 100));

        double tax = 0.0;

        double band2Width = TIER_2_LIMIT - TIER_1_LIMIT;
        double band3Width = TIER_3_LIMIT - TIER_2_LIMIT;
        double band4Width = TIER_4_LIMIT - TIER_3_LIMIT;
        double band5Width = TIER_5_LIMIT - TIER_4_LIMIT;
        double band6Width = TIER_6_LIMIT - TIER_5_LIMIT;

        if (chargeableIncome <= TIER_1_LIMIT) {
            logger.info("[PAYE] BRANCH: <= TIER 1 ({}). All income in 0% band. PAYE=0.00", TIER_1_LIMIT);
            tax = 0.0;

        } else if (chargeableIncome <= TIER_2_LIMIT) {
            logger.info("[PAYE] BRANCH: TIER 2 (>{} and <= {}).", TIER_1_LIMIT, TIER_2_LIMIT);

            double taxableAt5 = chargeableIncome - TIER_1_LIMIT;
            double taxAt5 = taxableAt5 * TIER_2_RATE;

            logger.info("[PAYE] Tier2 calc: taxable=(income - t1) = {} - {} = {}",
                    round(chargeableIncome), TIER_1_LIMIT, round(taxableAt5));
            logger.info("[PAYE] Tier2 tax: {} * {}% = {}", round(taxableAt5), (TIER_2_RATE * 100), round(taxAt5));

            tax = taxAt5;

        } else if (chargeableIncome <= TIER_3_LIMIT) {
            logger.info("[PAYE] BRANCH: TIER 3 (>{} and <= {}).", TIER_2_LIMIT, TIER_3_LIMIT);

            double taxTier2Full = band2Width * TIER_2_RATE;

            double taxableAt10 = chargeableIncome - TIER_2_LIMIT;
            double taxAt10 = taxableAt10 * TIER_3_RATE;

            logger.info("[PAYE] Tier2 full: {} * {}% = {}", round(band2Width), (TIER_2_RATE * 100), round(taxTier2Full));
            logger.info("[PAYE] Tier3 calc: taxable=(income - t2) = {} - {} = {}",
                    round(chargeableIncome), TIER_2_LIMIT, round(taxableAt10));
            logger.info("[PAYE] Tier3 tax: {} * {}% = {}", round(taxableAt10), (TIER_3_RATE * 100), round(taxAt10));

            tax = taxTier2Full + taxAt10;

        } else if (chargeableIncome <= TIER_4_LIMIT) {
            logger.info("[PAYE] BRANCH: TIER 4 (>{} and <= {}).", TIER_3_LIMIT, TIER_4_LIMIT);

            double taxTier2Full = band2Width * TIER_2_RATE;
            double taxTier3Full = band3Width * TIER_3_RATE;

            double taxableAt175 = chargeableIncome - TIER_3_LIMIT;
            double taxAt175 = taxableAt175 * TIER_4_RATE;

            logger.info("[PAYE] Tier2 full: {} * {}% = {}", round(band2Width), (TIER_2_RATE * 100), round(taxTier2Full));
            logger.info("[PAYE] Tier3 full: {} * {}% = {}", round(band3Width), (TIER_3_RATE * 100), round(taxTier3Full));
            logger.info("[PAYE] Tier4 calc: taxable=(income - t3) = {} - {} = {}",
                    round(chargeableIncome), TIER_3_LIMIT, round(taxableAt175));
            logger.info("[PAYE] Tier4 tax: {} * {}% = {}", round(taxableAt175), (TIER_4_RATE * 100), round(taxAt175));

            tax = taxTier2Full + taxTier3Full + taxAt175;

        } else if (chargeableIncome <= TIER_5_LIMIT) {
            logger.info("[PAYE] BRANCH: TIER 5 (>{} and <= {}).", TIER_4_LIMIT, TIER_5_LIMIT);

            double taxTier2Full = band2Width * TIER_2_RATE;
            double taxTier3Full = band3Width * TIER_3_RATE;
            double taxTier4Full = band4Width * TIER_4_RATE;

            double taxableAt25 = chargeableIncome - TIER_4_LIMIT;
            double taxAt25 = taxableAt25 * TIER_5_RATE;

            logger.info("[PAYE] Tier2 full: {} * {}% = {}", round(band2Width), (TIER_2_RATE * 100), round(taxTier2Full));
            logger.info("[PAYE] Tier3 full: {} * {}% = {}", round(band3Width), (TIER_3_RATE * 100), round(taxTier3Full));
            logger.info("[PAYE] Tier4 full: {} * {}% = {}", round(band4Width), (TIER_4_RATE * 100), round(taxTier4Full));
            logger.info("[PAYE] Tier5 calc: taxable=(income - t4) = {} - {} = {}",
                    round(chargeableIncome), TIER_4_LIMIT, round(taxableAt25));
            logger.info("[PAYE] Tier5 tax: {} * {}% = {}", round(taxableAt25), (TIER_5_RATE * 100), round(taxAt25));

            tax = taxTier2Full + taxTier3Full + taxTier4Full + taxAt25;

        } else if (chargeableIncome <= TIER_6_LIMIT) {
            logger.info("[PAYE] BRANCH: TIER 6 (>{} and <= {}). 30% applies above Tier 5.", TIER_5_LIMIT, TIER_6_LIMIT);

            double taxTier2Full = band2Width * TIER_2_RATE;
            double taxTier3Full = band3Width * TIER_3_RATE;
            double taxTier4Full = band4Width * TIER_4_RATE;
            double taxTier5Full = band5Width * TIER_5_RATE;

            double taxableAt30 = chargeableIncome - TIER_5_LIMIT;
            double taxAt30 = taxableAt30 * TIER_6_RATE;

            double baseTaxToTier5 = taxTier2Full + taxTier3Full + taxTier4Full + taxTier5Full;

            logger.info("[PAYE] Base tax (Tier2..Tier5 full) = {}", round(baseTaxToTier5));
            logger.info("[PAYE] Tier6 calc: taxable=(income - t5) = {} - {} = {}",
                    round(chargeableIncome), TIER_5_LIMIT, round(taxableAt30));
            logger.info("[PAYE] Tier6 tax: {} * {}% = {}", round(taxableAt30), (TIER_6_RATE * 100), round(taxAt30));

            tax = baseTaxToTier5 + taxAt30;

        } else {
            logger.info("[PAYE] BRANCH: TIER 7 (> {}). 35% applies above Tier 6.", TIER_6_LIMIT);

            double taxTier2Full = band2Width * TIER_2_RATE;
            double taxTier3Full = band3Width * TIER_3_RATE;
            double taxTier4Full = band4Width * TIER_4_RATE;
            double taxTier5Full = band5Width * TIER_5_RATE;
            double taxTier6Full = band6Width * TIER_6_RATE;

            double taxableAt35 = chargeableIncome - TIER_6_LIMIT;
            double taxAt35 = taxableAt35 * TIER_7_RATE;

            double baseTaxToTier6 = taxTier2Full + taxTier3Full + taxTier4Full + taxTier5Full + taxTier6Full;

            logger.info("[PAYE] Base tax (Tier2..Tier6 full) = {}", round(baseTaxToTier6));
            logger.info("[PAYE] Tier7 calc: taxable=(income - t6) = {} - {} = {}",
                    round(chargeableIncome), TIER_6_LIMIT, round(taxableAt35));
            logger.info("[PAYE] Tier7 tax: {} * {}% = {}", round(taxableAt35), (TIER_7_RATE * 100), round(taxAt35));

            tax = baseTaxToTier6 + taxAt35;
        }

        double rounded = round(tax);
        logger.info("[PAYE] END | rawTax={} | roundedTax={}", tax, rounded);
        return rounded;
    }

    public int calculateWorkingDaysInPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }

        int workingDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return workingDays;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public double applyCredits(double netSalary, double creditAmount) {
        if (creditAmount > 0) {
            double newNetSalary = netSalary + creditAmount;
            logger.info("Applying credit of {} to net salary. Net before credit: {}, Net after credit: {}",
                    creditAmount, netSalary, newNetSalary);
            return newNetSalary;
        }
        return netSalary;
    }

    private double calculateWithholdingTax(double taxableIncome, double withholdingRate) {
        if (taxableIncome <= 0 || withholdingRate <= 0) {
            return 0.0;
        }
        double tax = taxableIncome * (withholdingRate / 100.0);
        logger.info("[WITHHOLDING TAX] Taxable Income: {}, Rate: {}%, Tax: {}",
                taxableIncome, withholdingRate, tax);
        return tax;
    }

    public int calculateWorkingDaysInPeriod(LocalDate startDate, LocalDate endDate, Employee employee) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }

        boolean excludeWeekends = true;
        if (employee != null && employee.getCategory() != null) {
            Boolean setting = employee.getCategory().getExcludeWeekendsFromLeave();
            excludeWeekends = setting == null ? true : setting;
        }

        int workingDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

            if (excludeWeekends) {
                if (!isWeekend) workingDays++;
            } else {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }
}