package com.example.eac_backend_project.repository.lunch;

import com.example.eac_backend_project.entity.lunch.LunchServing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LunchServingRepository extends JpaRepository<LunchServing, Long> {

    // Find by employee and date
    Optional<LunchServing> findByEmployeeIdAndLunchDate(Long employeeId, LocalDate date);

    // Find all served records for a specific date
    List<LunchServing> findByLunchDate(LocalDate date);

    // Count served for a specific date
    long countByLunchDate(LocalDate date);

    // Find served records by location and date
    List<LunchServing> findByLocationAndLunchDate(String location, LocalDate date);

    // Count served by location for a date
    long countByLocationAndLunchDate(String location, LocalDate date);

    // Get all served records for a date grouped by location
    @Query("SELECT ls.location, COUNT(ls) FROM LunchServing ls WHERE ls.lunchDate = :date GROUP BY ls.location")
    List<Object[]> countByLocationGrouped(@Param("date") LocalDate date);

    // ✅ ADD THIS METHOD – find served records between two dates
    List<LunchServing> findByLunchDateBetween(LocalDate startDate, LocalDate endDate);

    // Optional: Find served records by employee between dates
    List<LunchServing> findByEmployeeIdAndLunchDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    // Optional: Count served records by date range
    long countByLunchDateBetween(LocalDate startDate, LocalDate endDate);

    // Optional: Get served records grouped by date
    @Query("SELECT ls.lunchDate, COUNT(ls) FROM LunchServing ls WHERE ls.lunchDate BETWEEN :startDate AND :endDate GROUP BY ls.lunchDate ORDER BY ls.lunchDate")
    List<Object[]> countByDateGrouped(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}