package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.Overtime;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OvertimeRepository extends JpaRepository<Overtime, Long> {

    List<Overtime> findByDate(LocalDate date);

    List<Overtime> findByEmployeeId(Long employeeId);

    List<Overtime> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    List<Overtime> findByStatus(String status);

    List<Overtime> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT o FROM Overtime o WHERE o.employee.id = :employeeId AND o.date BETWEEN :startDate AND :endDate")
    List<Overtime> findByEmployeeIdAndDateRange(@Param("employeeId") Long employeeId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT o FROM Overtime o WHERE o.date = :date AND o.status = 'Pending'")
    List<Overtime> findPendingOvertimesByDate(@Param("date") LocalDate date);

    boolean existsByEmployeeIdAndDate(Long employeeId, LocalDate date);

    @Query("SELECT SUM(o.overtimeHours) FROM Overtime o WHERE o.employee.id = :employeeId AND o.date BETWEEN :startDate AND :endDate")
    Optional<Double> findTotalOvertimeHoursByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT o.employee.id, SUM(o.overtimeHours), SUM(o.calculatedOvertimePay) " +
            "FROM Overtime o " +
            "WHERE o.date BETWEEN :startDate AND :endDate " +
            "GROUP BY o.employee.id")
    List<Object[]> summarizeOvertimeBetweenDates(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    boolean existsByAttendanceId(Long attendanceId);

    @Query("SELECT o.employee.id, SUM(o.overtimeHours) " +
            "FROM Overtime o " +
            "WHERE o.date BETWEEN :startDate AND :endDate " +
            "AND o.status = 'Approved' " +
            "AND o.overtimeHours > 0 " +
            "GROUP BY o.employee.id")
    List<Object[]> findApprovedOvertimeSummaryForPayroll(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT o FROM Overtime o " +
            "WHERE o.date BETWEEN :startDate AND :endDate " +
            "AND o.status = 'Approved' " +
            "AND o.employee IS NOT NULL")
    List<Overtime> findApprovedOvertimeForPayrollPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(o) FROM Overtime o " +
            "WHERE o.date BETWEEN :startDate AND :endDate " +
            "AND o.status != 'Approved' " +
            "AND o.overtimeHours > 0")
    long countNonApprovedOvertimeInPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ✅ UPDATED: Include multiplierAppliedToHours in the query - USING overtimeHours
    @Query("SELECT o.employee.id, SUM(o.overtimeHours), SUM(o.calculatedOvertimePay), o.multiplierAppliedToHours " +
            "FROM Overtime o " +
            "WHERE o.date BETWEEN :startDate AND :endDate " +
            "AND o.status = 'Approved' " +
            "AND o.overtimeHours > 0 " +
            "GROUP BY o.employee.id, o.multiplierAppliedToHours")
    List<Object[]> summarizeApprovedOvertimeBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<Overtime> findByEmployeeIdInAndDate(List<Long> employeeIds, LocalDate date);

    @Modifying
    @Query("UPDATE Overtime o SET o.multiplierAppliedToHours = :newValue WHERE o.id IN :ids")
    int batchUpdateMultiplierAppliedToHours(@Param("ids") List<Long> ids, @Param("newValue") Boolean newValue);

    // ✅ CORRECTED: Using overtimeHours instead of hours
    @Query("SELECT o.employee.id, SUM(o.overtimeHours), SUM(o.calculatedOvertimePay), o.multiplierAppliedToHours " +
            "FROM Overtime o " +
            "WHERE o.date BETWEEN :startDate AND :endDate " +
            "AND o.status = 'Approved' " +
            "AND o.employee.category.name = :category " +
            "GROUP BY o.employee.id, o.multiplierAppliedToHours")
    List<Object[]> summarizeApprovedOvertimeBetweenDatesForCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category);

    // NEW: Filter by multiple categories
    @Query("SELECT o FROM Overtime o WHERE o.date BETWEEN :startDate AND :endDate AND o.category IN :categories")
    List<Overtime> findByDateBetweenAndCategoryIn(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categories") List<String> categories
    );


    @Modifying
    @Transactional
    @Query("DELETE FROM Overtime o WHERE o.id IN :ids")
    int deleteAllByIdIn(@Param("ids") List<Long> ids);

    // For checking existence before delete
    boolean existsByIdIn(List<Long> ids);
}