package com.example.eac_backend_project.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;

final class PayrollOvertimeRateHours {
    record Split(double oldRate, double newRate) {}

    static Split split(Map<LocalDate, Double> approved, Map<LocalDate, Double> attendance,
                       double normalHours, double standardHours, boolean convertExcess,
                       Predicate<LocalDate> automaticWeekend, double weekendMultiplier,
                       LocalDate effectiveFrom, LocalDate effectiveTo) {
        double oldHours = 0, newHours = 0;
        TreeSet<LocalDate> dates = new TreeSet<>(approved.keySet());
        dates.addAll(attendance.keySet());
        double remainingNormal = Math.max(0, normalHours);
        for (LocalDate date : dates) {
            double approvedHours = Math.max(0, approved.getOrDefault(date, 0.0));
            double workedHours = Math.max(0, attendance.getOrDefault(date, 0.0));
            double overtime = approvedHours;
            if (automaticWeekend.test(date)) {
                overtime += Math.max(0, workedHours - approvedHours) * Math.max(0, weekendMultiplier);
            } else {
                double payable = Math.min(standardHours, workedHours);
                double normal = Math.min(remainingNormal, payable);
                remainingNormal -= normal;
                if (convertExcess) overtime += payable - normal;
            }
            boolean effective = effectiveFrom != null && !date.isBefore(effectiveFrom)
                    && (effectiveTo == null || !date.isAfter(effectiveTo));
            if (effective) newHours += overtime;
            else oldHours += overtime;
        }
        return new Split(oldHours, newHours);
    }
}
