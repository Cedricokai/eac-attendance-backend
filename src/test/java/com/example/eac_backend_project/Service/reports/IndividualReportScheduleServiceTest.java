package com.example.eac_backend_project.Service.reports;

import com.example.eac_backend_project.Service.AttendanceReportExcelService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.settings.IndividualReportSchedule;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.settings.IndividualReportScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IndividualReportScheduleServiceTest {
    final IndividualReportScheduleRepository schedules = mock(IndividualReportScheduleRepository.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final IndividualReportAttendanceRepository attendance = mock(IndividualReportAttendanceRepository.class);
    final OvertimeRepository overtime = mock(OvertimeRepository.class);
    final AttendanceReportExcelService excel = mock(AttendanceReportExcelService.class);
    final EmailService email = mock(EmailService.class);
    final ObjectMapper mapper = new ObjectMapper();
    final IndividualReportScheduleService service = new IndividualReportScheduleService(schedules, employees, attendance, overtime, excel, email, mapper, new com.example.eac_backend_project.Service.ApprovedLeaveAttendanceService(mock(AttendanceRepository.class), mock(LeaveRepository.class), overtime));
    IndividualReportScheduleService.Settings settings(String frequency, int day, String time) {
        return new IndividualReportScheduleService.Settings(true, frequency, day, time, List.of(1L, 2L), true, List.of(2L), "Please check <hours>");
    }
    @Test void computesNextDailyDeliveryAndMidnightRollover() {
        var s = settings("DAILY", 1, "00:00");
        assertEquals(Instant.parse("2026-09-10T00:00:00Z"), IndividualReportScheduleService.nextRun(s, Instant.parse("2026-09-09T23:59:59Z")));
        assertEquals(Instant.parse("2026-09-11T00:00:00Z"), IndividualReportScheduleService.nextRun(s, Instant.parse("2026-09-10T00:00:00Z")));
    }
    @Test void weeklyDayAndElapsedTimeAdvanceCorrectly() {
        var s = settings("WEEKLY", 1, "08:00");
        assertEquals(Instant.parse("2026-09-14T08:00:00Z"), IndividualReportScheduleService.nextRun(s, Instant.parse("2026-09-09T10:00:00Z")));
        assertEquals(Instant.parse("2026-09-21T08:00:00Z"), IndividualReportScheduleService.nextRun(s, Instant.parse("2026-09-14T08:00:00Z")));
    }
    @Test void usesCompletedPeriodsAcrossYearBoundary() {
        assertEquals(new IndividualReportScheduleService.Period(LocalDate.parse("2025-12-31"), LocalDate.parse("2025-12-31")), IndividualReportScheduleService.period(settings("DAILY", 1, "08:00"), LocalDate.parse("2026-01-01")));
        assertEquals(new IndividualReportScheduleService.Period(LocalDate.parse("2025-12-22"), LocalDate.parse("2025-12-28")), IndividualReportScheduleService.period(settings("WEEKLY", 4, "08:00"), LocalDate.parse("2026-01-01")));
    }
    @Test void rejectsInvalidFrequencyTimeDayAndEmptyRecipients() {
        assertThrows(Exception.class, () -> IndividualReportScheduleService.validate(settings("MONTHLY", 1, "08:00")));
        assertThrows(Exception.class, () -> IndividualReportScheduleService.validate(settings("DAILY", 8, "08:00")));
        assertThrows(Exception.class, () -> IndividualReportScheduleService.validate(settings("DAILY", 1, "25:00")));
        assertThrows(Exception.class, () -> IndividualReportScheduleService.validate(new IndividualReportScheduleService.Settings(true, "DAILY", 1, "08:00", List.of(), true, List.of(), "")));
    }
    @Test void unclaimedBatchNeverSends() throws Exception {
        var row = new IndividualReportSchedule(); row.setEnabled(true); row.setVersion(0L);
        row.setNextRun(Instant.now().minusSeconds(60)); row.setConfiguration(mapper.writeValueAsString(settings("DAILY", 1, "08:00")));
        when(schedules.findById(1L)).thenReturn(Optional.of(row));
        when(schedules.claim(any(), any(), any())).thenReturn(0);
        service.tick();
        verifyNoInteractions(attendance, email);
    }
    @Test void pausedScheduleNeverSends() {
        when(schedules.findById(1L)).thenReturn(Optional.of(new IndividualReportSchedule()));
        service.tick(); verifyNoInteractions(email); verify(schedules, never()).claim(any(), any(), any());
    }
    @Test void claimedBatchUsesScheduledPeriodAndPersistsOutcome() throws Exception {
        var row = new IndividualReportSchedule(); row.setEnabled(true); row.setVersion(0L);
        row.setNextRun(Instant.parse("2026-01-01T08:00:00Z")); row.setConfiguration(mapper.writeValueAsString(settings("DAILY", 1, "08:00")));
        when(schedules.findById(1L)).thenReturn(Optional.of(row));
        when(schedules.claim(any(), any(), any())).thenReturn(1);
        service.tick();
        verify(attendance).findAllForReportBetweenDates(LocalDate.parse("2025-12-31"), LocalDate.parse("2025-12-31"));
        verify(schedules).recordResult(any(), eq("0 sent, 2 skipped (missing employee/email), 0 failed"));
    }
    @Test void isolatesEmployeeAttachmentsAndIncludesContactEvenAfterOneFailure() {
        var first = mock(Employee.class); when(first.getId()).thenReturn(1L); when(first.getEmail()).thenReturn("first@example.org");
        var second = mock(Employee.class); when(second.getId()).thenReturn(2L); when(second.getEmail()).thenReturn("second@example.org");
        when(employees.findById(1L)).thenReturn(Optional.of(first)); when(employees.findById(2L)).thenReturn(Optional.of(second));
        var a = mock(Attendance.class); when(a.getEmployee()).thenReturn(first);
        var b = mock(Attendance.class); when(b.getEmployee()).thenReturn(second);
        var p = new IndividualReportScheduleService.Period(LocalDate.parse("2026-09-08"), LocalDate.parse("2026-09-08"));
        when(attendance.findAllForReportBetweenDates(p.start(), p.end())).thenReturn(List.of(a, b));
        when(excel.buildAttendanceReportExcel(anyList(), any(), any(), anyMap(), anyBoolean())).thenReturn(new byte[]{1});
        doThrow(new RuntimeException("SMTP failure")).when(email).sendEmailWithExcelAttachment(eq("first@example.org"), any(), any(), any(), any());
        assertEquals("1 sent, 0 skipped (missing employee/email), 1 failed", service.deliver(settings("DAILY", 1, "08:00"), p));
        verify(excel).buildAttendanceReportExcel(eq(List.of(a)), eq(p.start()), eq(p.end()), anyMap(), eq(true));
        verify(excel).buildAttendanceReportExcel(eq(List.of(b)), eq(p.start()), eq(p.end()), anyMap(), eq(false));
        verify(email).sendEmailWithExcelAttachment(eq("second@example.org"), any(), argThat(body -> body.contains("dianna.mensah@eaconcept.org") && body.contains("0595092422") && body.contains("&lt;hours&gt;")), any(), any());
    }
    @Test void skipsMissingEmployeesAndInvalidEmails() {
        var employee = mock(Employee.class); when(employee.getEmail()).thenReturn("invalid");
        when(employees.findById(1L)).thenReturn(Optional.of(employee)); when(employees.findById(2L)).thenReturn(Optional.empty());
        var date = LocalDate.parse("2026-09-08");
        assertEquals("0 sent, 2 skipped (missing employee/email), 0 failed", service.deliver(settings("DAILY", 1, "08:00"), new IndividualReportScheduleService.Period(date, date)));
        verifyNoInteractions(email, excel);
    }

    @Test void onlyApprovedOvertimeIsPassedToIndividualReport() {
        var person = mock(Employee.class);
        when(person.getId()).thenReturn(1L); when(person.getEmail()).thenReturn("first@example.org");
        when(employees.findById(1L)).thenReturn(Optional.of(person));
        var a = new Attendance(); a.setId(10L); a.setEmployee(person); a.setOvertime(99.0);
        var b = new Attendance(); b.setId(11L); b.setEmployee(person); b.setOvertime(88.0);
        var date = LocalDate.parse("2026-09-08");
        when(attendance.findAllForReportBetweenDates(date, date)).thenReturn(List.of(a,b));
        var entries = new ArrayList<com.example.eac_backend_project.entity.attendance.Overtime>();
        for (String status : List.of("Approved", "Approved", "Pending", "Rejected", "Supervisor_Approved")) {
            var ot = new com.example.eac_backend_project.entity.attendance.Overtime();
            ot.setAttendance(a); ot.setStatus(status); ot.setOvertimeHours(2.0); entries.add(ot);
        }
        when(overtime.findByDateBetween(date, date)).thenReturn(entries);
        when(excel.buildAttendanceReportExcel(anyList(), any(), any(), anyMap(), anyBoolean())).thenReturn(new byte[]{1});
        service.deliver(settings("DAILY",1,"08:00"), new IndividualReportScheduleService.Period(date,date));
        verify(excel).buildAttendanceReportExcel(eq(List.of(a,b)), eq(date), eq(date), eq(Map.of(10L,4.0,11L,0.0)), eq(true));
    }
}
