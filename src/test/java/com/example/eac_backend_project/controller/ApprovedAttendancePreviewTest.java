package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.ApprovedLeaveAttendanceService;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ApprovedAttendancePreviewTest {
    @Test void approvedPreviewExcludesPendingHoursAndIncludesStandaloneApprovedOvertime() {
        var attendance = mock(AttendanceRepository.class);
        var overtime = mock(OvertimeRepository.class);
        var leave = mock(ApprovedLeaveAttendanceService.class);
        var day = LocalDate.of(2026,9,21);
        var employee = new Employee(); employee.setId(1L); employee.setActive(true);
        var row = new Attendance(); row.setId(10L); row.setEmployee(employee); row.setDate(day);
        row.setStatus("Present"); row.setMinimumHour(8); row.setOvertime(99.0);
        when(leave.includeApprovedLeave(anyList(),eq(day),eq(day))).thenReturn(List.of(row));
        var approved = new Overtime(); approved.setId(1L); approved.setEmployee(employee);
        approved.setAttendance(row); approved.setDate(day); approved.setStatus("Approved");
        approved.setRawOvertimeHours(2.0); approved.setOvertimeHours(3.0);
        var second = new Overtime(); second.setId(2L); second.setEmployee(employee);
        second.setAttendance(row); second.setDate(day); second.setStatus("Approved");
        second.setRawOvertimeHours(1.0); second.setOvertimeHours(1.5);
        var pending = new Overtime(); pending.setId(3L); pending.setEmployee(employee);
        pending.setAttendance(row); pending.setDate(day); pending.setStatus("Pending"); pending.setOvertimeHours(88.0);
        var other = new Employee(); other.setId(2L); other.setActive(true);
        var standalone = new Overtime(); standalone.setId(4L); standalone.setEmployee(other);
        standalone.setDate(day); standalone.setStatus("Approved"); standalone.setOvertimeHours(2.0);
        when(overtime.findApprovedOvertimeForPayrollPeriod(day,day)).thenReturn(List.of(approved,second,pending,standalone,approved));
        var controller = new AttendanceReportsController(attendance,null,null,overtime,leave);
        var body = (Map<?,?>) controller.previewAttendanceReport(day,day,null,null,null,true).getBody();
        assertEquals(true,body.get("approvedOnly")); assertEquals(2,body.get("recordCount"));
        assertEquals(8.0,body.get("totalNormalHours")); assertEquals(6.5,body.get("totalOvertimeHours"));
        var filtered = (Map<?,?>) controller.previewAttendanceReport(day,day,null,List.of(1L),null,true).getBody();
        assertEquals(1,filtered.get("recordCount")); assertEquals(4.5,filtered.get("totalOvertimeHours"));
        var excluded = (Map<?,?>) controller.previewAttendanceReport(day,day,null,List.of(1L),List.of(1L),true).getBody();
        assertEquals(0.0,excluded.get("totalOvertimeHours"));
        when(overtime.findApprovedOvertimeForPayrollPeriod(day,day)).thenReturn(List.of());
        var noApproved = (Map<?,?>) controller.previewAttendanceReport(day,day,null,null,null,true).getBody();
        assertEquals(0.0,noApproved.get("totalOvertimeHours"));
        assertEquals(0.0,noApproved.get("totalRawOvertimeHours"));
        assertEquals(8.0,noApproved.get("totalNormalHours"));
        var operational = (Map<?,?>) controller.previewAttendanceReport(day,day,null,null,null).getBody();
        assertEquals(false,operational.get("approvedOnly"));
        assertEquals(99.0,operational.get("totalOvertimeHours"));
        verify(attendance,never()).saveAll(any());
    }
}