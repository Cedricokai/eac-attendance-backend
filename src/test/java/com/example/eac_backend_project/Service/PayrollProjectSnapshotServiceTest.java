package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollProjectSnapshotServiceTest {
    final AttendanceRepository attendance = mock(AttendanceRepository.class);
    final TimesheetRepository timesheets = mock(TimesheetRepository.class);
    final ObjectMapper mapper = new ObjectMapper();
    final PayrollProjectSnapshotService service = new PayrollProjectSnapshotService(attendance,timesheets,mapper);
    final LocalDate date = LocalDate.of(2026,9,1);
    final Employee employee = new Employee();
    final PayrollRecord record = new PayrollRecord();
    final Attendance entry = new Attendance();
    final Timesheet sheet = new Timesheet();
    PayrollProjectSnapshotServiceTest() {
        var job = new Job(); job.setId(99L); job.setName("Current assignment");
        employee.setId(1L); employee.setJob(job);
        var period = new PayrollPeriod(); period.setStartDate(LocalDate.of(2026,8,26)); period.setEndDate(LocalDate.of(2026,9,25));
        record.setEmployee(employee); record.setPeriod(period); record.setAppliedEmployeeRate(10.0);
        record.setBasicSalary(80.0); record.setOvertimePay(0.0); record.setGrossSalary(80.0);
        entry.setEmployee(employee); entry.setDate(date);
        sheet.setEmployee(employee); sheet.setDate(date); sheet.setId(12L); sheet.setJob(job);
        when(attendance.findByEmployeeIdAndDateBetween(1L,period.getStartDate(),period.getEndDate())).thenReturn(List.of(entry));
        when(timesheets.findByEmployeeIdAndDateBetween(1L,period.getStartDate(),period.getEndDate())).thenReturn(List.of(sheet));
    }
    void capture() { service.capture(record,Map.of(date,8.0),Map.of(),8,8,true,d -> false,1); }
    @Test void savedProjectUsesAttendanceProvenanceAndDoesNotChangeWithLiveTimesheet() throws Exception {
        entry.setMethod("TIMESHEET_SYNC"); entry.setTimesheetId(12L); entry.setProjectId(7L); entry.setProjectName("Original project");
        capture();
        var saved=mapper.readTree(record.getProjectEarningsSnapshot());
        assertEquals(7L,saved.path("days").get(0).path("projectId").asLong());
        assertEquals("Original project",saved.path("days").get(0).path("projectName").asText());
        sheet.getJob().setName("Edited later");
        assertEquals(saved,mapper.readTree(record.getProjectEarningsSnapshot()));
    }
    @Test void biometricWorkDoesNotBorrowProjectFromCurrentAssignmentOrMatchingTimesheet() throws Exception {
        entry.setBiometric(true); capture();
        var day=mapper.readTree(record.getProjectEarningsSnapshot()).path("days").get(0);
        assertTrue(day.path("projectId").isNull()); assertEquals("Biometric",day.path("source").asText());
    }
    @Test void legacySyncedWorkCanCaptureMatchingTimesheetAtGenerationTime() throws Exception {
        entry.setMethod("TIMESHEET_SYNC"); capture();
        var day=mapper.readTree(record.getProjectEarningsSnapshot()).path("days").get(0);
        assertEquals(12L,day.path("timesheetId").asLong()); assertEquals(99L,day.path("projectId").asLong());
    }
}
