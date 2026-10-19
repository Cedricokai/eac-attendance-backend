package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.entity.attendance.PayrollPeriod;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/** Unique paid leave dates shared by requests and leave attendance. */
final class PayrollLeaveDays {
    private PayrollLeaveDays() {}

    static Map<Long, Set<LocalDate>> collect(List<Leave> leaves, List<Object[]> attendance, PayrollPeriod period) {
        Map<Long, Set<LocalDate>> dates = new HashMap<>();
        if (leaves != null) for (Leave leave : leaves) {
            if (leave == null || leave.getEmployee() == null || leave.getEmployee().getId() == null
                    || !"Approved".equalsIgnoreCase(leave.getStatus())
                    || leave.getStartDate() == null || leave.getEndDate() == null) continue;
            LocalDate start = leave.getStartDate().isBefore(period.getStartDate()) ? period.getStartDate() : leave.getStartDate();
            LocalDate end = leave.getEndDate().isAfter(period.getEndDate()) ? period.getEndDate() : leave.getEndDate();
            boolean excludeWeekends = leave.getEmployee().getCategory() == null
                    || !Boolean.FALSE.equals(leave.getEmployee().getCategory().getExcludeWeekendsFromLeave());
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                if (excludeWeekends && (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)) continue;
                dates.computeIfAbsent(leave.getEmployee().getId(), id -> new HashSet<>()).add(date);
            }
        }
        if (attendance != null) for (Object[] row : attendance) {
            // Repository projection: employee ID, attendance date, status.
            if (row == null || row.length < 3 || !(row[0] instanceof Number)) continue;
            String status = Objects.toString(row[2], "");
            if (!"On Leave".equalsIgnoreCase(status) && !"Leave".equalsIgnoreCase(status)) continue;
            LocalDate date = row[1] instanceof LocalDate ? (LocalDate) row[1]
                    : row[1] instanceof java.sql.Date ? ((java.sql.Date) row[1]).toLocalDate() : null;
            if (date != null && !date.isBefore(period.getStartDate()) && !date.isAfter(period.getEndDate()))
                dates.computeIfAbsent(((Number) row[0]).longValue(), id -> new HashSet<>()).add(date);
        }
        Map<Long, Set<LocalDate>> unpaid = collectUnpaid(leaves, period);
        unpaid.forEach((id, days) -> { if (dates.containsKey(id)) dates.get(id).removeAll(days); });
        dates.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return dates;
    }

    static Map<Long, Set<LocalDate>> collectUnpaid(List<Leave> leaves, PayrollPeriod period) {
        Map<Long, Set<LocalDate>> dates = new HashMap<>();
        if (leaves == null) return dates;
        for (Leave leave : leaves) {
            if (leave == null || !leave.isUnpaid() || !"Approved".equalsIgnoreCase(leave.getStatus())
                    || leave.getEmployee() == null || leave.getEmployee().getId() == null
                    || leave.getStartDate() == null || leave.getEndDate() == null) continue;
            LocalDate start = leave.getStartDate().isBefore(period.getStartDate()) ? period.getStartDate() : leave.getStartDate();
            LocalDate end = leave.getEndDate().isAfter(period.getEndDate()) ? period.getEndDate() : leave.getEndDate();
            boolean exclude = leave.getEmployee().getCategory() == null
                    || !Boolean.FALSE.equals(leave.getEmployee().getCategory().getExcludeWeekendsFromLeave());
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
                if (exclude && (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY)) continue;
                dates.computeIfAbsent(leave.getEmployee().getId(), id -> new HashSet<>()).add(day);
            }
        }
        return dates;
    }

    static void excludeFromWorkedHours(Map<Long, Set<LocalDate>> leaveDates,
            Map<Long, List<LocalDate>> attendanceDates, Map<Long, Map<LocalDate, Double>> dailyHours) {
        leaveDates.forEach((id, dates) -> {
            if (attendanceDates.containsKey(id)) attendanceDates.get(id).removeAll(dates);
            if (dailyHours.containsKey(id)) dates.forEach(dailyHours.get(id)::remove);
        });
    }
}
