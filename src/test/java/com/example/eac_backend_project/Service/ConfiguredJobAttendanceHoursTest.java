package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.settings.SystemSettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguredJobAttendanceHoursTest {
    final Job job = new Job();
    final Attendance row = new Attendance();
    ConfiguredJobAttendanceHoursTest() {
        job.setStandardWorkHours(8); job.setExpectedOvertimeHours(5.0);
        job.setUseConfiguredAttendanceHours(true); job.setOvertimeMultiplier(1.5);
        job.setEnforceMinimumOvertime(true); job.setMinimumOvertimeHours(2.0);
        var employee = new Employee(); employee.setJob(job);
        row.setEmployee(employee); row.setStatus("Present");
        row.setCheckIn(LocalTime.of(8,0)); row.setCheckOut(LocalTime.of(18,0));
    }
    @Test void replacesBothHigherAndLowerHoursAndKeepsPunches() {
        for (double hours : new double[]{1,8,12}) {
            row.setMinimumHour(hours); row.setOvertime(hours);
            ConfiguredJobAttendanceHours.apply(row);
            assertEquals(8,row.getMinimumHour()); assertEquals(2,row.getOvertime());
            assertEquals(LocalTime.of(8,0),row.getCheckIn()); assertEquals(LocalTime.of(18,0),row.getCheckOut());
        }
    }
    @Test void screenshotHoursUseStandardAndMinimumInsteadOfExpectedOvertime() {
        row.setCheckIn(LocalTime.of(7,13)); row.setCheckOut(LocalTime.of(17,17));
        row.setMinimumHour(7.78); row.setOvertime(2.28);
        job.setEnforceMinimumOvertime(false);
        ConfiguredJobAttendanceHours.apply(row);
        assertEquals(8,row.getMinimumHour()); assertEquals(2,row.getOvertime());
        assertEquals(LocalTime.of(7,13),row.getCheckIn()); assertEquals(LocalTime.of(17,17),row.getCheckOut());
    }
    @Test void disabledIncompleteAndNonWorkedRowsKeepTheirHours() {
        row.setMinimumHour(4); row.setOvertime(0.0);
        job.setUseConfiguredAttendanceHours(false); ConfiguredJobAttendanceHours.apply(row);
        assertEquals(4,row.getMinimumHour());
        job.setUseConfiguredAttendanceHours(true); row.setCheckOut(null); ConfiguredJobAttendanceHours.apply(row);
        assertEquals(4,row.getMinimumHour()); row.setCheckOut(LocalTime.of(18,0));
        for (String status : List.of("On Leave","Absent","Holiday","Sick","Holiday Present","HP","H","Sick Leave","Maternity Leave")) {
            row.setStatus(status); ConfiguredJobAttendanceHours.apply(row); assertEquals(4,row.getMinimumHour());
        }
    }
    @Test void overtimeUsesExactJobHoursAndMultiplierWithoutMinimumOrCompounding() {
        var settings = mock(SystemSettingsRepository.class);
        var service = new OvertimeService(mock(OvertimeRepository.class),mock(EmployeeRepository.class),settings,null,null);
        ConfiguredJobAttendanceHours.apply(row);
        var overtime = new Overtime(); overtime.setAttendance(row); overtime.setEmployee(row.getEmployee());
        overtime.setStartTime(LocalTime.of(16,0)); overtime.setEndTime(LocalTime.of(23,0));
        for (int i=0;i<2;i++) {
            ReflectionTestUtils.invokeMethod(service,"calculateAndSetOvertimeHours",overtime);
            assertEquals(2,overtime.getRawOvertimeHours()); assertEquals(3,overtime.getOvertimeHours());
        }
        job.setOvertimeMultiplier(null); var system = new SystemSettings(); system.setDefaultOvertimeMultiplier(2.0);
        when(settings.findAll()).thenReturn(List.of(system));
        ReflectionTestUtils.invokeMethod(service,"applyConfiguredOvertimeHours",overtime);
        assertEquals(4,overtime.getOvertimeHours());
    }
}
