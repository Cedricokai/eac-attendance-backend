package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectEffectivePayrollRateTest {
    PayrollEmployeeScopeTest fixture;
    JobEmployeeRate rate;
    JobEmployeeRateRepository repository;

    void setup() {
        fixture = new PayrollEmployeeScopeTest(); fixture.setup();
        fixture.selected.setMinimumRate(15.0);
        Job job = new Job(); job.setId(9L); fixture.selected.setJob(job);
        rate = new JobEmployeeRate(); rate.setJob(job); rate.setEmployee(fixture.selected);
        rate.setNormalHourRate(50.0); rate.setOvertimeRate(75.0);
        rate.setEffectivePayrollRate(20.0); rate.setEffectivePayrollRateFrom(LocalDate.of(2026,9,22));
        repository = mock(JobEmployeeRateRepository.class);
        when(repository.findByJobIdAndEmployeeId(9L,1L)).thenReturn(Optional.of(rate));
        ReflectionTestUtils.setField(fixture.service,"jobEmployeeRateRepository",repository);
        PayrollPeriod period = fixture.old.getPeriod();
        period.setStartDate(LocalDate.of(2026,8,26)); period.setEndDate(LocalDate.of(2026,9,25));
        AttendanceRepository attendance = (AttendanceRepository) ReflectionTestUtils.getField(fixture.service,"attendanceRepository");
        List<Object[]> dates = new ArrayList<>(), hours = new ArrayList<>();
        for (int day=21; day<=25; day++) {
            LocalDate date = LocalDate.of(2026,9,day);
            dates.add(new Object[]{1L,date});
            hours.add(new Object[]{1L,date,day == 21 ? 12.0 : day == 23 ? 4.0 : 8.0});
        }
        when(attendance.findAttendanceDatesByPeriod(period.getStartDate(),period.getEndDate())).thenReturn(dates);
        when(attendance.findDailyHoursByPeriod(period.getStartDate(),period.getEndDate())).thenReturn(hours);
    }

    @Test void actualDatesRespectDailyCapAndPartialDays() {
        setup(); fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        assertEquals(8.0,fixture.old.getNormalRateHours());
        assertEquals(28.0,fixture.old.getEffectiveRateHours());
        assertEquals(680.0,fixture.old.getBasicSalary());
        assertEquals(680.0,fixture.old.getPrecalculatedHourlyBasicPay());
        assertEquals(15.0,fixture.selected.getMinimumRate());
        assertEquals(50.0,rate.getNormalHourRate());
    }

    @Test void savedSnapshotSurvivesProjectRateEditsAndCurrentUsesNewRate() {
        setup(); fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        rate.setEffectivePayrollRate(30.0); fixture.selected.setMinimumRate(25.0);
        fixture.request.setRateChoice("SAVED");
        fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        assertEquals(680.0,fixture.old.getBasicSalary());
        fixture.request.setRateChoice("CURRENT");
        fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        assertEquals(1040.0,fixture.old.getBasicSalary());
    }

    @Test void inclusiveEndDateReturnsToNormalRate() {
        setup(); rate.setEffectivePayrollRateTo(LocalDate.of(2026,9,23));
        fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        assertEquals(12.0,fixture.old.getEffectiveRateHours());
        assertEquals(600.0,fixture.old.getBasicSalary());
    }

    @Test void missingStartPreservesLegacyCalculation() {
        setup(); rate.setEffectivePayrollRateFrom(null);
        fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        assertNull(fixture.old.getPrecalculatedHourlyBasicPay());
        assertEquals(540.0,fixture.old.getBasicSalary());
    }

    @Test void fixedMonthlySalaryIsNotReplaced() {
        setup(); fixture.selected.setBasicSalary(2000.0); fixture.request.setIncludeBasicSalary(true);
        fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        assertEquals(2000.0,fixture.old.getBasicSalary());
        assertNull(fixture.old.getPrecalculatedHourlyBasicPay());
    }

    @Test void legacyBillingSavePreservesPayrollFields() {
        setup(); when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        new JobEmployeeRateService(repository).saveRate(rate.getJob(),fixture.selected,60.0,90.0);
        assertEquals(20.0,rate.getEffectivePayrollRate());
        assertEquals(LocalDate.of(2026,9,22),rate.getEffectivePayrollRateFrom());
        assertEquals(60.0,rate.getNormalHourRate());
    }

    @Test void leaveAndOvertimeKeepNormalRateWithOverriddenWorkedPay() {
        setup(); PayrollRecord record = new PayrollRecord();
        new GhanaPayrollCalculator().calculatePayrollWithMultipliers(fixture.selected,
                LocalDate.of(2026,9,1),LocalDate.of(2026,9,30),36,5,2,1,8,0,0,0,
                fixture.request.getSettings(),true,record,new com.example.eac_backend_project.entity.settings.SystemSettings(),
                0,0,false,680.0);
        assertEquals(800.0,record.getBasicSalary());
        assertEquals(30.0,record.getOvertimePay());
        assertEquals(44.0,record.getSsnitEmployee());
    }

    @Test void dateBoundsAndZeroOverrideAreSupported() {
        setup(); rate.setEffectivePayrollRate(0.0);
        assertFalse(rate.appliesOn(LocalDate.of(2026,9,21)));
        assertTrue(rate.appliesOn(LocalDate.of(2026,9,22)));
        assertTrue(rate.appliesOn(LocalDate.of(2027,1,1)));
        fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        assertEquals(120.0,fixture.old.getBasicSalary());
    }

    @Test void periodCapAllocatesActualDaysBeforeConvertingExcessToOvertime() {
        setup(); PayrollPeriod period=fixture.old.getPeriod();
        period.setStartDate(LocalDate.of(2026,9,21)); period.setEndDate(LocalDate.of(2026,9,27));
        period.setConvertExcessToOvertime(true);
        AttendanceRepository attendance=(AttendanceRepository)ReflectionTestUtils.getField(fixture.service,"attendanceRepository");
        List<Object[]> dates=new ArrayList<>(), hours=new ArrayList<>();
        for(int day=21;day<=27;day++) {
            LocalDate date=LocalDate.of(2026,9,day);
            dates.add(new Object[]{1L,date}); hours.add(new Object[]{1L,date,8.0});
        }
        when(attendance.findAttendanceDatesByPeriod(period.getStartDate(),period.getEndDate())).thenReturn(dates);
        when(attendance.findDailyHoursByPeriod(period.getStartDate(),period.getEndDate())).thenReturn(hours);
        fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        assertEquals(32.0,fixture.old.getEffectiveRateHours());
        assertEquals(760.0,fixture.old.getBasicSalary());
        assertEquals(16.0,fixture.old.getOvertimeHours());
        assertEquals(240.0,fixture.old.getOvertimePay());
    }

    @Test void savedAbsenceDoesNotPickUpNewProjectOverride() {
        setup(); rate.setEffectivePayrollRate(null);
        fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        rate.setEffectivePayrollRate(20.0); fixture.request.setRateChoice("SAVED");
        fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        assertEquals(540.0,fixture.old.getBasicSalary());
        assertNull(fixture.old.getPrecalculatedHourlyBasicPay());
    }

    @Test void rejectsInvalidDatesBeforeSavingBillingAndAllowsClearingOverride() {
        setup(); JobEmployeeRateService service=new JobEmployeeRateService(repository);
        assertThrows(IllegalArgumentException.class,()->service.saveRate(rate.getJob(),fixture.selected,50.0,75.0,20.0,
                LocalDate.of(2026,9,22),LocalDate.of(2026,9,21)));
        verify(repository,never()).save(any());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.saveRate(rate.getJob(),fixture.selected,50.0,75.0,null,null,null);
        assertNull(rate.getEffectivePayrollRate()); assertEquals(50.0,rate.getNormalHourRate());
    }
}
