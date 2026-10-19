package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.entity.settings.AttendanceReportScheduleSettings;
import com.example.eac_backend_project.repository.settings.AttendanceReportScheduleSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;

@RestController
@RequestMapping("/api/report-schedule")
public class AttendanceReportScheduleController {

    private final AttendanceReportScheduleSettingsRepository repo;

    public AttendanceReportScheduleController(AttendanceReportScheduleSettingsRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<AttendanceReportScheduleSettings> get() {
        AttendanceReportScheduleSettings s = repo.findAll().stream().findFirst().orElseGet(() -> repo.save(new AttendanceReportScheduleSettings()));
        return ResponseEntity.ok(s);
    }

    @PutMapping
    public ResponseEntity<AttendanceReportScheduleSettings> update(@RequestBody AttendanceReportScheduleSettings incoming) {
        AttendanceReportScheduleSettings s = repo.findAll().stream().findFirst().orElseGet(() -> repo.save(new AttendanceReportScheduleSettings()));

        if (incoming.isEnabled() && Boolean.TRUE.equals(incoming.getMorningReportEnabled()) && incoming.getMorningReportTime() == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Choose a morning report time.");
        }
        if (incoming.isEnabled() && (incoming.getRecipientEmails() == null || incoming.getRecipientEmails().isBlank())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Enter report recipients.");
        }
        s.setMorningReportEnabled(Boolean.TRUE.equals(incoming.getMorningReportEnabled()));
        s.setMorningReportTime(incoming.getMorningReportTime());
        s.setEveningReportEnabled(!Boolean.FALSE.equals(incoming.getEveningReportEnabled()));
        if (incoming.isEnabled() && ((Boolean.TRUE.equals(incoming.getMorningReportEnabled()) && incoming.includedStatuses(true).isEmpty())
                || (!Boolean.FALSE.equals(incoming.getEveningReportEnabled()) && incoming.includedStatuses(false).isEmpty()))) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Select at least one attendance status for each enabled report.");
        }
        s.setMorningIncludePresent(incoming.getMorningIncludePresent());
        s.setMorningIncludeLate(incoming.getMorningIncludeLate());
        s.setMorningIncludeAbsent(incoming.getMorningIncludeAbsent());
        s.setEveningIncludePresent(incoming.getEveningIncludePresent());
        s.setEveningIncludeLate(incoming.getEveningIncludeLate());
        s.setEveningIncludeAbsent(incoming.getEveningIncludeAbsent());
        s.setEnabled(incoming.isEnabled());
        s.setRecipientEmails(incoming.getRecipientEmails());
        s.setSendOnlyExceptions(incoming.isSendOnlyExceptions());
        s.setAttachExcel(incoming.isAttachExcel());

        LocalTime t = incoming.getSendTime();
        if (t != null) s.setSendTime(t);

        return ResponseEntity.ok(repo.save(s));
    }
}
