package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.PayrollPeriod;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    List<PayrollPeriod> findAllByOrderByStartDateDesc();

    Optional<PayrollPeriod> findTopByOrderByEndDateDesc();

    // Find periods by category
    List<PayrollPeriod> findByCategoryOrderByStartDateDesc(String category);

    // Find periods by status
    List<PayrollPeriod> findByStatus(String status);

    // Find periods by category and status
    List<PayrollPeriod> findByCategoryAndStatus(String category, String status);

    // Check for overlapping periods for the SAME category only
    @Query("SELECT p FROM PayrollPeriod p WHERE " +
            "p.category = :category AND COALESCE(p.periodType, 'CATEGORY') = 'CATEGORY' AND " +  // Only check same category
            "((p.startDate BETWEEN :startDate AND :endDate) OR " +
            "(p.endDate BETWEEN :startDate AND :endDate) OR " +
            "(p.startDate <= :startDate AND p.endDate >= :endDate))")
    List<PayrollPeriod> findOverlappingPeriodsForCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category);

    // Get all distinct categories that have periods
    @Query("SELECT DISTINCT p.category FROM PayrollPeriod p WHERE p.category IS NOT NULL")
    List<String> findDistinctCategories();

    // Find periods by date range and category
    List<PayrollPeriod> findByStartDateBetweenAndCategory(LocalDate start, LocalDate end, String category);

    // Find active periods (status = 'Open')
    @Query("SELECT p FROM PayrollPeriod p WHERE p.status = 'Open' ORDER BY p.endDate DESC")
    List<PayrollPeriod> findOpenPeriods();

    Optional<PayrollPeriod> findByName(String name);
    @Query("SELECT p FROM PayrollPeriod p WHERE p.periodType = 'PROJECT' AND p.projectId = :projectId AND p.startDate <= :end AND p.endDate >= :start")
    List<PayrollPeriod> findOverlappingProjectPeriods(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("projectId") Long projectId);
    // All payroll writers take these locks in the same order, across period types.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PayrollPeriod p ORDER BY p.id")
    List<PayrollPeriod> lockForPayrollGeneration();

    // ==================== PAYROLL RECORD REPOSITORY METHODS ====================


}