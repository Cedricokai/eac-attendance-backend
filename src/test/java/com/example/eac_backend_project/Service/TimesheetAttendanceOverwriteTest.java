package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.Category;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.settings.CategorySettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class TimesheetAttendanceOverwriteTest {
    final AttendanceRepository attendance = mock(AttendanceRepository.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final OvertimeRepository overtime = mock(OvertimeRepository.class);
    final AttendanceService service = new AttendanceService(attendance, employees,
        mock(OverviewRepository.class), mock(CategorySettingsRepository.class));
    final LocalDate date = LocalDate.of(2026,9,21);
    final Employee employee = new Employee();
    TimesheetAttendanceOverwriteTest() {
        employee.setId(1L); employee.setCategory(new Category());
        ReflectionTestUtils.setField(service, "systemSettingsRepository", mock(com.example.eac_backend_project.repository.settings.SystemSettingsRepository.class));
        ReflectionTestUtils.setField(service, "overtimeRepository", overtime);
        var overtimeService = mock(OvertimeService.class);
        when(overtimeService.createOvertime(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "overtimeService", overtimeService);
        when(employees.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(employee));
        when(attendance.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    Attendance incoming() {
        var row = new Attendance(); row.setEmployee(employee); row.setDate(date);
        row.setStatus("Present"); row.setMinimumHour(8); row.setOvertime(0.0);
        return row;
    }
    Attendance existing(String status) {
        var row = incoming(); row.setId(10L); row.setVersion(3); row.setStatus(status);
        when(attendance.existsByEmployeeIdAndDate(1L,date)).thenReturn(true);
        when(attendance.findByEmployeeIdAndDate(1L,date)).thenReturn(Optional.of(row));
        return row;
    }
    @Test void syncReplacesExistingStatusesAndRepeatedSyncKeepsSameId() {
        for (String status : List.of("Absent", "Off Day", "Present", "Holiday Present")) {
            existing(status);
            var saved = service.createBatchAttendances(List.of(incoming()), true).get(0);
            assertEquals(10L,saved.getId()); assertEquals(3,saved.getVersion());
            assertEquals("Present",saved.getStatus()); assertEquals(8,saved.getMinimumHour());
            assertEquals("TIMESHEET_SYNC",saved.getMethod());
            assertEquals(10L,service.createBatchAttendances(List.of(incoming()),true).get(0).getId());
        }
    }
    @Test void normalBatchStillSkipsExistingRows() {
        existing("Absent");
        assertTrue(service.createBatchAttendances(List.of(incoming())).isEmpty());
        verify(attendance,never()).save(any());
    }
    @Test void syncCreatesMissingAttendance() {
        assertEquals(1,service.createBatchAttendances(List.of(incoming()),true).size());
        verify(attendance).save(any());
    }
    @Test void replacingHoursWithZeroOvertimeClearsLinkedOvertimeAmounts() {
        existing("Present");
        var old = new Overtime(); old.setOvertimeHours(3.0); old.setRawOvertimeHours(2.0);
        old.setCalculatedOvertimePay(90.0);
        when(overtime.findByAttendanceId(10L)).thenReturn(Optional.of(old));
        service.createBatchAttendances(List.of(incoming()),true);
        assertEquals(0,old.getOvertimeHours()); assertEquals(0,old.getRawOvertimeHours());
        assertEquals(0,old.getCalculatedOvertimePay()); verify(overtime).save(old);
    }
    @Test void syncPreservesAuthoritativeTimesheetProjectAndChecksEmployeeAndDate() {
        var sheets = mock(TimesheetRepository.class);
        ReflectionTestUtils.setField(service, "payrollTimesheetRepository", sheets);
        var job = new Job(); job.setId(7L); job.setName("September project");
        var sheet = new Timesheet(); sheet.setId(12L); sheet.setEmployee(employee); sheet.setDate(date); sheet.setJob(job); sheet.setRegularHours(8); sheet.setAttendanceCode("P");
        sheet.setPayrollApprovalStatus("APPROVED"); sheet.setPayrollApprovedBy("reviewer"); sheet.setPayrollApprovedAt(java.time.LocalDateTime.now()); sheet.setPayrollReviewFingerprint(sheet.payrollFingerprint());
        when(sheets.findById(12L)).thenReturn(Optional.of(sheet));
        var row = incoming(); row.setTimesheetId(12L); row.setProjectId(999L);
        var saved = service.createBatchAttendances(List.of(row), true).get(0);
        assertEquals(12L,saved.getTimesheetId()); assertEquals(7L,saved.getProjectId()); assertEquals("September project",saved.getProjectName());
        sheet.setDate(date.plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.createBatchAttendances(List.of(row), true));
    }
    @Test void syncPreservesWorkedHoursWithoutPunchesOnConfiguredOffDay() {
        employee.setWeeklyOffDays("1");
        existing("Off Day");
        var row = incoming(); row.setOvertime(2.0);
        var saved = service.createBatchAttendances(List.of(row), true).get(0);
        assertEquals("Present", saved.getStatus());
        assertEquals(8, saved.getMinimumHour());
        assertEquals(2.0, saved.getOvertime());
    }
    @Test void unworkedTimesheetStillBecomesOffDay() {
        employee.setWeeklyOffDays("1");
        var row = incoming(); row.setMinimumHour(0);
        var saved = service.createBatchAttendances(List.of(row), true).get(0);
        assertEquals("Off Day", saved.getStatus());
        assertEquals(0, saved.getMinimumHour());
    }
    @Test void unapprovedProjectTimesheetCannotSync() {
        var sheets = mock(TimesheetRepository.class);
        ReflectionTestUtils.setField(service,"payrollTimesheetRepository",sheets);
        var sheet=new Timesheet();sheet.setId(12L);sheet.setEmployee(employee);sheet.setDate(date);sheet.setJob(new Job());
        when(sheets.findById(12L)).thenReturn(Optional.of(sheet));
        var row=incoming();row.setTimesheetId(12L);
        assertThrows(IllegalArgumentException.class,()->service.createBatchAttendances(List.of(row),true));
        verify(attendance,never()).save(any());
    }
    @Test void approvedSyncUsesAuthoritativeHoursInsteadOfClientHours() {
        var sheets = mock(TimesheetRepository.class);
        ReflectionTestUtils.setField(service,"payrollTimesheetRepository",sheets);
        var sheet=new Timesheet();sheet.setId(12L);sheet.setEmployee(employee);sheet.setDate(date);sheet.setJob(new Job());sheet.setRegularHours(8);sheet.setOvertimeHours(4);sheet.setAttendanceCode("P");
        sheet.setPayrollApprovalStatus("APPROVED");sheet.setPayrollApprovedBy("reviewer");sheet.setPayrollApprovedAt(java.time.LocalDateTime.now());sheet.setPayrollReviewFingerprint(sheet.payrollFingerprint());
        when(sheets.findById(12L)).thenReturn(Optional.of(sheet));
        var row=incoming();row.setTimesheetId(12L);
        row.setMinimumHour(99);row.setOvertime(99.0);
        var result=service.createBatchAttendances(List.of(row),true).get(0);
        assertEquals(8,result.getMinimumHour());assertEquals(4,result.getOvertime());assertEquals(sheet.payrollFingerprint(),result.getTimesheetApprovalFingerprint());
    }

    @Test void projectDailyTotalRuleAdjustsSyncedHoursWithoutChangingTimesheet() {
        var sheets = mock(TimesheetRepository.class);
        ReflectionTestUtils.setField(service, "payrollTimesheetRepository", sheets);
        var job = new Job(); job.setId(7L); job.setName("Ten hour project");
        job.setDailyHoursTopUpEnabled(true); job.setDailyHoursTopUpThreshold(9.3); job.setDailyHoursTopUpTarget(10.0);
        var sheet = new Timesheet(); sheet.setId(12L); sheet.setEmployee(employee); sheet.setDate(date); sheet.setJob(job);
        sheet.setRegularHours(8); sheet.setOvertimeHours(1.5); sheet.setAttendanceCode("P");
        sheet.setPayrollApprovalStatus("APPROVED"); sheet.setPayrollApprovedBy("reviewer");
        sheet.setPayrollApprovedAt(java.time.LocalDateTime.now()); sheet.setPayrollReviewFingerprint(sheet.payrollFingerprint());
        when(sheets.findById(12L)).thenReturn(Optional.of(sheet));
        var row = incoming(); row.setTimesheetId(12L);
        var saved = service.createBatchAttendances(List.of(row),true).get(0);
        assertEquals(8,saved.getMinimumHour()); assertEquals(2,saved.getOvertime());
        assertEquals(1.5,sheet.getOvertimeHours());
        assertEquals(sheet.payrollFingerprint(),saved.getTimesheetApprovalFingerprint());
        assertEquals(saved.approvedWorkSignature(),saved.getTimesheetApprovedWork());
    }
}
