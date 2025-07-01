package com.eacattendance.repository;

import com.eacattendance.entity.Overview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OverviewRepository extends JpaRepository<Overview, Long> {
    Optional<Overview> findByAttendanceId(Long attendanceId);
    Optional<Overview> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
    /**
     * Finds all overview records for a specific employee within a date range
     * @param employeeId The employee ID
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return List of Overview records
     */
    List<Overview> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    List<Overview> findByDate(LocalDate date);

    List<Overview> findByDateBetween(LocalDate startDate, LocalDate endDate);
}
