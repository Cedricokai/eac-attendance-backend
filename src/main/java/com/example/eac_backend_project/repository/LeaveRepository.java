package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    List<Leave> findByEmployeeId(Long employeeId);

    List<Leave> findByEmployeeIdAndStatus(Long employeeId, String status);

    List<Leave> findByEmployeeIdAndStatusAndStartDateBetween(
            Long employeeId,
            String status,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Leave> findByStatus(String status);
    List<Leave> findBySupervisorStatus(String status);
    List<Leave> findByPlannerStatus(String status);
    List<Leave> findByHrStatus(String status);

    @Query("SELECT l FROM Leave l WHERE l.status = 'Approved' " +
            "AND l.startDate <= :periodEnd AND l.endDate >= :periodStart")
    List<Leave> findApprovedLeavesInPeriod(
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    @Query("SELECT l FROM Leave l " +
            "WHERE l.status = 'Approved' " +
            "AND l.employee.category.name = :category " +
            "AND ((l.startDate BETWEEN :startDate AND :endDate) " +
            "OR (l.endDate BETWEEN :startDate AND :endDate) " +
            "OR (l.startDate <= :startDate AND l.endDate >= :endDate))")
    List<Leave> findApprovedLeavesInPeriodForCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category);

    // Fix: Use @Query to check if leave exists for a specific date
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Leave l " +
            "WHERE l.employee.id = :employeeId AND :date BETWEEN l.startDate AND l.endDate")
    boolean existsByEmployeeIdAndDate(@Param("employeeId") Long employeeId,
                                      @Param("date") LocalDate date);

    @Query("SELECT l FROM Leave l WHERE l.status = :status " +
            "AND l.startDate <= :date AND l.endDate >= :date")
    List<Leave> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            @Param("status") String status,
            @Param("date") LocalDate date);

    @Query("SELECT DISTINCT l FROM Leave l " +
            "LEFT JOIN FETCH l.employee e " +
            "LEFT JOIN FETCH e.category " +
            "ORDER BY l.startDate DESC")
    List<Leave> findAllWithEmployeeAndCategory();
}
