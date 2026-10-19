package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UnpaidLeaveTest {
    Leave leave() {
        var e=new Employee(); e.setId(1L);
        var l=new Leave(); l.setEmployee(e); l.setLeaveType("Unpaid Leave"); l.setStatus("Approved");
        l.setStartDate(LocalDate.of(2026,9,7)); l.setEndDate(LocalDate.of(2026,9,8)); return l;
    }
    @Test void unpaidNeverReturnsStoredPaidHours() {
        var l=leave(); l.setTotalLeaveHours(16.0); assertEquals(0.0,l.getTotalLeaveHours());
        l.calculateTotalLeaveHours(); assertEquals(0.0,l.getTotalLeaveHours());
        l.setLeaveType(" unpaid LEAVE "); assertTrue(l.isUnpaid());
        l.setLeaveType("Annual Leave"); l.calculateTotalLeaveHours(); assertEquals(16.0,l.getTotalLeaveHours());
    }
    @Test void unpaidRequestsOverrideLegacyPaidAttendanceWithoutRemovingPaidLeave() {
        var l=leave(); var paid=leave(); paid.setLeaveType("Annual Leave");
        paid.setStartDate(LocalDate.of(2026,9,9)); paid.setEndDate(paid.getStartDate());
        var period=new PayrollPeriod(); period.setStartDate(l.getStartDate()); period.setEndDate(paid.getEndDate());
        List<Object[]> rows=List.of(new Object[]{1L,l.getStartDate(),"On Leave"},new Object[]{1L,l.getEndDate(),"On Leave"});
        assertEquals(Set.of(paid.getStartDate()),PayrollLeaveDays.collect(List.of(l,paid),rows,period).get(1L));
        assertEquals(2,PayrollLeaveDays.collectUnpaid(List.of(l),period).get(1L).size());
    }
    @Test void approvalAndReportProjectionAwardZeroHours() {
        var attendance=mock(AttendanceRepository.class);var leaves=mock(LeaveRepository.class);var overtime=mock(OvertimeRepository.class);
        var l=leave(); var service=new ApprovedLeaveAttendanceService(attendance,leaves,overtime);
        when(attendance.findByEmployeeIdAndDateBetween(1L,l.getStartDate(),l.getEndDate())).thenReturn(List.of());
        service.synchronize(l);
        verify(attendance).saveAll(argThat(rows -> {for(var row:rows) if(row.getMinimumHour()!=0 || !"On Leave".equals(row.getStatus())) return false;return true;}));
        when(leaves.findApprovedForAttendanceReport(l.getStartDate(),l.getEndDate())).thenReturn(List.of(l));
        var report=service.includeApprovedLeave(List.of(),l.getStartDate(),l.getEndDate());
        assertEquals(2,report.size());assertTrue(report.stream().allMatch(row -> row.getMinimumHour()==0 && row.getOvertime()==0));
    }
}
