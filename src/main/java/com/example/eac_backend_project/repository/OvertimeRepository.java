package com.example.eac_backend_project.repository;


import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Overtime;
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

    @Query("SELECT o.employee.id, COALESCE(SUM(o.overtimeHours), 0) " +
            "FROM Overtime o " +
            "WHERE o.date BETWEEN :startDate AND :endDate " +
            "AND o.status = 'Approved' " +
            "GROUP BY o.employee.id")
    List<Object[]> summarizeOvertimeBetweenDates(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
}

