package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.entity.attendance.Attendance;
import java.util.Locale;
import java.util.Set;

public final class LunchAttendancePolicy {
    private LunchAttendancePolicy() {}

    public static boolean isPresent(Attendance attendance) {
        if (attendance == null) return false;
        if (attendance.getCheckIn() != null) return true;
        String status = attendance.getStatus() == null ? "" : attendance.getStatus().trim()
                .toUpperCase(Locale.ROOT).replace('_', ' ');
        return Set.of("PRESENT", "LATE", "HOLIDAY PRESENT", "WEEKEND PRESENT").contains(status);
    }
}
