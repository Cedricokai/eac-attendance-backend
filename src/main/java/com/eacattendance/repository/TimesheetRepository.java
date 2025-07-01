package com.eacattendance.repository;

import com.eacattendance.entity.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByEmployeeId(Long employeeId);

    List<Timesheet> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Timesheet> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    List<Timesheet> findByStatus(String status);

    @Query("SELECT t FROM Timesheet t JOIN FETCH t.employee WHERE t.date BETWEEN :startDate AND :endDate")
    List<Timesheet> findByDateBetweenWithEmployee(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    List<Timesheet> findByDate(LocalDate date);

    boolean existsByEmployeeIdAndDate(Long id, LocalDate date);

    Timesheet findByEmployeeIdAndDate(Long id, LocalDate date);
}