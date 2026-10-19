package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

/** A reporting allocation of the calculated payroll, never an additional payment. */
public final class PayrollProjectEarnings {
    private PayrollProjectEarnings() {}
    public record Work(Long timesheetId, Long projectId, String projectName, String source) {}
    public record Day(String date, Long timesheetId, Long projectId, String projectName,
                      String source, double normalHours, double overtimeHours,
                      double normalEarnings, double overtimeEarnings, double totalEarnings) {}
    public record Snapshot(int version, String periodStart, String periodEnd, String allocationMethod,
                           List<Day> days, double workEarnings, double otherPeriodEarnings,
                           double periodGross) {}

    public static Snapshot build(PayrollRecord payroll, Map<LocalDate, Double> attendance,
                                 Map<LocalDate, Double> approvedOvertime, Map<LocalDate, Work> work,
                                 double normalHours, double standardHours, boolean convertExcess,
                                 Predicate<LocalDate> automaticWeekend, double weekendMultiplier) {
        PayrollPeriod period = payroll.getPeriod();
        TreeSet<LocalDate> dates = new TreeSet<>(attendance.keySet());
        dates.addAll(approvedOvertime.keySet());
        dates.removeIf(date -> date.isBefore(period.getStartDate()) || date.isAfter(period.getEndDate()));
        List<LocalDate> dated = new ArrayList<>(dates);
        double[] normal = new double[dated.size()], overtime = new double[dated.size()];
        double[] normalWeights = new double[dated.size()], overtimeWeights = new double[dated.size()];
        double remaining = Math.max(0, normalHours);
        double baseRate = value(payroll.getAppliedEmployeeRate());
        boolean monthly = Boolean.TRUE.equals(payroll.getIncludeBasicSalary()) && value(payroll.getAppliedMonthlySalary()) > 0;
        boolean allowanceMode = Boolean.TRUE.equals(payroll.getUseNonTaxableAllowances());
        for (int i = 0; i < dated.size(); i++) {
            LocalDate date = dated.get(i);
            double worked = Math.max(0, attendance.getOrDefault(date, 0.0));
            double approved = Math.max(0, approvedOvertime.getOrDefault(date, 0.0));
            overtime[i] = approved;
            if (automaticWeekend.test(date)) {
                overtime[i] += Math.max(0, worked - approved) * Math.max(0, weekendMultiplier);
            } else {
                double payable = Math.min(standardHours, worked);
                normal[i] = Math.min(remaining, payable);
                remaining -= normal[i];
                if (convertExcess) overtime[i] += payable - normal[i];
            }
            // Only a rate actually captured by this payroll may affect the allocation.
            double rate = baseRate;
            LocalDate from = payroll.getAppliedEffectivePayrollRateFrom(), to = payroll.getAppliedEffectivePayrollRateTo();
            if (from != null && !date.isBefore(from) && (to == null || !date.isAfter(to))
                    && payroll.getAppliedEffectivePayrollRate() != null) rate = payroll.getAppliedEffectivePayrollRate();
            normalWeights[i] = normal[i] * rate;
            // The existing regular calculator pays OT at the captured base rate.
            overtimeWeights[i] = allowanceMode ? (normal[i] + overtime[i]) * baseRate : overtime[i] * baseRate;
        }
        double normalPool = monthly || allowanceMode ? 0 : Math.max(0,
                value(payroll.getBasicSalary()) - value(payroll.getLeaveHours()) * baseRate);
        double datedNormal = Arrays.stream(normal).sum(), datedOvertime = Arrays.stream(overtime).sum();
        if (normalHours > 0) normalPool *= Math.min(1, datedNormal / normalHours);
        double overtimeDenominator = value(payroll.getOvertimeHours()) + (allowanceMode ? normalHours : 0);
        double datedOvertimeBasis = datedOvertime + (allowanceMode ? datedNormal : 0);
        double overtimePool = value(payroll.getOvertimePay());
        if (overtimeDenominator > 0) overtimePool *= Math.min(1, datedOvertimeBasis / overtimeDenominator);
        double[] normalPay = allocate(normalPool, normalWeights);
        double[] overtimePay = allocate(overtimePool, overtimeWeights);
        List<Day> days = new ArrayList<>();
        long workCents = 0;
        for (int i = 0; i < dated.size(); i++) {
            if (normal[i] == 0 && overtime[i] == 0 && normalPay[i] == 0 && overtimePay[i] == 0) continue;
            Work provenance = work.getOrDefault(dated.get(i), new Work(null, null, null, "Attendance"));
            double total = money(normalPay[i] + overtimePay[i]);
            workCents += Math.round(total * 100);
            days.add(new Day(dated.get(i).toString(), provenance.timesheetId(), provenance.projectId(),
                    provenance.projectName(), provenance.source(), normal[i], overtime[i],
                    normalPay[i], overtimePay[i], total));
        }
        double earnings = workCents / 100.0;
        return new Snapshot(1, period.getStartDate().toString(), period.getEndDate().toString(),
                "Calculated payroll work pay allocated by dated payable hours and captured rates; monthly salary, leave pay, allowances and any undated pay remain separate.",
                days, earnings, money(value(payroll.getGrossSalary()) - earnings),
                value(payroll.getGrossSalary()));
    }

    // Cumulative cent allocation reconciles exactly, including very small payments.
    static double[] allocate(double amount, double[] weights) {
        double[] result = new double[weights.length];
        double sum = Arrays.stream(weights).sum();
        if (sum <= 0) return result;
        long cents = Math.round(amount * 100), previous = 0;
        double cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            long current = Math.round(cents * cumulative / sum);
            result[i] = (current - previous) / 100.0;
            previous = current;
        }
        return result;
    }
    private static double value(Double value) { return value == null ? 0 : value; }
    private static double money(double value) { return Math.round(value * 100) / 100.0; }
}
