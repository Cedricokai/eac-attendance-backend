package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.*;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.repository.settings.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceAutoMarkTest {
    @Mock AttendanceRepository attendanceRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock OverviewRepository overviewRepository;
    @Mock CategorySettingsRepository categorySettingsRepository;
    @Mock LeaveRepository leaveRepository;
    @Mock SystemSettingsRepository systemSettingsRepository;
    @Mock OvertimeService overtimeService;
    @Mock OvertimeRepository overtimeRepository;
    @Mock AttendanceReportScheduleSettingsRepository scheduleRepository;
    @InjectMocks AttendanceService service;
    @org.junit.jupiter.api.BeforeEach void injectFields() {
        org.springframework.test.util.ReflectionTestUtils.setField(service,"scheduleRepository",scheduleRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"leaveRepository",leaveRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"systemSettingsRepository",systemSettingsRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"overtimeService",overtimeService);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"overtimeRepository",overtimeRepository);
    }
    final LocalDate today = LocalDate.now(ZoneId.of("Africa/Accra"));

    Employee employee(long id, String start) {
        var e = new Employee(); e.setId(id); e.setStartDate(today.minusDays(1));
        var c = new Category(); c.setId(id); c.setWorkStartTime(start); e.setCategory(c);
        return e;
    }
    @Test void noCutoffOrDisabledDoesNotMarkAnyone() {
        var settings = new AttendanceReportScheduleSettings();
        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        service.markAbsencesAt(today.atTime(10,0));
        settings.setAutoAbsentEnabled(false); settings.setAutoAbsentCutoffTime(LocalTime.MIN);
        service.markAbsencesAt(today.atTime(10,0));
        verify(attendanceRepository,never()).saveAll(anyList());
    }
    @Test void beforeCutoffDoesNotMarkAnyone() {
        var settings = new AttendanceReportScheduleSettings(); settings.setAutoAbsentCutoffTime(LocalTime.MAX);
        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        service.markAbsencesAt(today.atTime(10,0));
        verify(attendanceRepository,never()).saveAll(anyList());
    }
    @Test void marksTodayAfterCutoffAndSkipsLeaveAttendanceFutureStartsAndCategories() {
        var settings = new AttendanceReportScheduleSettings(); settings.setAutoAbsentCutoffTime(LocalTime.MIN);
        settings.setAutoAbsentSkipCategories(List.of(4L));
        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        var missing = employee(1,"00:00"); var present = employee(2,"00:00");
        var onLeave = employee(3,"00:00"); var excluded = employee(4,"00:00");
        var future = employee(5,"00:00"); future.setStartDate(today.plusDays(1));
        var laterShift = employee(6,"23:59:59.999999999");
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(missing,present,onLeave,excluded,future,laterShift));
        var existing = new Attendance(); existing.setEmployee(present);
        var records = new ArrayList<Attendance>(); records.add(existing);
        when(attendanceRepository.findByDate(today)).thenAnswer(invocation -> new ArrayList<>(records));
        var leave = new Leave(); leave.setEmployee(onLeave);
        when(leaveRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual("Approved",today)).thenReturn(List.of(leave));
        when(attendanceRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Attendance> saved = invocation.getArgument(0); records.addAll(saved); return saved;
        });
        service.markAbsencesAt(today.atTime(10,0)); service.markAbsencesAt(today.atTime(10,0));
        verify(attendanceRepository,times(1)).saveAll(anyList());
        var marked = records.get(1);
        assertEquals(missing,marked.getEmployee()); assertEquals(today,marked.getDate());
        assertEquals("Absent",marked.getStatus()); assertEquals(0,marked.getMinimumHour());
        assertEquals("AUTO_ABSENT",marked.getMethod());
    }
    @Test void mobileLateCheckInUpdatesExistingAbsenceImmediately() {
        var e = employee(1,"08:00"); var existing = new Attendance();
        existing.setId(42L); existing.setEmployee(e); existing.setDate(today); existing.setStatus("Absent");
        when(employeeRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(e));
        when(attendanceRepository.findByEmployeeIdAndDate(1L,today)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var incoming = new Attendance(); incoming.setEmployee(e); incoming.setDate(today); incoming.setCheckIn(LocalTime.of(8,1));
        var result = service.processMobileAttendance(incoming);
        assertEquals(42L,result.getId()); assertEquals("Late",result.getStatus());
        assertEquals(LocalTime.of(8,1),result.getCheckIn());
    }
    @Test void checkInExactlyAtStartIsNotLate() {
        var e = employee(1,"08:00");
        when(employeeRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(e));
        when(attendanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var incoming = new Attendance(); incoming.setEmployee(e); incoming.setDate(today); incoming.setCheckIn(LocalTime.of(8,0));
        assertEquals("Present", service.processMobileAttendance(incoming).getStatus());
    }

    @Test void nightCategoryWaitsForOwnCutoffAndUsesShiftDateAfterMidnight() {
        var settings = new AttendanceReportScheduleSettings(); settings.setAutoAbsentCutoffTime(LocalTime.of(9,0));
        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        var guard = employee(10,"18:00"); guard.getCategory().setOvernightShift(true);
        guard.getCategory().setAbsenceCutoffTime(LocalTime.of(20,0));
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(guard));
        service.markAbsencesAt(today.atTime(19,0));
        verify(attendanceRepository,never()).saveAll(anyList());
        service.markAbsencesAt(today.atTime(20,0));
        verify(attendanceRepository).saveAll(argThat(list -> {
            var a=list.iterator().next(); return a.getDate().equals(today) && "Night".equals(a.getShift());
        }));
        clearInvocations(attendanceRepository);
        service.markAbsencesAt(today.plusDays(1).atTime(6,0));
        verify(attendanceRepository).findByDate(today);
    }
    @Test void overnightCutoffCanBeNextMorning() {
        var settings = new AttendanceReportScheduleSettings();
        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        var guard=employee(10,"18:00"); guard.getCategory().setOvernightShift(true);
        guard.getCategory().setAbsenceCutoffTime(LocalTime.of(1,0));
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(guard));
        service.markAbsencesAt(today.atTime(23,0));
        verify(attendanceRepository,never()).saveAll(anyList());
        service.markAbsencesAt(today.plusDays(1).atTime(1,0));
        verify(attendanceRepository).findByDate(today);
        verify(attendanceRepository).saveAll(anyList());
    }
    @Test void biometricMorningCheckoutAndEveningCheckInShareOneRecord() {
        var guard=employee(10,"18:00"); guard.getCategory().setOvernightShift(true);
        guard.getCategory().setStandardRateHours(12);
        when(employeeRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(guard));
        when(attendanceRepository.save(any())).thenAnswer(i->i.getArgument(0));
        var in=new com.example.eac_backend_project.dto.BiometricRecordDTO();
        in.setEmployeeId(10L); in.setAction("SIGN ON"); in.setTimestamp(today.atTime(18,0));
        var out=new com.example.eac_backend_project.dto.BiometricRecordDTO();
        out.setEmployeeId(10L); out.setAction("SIGN OFF"); out.setTimestamp(today.plusDays(1).atTime(6,0));
        var result=service.processBiometricRecords(List.of(in,out));
        assertEquals(1,result.size()); assertEquals(today,result.get(0).getDate());
        assertEquals(12,result.get(0).getMinimumHour()); assertEquals("Present",result.get(0).getStatus());
        verify(attendanceRepository,times(1)).save(any());
    }

    @Test void categoryOffDaysAndEmployeeOverridesDetermineStatus() {
        var settings = new AttendanceReportScheduleSettings(); settings.setAutoAbsentCutoffTime(LocalTime.MIN);
        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        String day = String.valueOf(today.getDayOfWeek().getValue() % 7);
        var inherited = employee(1, "00:00"); inherited.getCategory().setWeeklyOffDays(day);
        var working = employee(2, "00:00"); working.getCategory().setWeeklyOffDays(day); working.setWeeklyOffDays("");
        var personalOff = employee(3, "00:00"); personalOff.setWeeklyOffDays(day);
        var reset = employee(4, "00:00"); reset.getCategory().setWeeklyOffDays(day); reset.setWeeklyOffDays("INHERIT");
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(inherited, working, personalOff, reset));
        service.markAbsencesAt(today.atTime(10, 0));
        verify(attendanceRepository).saveAll(argThat(list -> {
            var rows = new ArrayList<Attendance>(); list.forEach(rows::add);
            return rows.size() == 4 && "Off Day".equals(rows.get(0).getStatus())
                && "AUTO_OFF".equals(rows.get(0).getMethod()) && rows.get(0).getMinimumHour() == 0
                && "Absent".equals(rows.get(1).getStatus())
                && "Off Day".equals(rows.get(2).getStatus()) && "Off Day".equals(rows.get(3).getStatus());
        }));
    }

    @Test void offDaysDoNotReplaceAttendanceOrApprovedLeave() {
        var settings = new AttendanceReportScheduleSettings(); settings.setAutoAbsentCutoffTime(LocalTime.MIN);
        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        var present = employee(1, "00:00"); var leaveEmployee = employee(2, "00:00");
        String day = String.valueOf(today.getDayOfWeek().getValue() % 7);
        present.setWeeklyOffDays(day); leaveEmployee.setWeeklyOffDays(day);
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(present, leaveEmployee));
        var record = new Attendance(); record.setEmployee(present);
        when(attendanceRepository.findByDate(today)).thenReturn(List.of(record));
        var leave = new Leave(); leave.setEmployee(leaveEmployee);
        when(leaveRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual("Approved", today)).thenReturn(List.of(leave));
        service.markAbsencesAt(today.atTime(10, 0));
        verify(attendanceRepository, never()).saveAll(anyList());
    }

    @Test void overnightOffDayUsesShiftStartDate() {
        var settings = new AttendanceReportScheduleSettings();
        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        var guard = employee(1, "18:00"); guard.getCategory().setOvernightShift(true);
        guard.getCategory().setAbsenceCutoffTime(LocalTime.of(1, 0));
        guard.getCategory().setWeeklyOffDays(String.valueOf(today.getDayOfWeek().getValue() % 7));
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(guard));
        service.markAbsencesAt(today.plusDays(1).atTime(1, 0));
        verify(attendanceRepository).saveAll(argThat(list -> {
            var row = list.iterator().next();
            return today.equals(row.getDate()) && "Off Day".equals(row.getStatus());
        }));
    }

    @Test void rejectsInvalidWeeklyOffDays() {
        assertThrows(IllegalArgumentException.class, () -> new Employee().setWeeklyOffDays("7"));
        assertThrows(IllegalArgumentException.class, () -> new Category().setWeeklyOffDays("Monday"));
    }

    @Test void absenceRoutineDoesNotMarkConfiguredHolidays() {
        var settings = new AttendanceReportScheduleSettings(); settings.setAutoAbsentCutoffTime(LocalTime.MIN);
        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(employee(1, "00:00")));
        when(attendanceRepository.countHolidaysOnDate(today)).thenReturn(1L);
        service.markAbsencesAt(today.atTime(10, 0));
        verify(attendanceRepository, never()).saveAll(anyList());
    }

    @Test void lateCheckInReplacesAutomaticHolidayInsteadOfCreatingDuplicate() {
        var employee = employee(1, "08:00");
        var holiday = new Attendance(); holiday.setId(55L); holiday.setVersion(2);
        holiday.setEmployee(employee); holiday.setDate(today); holiday.setMethod("AUTO_HOLIDAY");
        holiday.setStatus("Holiday Present"); holiday.setMinimumHour(8);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndDate(1L, today)).thenReturn(Optional.of(holiday));
        when(attendanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var arrival = new Attendance(); arrival.setEmployee(employee); arrival.setDate(today);
        arrival.setCheckIn(LocalTime.of(8, 0)); arrival.setStatus("Present");
        var result = service.createAttendance(arrival);
        assertEquals(55L, result.getId()); assertEquals(2, result.getVersion());
        assertEquals(LocalTime.of(8, 0), result.getCheckIn());
        assertEquals(0, result.getMinimumHour());
    }

    @Test void biometricCheckInRemovesAutomaticCreditAndReusesRecord() {
        var employee = employee(1, "08:00");
        var holiday = new Attendance(); holiday.setId(55L); holiday.setEmployee(employee);
        holiday.setDate(today); holiday.setMethod("AUTO_HOLIDAY"); holiday.setMinimumHour(8);
        holiday.setStatus("Holiday Present");
        when(employeeRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndDate(1L, today)).thenReturn(Optional.of(holiday));
        when(attendanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var record = new com.example.eac_backend_project.dto.BiometricRecordDTO();
        record.setEmployeeId(1L); record.setAction("SIGN ON"); record.setTimestamp(today.atTime(8, 0));
        var result = service.processBiometricRecords(List.of(record)).get(0);
        assertEquals(55L, result.getId()); assertEquals(0, result.getMinimumHour());
        assertEquals("BIOMETRIC", result.getMethod());
    }

    @Test void mobileCheckInReplacesHolidayCreditWithActualAttendance() {
        var employee = employee(1, "08:00");
        var holiday = new Attendance(); holiday.setId(55L); holiday.setEmployee(employee);
        holiday.setDate(today); holiday.setMethod("AUTO_HOLIDAY"); holiday.setMinimumHour(8);
        holiday.setStatus("Holiday Present");
        when(employeeRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndDate(1L, today)).thenReturn(Optional.of(holiday));
        when(attendanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var record = new Attendance(); record.setEmployee(employee); record.setDate(today);
        record.setCheckIn(LocalTime.of(8, 0));
        var result = service.processMobileAttendance(record);
        assertEquals(55L, result.getId()); assertEquals(0, result.getMinimumHour());
        assertEquals("MOBILE", result.getMethod()); assertEquals("Present", result.getStatus());
    }
}
