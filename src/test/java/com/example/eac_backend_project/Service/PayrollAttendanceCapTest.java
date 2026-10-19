package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.PayrollPeriod;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.OvertimeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollAttendanceCapTest {
    void verifyCap(boolean enabled, double expectedNormal, double expectedOvertime) {
        PayrollEmployeeScopeTest fixture = new PayrollEmployeeScopeTest(); fixture.setup();
        PayrollPeriod period = fixture.old.getPeriod();
        period.setStartDate(LocalDate.of(2026,7,1)); period.setEndDate(LocalDate.of(2026,7,31));
        period.setConvertExcessToOvertime(enabled);
        AttendanceRepository attendance = (AttendanceRepository) ReflectionTestUtils.getField(fixture.service,"attendanceRepository");
        OvertimeRepository overtime = (OvertimeRepository) ReflectionTestUtils.getField(fixture.service,"overtimeRepository");
        List<Object[]> dates = new ArrayList<>(), hours = new ArrayList<>();
        for (int day=1; day<=25; day++) {
            dates.add(new Object[]{1L,LocalDate.of(2026,7,day)});
            hours.add(new Object[]{1L,LocalDate.of(2026,7,day),8.0});
        }
        when(attendance.findAttendanceDatesByPeriod(period.getStartDate(),period.getEndDate())).thenReturn(dates);
        when(attendance.findDailyHoursByPeriod(period.getStartDate(),period.getEndDate())).thenReturn(hours);
        when(overtime.summarizeApprovedOvertimeBetweenDatesForCategory(period.getStartDate(),period.getEndDate(),"Staff"))
                .thenReturn(Collections.singletonList(new Object[]{1L,40.0,400.0,true}));
        fixture.service.generatePayrollWithSelectedCredits(fixture.request);
        assertEquals(expectedNormal,fixture.old.getTotalHours());
        assertEquals(expectedOvertime,fixture.old.getOvertimeHours());
        assertEquals(23,fixture.old.getTotalDaysInPeriod());
        assertEquals(enabled ? 23 : 25,fixture.old.getWorkingDays());
        assertEquals(999,fixture.untouched.getNetSalary());
    }
    @Test void checkedMovesSixteenAttendanceHoursToExistingFortyOvertimeHours() { verifyCap(true,184,56); }
    @Test void uncheckedPreservesTwoHundredNormalHoursAndFortyOvertimeHours() { verifyCap(false,200,40); }
}
