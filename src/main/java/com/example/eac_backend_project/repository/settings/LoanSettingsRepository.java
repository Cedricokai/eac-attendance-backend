package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.LoanSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanSettingsRepository extends JpaRepository<LoanSettings, Long> {

    @Override
    Optional<LoanSettings> findById(Long aLong);
}
