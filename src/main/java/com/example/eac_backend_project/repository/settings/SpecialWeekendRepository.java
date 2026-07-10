package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.SpecialWeekend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SpecialWeekendRepository extends JpaRepository<SpecialWeekend, Long> {
    boolean existsByDate(LocalDate date);
    List<SpecialWeekend> findAllByOrderByDateAsc();
}