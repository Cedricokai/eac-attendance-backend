package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Attendance;
import java.util.Locale;
import java.util.Set;

/** Fixed job hours replace the worked-hours split, never the original punches. */
public final class ConfiguredJobAttendanceHours {
    private ConfiguredJobAttendanceHours() {}

    public static boolean applies(Attendance row) {
        if (row == null || row.getEmployee() == null || row.getEmployee().getJob() == null
                || row.getCheckIn() == null || row.getCheckOut() == null) return false;
        if (!Boolean.TRUE.equals(row.getEmployee().getJob().getUseConfiguredAttendanceHours())) return false;
        String status = row.getStatus() == null ? "present" : row.getStatus().toLowerCase(Locale.ROOT);
        return Set.of("present", "late", "half day").contains(status);
    }

    public static boolean isWeeklyOffDay(Attendance row, String fallbackDays) {
        if (row == null || row.getDate() == null || row.getEmployee() == null) return false;
        var employee = row.getEmployee();
        String days = employee.getWeeklyOffDays();
        if (days == null || days.isBlank() || "INHERIT".equalsIgnoreCase(days))
            days = employee.getCategory() != null ? employee.getCategory().getWeeklyOffDays() : null;
        if (days == null || days.isBlank() || "INHERIT".equalsIgnoreCase(days)) days = fallbackDays;
        if (days == null || days.isBlank()) return false;
        String day = String.valueOf(row.getDate().getDayOfWeek().getValue() % 7);
        for (String value : days.split(",")) if (day.equals(value.trim())) return true;
        return false;
    }

    public static void apply(Attendance row) {
        if (!applies(row)) return;
        var job = row.getEmployee().getJob();
        double normal = job.getStandardWorkHours() != null ? job.getStandardWorkHours() : 8.0;
        double overtime = job.getMinimumOvertimeHours() != null ? job.getMinimumOvertimeHours() : 0.0;
        if (normal <= 0 || overtime < 0 || !Double.isFinite(overtime))
            throw new IllegalArgumentException("Configured job hours must have positive normal hours and nonnegative overtime");
        row.setMinimumHour(normal);
        row.setStandardHours(normal);
        row.setOvertime(overtime);
    }
}
