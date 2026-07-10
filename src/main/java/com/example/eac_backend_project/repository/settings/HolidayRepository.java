package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findAllByOrderByDateAsc();

    @Query("SELECT h FROM Holiday h WHERE YEAR(h.date) = :year ORDER BY h.date")
    List<Holiday> findByYear(int year);

    boolean existsByDate(LocalDate date);

    List<Holiday> findByRecurringTrue();
}