package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Holiday h WHERE h.date = :date OR (h.recurring = true AND MONTH(h.date) = MONTH(:date) AND DAY(h.date) = DAY(:date)) ORDER BY h.id")
    List<Holiday> lockHolidaysOn(@org.springframework.data.repository.query.Param("date") LocalDate date);

    List<Holiday> findAllByOrderByDateAsc();

    @Query("SELECT h FROM Holiday h WHERE YEAR(h.date) = :year ORDER BY h.date")
    List<Holiday> findByYear(int year);

    boolean existsByDate(LocalDate date);

    List<Holiday> findByRecurringTrue();
}