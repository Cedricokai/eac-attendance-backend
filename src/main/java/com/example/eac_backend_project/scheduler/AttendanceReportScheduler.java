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
        LocalTime now = LocalTime.now();

        // Get schedule settings
        AttendanceReportScheduleSettings settings = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(null);

        // ===== AUTO-ABSENT MARKING =====
        if (settings != null && settings.isAutoAbsentEnabled()) {
            LocalTime targetTime = settings.getAutoAbsentRunTime();
            if (targetTime != null && shouldRunNow(now, targetTime)) {
                System.out.println("🕐 Running auto-absent marking at " + now + " for date: " + LocalDate.now().minusDays(1));
                attendanceService.createAbsentRecordsForPreviousDay();
            }
        }

        // ===== EMAIL REPORT SCHEDULE =====
        if (settings != null && settings.isEnabled()) {
            LocalTime sendTime = settings.getSendTime();
            LocalDate today = LocalDate.now();
            LocalDate lastSent = settings.getLastSentDate();

            if (sendTime != null && shouldRunNow(now, sendTime) &&
                    (lastSent == null || !lastSent.equals(today))) {
                System.out.println("📧 Running scheduled email report at " + now);
                // UNCOMMENT THIS LINE:
                autoReportService.sendDailyAutoReportIfEnabled(today);

                settings.setLastSentDate(today);
                settingsRepository.save(settings);
            }
        }
    }

    private boolean shouldRunNow(LocalTime now, LocalTime targetTime) {
        // Run within 1 minute of target time
        return !now.isBefore(targetTime) && now.isBefore(targetTime.plusMinutes(1));
    }
}