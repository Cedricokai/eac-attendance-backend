package com.example.eac_backend_project.repository.lunch;

import com.example.eac_backend_project.entity.lunch.LunchAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LunchAssignmentRepository extends JpaRepository<LunchAssignment, Long> {

    // ----- Existing methods -----
    List<LunchAssignment> findByEmployeeId(Long employeeId);

    // ✅ Find active assignment for a specific employee on a specific date
    @Query("SELECT la FROM LunchAssignment la WHERE la.employee.id = :employeeId " +
            "AND la.startDate <= :date AND la.endDate >= :date " +
            "AND la.status = 'ACTIVE' AND la.deleted = false")
    Optional<LunchAssignment> findActiveAssignmentByEmployeeAndDate(@Param("employeeId") Long employeeId,
                                                                    @Param("date") LocalDate date);

    // ✅ Find all active assignments for a specific date
    @Query("SELECT la FROM LunchAssignment la WHERE la.startDate <= :date " +
            "AND la.endDate >= :date AND la.status = 'ACTIVE' AND la.deleted = false")
    List<LunchAssignment> findActiveAssignmentsForDate(@Param("date") LocalDate date);

    // ✅ Find all active assignments
    @Query("SELECT la FROM LunchAssignment la WHERE la.status = 'ACTIVE' AND la.deleted = false")
    List<LunchAssignment> findAllActive();

    // ✅ Find assignment by employee and start date
    Optional<LunchAssignment> findByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate);

    // ✅ Find assignments by employee and status
    List<LunchAssignment> findByEmployeeIdAndStatus(Long employeeId, String status);

    // ✅ Find assignments within a date range (overlapping)
    @Query("SELECT la FROM LunchAssignment la WHERE la.startDate BETWEEN :start AND :end " +
            "OR la.endDate BETWEEN :start AND :end " +
            "OR (la.startDate <= :start AND la.endDate >= :end)")
    List<LunchAssignment> findAssignmentsOverlappingDateRange(@Param("start") LocalDate start,
                                                              @Param("end") LocalDate end);

    // ✅ Find assignments by date range with status filter
    @Query("SELECT la FROM LunchAssignment la WHERE la.startDate <= :endDate " +
            "AND la.endDate >= :startDate AND la.status = :status AND la.deleted = false")
    List<LunchAssignment> findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(
            @Param("endDate") LocalDate endDate,
            @Param("startDate") LocalDate startDate,
            @Param("status") String status);

    // ✅ Count active assignments for a date
    @Query("SELECT COUNT(la) FROM LunchAssignment la WHERE la.startDate <= :date " +
            "AND la.endDate >= :date AND la.status = 'ACTIVE' AND la.deleted = false")
    long countActiveAssignmentsForDate(@Param("date") LocalDate date);

    // ✅ Get assignments by date range
    @Query("SELECT la FROM LunchAssignment la WHERE la.startDate >= :startDate AND la.endDate <= :endDate AND la.deleted = false")
    List<LunchAssignment> findByDateRange(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    // ✅ Get assignments by date range and status
    @Query("SELECT la FROM LunchAssignment la WHERE la.startDate >= :startDate AND la.endDate <= :endDate " +
            "AND la.status = :status AND la.deleted = false")
    List<LunchAssignment> findByDateRangeAndStatus(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("status") String status);

    // ✅ Check if employee has active assignment on a date
    @Query("SELECT CASE WHEN COUNT(la) > 0 THEN true ELSE false END FROM LunchAssignment la " +
            "WHERE la.employee.id = :employeeId AND la.startDate <= :date AND la.endDate >= :date " +
            "AND la.status = 'ACTIVE' AND la.deleted = false")
    boolean hasActiveAssignmentOnDate(@Param("employeeId") Long employeeId,
                                      @Param("date") LocalDate date);

    // ✅ Get assignments by employee and date range
    @Query("SELECT la FROM LunchAssignment la WHERE la.employee.id = :employeeId " +
            "AND la.startDate <= :endDate AND la.endDate >= :startDate AND la.deleted = false")
    List<LunchAssignment> findByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);
}