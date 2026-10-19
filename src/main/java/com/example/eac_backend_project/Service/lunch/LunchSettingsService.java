package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.entity.lunch.LunchSettings;
import com.example.eac_backend_project.repository.lunch.LunchSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class LunchSettingsService {

    private final LunchSettingsRepository settingsRepository;

    public Optional<LunchSettings> getSetting(String key) {
        return settingsRepository.findBySettingKey(key);
    }

    public static final ZoneId ZONE = ZoneId.of("Africa/Accra");

    public ZonedDateTime now() { return ZonedDateTime.now(ZONE); }

    public Map<String, String> getSettings() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("costPerLunch", "5.50");
        values.put("monthlyBudget", "5000");
        values.put("weeklyBudget", "1500");
        values.put("mealChangeCutoffTime", "17:00");
        values.put("mealChangeDaysBefore", "1");
        values.put("mealChangeFrequency", "DAILY");
        values.put("mealChangeMonthlyDay", "1");
        values.put("mealChangeNotificationEmail", "");
        settingsRepository.findAll().forEach(s -> values.put(s.getSettingKey(), s.getSettingValue()));
        return values;
    }

    public MealChangePolicy getMealChangePolicy() {
        Map<String, String> values = getSettings();
        return new MealChangePolicy(values.get("mealChangeCutoffTime"),
                Integer.parseInt(values.get("mealChangeDaysBefore")), ZONE.getId(),
                values.get("mealChangeFrequency"), Integer.parseInt(values.get("mealChangeMonthlyDay")));
    }

    @Transactional
    public Map<String, String> updateSettings(Map<String, Object> values) {
        values.forEach((key, value) -> validate(key, value == null ? null : value.toString()));
        values.forEach((key, value) -> {
            LunchSettings setting = new LunchSettings();
            setting.setSettingKey(key);
            setting.setSettingValue(value.toString());
            saveSetting(setting);
        });
        return getSettings();
    }

    @Transactional
    public LunchSettings saveSetting(LunchSettings setting) {
        validate(setting.getSettingKey(), setting.getSettingValue());
        LunchSettings existing = getSetting(setting.getSettingKey()).orElseGet(LunchSettings::new);
        existing.setSettingKey(setting.getSettingKey());
        existing.setSettingValue(setting.getSettingValue());
        if (setting.getDescription() != null) existing.setDescription(setting.getDescription());
        return settingsRepository.save(existing);
    }

    private void validate(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lunch setting");
        }
        try {
            if (key.equals("mealChangeFrequency") && !Set.of("DAILY", "MONTHLY").contains(value)) throw new IllegalArgumentException();
            if (key.equals("mealChangeMonthlyDay") && (Integer.parseInt(value) < 1 || Integer.parseInt(value) > 31)) throw new IllegalArgumentException();
            if (key.equals("mealChangeNotificationEmail") && !value.isEmpty()) {
                if (!value.matches("[^\\s@,;<>]+@[^\\s@,;<>]+\\.[^\\s@,;<>]+")) throw new IllegalArgumentException();
                try { new jakarta.mail.internet.InternetAddress(value, true).validate(); }
                catch (jakarta.mail.internet.AddressException e) { throw new IllegalArgumentException(); }
            }
            if (key.equals("mealChangeCutoffTime")) {
                if (!value.matches("\\d{2}:\\d{2}")) throw new IllegalArgumentException();
                LocalTime.parse(value);
            }
            if (key.equals("mealChangeDaysBefore") && !value.matches("[01]")) throw new IllegalArgumentException();
            if (Set.of("costPerLunch", "monthlyBudget", "weeklyBudget").contains(key)
                    && new java.math.BigDecimal(value).signum() < 0) throw new IllegalArgumentException();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value for " + key);
        }
    }
}
