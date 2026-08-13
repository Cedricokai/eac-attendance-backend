package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.dto.TimesheetListDTO;
import com.example.eac_backend_project.entity.attendance.Timesheet;
import com.example.eac_backend_project.entity.attendance.TimesheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    List<Timesheet> findByEmployeeId(Long employeeId);

    List<Timesheet> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Timesheet> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    List<Timesheet> findByStatus(TimesheetStatus status);

    List<Timesheet> findByJobId(Long jobId);

    boolean existsByEmployeeIdAndDate(Long employeeId, LocalDate date);


    Optional<Timesheet> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    @Query("SELECT SUM(t.totalHours) FROM Timesheet t WHERE t.employee.id = :employeeId AND t.date BETWEEN :startDate AND :endDate")
    Double findTotalHoursByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.earnings) FROM Timesheet t WHERE t.employee.id = :employeeId AND t.date BETWEEN :startDate AND :endDate")
    Double findTotalEarningsByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("DELETE FROM Timesheet t WHERE t.employee.id IN :employeeIds AND t.date = :date")
    int deleteByEmployeeIdInAndDate(@Param("employeeIds") List<Long> employeeIds, @Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM Timesheet t WHERE t.employee.id IN :employeeIds AND t.date BETWEEN :startDate AND :endDate")
    int deleteByEmployeeIdInAndDateBetween(@Param("employeeIds") List<Long> employeeIds,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("DELETE FROM Timesheet t WHERE t.employee.id IN :employeeIds")
    int deleteByEmployeeIdIn(@Param("employeeIds") List<Long> employeeIds);

    @Modifying
    @Query("DELETE FROM Timesheet t WHERE t.date = :date")
    int deleteByDate(@Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM Timesheet t WHERE t.date BETWEEN :startDate AND :endDate")
    int deleteByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Add these methods to your TimesheetRepository interface

    @Query("SELECT t FROM Timesheet t WHERE t.sourceDocumentMissing = true AND t.date BETWEEN :startDate AND :endDate")
    List<Timesheet> findUnverifiedByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Timesheet t WHERE t.sourceDocumentMissing = true AND t.employee.id = :employeeId AND t.date BETWEEN :startDate AND :endDate")
    List<Timesheet> findUnverifiedByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(t) FROM Timesheet t WHERE t.sourceDocumentMissing = true AND t.date BETWEEN :startDate AND :endDate")
    Long countUnverifiedByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("UPDATE Timesheet t SET t.sourceDocumentMissing = :missing, t.sourceDocumentMissingReason = :reason, t.sourceDocumentMissingFlaggedAt = :flaggedAt, t.sourceDocumentMissingFlaggedBy = :flaggedBy WHERE t.employee.id IN :employeeIds AND t.date BETWEEN :startDate AND :endDate")
    int bulkUpdateSourceDocumentStatus(@Param("employeeIds") List<Long> employeeIds,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("missing") Boolean missing,
                                       @Param("reason") String reason,
                                       @Param("flaggedAt") LocalDateTime flaggedAt,
                                       @Param("flaggedBy") String flaggedBy);

    @Modifying
    @Query("UPDATE Timesheet t SET t.sourceDocumentMissing = :missing, t.sourceDocumentMissingReason = :reason WHERE t.id = :id")
    int updateSourceDocumentStatusById(@Param("id") Long id,
                                       @Param("missing") Boolean missing,
                                       @Param("reason") String reason);

    @Query("SELECT t FROM Timesheet t WHERE t.sourceDocumentMissing = false AND t.verifiedAt IS NULL AND t.date BETWEEN :startDate AND :endDate")
    List<Timesheet> findUnverifiedButNotFlaggedByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Add this method to your TimesheetRepository interface

    @Query("SELECT t FROM Timesheet t WHERE t.job.id = :jobId AND t.date BETWEEN :startDate AND :endDate AND (t.regularHours > 0 OR t.overtimeHours > 0)")
    List<Timesheet> findByJobIdAndDateBetweenWithHours(@Param("jobId") Long jobId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("DELETE FROM Timesheet t WHERE t.job.id = :jobId")
    int deleteByJobId(@Param("jobId") Long jobId);

    @Query("""
    SELECT new com.example.eac_backend_project.dto.TimesheetListDTO(
        t.id,
        t.date,
        t.attendanceCode,
        t.regularHours,
        t.overtimeHours,
        t.breakHours,
        t.totalHours,
        t.earnings,
        t.status,
        t.notes,
        t.sourceDocumentMissing,
        t.sourceDocumentMissingReason,
        t.sourceDocumentMissingFlaggedAt,
        t.sourceDocumentMissingFlaggedBy,
        t.sourceDocumentType,
        t.sourceDocumentReference,
        t.verifiedAt,
        t.verifiedBy,
        e.id,
        e.employeeId,
        e.firstName,
        e.lastName,
        c.id,
        c.name,
        j.id,
        j.name,
        ov.id
    )
    FROM Timesheet t
    JOIN t.employee e
    LEFT JOIN e.category c
    LEFT JOIN t.job j
    LEFT JOIN t.overview ov
    WHERE t.date BETWEEN :startDate AND :endDate
      AND (
          :employeeId IS NULL
          OR e.id = :employeeId
      )
      AND (
          :categoryId IS NULL
          OR c.id = :categoryId
      )
      AND (
          :jobId IS NULL
          OR j.id = :jobId
      )
      AND (
          :withoutJob = false
          OR j.id IS NULL
      )
      AND (
          :status IS NULL
          OR t.status = :status
      )
      AND (
          :sourceMissing IS NULL
          OR t.sourceDocumentMissing = :sourceMissing
      )
      AND (
          :search IS NULL
          OR :search = ''
          OR LOWER(e.firstName)
                LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(e.lastName)
                LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(CONCAT(e.firstName, ' ', e.lastName))
                LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(e.employeeId)
                LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(COALESCE(j.name, ''))
                LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(COALESCE(t.attendanceCode, ''))
                LIKE LOWER(CONCAT('%', :search, '%'))
      )
    ORDER BY e.firstName, e.lastName, t.date
    """)
    List<TimesheetListDTO> findTimesheetRows(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("employeeId") Long employeeId,
            @Param("categoryId") Long categoryId,
            @Param("jobId") Long jobId,
            @Param("withoutJob") boolean withoutJob,
            @Param("status") TimesheetStatus status,
            @Param("sourceMissing") Boolean sourceMissing,
            @Param("search") String search
    );
}