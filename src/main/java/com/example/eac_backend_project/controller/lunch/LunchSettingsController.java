package com.example.eac_backend_project.controller.lunch;

import com.example.eac_backend_project.Service.lunch.LunchSettingsService;
import com.example.eac_backend_project.entity.lunch.LunchSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/lunch/settings")
@RequiredArgsConstructor
public class LunchSettingsController {

    private final LunchSettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<java.util.Map<String, String>> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, String>> updateSettings(@RequestBody java.util.Map<String, Object> values) {
        return ResponseEntity.ok(settingsService.updateSettings(values));
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<LunchSettings> getSetting(@PathVariable String key) {
        Optional<LunchSettings> setting = settingsService.getSetting(key);
        return setting.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<LunchSettings> createOrUpdateSetting(@RequestBody LunchSettings setting) {
        return ResponseEntity.ok(settingsService.saveSetting(setting));
    }
}
