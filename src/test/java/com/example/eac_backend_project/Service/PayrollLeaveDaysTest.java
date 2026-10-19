package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.Category;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.dto.SettingsDTO;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PayrollLeaveDaysTest {
    LocalDate date(String day) { return LocalDate.parse(day); }
    PayrollPeriod period() {
        PayrollPeriod period = new PayrollPeriod();
        period.setStartDate(date("2026-09-07")); period.setEndDate(date("2026-09-13"));
        return period;
    }
    Leave leave(String start, String end, boolean excludeWeekends) {
        Employee employee = new Employee(); employee.setId(1L);
        Category category = new Category(); category.setExcludeWeekendsFromLeave(excludeWeekends);
        employee.setCategory(category);
        Leave leave = new Leave(); leave.setEmployee(employee); leave.setStatus("Approved");
        leave.setStartDate(date(start)); leave.setEndDate(date(end));
        return leave;
    }
    @Test void approvedLeaveWithoutAttendanceIsClippedToPeriodAndHonorsWeekends() {
        assertEquals(5, PayrollLeaveDays.collect(List.of(leave("2026-09-01", "2026-09-30", true)), null, period()).get(1L).size());
        assertEquals(7, PayrollLeaveDays.collect(List.of(leave("2026-09-01", "2026-09-30", false)), null, period()).get(1L).size());
    }
    @Test void realThreeColumnProjectionCountsAndDeduplicatesBothSources() {
        List<Object[]> attendance = List.of(
                new Object[]{1L, date("2026-09-07"), "On Leave"},
                new Object[]{1L, java.sql.Date.valueOf("2026-09-07"), "Leave"},
                new Object[]{1L, date("2026-09-09"), "Leave"},
                new Object[]{1L, date("2026-09-14"), "Leave"},
                new Object[]{2L, date("2026-09-08"), "Present"});
        var dates = PayrollLeaveDays.collect(List.of(leave("2026-09-07", "2026-09-08", true),
                leave("2026-09-08", "2026-09-08", true)), attendance, period());
        assertEquals(Set.of(date("2026-09-07"), date("2026-09-08"), date("2026-09-09")), dates.get(1L));
        assertFalse(dates.containsKey(2L));
        assertEquals(2, PayrollLeaveDays.collect(null, attendance, period()).get(1L).size());
    }
    @Test void pendingRejectedAndNonOverlappingRequestsAreNotPaid() {
        Leave pending = leave("2026-09-07", "2026-09-09", true); pending.setStatus("Pending");
        Leave rejected = leave("2026-09-07", "2026-09-09", true); rejected.setStatus("Rejected");
        assertTrue(PayrollLeaveDays.collect(List.of(pending, rejected, leave("2026-08-01", "2026-08-31", true)), List.of(), period()).isEmpty());
    }
    @Test void leaveHoursArePaidOnceAlongsideWorkedHours() {
        var leaves = PayrollLeaveDays.collect(List.of(leave("2026-09-07", "2026-09-08", true)), List.of(), period());
        Map<Long, List<LocalDate>> worked = new HashMap<>();
        worked.put(1L, new ArrayList<>(List.of(date("2026-09-07"), date("2026-09-08"), date("2026-09-09"))));
        Map<Long, Map<LocalDate, Double>> hours = new HashMap<>();
        hours.put(1L, new HashMap<>(Map.of(date("2026-09-07"), 8.0, date("2026-09-08"), 8.0, date("2026-09-09"), 8.0)));
        PayrollLeaveDays.excludeFromWorkedHours(leaves, worked, hours);
        assertEquals(List.of(date("2026-09-09")), worked.get(1L));
        assertEquals(Map.of(date("2026-09-09"), 8.0), hours.get(1L));
        Employee employee = new Employee(); employee.setId(1L); employee.setMinimumRate(10.0); employee.setBasicSalary(0.0);
        PayrollRecord record = new PayrollRecord();
        // PayrollService persists these inputs before invoking the calculator.
        record.setLeaveDays(leaves.get(1L).size());
        record.setLeaveHours(leaves.get(1L).size() * 8.0);
        new GhanaPayrollCalculator().calculatePayrollWithMultipliers(employee, period().getStartDate(), period().getEndDate(),
                8, 1, 0, leaves.get(1L).size(), leaves.get(1L).size() * 8, 0, 0, 0,
                new SettingsDTO(), true, record, new SystemSettings(), 0, 0, true);
        assertEquals(2, record.getLeaveDays());
        assertEquals(16.0, record.getLeaveHours());
        assertEquals(24.0, record.getTotalHours());
        assertEquals(240.0, record.getBasicSalary());
    }
}
