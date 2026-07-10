package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.LeaveSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveSettingsRepository extends JpaRepository<LeaveSettings, Long> {
    @Query("SELECT ls FROM LeaveSettings ls ORDER BY ls.id DESC LIMIT 1")
    LeaveSettings findTopByOrderByIdDesc();
}