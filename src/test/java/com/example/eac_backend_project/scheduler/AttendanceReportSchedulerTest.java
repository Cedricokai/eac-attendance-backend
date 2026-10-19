package com.example.eac_backend_project.scheduler;
import com.example.eac_backend_project.Service.AttendanceService;
import com.example.eac_backend_project.Service.reports.AttendanceAutoReportService;
import com.example.eac_backend_project.entity.settings.AttendanceReportScheduleSettings;
import com.example.eac_backend_project.repository.settings.AttendanceReportScheduleSettingsRepository;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class AttendanceReportSchedulerTest {
    @Test void bothReportsRunIndependentlyOncePerDayAfterMarking() {
        var attendance=mock(AttendanceService.class);var repo=mock(AttendanceReportScheduleSettingsRepository.class);
        var reports=mock(AttendanceAutoReportService.class);
        var s=new AttendanceReportScheduleSettings();s.setEnabled(true);s.setMorningReportEnabled(true);
        s.setEveningReportEnabled(true);s.setMorningReportTime(LocalTime.MIN);s.setSendTime(LocalTime.MIN);
        when(repo.findFirstByOrderByIdAsc()).thenReturn(Optional.of(s));
        when(reports.sendScheduledReport(any(),anyBoolean())).thenReturn(true);
        var scheduler=new AttendanceReportScheduler(attendance,repo,reports);
        scheduler.runEveryMinute();scheduler.runEveryMinute();
        var date=LocalDate.now(ZoneId.of("Africa/Accra"));
        var order=inOrder(attendance,reports);
        order.verify(attendance).refreshPendingCheckInStatuses();order.verify(attendance).createAbsentRecordsForToday();
        order.verify(reports).sendScheduledReport(date,true);order.verify(reports).sendScheduledReport(date,false);
        verify(reports,times(1)).sendScheduledReport(date,true);verify(reports,times(1)).sendScheduledReport(date,false);
    }
    @Test void dueUsesSeparateDateAndCatchesMissedMinute() {
        var today=LocalDate.of(2026,9,10);
        assertTrue(AttendanceReportScheduler.due(LocalTime.of(10,0),LocalTime.of(9,0),today,null));
        assertFalse(AttendanceReportScheduler.due(LocalTime.of(8,0),LocalTime.of(9,0),today,null));
        assertFalse(AttendanceReportScheduler.due(LocalTime.of(10,0),LocalTime.of(9,0),today,today));
    }
}
