package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.entity.lunch.LunchSettings;
import com.example.eac_backend_project.repository.lunch.LunchSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LunchSettingsService {

    private final LunchSettingsRepository settingsRepository;

    public Optional<LunchSettings> getSetting(String key) {
        return settingsRepository.findBySettingKey(key);
    }

    public LunchSettings saveSetting(LunchSettings setting) {
        return settingsRepository.save(setting);
    }
}