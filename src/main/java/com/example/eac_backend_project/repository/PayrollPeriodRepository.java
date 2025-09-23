package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    // Latest period by end date
    Optional<PayrollPeriod> findTopByOrderByEndDateDesc();

    // All periods sorted by start date (descending)
    List<PayrollPeriod> findAllByOrderByStartDateDesc();

    // Custom query to check overlaps
    @Query("SELECT p FROM PayrollPeriod p " +
            "WHERE (p.startDate < :endDate AND p.endDate > :startDate)")
    List<PayrollPeriod> findOverlappingPeriods(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

}
