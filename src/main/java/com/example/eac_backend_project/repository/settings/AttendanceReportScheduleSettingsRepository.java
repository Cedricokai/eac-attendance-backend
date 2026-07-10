package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.AttendanceReportScheduleSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceReportScheduleSettingsRepository extends JpaRepository<AttendanceReportScheduleSettings, Long> {

    Optional<AttendanceReportScheduleSettings> findFirstByOrderByIdAsc();

}
