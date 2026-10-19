package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.dto.*;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.*;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollExclusionTest {
    PayrollPeriodRepository periods = mock(PayrollPeriodRepository.class);
    PayrollRecordRepository records = mock(PayrollRecordRepository.class);
    EmployeeRepository employees = mock(EmployeeRepository.class);
    PendingCreditRepository credits = mock(PendingCreditRepository.class);
    LoanDeductionService loans = mock(LoanDeductionService.class);
    SettingsService settings = mock(SettingsService.class);
    PayrollService service;
    Employee selected = new Employee();
    Employee other = new Employee();
    PayrollRecord old = new PayrollRecord();
    PayrollRecord untouched = new PayrollRecord();
    GeneratePayrollWithCreditsRequest request = new GeneratePayrollWithCreditsRequest();

    @BeforeEach void setup() {
        Category category = new Category(); category.setName("Staff");
        selected.setId(1L); selected.setCategory(category); selected.setMinimumRate(10.0); selected.setBasicSalary(0.0);
        other.setId(2L); other.setCategory(category);
        old.setId(101L); old.setEmployee(selected); old.setStatus("Generated"); old.setCreditAmount(30.0);
        untouched.setId(102L); untouched.setEmployee(other); untouched.setStatus("Processed"); untouched.setNetSalary(999.0);
        PayrollPeriod period = new PayrollPeriod(); period.setId(7L); period.setCategory("Staff");
        period.setStartDate(LocalDate.of(2026,9,1)); period.setEndDate(LocalDate.of(2026,9,30));
        old.setPeriod(period); untouched.setPeriod(period);
        when(periods.findById(7L)).thenReturn(Optional.of(period));
        when(records.findByPeriodId(7L)).thenReturn(List.of(old, untouched));
        when(employees.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(selected));
        when(employees.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(other));
        when(settings.getSystemSettings()).thenReturn(new SystemSettings());
        service = new PayrollService(periods, records, mock(AttendanceRepository.class), employees,
                new GhanaPayrollCalculator(), mock(OvertimeRepository.class), mock(LeaveRepository.class),
                new PayrollCalculator(), settings, credits, loans, mock(LoanDeductionHistoryRepository.class), mock(LoanRequestRepository.class));
        request.setPeriodId(7L); request.setEmployeeId(1L); request.setRateChoice("CURRENT");
        request.setTemporaryAllowances(Map.of(1L, Map.of("housingAllowance", 100.0), 2L, Map.of("housingAllowance", 200.0)));
        request.setSettings(new SettingsDTO(10.0,15.0,1.0,1.0,List.of(0,6),List.of(),8.0));
    }


    @Test void excludesEmployeeFromGeneratedRecords() {
        request.setEmployeeId(null); request.setExcludedEmployeeIds(List.of(2L));
        old.setCreditAmount(0.0); untouched.setStatus("Generated"); untouched.setCreditAmount(0.0);
        service.generatePayrollWithSelectedCredits(request);
        ArgumentCaptor<Iterable<PayrollRecord>> saved = ArgumentCaptor.forClass(Iterable.class);
        verify(records).saveAll(saved.capture());
        List<PayrollRecord> result = new ArrayList<>(); saved.getValue().forEach(result::add);
        assertEquals(1, result.size()); assertEquals(1L, result.get(0).getEmployee().getId());
    }
    @Test void rejectsExcludedEmployeePendingCreditsBeforeApplying() {
        request.setEmployeeId(null); request.setExcludedEmployeeIds(List.of(2L)); request.setSelectedCreditIds(List.of(9L));
        PendingCredit credit = new PendingCredit(); credit.setEmployee(other);
        when(credits.findAllById(List.of(9L))).thenReturn(List.of(credit));
        assertThrows(IllegalArgumentException.class, () -> service.generatePayrollWithSelectedCredits(request));
        verify(credits, never()).markCreditsAsApplied(anyList(), anyLong());
    }
    @Test void preservesExistingLoanHistoryWhenExclusionIsUnsafe() {
        request.setEmployeeId(null); request.setExcludedEmployeeIds(List.of(2L));
        untouched.setStatus("Generated"); untouched.setCreditAmount(0.0);
        when(loans.hasLoanDeductionsForEmployee(7L, 2L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.generatePayrollWithSelectedCredits(request));
        verify(records, never()).deleteAll(anyList());
    }
}
