package com.eacattendance.repository;

import com.eacattendance.entity.Attendance;
import com.eacattendance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Basic queries
    List<Attendance> findByEmployee(Employee employee);
    List<Attendance> findByDate(LocalDate date);

    // Mobile integration specific queries
    Optional<Attendance> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :employeeId AND a.date = :date AND a.checkIn IS NOT NULL")
    Optional<Attendance> findCheckInRecord(@Param("employeeId") Long employeeId,
                                           @Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :employeeId AND a.date = :date AND a.checkOut IS NULL")
    Optional<Attendance> findOpenAttendance(@Param("employeeId") Long employeeId,
                                            @Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND (a.checkIn IS NULL OR a.checkOut IS NULL)")
    List<Attendance> findIncompleteAttendances(@Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :employeeId AND a.date BETWEEN :startDate AND :endDate")
    List<Attendance> findByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    Optional<Attendance> findTopByEmployeeIdOrderByDateDesc(Long employeeId);

    List<Attendance> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);

    // For mobile app verification
    boolean existsByEmployeeIdAndDateAndCheckInIsNotNull(Long employeeId, LocalDate date);
    boolean existsByEmployeeIdAndDateAndCheckOutIsNotNull(Long employeeId, LocalDate date);

    Optional<Attendance> findFirstByEmployeeIdAndBiometricTrueOrderByTimestampDesc(Long employeeId);

    List<Attendance> findByBiometricTrue();

    List<Attendance> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT a.employee.id, COUNT(DISTINCT a.date), SUM(a.minimumHour) " +
            "FROM Attendance a WHERE a.date BETWEEN :startDate AND :endDate " +
            "GROUP BY a.employee.id")
    List<Object[]> summarizeAttendanceBetweenDates(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);


}