package com.example.eac_backend_project.Service;

import java.time.LocalTime;

/** Splits actual work against the job's fixed normal-hours window. No break deduction. */
public final class JobScheduleHours {
    private JobScheduleHours() {}
    public record Result(double normalHours, double overtimeHours, LocalTime overtimeStart) {}

    public static Result calculate(LocalTime start, double standardHours, LocalTime checkIn, LocalTime checkOut) {
        if (!Double.isFinite(standardHours) || standardHours <= 0 || standardHours > 24) {
            throw new IllegalArgumentException("Standard work hours must be greater than 0 and at most 24");
        }
        long startMinute = start.toSecondOfDay() / 60;
        long normalEnd = startMinute + Math.round(standardHours * 60);
        long in = checkIn.toSecondOfDay() / 60;
        long out = checkOut.toSecondOfDay() / 60;
        // A post-midnight arrival during an overnight normal window belongs to that shift.
        if (normalEnd >= 1440 && in < normalEnd % 1440) in += 1440;
        out += (in / 1440) * 1440;
        if (out < in) out += 1440;
        long effectiveStart = Math.max(startMinute, in);
        long normalMinutes = Math.max(0, Math.min(out, normalEnd) - effectiveStart);
        long overtimeStart = Math.max(effectiveStart, normalEnd);
        long overtimeMinutes = Math.max(0, out - overtimeStart);
        return new Result(normalMinutes / 60.0, overtimeMinutes / 60.0,
                LocalTime.ofSecondOfDay((overtimeStart % 1440) * 60));
    }
}
