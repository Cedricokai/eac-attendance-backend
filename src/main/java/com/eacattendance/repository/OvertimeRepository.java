package com.eacattendance.repository;

import com.eacattendance.entity.Employee;
import com.eacattendance.entity.Overtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OvertimeRepository extends JpaRepository<Overtime, Long> {
    List<Overtime> findByEmployeeId(Long employeeId);
    List<Overtime> findByEmployeeAndDate(Employee employee, LocalDate date);
    List<Overtime> findByDate(LocalDate date);
    List<Overtime> findByStatus(String status);

    @Query("SELECT o FROM Overtime o WHERE o.date BETWEEN :startDate AND :endDate")
    List<Overtime> findByDateRange(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
}

