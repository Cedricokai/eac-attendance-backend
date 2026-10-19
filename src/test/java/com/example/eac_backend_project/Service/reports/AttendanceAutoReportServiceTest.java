package com.example.eac_backend_project.Service.reports;
import com.example.eac_backend_project.Service.AttendanceReportExcelService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.settings.AttendanceReportScheduleSettings;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.settings.AttendanceReportScheduleSettingsRepository;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class AttendanceAutoReportServiceTest {
    final AttendanceRepository attendance=mock(AttendanceRepository.class);
    final AttendanceReportExcelService excel=mock(AttendanceReportExcelService.class);
    final EmailService email=mock(EmailService.class);
    final AttendanceReportScheduleSettingsRepository settings=mock(AttendanceReportScheduleSettingsRepository.class);
    final AttendanceAutoReportService service=new AttendanceAutoReportService(attendance,excel,email,settings);
    final LocalDate day=LocalDate.of(2026,9,10);
    AttendanceReportScheduleSettings enabled() {
        var s=new AttendanceReportScheduleSettings();s.setEnabled(true);s.setRecipientEmails("hr@example.org");s.setAttachExcel(false);
        when(settings.findAll()).thenReturn(List.of(s));return s;
    }
    @Test void morningSelectsExceptionsAndReportsPendingCheckout() {
        enabled(); var late=new Attendance();late.setStatus("Late");late.setCheckIn(LocalTime.of(9,1));
        var absent=new Attendance();absent.setStatus("Absent");
        when(attendance.findByDate(day)).thenReturn(List.of(late,absent));
        assertTrue(service.sendScheduledReport(day,true));
        verify(attendance,never()).findLateAndAbsentByDate(any());
        verify(email).sendCustomEmail(eq("hr@example.org"),contains("Morning"),argThat(html->html.contains("Late: 1")&&html.contains("Absent: 1")&&html.contains("Checkout pending: 1")));
    }
    @Test void eveningIncludesPresentAndAllOtherRecords() {
        enabled(); var present=new Attendance();present.setStatus("Present");
        when(attendance.findByDate(day)).thenReturn(List.of(present));
        assertTrue(service.sendScheduledReport(day,false));
        verify(attendance,never()).findLateAndAbsentByDate(any());
        verify(email).sendCustomEmail(eq("hr@example.org"),contains("Evening"),contains("Present: 1"));
    }
    @Test void emptyMorningStillSendsZeroSummary() {
        enabled();assertTrue(service.sendScheduledReport(day,true));
        verify(email).sendCustomEmail(any(),contains("Morning"),contains("Total records: 0"));
    }
    @Test void noRecipientsDoesNotClaimDelivery() {
        enabled().setRecipientEmails("");assertFalse(service.sendScheduledReport(day,true));verifyNoInteractions(email);
    }

    @Test void everySelectedCombinationFiltersBothReportsAndLabelsAttachments() {
        for(boolean morning : new boolean[]{true,false}) for(int mask=1;mask<8;mask++) {
            var s=enabled();s.setAttachExcel(true);
            boolean present=(mask&1)!=0, late=(mask&2)!=0, absent=(mask&4)!=0;
            s.setMorningIncludePresent(present);s.setMorningIncludeLate(late);s.setMorningIncludeAbsent(absent);
            s.setEveningIncludePresent(present);s.setEveningIncludeLate(late);s.setEveningIncludeAbsent(absent);
            var records=new ArrayList<Attendance>();
            for(String status : List.of("Present","Late","Absent","Half Day")) {var a=new Attendance();a.setStatus(status);records.add(a);}
            when(attendance.findByDate(day)).thenReturn(records);
            when(excel.buildAttendanceReportExcel(anyList(),any(),any())).thenReturn(new byte[]{1});
            clearInvocations(excel,email);
            assertTrue(service.sendScheduledReport(day,morning));
            var expected=s.includedStatuses(morning);
            verify(excel).buildAttendanceReportExcel(argThat(rows->rows.stream().map(Attendance::getStatus).toList().equals(expected)),eq(day),eq(day));
            String prefix=morning?"morning_":"evening_";
            verify(email).sendEmailWithExcelAttachment(eq("hr@example.org"),contains(String.join(", ",expected)),contains("Included statuses: "+String.join(", ",expected)),eq(prefix+String.join("_",expected).toLowerCase(Locale.ROOT)+"_"+day+".xlsx"),any());
        }
    }
    @Test void emptySelectionNeverSends() {
        var s=enabled();s.setMorningIncludePresent(false);s.setMorningIncludeLate(false);s.setMorningIncludeAbsent(false);
        assertFalse(service.sendScheduledReport(day,true));verifyNoInteractions(email,excel,attendance);
    }
}
