package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.Category;
import com.example.eac_backend_project.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollWorkScopeServiceTest {
    final AttendanceRepository attendance=mock(AttendanceRepository.class);
    final TimesheetRepository sheets=mock(TimesheetRepository.class);
    final OvertimeRepository overtime=mock(OvertimeRepository.class);
    final PayrollRecordRepository records=mock(PayrollRecordRepository.class);
    final PayrollWorkScopeService service=new PayrollWorkScopeService(attendance,sheets,overtime,records,new ObjectMapper());
    final LocalDate date=LocalDate.of(2026,9,1);
    final Employee employee=new Employee(); final Job job=new Job();
    PayrollWorkScopeServiceTest(){employee.setId(1L);employee.setActive(true);Category category=new Category();category.setName("Staff");employee.setCategory(category);job.setId(7L);job.setName("Project A");}
    PayrollPeriod period(boolean project){PayrollPeriod p=new PayrollPeriod();p.setId(2L);p.setStartDate(date);p.setEndDate(date.plusDays(4));p.setCategory("Staff");if(project){p.setPeriodType("PROJECT");p.setProjectId(7L);p.setCategory(null);}return p;}
    Timesheet sheet(){Timesheet t=new Timesheet();t.setId(10L);t.setEmployee(employee);t.setDate(date);t.setJob(job);t.setRegularHours(8);t.setOvertimeHours(2);t.setAttendanceCode("P");t.setPayrollApprovalStatus("APPROVED");t.setPayrollApprovedBy("approver");t.setPayrollApprovedAt(LocalDateTime.now());t.setPayrollReviewFingerprint(t.payrollFingerprint());return t;}
    Attendance synced(Timesheet t){Attendance a=new Attendance();a.setEmployee(employee);a.setDate(date);a.setMethod("TIMESHEET_SYNC");a.setTimesheetId(t.getId());a.setProjectId(t.getJob().getId());a.setMinimumHour(8);a.setOvertime(2.0);a.setStatus("Present");a.setTimesheetApprovalFingerprint(t.payrollFingerprint());a.setTimesheetApprovedWork(a.approvedWorkSignature());return a;}
    void setup(Timesheet t,Attendance a){when(sheets.findByDateBetween(any(),any())).thenReturn(List.of(t));when(attendance.findByDateBetween(any(),any())).thenReturn(List.of(a));Overtime o=new Overtime();o.setEmployee(employee);o.setDate(date);o.setStatus("Approved");o.setOvertimeHours(2.0);when(overtime.findApprovedOvertimeForPayrollPeriod(any(),any())).thenReturn(List.of(o));}
    @Test void selectsOnlyApprovedSyncedProjectWorkAndApprovedOT(){Timesheet t=sheet();setup(t,synced(t));var scope=service.prepare(period(true));assertEquals(8,scope.daily.get(1L).get(date));assertEquals(2,scope.approvedOvertime.get(0).getOvertimeHours());assertEquals(Set.of(date),scope.workDates.get(1L));assertEquals(1,scope.normalSummary().size());}
    @Test void rejectsPendingStaleAndOtherProjectWork(){Timesheet t=sheet();Attendance a=synced(t);setup(t,a);t.setPayrollApprovalStatus("SUBMITTED");assertTrue(service.prepare(period(true)).workDates.isEmpty());t.setPayrollApprovalStatus("APPROVED");a.setMinimumHour(4);assertTrue(service.prepare(period(true)).workDates.isEmpty());a=synced(t);setup(t,a);PayrollPeriod p=period(true);p.setProjectId(8L);assertTrue(service.prepare(p).workDates.isEmpty());}
    @Test void categoryAndProjectBothExcludeWorkAlreadyInAnotherRun(){Timesheet t=sheet();setup(t,synced(t));PayrollRecord previous=new PayrollRecord();previous.setEmployee(employee);previous.setPeriod(period(true));previous.setPayrollWorkDates("[\"2026-09-01\"]");when(records.findWorkReservations(any(),any(),any())).thenReturn(List.of(previous));for(boolean project:List.of(true,false)){var scope=service.prepare(period(project));assertTrue(scope.workDates.isEmpty());assertEquals(1,scope.excludedDays);}}
    @Test void legacyRunsProtectDatesAndRegenerationIgnoresOwnPeriodAtRepositoryBoundary(){Timesheet t=sheet();setup(t,synced(t));PayrollRecord previous=new PayrollRecord();previous.setEmployee(employee);previous.setPeriod(period(false));when(records.findWorkReservations(eq(2L),any(),any())).thenReturn(List.of(previous));assertTrue(service.prepare(period(true)).workDates.isEmpty());when(records.findWorkReservations(eq(2L),any(),any())).thenReturn(List.of());assertEquals(Set.of(date),service.prepare(period(true)).workDates.get(1L));}
    @Test void categoryKeepsUnpaidAttendanceButProjectDoesNotInferCurrentAssignment(){Attendance a=new Attendance();a.setEmployee(employee);a.setDate(date);a.setStatus("Present");a.setMinimumHour(8);when(attendance.findByDateBetween(any(),any())).thenReturn(List.of(a));assertEquals(8,service.prepare(period(false)).daily.get(1L).get(date));assertTrue(service.prepare(period(true)).workDates.isEmpty());}
    @Test void snapshotIsStableAndParseable(){assertEquals("[\"2026-09-01\",\"2026-09-02\"]",service.snapshot(Set.of(date.plusDays(1),date)));}
}
