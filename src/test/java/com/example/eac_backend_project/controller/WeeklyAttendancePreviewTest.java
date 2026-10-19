package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.ApprovedLeaveAttendanceService;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.Category;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class WeeklyAttendancePreviewTest {
    final AttendanceRepository attendance = mock(AttendanceRepository.class);
    final LeaveRepository leaves = mock(LeaveRepository.class);
    final OvertimeRepository overtime = mock(OvertimeRepository.class);
    final AttendanceReportsController controller = new AttendanceReportsController(attendance, null, null, overtime,
            new ApprovedLeaveAttendanceService(attendance, leaves, overtime));
    final LocalDate start = LocalDate.of(2026,9,21);
    final LocalDate end = start.plusDays(6);
    final Employee employee = new Employee();
    WeeklyAttendancePreviewTest() {
        employee.setId(1L); employee.setCategory(new Category());
        var leave = new Leave(); leave.setId(3L); leave.setEmployee(employee);
        leave.setStartDate(start); leave.setEndDate(start.plusDays(1)); leave.setStatus("Approved");
        when(leaves.findApprovedForAttendanceReport(start,end)).thenReturn(List.of(leave,leave));
    }
    @Test void previewIncludesLeaveOnlyDaysAndReplacesAbsentWithoutDoubleCountingOrStaleOvertime() {
        var absent = new Attendance(); absent.setId(10L); absent.setEmployee(employee);
        absent.setDate(start); absent.setStatus("Absent");
        when(attendance.findAllForReportBetweenDates(start,end)).thenReturn(List.of(absent));
        var staleOvertime = new Overtime(); staleOvertime.setAttendance(absent); staleOvertime.setOvertimeHours(4.0);
        when(overtime.findByDateBetween(start,end)).thenReturn(List.of(staleOvertime));
        var body = (Map<?,?>) controller.previewAttendanceReport(start,end,null,null,null).getBody();
        assertNotNull(body); assertEquals(16.0,body.get("totalNormalHours"));
        assertEquals(0.0,body.get("totalOvertimeHours")); assertEquals(2,body.get("recordCount"));
        var rows = (List<?>) body.get("rows");
        for (Object item : rows) {
            var row = (Map<?,?>) item; assertEquals("On Leave",row.get("status"));
            assertEquals(8.0,row.get("normalHours")); assertNull(row.get("checkIn")); assertNull(row.get("checkOut"));
        }
        var total = (Map<?,?>) ((Collection<?>) body.get("totals")).iterator().next();
        assertEquals(16.0,total.get("totalNormalHours")); assertEquals("Absent",absent.getStatus());
        verify(attendance,never()).saveAll(any());
    }
    @Test void employeeFilterAlsoAppliesToLeaveOnlyRows() {
        var body = (Map<?,?>) controller.previewAttendanceReport(start,end,null,List.of(2L),null).getBody();
        assertNotNull(body); assertEquals(0,body.get("recordCount")); assertEquals(0.0,body.get("totalNormalHours"));
    }
}
