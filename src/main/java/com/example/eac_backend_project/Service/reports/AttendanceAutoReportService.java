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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public boolean sendScheduledReport(LocalDate reportDate, boolean morning) {
        AttendanceReportScheduleSettings settings = getOrCreateSettings();
        if (!settings.isEnabled()) return false;

        List<String> recipients = parseEmails(settings.getRecipientEmails());
        if (recipients.isEmpty()) return false;

        List<String> included = settings.includedStatuses(morning);
        if (included.isEmpty()) return false;
        List<Attendance> daily = attendanceRepository.findByDate(reportDate);
        List<Attendance> records = (daily == null ? List.<Attendance>of() : daily).stream()
                .filter(a -> included.stream().anyMatch(status -> status.equalsIgnoreCase(a.getStatus())))
                .collect(Collectors.toList());

        byte[] xlsx = null;
        String filename = (morning ? "morning_" : "evening_") + String.join("_", included).toLowerCase(java.util.Locale.ROOT) + "_" + reportDate + ".xlsx";

        if (settings.isAttachExcel()) {
            xlsx = excelService.buildAttendanceReportExcel(records, reportDate, reportDate);
        }

        String subject = (morning ? "Morning" : "Evening") + " Attendance Report - " + String.join(", ", included) + " (" + reportDate + ")";
        String html = buildSummaryHtml(records, reportDate, morning, settings.isAttachExcel(), included);

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
        return true;
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

    private String buildSummaryHtml(List<Attendance> records, LocalDate date, boolean morning, boolean attached, List<String> included) {
        String counts = included.stream().map(status -> "<li>" + status + ": "
                + records.stream().filter(a -> status.equalsIgnoreCase(a.getStatus())).count() + "</li>")
                .collect(Collectors.joining());
        long pending = records.stream().filter(a -> a.getCheckIn() != null && a.getCheckOut() == null).count();
        return """
            <div style="font-family:Arial,sans-serif;line-height:1.5">
              <h2>%s</h2><p>Attendance date: <strong>%s</strong></p>
              <p>Included statuses: %s</p><ul>%s<li>Checkout pending: %d</li><li>Total records: %d</li></ul>
              <p>%s</p><p>Absences reflect configured category cutoffs. Night-shift attendance is dated by shift start; a checkout may still be pending when this report is sent.</p>
            </div>
            """.formatted(morning ? "Morning Attendance Report" : "Evening Attendance Report", date, String.join(", ", included), counts, pending, records.size(),
                    attached ? "The Excel attachment contains the detailed list." : "Excel attachment is disabled.");
    }
}
