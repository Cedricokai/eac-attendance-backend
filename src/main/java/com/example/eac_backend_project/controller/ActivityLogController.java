package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.ActivityLogService;
import com.example.eac_backend_project.entity.attendance.ActivityLog;
import com.example.eac_backend_project.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/activity-logs")
public class ActivityLogController {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @GetMapping
    public ResponseEntity<Page<ActivityLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        Page<ActivityLog> logs = activityLogRepository.searchLogs(
                source, type, fromDate, toDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        return ResponseEntity.ok(logs);
    }
}