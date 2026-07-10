// AttendanceCodeSettingsService.java
package com.example.eac_backend_project.Service.settings;

import com.example.eac_backend_project.entity.settings.AttendanceCodeSettings;
import com.example.eac_backend_project.repository.settings.AttendanceCodeSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AttendanceCodeSettingsService {

    private final AttendanceCodeSettingsRepository repository;

    public AttendanceCodeSettingsService(AttendanceCodeSettingsRepository repository) {
        this.repository = repository;
        initializeDefaults();
    }

    private void initializeDefaults() {
        if (repository.count() == 0) {
            List<AttendanceCodeSettings> defaults = List.of(
                    new AttendanceCodeSettings("P", false, 8.0, true, "Regular Present", true),
                    new AttendanceCodeSettings("WP", false, 8.0, true, "Weekend Present", true),
                    new AttendanceCodeSettings("HP", false, 8.0, true, "Holiday Present", true),
                    new AttendanceCodeSettings("H", false, 8.0, false, "Holiday", true),
                    new AttendanceCodeSettings("L", false, 8.0, false, "Leave", true),
                    new AttendanceCodeSettings("S", false, 8.0, false, "Sick Leave", true),
                    new AttendanceCodeSettings("ML", false, 8.0, false, "Maternity Leave", true),
                    new AttendanceCodeSettings("PL", false, 8.0, false, "Paternity Leave", true),
                    new AttendanceCodeSettings("OFF", false, 0.0, false, "Off Day", true),
                    new AttendanceCodeSettings("A", false, 0.0, false, "Absent", true)
            );
            repository.saveAll(defaults);
        }
    }

    public List<AttendanceCodeSettings> getAllSettings() {
        return repository.findAll();
    }

    public AttendanceCodeSettings getSettings(String code) {
        return repository.findById(code).orElse(null);
    }

    public AttendanceCodeSettings updateSettings(String code, AttendanceCodeSettings settings) {
        settings.setCode(code);
        return repository.save(settings);
    }

    public Map<String, AttendanceCodeSettings> getSettingsMap() {
        List<AttendanceCodeSettings> all = getAllSettings();
        Map<String, AttendanceCodeSettings> map = new java.util.HashMap<>();
        for (AttendanceCodeSettings s : all) {
            map.put(s.getCode(), s);
        }
        return map;
    }
}