package com.example.eac_backend_project.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

final class PayrollAttendanceHours {
    private PayrollAttendanceHours() {}
    record Split(double normal, double overtime) {}

    static Split split(double attendanceHours, int periodDays, double standardHours, boolean enabled) {
        if (!enabled) return new Split(attendanceHours, 0);
        double normal = Math.min(attendanceHours, periodDays * standardHours);
        return new Split(normal, Math.max(0, attendanceHours - normal));
    }

    static int weekdays(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) return 0;
        int days = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) days++;
        }
        return days;
    }
}
