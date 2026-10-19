package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PayrollProjectEarningsTest {
    final LocalDate first = LocalDate.of(2026, 9, 1), fifth = LocalDate.of(2026, 9, 5);
    PayrollRecord payroll() {
        var period = new PayrollPeriod(); period.setStartDate(LocalDate.of(2026,8,26)); period.setEndDate(LocalDate.of(2026,9,25));
        var record = new PayrollRecord(); record.setPeriod(period); record.setAppliedEmployeeRate(10.0);
        record.setBasicSalary(160.0); record.setOvertimePay(20.0); record.setGrossSalary(210.0); record.setNetSalary(190.0);
        return record;
    }
    PayrollProjectEarnings.Snapshot build(PayrollRecord record, Map<LocalDate,Double> daily, Map<LocalDate,Double> ot, double normal) {
        return PayrollProjectEarnings.build(record,daily,ot,Map.of(first,new PayrollProjectEarnings.Work(11L,7L,"Project X","Timesheet")),normal,8,true,date -> false,1);
    }
    @Test void includesProjectDaysWithinFullPeriodAndReconcilesGross() {
        var result = build(payroll(),Map.of(first,8.0,fifth,8.0),Map.of(first,2.0),16);
        assertEquals("2026-08-26",result.periodStart()); assertEquals("2026-09-25",result.periodEnd());
        assertEquals(100,result.days().get(0).totalEarnings()); assertEquals(7L,result.days().get(0).projectId());
        assertNull(result.days().get(1).projectId()); assertEquals(180,result.workEarnings());
        assertEquals(30,result.otherPeriodEarnings()); assertEquals(result.periodGross(),result.workEarnings()+result.otherPeriodEarnings());
    }
    @Test void excludesWorkOutsideInclusivePayrollBoundaries() {
        var start=LocalDate.of(2026,8,26); var end=LocalDate.of(2026,9,25);
        var result=build(payroll(),Map.of(start.minusDays(1),8.0,start,8.0,end,8.0,end.plusDays(1),8.0),Map.of(),16);
        assertEquals(List.of(start.toString(),end.toString()),result.days().stream().map(PayrollProjectEarnings.Day::date).toList());
    }
    @Test void monthlySalaryAndAllowancesStayOutsideProjectHourlyPay() {
        var record=payroll(); record.setIncludeBasicSalary(true); record.setAppliedMonthlySalary(160.0);
        var result=build(record,Map.of(first,8.0,fifth,8.0),Map.of(first,2.0),16);
        assertEquals(0,result.days().get(0).normalEarnings()); assertEquals(20,result.workEarnings()); assertEquals(190,result.otherPeriodEarnings());
    }
    @Test void leavePayIsNotAssignedToWorkedProjectDates() {
        var record=payroll(); record.setLeaveHours(8.0); record.setBasicSalary(240.0); record.setGrossSalary(290.0);
        var result=build(record,Map.of(first,8.0,fifth,8.0),Map.of(first,2.0),16);
        assertEquals(180,result.workEarnings()); assertEquals(110,result.otherPeriodEarnings());
    }
    @Test void effectiveRateDatesUseCapturedPayrollRate() {
        var record=payroll(); record.setAppliedEffectivePayrollRateFrom(fifth); record.setAppliedEffectivePayrollRateTo(fifth);
        record.setAppliedEffectivePayrollRate(20.0); record.setBasicSalary(240.0); record.setOvertimePay(0.0); record.setGrossSalary(240.0);
        var result=build(record,Map.of(first,8.0,fifth,8.0),Map.of(),16);
        assertEquals(80,result.days().get(0).normalEarnings()); assertEquals(160,result.days().get(1).normalEarnings());
    }
    @Test void periodCapMovesExcessHoursToOvertimeWithoutDuplicatingEarnings() {
        var record=payroll(); record.setBasicSalary(80.0); record.setOvertimePay(80.0); record.setGrossSalary(160.0);
        var result=build(record,Map.of(first,8.0,fifth,8.0),Map.of(),8);
        assertEquals(8,result.days().get(0).normalHours()); assertEquals(0,result.days().get(1).normalHours());
        assertEquals(8,result.days().get(1).overtimeHours()); assertEquals(160,result.workEarnings());
    }
    @Test void weekendConversionAndAllowanceModeReconcile() {
        var record=payroll(); record.setUseNonTaxableAllowances(true); record.setBasicSalary(0.0);
        record.setOvertimePay(240.0); record.setGrossSalary(270.0);
        var result=PayrollProjectEarnings.build(record,Map.of(first,8.0,fifth,8.0),Map.of(),Map.of(),8,8,true,date -> date.equals(fifth),2);
        assertEquals(16,result.days().get(1).overtimeHours()); assertEquals(240,result.workEarnings()); assertEquals(30,result.otherPeriodEarnings());
    }
    @Test void roundingAllocatesEveryCentAndNoDatedWorkLeavesPayUnallocated() {
        assertEquals(0.01,Arrays.stream(PayrollProjectEarnings.allocate(0.01,new double[]{1,1,1})).sum(),0.000001);
        var result=build(payroll(),Map.of(),Map.of(),0);
        assertTrue(result.days().isEmpty()); assertEquals(0,result.workEarnings()); assertEquals(210,result.otherPeriodEarnings());
    }
    @Test void undatedHoursAreNotLoadedOntoKnownProjectDays() {
        var record=payroll(); record.setOvertimeHours(4.0);
        var result=build(record,Map.of(first,8.0),Map.of(first,2.0),16);
        assertEquals(80,result.days().get(0).normalEarnings());
        assertEquals(10,result.days().get(0).overtimeEarnings());
        assertEquals(120,result.otherPeriodEarnings());
    }
}
