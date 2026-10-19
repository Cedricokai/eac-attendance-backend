package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.*;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.settings.HolidayRepository;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class HolidayAttendanceServiceTest {
    final HolidayRepository holidays = mock(HolidayRepository.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final AttendanceRepository attendance = mock(AttendanceRepository.class);
    final HolidayAttendanceService service = new HolidayAttendanceService(holidays, employees, attendance);
    final LocalDate today = LocalDate.of(2026, 9, 21);

    @Test void creditsExactlyEightNormalHoursWithoutInventingCheckTimesOrOvertime() {
        var employee = new Employee(); employee.setId(1L);
        var category = new Category(); category.setStandardRateHours(12); employee.setCategory(category);
        when(holidays.lockHolidaysOn(today)).thenReturn(List.of(new Holiday()));
        when(employees.findEligibleForHoliday(today)).thenReturn(List.of(employee));
        service.fillDate(today, today);
        verify(attendance).saveAll(argThat(rows -> {
            var iterator = rows.iterator();
            var row = iterator.next();
            return !iterator.hasNext() && row.getEmployee() == employee && row.getDate().equals(today)
                && row.getMinimumHour() == 8 && row.getStandardHours() == 8 && row.getOvertime() == 0
                && row.getCheckIn() == null && row.getCheckOut() == null
                && "Holiday".equals(row.getStatus()) && "AUTO_HOLIDAY".equals(row.getMethod());
        }));
    }
    @Test void futureDatesNeverCreateAttendance() {
        service.fillDate(today.plusDays(1), today);
        verifyNoInteractions(holidays, employees, attendance);
    }
    @Test void ordinaryDatesNeverCreateAttendance() {
        service.fillDate(today, today);
        verifyNoInteractions(employees, attendance);
    }
    @Test void repeatRunsRecheckEligibilityAndDoNotResaveExistingRows() {
        var employee = new Employee(); employee.setId(1L);
        when(holidays.lockHolidaysOn(today)).thenReturn(List.of(new Holiday()));
        when(employees.findEligibleForHoliday(today)).thenReturn(List.of(employee), List.of());
        service.fillDate(today, today);
        service.fillDate(today, today);
        verify(attendance, times(1)).saveAll(anyList());
        verify(employees, times(2)).findEligibleForHoliday(today);
    }
    @Test void savedPastHolidayIsProcessedAndRecurringUsesCurrentYear() {
        var current = LocalDate.now(ZoneId.of("Africa/Accra"));
        var holiday = new Holiday(); holiday.setDate(current.minusDays(2));
        service.holidaySaved(holiday);
        verify(holidays).lockHolidaysOn(current.minusDays(2));
        holiday.setRecurring(true); holiday.setDate(LocalDate.of(2020, 1, 1));
        service.holidaySaved(holiday);
        verify(holidays).lockHolidaysOn(current.withDayOfYear(1));
    }
}
