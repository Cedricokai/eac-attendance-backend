package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.*;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.settings.*;
import com.example.eac_backend_project.Service.settings.AttendanceCodeSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class HolidayAttendanceHoursTest {
    final AttendanceRepository repository = mock(AttendanceRepository.class);
    final SystemSettingsRepository settingsRepository = mock(SystemSettingsRepository.class);
    final AttendanceCodeSettingsService codes = mock(AttendanceCodeSettingsService.class);
    final AttendanceService service = new AttendanceService(repository, mock(EmployeeRepository.class),
            mock(OverviewRepository.class), mock(CategorySettingsRepository.class));
    final SystemSettings settings = new SystemSettings();
    final Attendance row = new Attendance();
    HolidayAttendanceHoursTest() {
        ReflectionTestUtils.setField(service, "systemSettingsRepository", settingsRepository);
        ReflectionTestUtils.setField(service, "holidayCodeSettingsService", codes);
        settings.setStandardWorkHours(8); settings.setHolidayOvertimeMultiplier(2.0);
        settings.setDefaultOvertimeMultiplier(1.5);
        when(settingsRepository.findAll()).thenReturn(List.of(settings));
        when(codes.getSettings("HP")).thenReturn(new AttendanceCodeSettings("HP", false, 8.0, true, "Holiday Present", true));
        row.setDate(LocalDate.of(2026, 9, 21)); row.setStatus("Present");
        when(repository.countHolidaysOnDate(row.getDate())).thenReturn(1L);
    }
    void apply() { ReflectionTestUtils.invokeMethod(service, "applyHolidayCredit", row); }
    @Test void workedHolidayUsesBothConfiguredMultipliersWithoutCompounding() {
        row.setCheckIn(LocalTime.of(8,0)); row.setCheckOut(LocalTime.of(17,0));
        apply(); apply();
        assertEquals("Holiday Present", row.getStatus());
        assertEquals(8, row.getMinimumHour()); assertEquals(16, row.getOvertime());
        settings.setHolidayOvertimeMultiplier(3.0); apply(); assertEquals(28, row.getOvertime());
    }
    @Test void unworkedHolidayHasEightNormalHoursAndNoPunches() {
        apply(); assertEquals("Holiday", row.getStatus()); assertEquals(8, row.getMinimumHour());
        assertEquals(0, row.getOvertime()); assertNull(row.getCheckIn()); assertNull(row.getCheckOut());
    }
    @Test void leaveAbsenceAndIncompleteShiftsArePreserved() {
        for (String status : List.of("On Leave", "Sick Leave", "Absent")) {
            row.setStatus(status); row.setMinimumHour(0); apply();
            assertEquals(status, row.getStatus()); assertEquals(0, row.getMinimumHour());
        }
        row.setStatus("Present"); row.setCheckIn(LocalTime.of(8,0)); apply();
        assertEquals(0, row.getMinimumHour());
    }
    @Test void creditedOvertimeIgnoresClockDurationAndJobMultiplierOnRepeatedCalculations() {
        row.setStatus("Holiday Present"); row.setOvertime(16.0);
        var ot = new Overtime(); ot.setAttendance(row); ot.setBaseHourlyRate(10.0);
        ot.setStartTime(LocalTime.of(17,0)); ot.setEndTime(LocalTime.of(8,0));
        var overtimeService = new OvertimeService(mock(OvertimeRepository.class), mock(EmployeeRepository.class), settingsRepository, null, null);
        for (int i=0;i<2;i++) {
            ReflectionTestUtils.invokeMethod(overtimeService, "calculateAndSetOvertimeHours", ot);
            ReflectionTestUtils.invokeMethod(overtimeService, "calculateOvertimePay", ot);
            assertEquals(16,ot.getOvertimeHours()); assertEquals(1,ot.getOvertimeMultiplier());
            assertTrue(ot.getMultiplierAppliedToHours()); assertEquals(0,ot.getCalculatedOvertimePay());
        }
    }
}
