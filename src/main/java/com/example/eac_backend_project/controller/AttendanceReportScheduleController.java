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

        s.setEnabled(incoming.isEnabled());
        s.setRecipientEmails(incoming.getRecipientEmails());
        s.setSendOnlyExceptions(incoming.isSendOnlyExceptions());
        s.setAttachExcel(incoming.isAttachExcel());

        LocalTime t = incoming.getSendTime();
        if (t != null) s.setSendTime(t);

        return ResponseEntity.ok(repo.save(s));
    }
}
