package com.example.eac_backend_project.Service.reports;

import com.example.eac_backend_project.entity.attendance.*;
import java.util.*;

/** Read-only projection of the same approved overtime entries consumed by payroll. */
public final class ApprovedOvertimePreview {
    private ApprovedOvertimePreview() {}
    public record Data(List<Attendance> records, Map<Long, Overtime> overtimeByAttendanceId) {}

    public static Data build(List<Attendance> attendance, List<Overtime> approvedOvertime) {
        List<Attendance> records = new ArrayList<>(attendance.stream()
                .filter(row -> row.getEmployee() != null && Boolean.TRUE.equals(row.getEmployee().getActive())).toList());
        Map<Long, Attendance> byId = new HashMap<>();
        Map<String, Attendance> byDay = new HashMap<>();
        for (Attendance row : records) {
            byId.put(row.getId(), row);
            byDay.putIfAbsent(row.getEmployee().getId() + ":" + row.getDate(), row);
        }
        Map<Long, Overtime> totals = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        long syntheticId = -1;
        for (Overtime source : approvedOvertime) {
            if (!"Approved".equals(source.getStatus()) || source.getEmployee() == null
                    || !Boolean.TRUE.equals(source.getEmployee().getActive()) || source.getDate() == null) continue;
            if (source.getId() != null && !seen.add(source.getId())) continue;
            String day = source.getEmployee().getId() + ":" + source.getDate();
            Attendance row = source.getAttendance() == null ? null : byId.get(source.getAttendance().getId());
            if (row == null) row = byDay.get(day);
            if (row == null) {
                while (byId.containsKey(syntheticId)) syntheticId--;
                row = new Attendance(); row.setId(syntheticId--); row.setEmployee(source.getEmployee());
                row.setDate(source.getDate()); row.setStatus("Approved Overtime"); row.setMinimumHour(0); row.setOvertime(0.0);
                records.add(row); byId.put(row.getId(), row); byDay.put(day, row);
            }
            double finalHours = source.getOvertimeHours() == null ? 0 : source.getOvertimeHours();
            // Legacy entries may have final hours only. Never fall back to unapproved attendance OT.
            double rawHours = source.getRawOvertimeHours() != null ? source.getRawOvertimeHours()
                    : Boolean.TRUE.equals(source.getMultiplierAppliedToHours()) && source.getOvertimeMultiplier() != null
                      && source.getOvertimeMultiplier() > 0 ? finalHours / source.getOvertimeMultiplier() : finalHours;
            Overtime total = totals.get(row.getId());
            if (total == null) {
                total = new Overtime(); total.setStatus("Approved"); total.setRawOvertimeHours(0.0); total.setOvertimeHours(0.0);
                total.setOvertimeMultiplier(source.getOvertimeMultiplier()); total.setMultiplierAppliedToHours(source.getMultiplierAppliedToHours());
                totals.put(row.getId(), total);
            } else if (!Objects.equals(total.getOvertimeMultiplier(), source.getOvertimeMultiplier())
                    || !Objects.equals(total.getMultiplierAppliedToHours(), source.getMultiplierAppliedToHours())) {
                total.setOvertimeMultiplier(null); total.setMultiplierAppliedToHours(false);
            }
            total.setRawOvertimeHours(total.getRawOvertimeHours() + rawHours);
            total.setOvertimeHours(total.getOvertimeHours() + finalHours);
        }
        records.sort(Comparator.comparing(Attendance::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
        return new Data(records, totals);
    }
}
