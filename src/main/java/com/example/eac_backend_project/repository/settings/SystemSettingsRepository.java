package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
    // Since we'll only have one settings record
    Optional<SystemSettings> findFirstByOrderByIdAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SystemSettings s where s.id=(select min(s2.id) from SystemSettings s2)")
    Optional<SystemSettings> findFirstForUpdate();

}
