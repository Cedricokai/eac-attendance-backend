package com.eacattendance.repository;

import com.eacattendance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Override
    Optional<Employee> findById(Long id);

    @Query("SELECT e FROM Employee e WHERE e.id NOT IN " +
            "(SELECT t.employee.id FROM Timesheet t WHERE t.date = :date AND t.status = 'APPROVED')")
    List<Employee> findEmployeesWithoutApprovedTimesheet(@Param("date") LocalDate date);

    @Query("SELECT e FROM Employee e WHERE e.id IN " +
            "(SELECT t.employee.id FROM Timesheet t WHERE t.date BETWEEN :startDate AND :endDate)")
    List<Employee> findEmployeesWithTimesheetsInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
