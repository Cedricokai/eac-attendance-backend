package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
    // Since we'll only have one settings record
    Optional<SystemSettings> findFirstByOrderByIdAsc();

}