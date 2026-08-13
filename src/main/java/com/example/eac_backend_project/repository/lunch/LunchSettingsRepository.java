package com.example.eac_backend_project.repository.lunch;

import com.example.eac_backend_project.entity.lunch.LunchSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LunchSettingsRepository extends JpaRepository<LunchSettings, Long> {

    Optional<LunchSettings> findBySettingKey(String key);
}