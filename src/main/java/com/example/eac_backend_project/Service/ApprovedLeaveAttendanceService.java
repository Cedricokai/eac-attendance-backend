package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class ApprovedLeaveAttendanceService {
    private final AttendanceRepository attendance;
    private final LeaveRepository leaves;
    private final OvertimeRepository overtime;
    public ApprovedLeaveAttendanceService(AttendanceRepository attendance, LeaveRepository leaves, OvertimeRepository overtime) {
        this.attendance = attendance; this.leaves = leaves; this.overtime = overtime;
    }
    private boolean eligibleDay(Leave leave, LocalDate date) {
        var category = leave.getEmployee().getCategory();
        boolean exclude = category == null || !Boolean.FALSE.equals(category.getExcludeWeekendsFromLeave());
        return !exclude || (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY);
    }
    private boolean replaceable(Attendance row) {
        return "Absent".equalsIgnoreCase(row.getStatus()) || "APPROVED_LEAVE".equals(row.getMethod())
            || ("On Leave".equalsIgnoreCase(row.getStatus()) && row.getCheckIn() == null && row.getCheckOut() == null);
    }
    private void apply(Attendance row, Leave leave, LocalDate date) {
        row.setEmployee(leave.getEmployee()); row.setDate(date); row.setStatus("On Leave");
        row.setMinimumHour(leave.isUnpaid() ? 0.0 : 8.0); row.setStandardHours(8.0); row.setOvertime(0.0);
        row.setCheckIn(null); row.setCheckOut(null); row.setBiometric(false);
        row.setMethod(leave.isUnpaid() ? "APPROVED_UNPAID_LEAVE" : "APPROVED_LEAVE"); row.setShift("Day"); row.setWorkType("Regular");
    }
    @Transactional
    public void synchronize(Leave leave) {
        if (!"Approved".equalsIgnoreCase(leave.getStatus()) || leave.getEmployee() == null
                || leave.getStartDate() == null || leave.getEndDate() == null) return;
        var existing = attendance.findByEmployeeIdAndDateBetween(leave.getEmployee().getId(), leave.getStartDate(), leave.getEndDate());
        Map<LocalDate, List<Attendance>> byDate = new HashMap<>();
        for (var row : existing) byDate.computeIfAbsent(row.getDate(), key -> new ArrayList<>()).add(row);
        var updates = new ArrayList<Attendance>();
        for (LocalDate date = leave.getStartDate(); !date.isAfter(leave.getEndDate()); date = date.plusDays(1)) {
            if (!eligibleDay(leave,date)) continue;
            var daily = byDate.getOrDefault(date,List.of());
            if (daily.stream().anyMatch(row -> !replaceable(row))) continue;
            if (daily.isEmpty()) {
                var row = new Attendance(); apply(row,leave,date); updates.add(row);
            } else for (var row : daily) {
                apply(row,leave,date); updates.add(row);
                overtime.findByAttendanceId(row.getId()).ifPresent(ot -> {
                    ot.setRawOvertimeHours(0.0); ot.setOvertimeHours(0.0); ot.setCalculatedOvertimePay(0.0);
                    overtime.save(ot);
                });
            }
        }
        if (!updates.isEmpty()) attendance.saveAll(updates);
    }
    // Older approvals also appear in reports without writing to the database while reading.
    @Transactional(readOnly = true)
    public List<Attendance> includeApprovedLeave(List<Attendance> records, LocalDate start, LocalDate end) {
        var result = new ArrayList<>(records);
        Map<String, List<Attendance>> byDay = new HashMap<>();
        for (var row : records) if (row.getEmployee() != null)
            byDay.computeIfAbsent(row.getEmployee().getId() + ":" + row.getDate(), key -> new ArrayList<>()).add(row);
        long syntheticId = Long.MIN_VALUE;
        for (var leave : leaves.findApprovedForAttendanceReport(start,end)) {
            LocalDate first = leave.getStartDate().isBefore(start) ? start : leave.getStartDate();
            LocalDate last = leave.getEndDate().isAfter(end) ? end : leave.getEndDate();
            for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
                if (!eligibleDay(leave,date)) continue;
                String key = leave.getEmployee().getId() + ":" + date;
                var daily = byDay.getOrDefault(key,List.of());
                if (daily.stream().anyMatch(row -> !replaceable(row))) continue;
                result.removeAll(daily);
                var row = new Attendance(); apply(row,leave,date); row.setId(syntheticId++);
                result.add(row); byDay.put(key,List.of(row));
            }
        }
        result.sort(Comparator.comparing(Attendance::getDate,Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }
}
