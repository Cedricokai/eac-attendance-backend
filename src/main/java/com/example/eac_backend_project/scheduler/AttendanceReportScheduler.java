package com.example.eac_backend_project.scheduler;

import com.example.eac_backend_project.Service.AttendanceService;
import com.example.eac_backend_project.Service.reports.AttendanceAutoReportService;  // ← ADD THIS IMPORT
import com.example.eac_backend_project.entity.settings.AttendanceReportScheduleSettings;
import com.example.eac_backend_project.repository.settings.AttendanceReportScheduleSettingsRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class AttendanceReportScheduler {

    private final AttendanceService attendanceService;
    private final AttendanceReportScheduleSettingsRepository settingsRepository;
    private final AttendanceAutoReportService autoReportService;  // ← ADD THIS

    public AttendanceReportScheduler(
            AttendanceService attendanceService,
            AttendanceReportScheduleSettingsRepository settingsRepository,
            AttendanceAutoReportService autoReportService  // ← ADD THIS PARAMETER
    ) {
        this.attendanceService = attendanceService;
        this.settingsRepository = settingsRepository;
        this.autoReportService = autoReportService;  // ← ADD THIS
    }

    @Scheduled(fixedRate = 60000)  // Runs every minute
    public void runEveryMinute() {
        LocalTime now = LocalTime.now(java.time.ZoneId.of("Africa/Accra"));

        // Get schedule settings
        AttendanceReportScheduleSettings settings = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(null);

        attendanceService.refreshPendingCheckInStatuses();

        // Recheck after the cutoff so a missed minute or restart does not skip today.
        // Existing records and approved leave are excluded by the service.
        if (settings != null && settings.isAutoAbsentEnabled()) {
            attendanceService.createAbsentRecordsForToday();
        }

        if (settings == null || !settings.isEnabled()) return;
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Africa/Accra"));
        if (Boolean.TRUE.equals(settings.getMorningReportEnabled())
                && due(now, settings.getMorningReportTime(), today, settings.getLastMorningReportDate())) {
            try {
                if (autoReportService.sendScheduledReport(today, true)) {
                    settings.setLastMorningReportDate(today);
                    settingsRepository.save(settings);
                }
            } catch (Exception e) {
                System.err.println("Morning attendance report failed: " + e.getMessage());
            }
        }
        if (!Boolean.FALSE.equals(settings.getEveningReportEnabled())
                && due(now, settings.getSendTime(), today, settings.getLastSentDate())) {
            try {
                if (autoReportService.sendScheduledReport(today, false)) {
                    settings.setLastSentDate(today);
                    settingsRepository.save(settings);
                }
            } catch (Exception e) {
                System.err.println("Evening attendance report failed: " + e.getMessage());
            }
        }
    }

    static boolean due(LocalTime now, LocalTime target, LocalDate today, LocalDate lastSent) {
        return target != null && !now.isBefore(target) && !today.equals(lastSent);
    }
}
