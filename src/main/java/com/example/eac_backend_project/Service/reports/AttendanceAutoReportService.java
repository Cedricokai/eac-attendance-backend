package com.example.eac_backend_project.Service.reports;

import com.example.eac_backend_project.Service.AttendanceReportExcelService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.settings.AttendanceReportScheduleSettings;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.settings.AttendanceReportScheduleSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceAutoReportService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceReportExcelService excelService;
    private final EmailService emailService;
    private final AttendanceReportScheduleSettingsRepository settingsRepository;

    public AttendanceAutoReportService(
            AttendanceRepository attendanceRepository,
            AttendanceReportExcelService excelService,
            EmailService emailService,
            AttendanceReportScheduleSettingsRepository settingsRepository
    ) {
        this.attendanceRepository = attendanceRepository;
        this.excelService = excelService;
        this.emailService = emailService;
        this.settingsRepository = settingsRepository;
    }

    public void sendDailyAutoReportIfEnabled(LocalDate reportDate) {
        AttendanceReportScheduleSettings settings = getOrCreateSettings();
        if (!settings.isEnabled()) return;

        List<String> recipients = parseEmails(settings.getRecipientEmails());
        if (recipients.isEmpty()) return;

        List<Attendance> records;
        if (settings.isSendOnlyExceptions()) {
            records = attendanceRepository.findLateAndAbsentByDate(reportDate);
        } else {
            records = attendanceRepository.findByDate(reportDate);
        }

        if (records == null || records.isEmpty()) return;

        byte[] xlsx = null;
        String filename = "attendance_report_" + reportDate + ".xlsx";

        if (settings.isAttachExcel()) {
            xlsx = excelService.buildAttendanceReportExcel(records, reportDate, reportDate);
        }

        String subject = "Daily Attendance Report (" + reportDate + ")";
        String html = buildSummaryHtml(records, reportDate);

        for (String to : recipients) {
            if (settings.isAttachExcel() && xlsx != null) {
                emailService.sendEmailWithExcelAttachment(
                        to,
                        subject,
                        html,
                        filename,
                        xlsx
                );
            } else {
                emailService.sendCustomEmail(to, subject, html);
            }
        }
    }

    private AttendanceReportScheduleSettings getOrCreateSettings() {
        return settingsRepository.findAll().stream().findFirst().orElseGet(() -> {
            AttendanceReportScheduleSettings s = new AttendanceReportScheduleSettings();
            s.setEnabled(false);
            s.setRecipientEmails("");
            return settingsRepository.save(s);
        });
    }

    private List<String> parseEmails(String raw) {
        if (raw == null || raw.trim().isEmpty()) return List.of();
        return Arrays.stream(raw.split("[,;\\s]+"))
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String buildSummaryHtml(List<Attendance> records, LocalDate date) {
        long late = records.stream().filter(a -> "Late".equalsIgnoreCase(a.getStatus())).count();
        long absent = records.stream().filter(a -> "Absent".equalsIgnoreCase(a.getStatus())).count();

        return """
                <div style="font-family:Arial,sans-serif;color:#111827;line-height:1.5">
                  <h2 style="margin:0 0 8px 0;">Daily Attendance Exceptions</h2>
                  <p style="margin:0 0 8px 0;">Date: <strong>%s</strong></p>
                  <ul style="margin:0 0 12px 18px;">
                    <li><strong>Late:</strong> %d</li>
                    <li><strong>Absent:</strong> %d</li>
                    <li><strong>Total records attached:</strong> %d</li>
                  </ul>
                  <p style="margin:0;">Excel attachment contains the detailed list.</p>
                </div>
                """.formatted(date, late, absent, records.size());
    }
}
