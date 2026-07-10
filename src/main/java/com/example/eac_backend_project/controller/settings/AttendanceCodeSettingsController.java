// AttendanceCodeSettingsController.java
package com.example.eac_backend_project.controller.settings;

import com.example.eac_backend_project.Service.settings.AttendanceCodeSettingsService;
import com.example.eac_backend_project.entity.settings.AttendanceCodeSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings/attendance-codes")
public class AttendanceCodeSettingsController {

    private final AttendanceCodeSettingsService service;

    public AttendanceCodeSettingsController(AttendanceCodeSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, AttendanceCodeSettings>> getAllSettings() {
        return ResponseEntity.ok(service.getSettingsMap());
    }

    @GetMapping("/list")
    public ResponseEntity<List<AttendanceCodeSettings>> getAllSettingsList() {
        return ResponseEntity.ok(service.getAllSettings());
    }

    @GetMapping("/{code}")
    public ResponseEntity<AttendanceCodeSettings> getSettings(@PathVariable String code) {
        AttendanceCodeSettings settings = service.getSettings(code);
        if (settings == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/{code}")
    public ResponseEntity<AttendanceCodeSettings> updateSettings(
            @PathVariable String code,
            @RequestBody AttendanceCodeSettings settings) {
        return ResponseEntity.ok(service.updateSettings(code, settings));
    }
}