package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.*;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.settings.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AttendanceOvertimeRecalculationTest {
    final AttendanceRepository attendance = mock(AttendanceRepository.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final OvertimeRepository overtimes = mock(OvertimeRepository.class);
    final SystemSettingsRepository settings = mock(SystemSettingsRepository.class);
    final AttendanceService service = new AttendanceService(attendance, employees, mock(OverviewRepository.class), mock(CategorySettingsRepository.class));
    final Attendance row = new Attendance();
    final Job job = new Job();
    final Overtime overtime = new Overtime();
    AttendanceOvertimeRecalculationTest() {
        job.setWorkStartTime(LocalTime.of(8,0)); job.setStandardWorkHours(8);
        job.setApplyOvertimeMultiplierToHours(true); job.setOvertimeMultiplier(1.5);
        var employee = new Employee(); employee.setId(1L); employee.setJob(job); employee.setCategory(new Category()); employee.setMinimumRate(10.0);
        row.setId(10L); row.setEmployee(employee); row.setDate(LocalDate.of(2026,9,22)); row.setStatus("Present");
        row.setCheckIn(LocalTime.of(8,0)); row.setCheckOut(LocalTime.of(17,0));
        overtime.setId(20L); overtime.setAttendance(row); overtime.setStatus("Approved");
        when(attendance.findById(10L)).thenReturn(Optional.of(row));
        when(attendance.save(any())).thenAnswer(i -> i.getArgument(0));
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        when(overtimes.findByAttendanceId(10L)).thenReturn(Optional.of(overtime));
        when(overtimes.save(any())).thenAnswer(i -> i.getArgument(0));
        var system = new SystemSettings(); system.setDefaultOvertimeMultiplier(1.5);
        when(settings.findAll()).thenReturn(List.of(system));
        ReflectionTestUtils.setField(service,"systemSettingsRepository",settings);
        ReflectionTestUtils.setField(service,"overtimeRepository",overtimes);
        ReflectionTestUtils.setField(service,"overtimeService",new OvertimeService(overtimes,employees,settings,null,null));
    }
    Attendance details() {
        var d = new Attendance(); d.setDate(row.getDate()); d.setStatus("Present");
        d.setCheckIn(row.getCheckIn()); d.setCheckOut(row.getCheckOut()); return d;
    }
    @Test void recalculationUpdatesSameOvertimeRecordAndClearsItWhenStandardIncreases() {
        service.updateAttendance(10L,details());
        assertEquals(8,row.getMinimumHour()); assertEquals(1,row.getOvertime()); assertEquals(1.5,overtime.getOvertimeHours());
        job.setStandardWorkHours(9); service.updateAttendance(10L,details());
        assertEquals(9,row.getMinimumHour()); assertEquals(0,row.getOvertime());
        assertEquals(0,overtime.getRawOvertimeHours()); assertEquals(0,overtime.getOvertimeHours()); assertEquals(0,overtime.getCalculatedOvertimePay());
        job.setStandardWorkHours(7); job.setOvertimeMultiplier(2.0); service.updateAttendance(10L,details());
        assertEquals(2,row.getOvertime()); assertEquals(4,overtime.getOvertimeHours()); assertEquals(20L,overtime.getId());
        service.updateAttendance(10L,details()); assertEquals(4,overtime.getOvertimeHours());
    }
    @Test void reassignedEmployeeUsesCurrentProjectHoursAndOvertime() {
        var project = new Job(); project.setName("New project"); project.setWorkStartTime(LocalTime.of(8,0));
        project.setStandardWorkHours(9); project.setMinimumOvertimeHours(2.0);
        project.setUseConfiguredAttendanceHours(true); project.setOvertimeMultiplier(2.0);
        var current = new Employee(); current.setId(1L); current.setJob(project);
        current.setCategory(new Category()); current.setMinimumRate(10.0);
        when(employees.findById(1L)).thenReturn(Optional.of(current));
        service.updateAttendance(10L,details());
        assertSame(project,row.getEmployee().getJob());
        assertEquals(9,row.getMinimumHour()); assertEquals(2,row.getOvertime());
        assertEquals(4,overtime.getOvertimeHours()); assertEquals(20L,overtime.getId());
    }

    @Test void configuredProjectHoursNeverReplaceWeekendRules() {
        job.setUseConfiguredAttendanceHours(true); job.setMinimumOvertimeHours(2.0); job.setOvertimeMultiplier(3.0);
        row.getEmployee().getCategory().setWeekendAttendanceAsOvertime(true);
        row.getEmployee().setWeeklyOffDays("0");
        row.setDate(LocalDate.of(2026,9,27));
        service.updateAttendance(10L,details());
        assertEquals(0,row.getMinimumHour()); assertEquals(9,row.getOvertime());
        assertEquals(9,overtime.getRawOvertimeHours()); assertEquals(13.5,overtime.getOvertimeHours());
        assertEquals(1.5,overtime.getOvertimeMultiplier());
    }

    @Test void recalculationPreservesAbsentAndLeaveEvenWithStoredPunches() {
        job.setUseConfiguredAttendanceHours(true); job.setMinimumOvertimeHours(2.0);
        for (String status : List.of("Absent", "On Leave", "Sick Leave", "Maternity Leave")) {
            double normal = "Absent".equals(status) ? 0.0 : 8.0;
            row.setStatus(status); row.setMinimumHour(normal); row.setOvertime(0.0);
            var request = details(); request.setStatus(status);
            service.updateAttendance(10L,request);
            assertEquals(status,row.getStatus()); assertEquals(normal,row.getMinimumHour());
            assertEquals(0,row.getOvertime()); assertEquals(0,overtime.getOvertimeHours());
        }
    }

    @Test void editingOnlyCheckoutUsesStoredCheckinAndRefreshesOvertime() {
        var d = details(); d.setCheckIn(null); d.setCheckOut(LocalTime.of(18,0));
        service.updateAttendance(10L,d);
        assertEquals(2,row.getOvertime()); assertEquals(3,overtime.getOvertimeHours());
    }
    @Test void overtimeFailureIsReportedRatherThanSilentlySucceeding() {
        when(overtimes.save(any())).thenThrow(new IllegalStateException("write failed"));
        assertThrows(IllegalStateException.class, () -> service.updateAttendance(10L,details()));
    }
}
