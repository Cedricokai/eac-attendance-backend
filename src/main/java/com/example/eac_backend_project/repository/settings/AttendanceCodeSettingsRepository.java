// AttendanceCodeSettingsRepository.java
package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.AttendanceCodeSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceCodeSettingsRepository extends JpaRepository<AttendanceCodeSettings, String> {
}