package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.attendance.Overtime;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.OvertimeRepository;
import com.example.eac_backend_project.repository.settings.SystemSettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class HolidayWorkedHoursFormulaTest {
    @Test void multipliesAllWorkedHoursThenSubtractsNormalHoursWithoutCompounding() {
        var settings = new SystemSettings();
        settings.setHolidayOvertimeMultiplier(2.0);
        settings.setDefaultOvertimeMultiplier(1.5);
        var repository = mock(SystemSettingsRepository.class);
        when(repository.findAll()).thenReturn(List.of(settings));
        var service = new OvertimeService(mock(OvertimeRepository.class),
                mock(EmployeeRepository.class), repository, null, null);
        var attendance = new Attendance();
        attendance.setStatus("Holiday Present");
        attendance.setMinimumHour(8.0);
        attendance.setOvertime(2.0);
        var overtime = new Overtime();
        overtime.setAttendance(attendance);
        for (int i = 0; i < 2; i++) {
            ReflectionTestUtils.invokeMethod(service, "calculateAndSetOvertimeHours", overtime);
            assertEquals(12.0, overtime.getOvertimeHours());
            assertEquals(2.0, overtime.getRawOvertimeHours());
            assertEquals(1.0, overtime.getOvertimeMultiplier());
            assertTrue(overtime.getMultiplierAppliedToHours());
            assertEquals(8.0, attendance.getMinimumHour());
        }
        // The default OT multiplier must not participate in HP calculations.
        settings.setDefaultOvertimeMultiplier(null);
        ReflectionTestUtils.invokeMethod(service, "calculateAndSetOvertimeHours", overtime);
        assertEquals(12.0, overtime.getOvertimeHours());
        settings.setHolidayOvertimeMultiplier(3.0);
        ReflectionTestUtils.invokeMethod(service, "calculateAndSetOvertimeHours", overtime);
        assertEquals(22.0, overtime.getOvertimeHours());
        attendance.setOvertime(0.0);
        ReflectionTestUtils.invokeMethod(service, "calculateAndSetOvertimeHours", overtime);
        assertEquals(16.0, overtime.getOvertimeHours());
    }
}