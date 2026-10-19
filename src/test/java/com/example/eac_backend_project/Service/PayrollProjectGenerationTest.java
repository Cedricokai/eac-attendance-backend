package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PayrollProjectGenerationTest extends PayrollEmployeeScopeTest {
    @Test void projectPayrollUsesScopedWorkAndNormalSalaryAllowancesDeductions() {
        PayrollPeriod p=periods.findById(7L).orElseThrow();p.setPeriodType("PROJECT");p.setCategory(null);p.setProjectId(42L);p.setProjectName("Project A");
        selected.setActive(true);selected.setBasicSalary(500.0);
        when(records.findByPeriodId(7L)).thenReturn(List.of());
        request.setTemporaryAllowances(Map.of(1L,Map.of("housingAllowance",100.0)));
        Job project=new Job();project.setId(42L);project.setName("Project A");project.setStandardWorkHours(8);
        JobRepository jobs=mock(JobRepository.class);when(jobs.findById(42L)).thenReturn(Optional.of(project));ReflectionTestUtils.setField(service,"projectPayrollJobs",jobs);
        ReflectionTestUtils.setField(service,"jobEmployeeRateRepository",mock(JobEmployeeRateRepository.class));
        AttendanceRepository attendance=(AttendanceRepository)ReflectionTestUtils.getField(service,"attendanceRepository");
        OvertimeRepository overtime=(OvertimeRepository)ReflectionTestUtils.getField(service,"overtimeRepository");
        TimesheetRepository sheets=mock(TimesheetRepository.class);
        Timesheet sheet=new Timesheet();sheet.setId(9L);sheet.setEmployee(selected);sheet.setJob(project);sheet.setDate(p.getStartDate());sheet.setAttendanceCode("P");sheet.setRegularHours(8);sheet.setOvertimeHours(2);
        sheet.setPayrollApprovalStatus("APPROVED");sheet.setPayrollApprovedBy("approver");sheet.setPayrollApprovedAt(LocalDateTime.now());sheet.setPayrollReviewFingerprint(sheet.payrollFingerprint());
        Attendance a=new Attendance();a.setEmployee(selected);a.setDate(p.getStartDate());a.setStatus("Present");a.setMinimumHour(8);a.setOvertime(2.0);a.setMethod("TIMESHEET_SYNC");a.setTimesheetId(9L);a.setProjectId(42L);a.setTimesheetApprovalFingerprint(sheet.payrollFingerprint());a.setTimesheetApprovedWork(a.approvedWorkSignature());
        when(attendance.findByDateBetween(any(),any())).thenReturn(List.of(a));when(sheets.findByDateBetween(any(),any())).thenReturn(List.of(sheet));
        Overtime o=new Overtime();o.setEmployee(selected);o.setDate(p.getStartDate());o.setStatus("Approved");o.setOvertimeHours(2.0);when(overtime.findApprovedOvertimeForPayrollPeriod(any(),any())).thenReturn(List.of(o));
        ReflectionTestUtils.setField(service,"payrollWorkScopeService",new PayrollWorkScopeService(attendance,sheets,overtime,records,new ObjectMapper()));
        service.generatePayrollWithSelectedCredits(request);
        ArgumentCaptor<Iterable<PayrollRecord>> saved=ArgumentCaptor.forClass(Iterable.class);verify(records).saveAll(saved.capture());PayrollRecord result=saved.getValue().iterator().next();
        assertEquals(8,result.getTotalHours());assertEquals(2,result.getOvertimeHours());assertEquals(20,result.getOvertimePay());
        assertTrue(result.getBasicSalary()>0);assertEquals(100,result.getRentAllowance());assertTrue(result.getSsnitEmployee()>0);
        assertTrue(result.getGrossSalary()>result.getNetSalary());assertEquals("[\"2026-09-01\"]",result.getPayrollWorkDates());
        verify(loans).processLoanDeductions(7L,1L);verify(jobs).findById(42L);
    }
    @Test void processedPaymentHistoryCannotBeCleared() {
        assertThrows(IllegalStateException.class,()->service.deletePayrollRecordsByPeriodId(7L));
        verifyNoInteractions(loans,credits);
        verify(records,never()).deleteByPeriodId(anyLong());
    }
    @Test void periodWithPayrollCannotBeDeletedUntilDraftRecordsAreCleared() {
        assertThrows(IllegalStateException.class,()->service.deletePayrollPeriod(7L));
        verify(records,never()).deleteByPeriodId(anyLong());
        verify(periods,never()).delete(any());
    }
}
