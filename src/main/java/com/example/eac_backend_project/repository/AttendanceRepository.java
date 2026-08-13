package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.dto.AttendanceListDTO;
import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.attendance.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // =========================================================
    // BASIC ATTENDANCE QUERIES
    // =========================================================

    List<Attendance> findByEmployee(Employee employee);

    List<Attendance> findByDate(LocalDate date);

    List<Attendance> findByDateAndCheckInIsNotNull(LocalDate date);

    Optional<Attendance> findByEmployeeIdAndDate(
            Long employeeId,
            LocalDate date
    );

    boolean existsByEmployeeIdAndDate(
            Long employeeId,
            LocalDate date
    );

    List<Attendance> findByEmployeeIdAndDateBetween(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Attendance> findTopByEmployeeIdOrderByDateDesc(
            Long employeeId
    );

    // =========================================================
    // CHECK-IN / CHECK-OUT QUERIES
    // =========================================================

    @Query("""
        SELECT a
        FROM Attendance a
        WHERE a.employee.id = :employeeId
          AND a.date = :date
          AND a.checkIn IS NOT NULL
        """)
    Optional<Attendance> findCheckInRecord(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT a
        FROM Attendance a
        WHERE a.employee.id = :employeeId
          AND a.date = :date
          AND a.checkOut IS NULL
        """)
    Optional<Attendance> findOpenAttendance(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT a
        FROM Attendance a
        WHERE a.date = :date
          AND (
              a.checkIn IS NULL
              OR a.checkOut IS NULL
          )
        """)
    List<Attendance> findIncompleteAttendances(
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT a
        FROM Attendance a
        WHERE a.employee.id = :employeeId
          AND a.date BETWEEN :startDate AND :endDate
        """)
    List<Attendance> findByEmployeeAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // =========================================================
    // MOBILE ATTENDANCE QUERIES
    // =========================================================

    boolean existsByEmployeeIdAndDateAndCheckInIsNotNull(
            Long employeeId,
            LocalDate date
    );

    boolean existsByEmployeeIdAndDateAndCheckOutIsNotNull(
            Long employeeId,
            LocalDate date
    );

    Optional<Attendance>
    findFirstByEmployeeIdAndBiometricTrueOrderByTimestampDesc(
            Long employeeId
    );

    List<Attendance> findByBiometricTrue();

    // =========================================================
    // DATE-RANGE QUERIES
    // =========================================================

    List<Attendance> findByDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    Page<Attendance> findByDateBetween(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    List<Attendance> findByDateBetweenOrderByDateDesc(
            LocalDate startDate,
            LocalDate endDate
    );

    // =========================================================
    // ATTENDANCE SUMMARY QUERIES
    // =========================================================

    @Query("""
        SELECT
            a.employee.id,
            COUNT(DISTINCT a.date),
            COALESCE(SUM(a.minimumHour), 0)
        FROM Attendance a
        WHERE a.date BETWEEN :startDate AND :endDate
          AND a.status IN (
              'Present',
              'Holiday Present',
              'Weekend Present'
          )
          AND a.minimumHour > 0
        GROUP BY a.employee.id
        """)
    List<Object[]> summarizeAttendanceBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT
            a.employee.id,
            a.date,
            SUM(a.minimumHour)
        FROM Attendance a
        WHERE a.date BETWEEN :startDate AND :endDate
        GROUP BY a.employee.id, a.date
        """)
    List<Object[]> findDailyHoursByPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT DISTINCT
            a.employee.id,
            a.date
        FROM Attendance a
        WHERE a.date BETWEEN :startDate AND :endDate
        """)
    List<Object[]> findAttendanceDatesByPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT
            a.employee.id,
            a.date,
            a.minimumHour,
            a.status
        FROM Attendance a
        WHERE a.date BETWEEN :startDate AND :endDate
        ORDER BY a.employee.id, a.date
        """)
    List<Object[]> findAllAttendanceWithStatusBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT
            a.employee.id,
            SUM(a.minimumHour),
            COUNT(a.id)
        FROM Attendance a
        WHERE a.date BETWEEN :startDate AND :endDate
          AND a.status IN (
              'Present',
              'Holiday Present',
              'Weekend Present',
              'On Leave'
          )
        GROUP BY a.employee.id
        """)
    List<Object[]> summarizeAllPresentAttendanceBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // =========================================================
    // PAYROLL QUERIES
    // =========================================================

    @Query("""
        SELECT
            a.employee.id,
            a.date,
            a.minimumHour,
            a.status
        FROM Attendance a
        WHERE a.date BETWEEN :startDate AND :endDate
          AND a.status IN (
              'Present',
              'Holiday Present',
              'Weekend Present',
              'On Leave'
          )
          AND a.minimumHour > 0
        ORDER BY a.employee.id, a.date
        """)
    List<Object[]> findValidAttendanceForPayroll(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT COUNT(h)
        FROM Holiday h
        WHERE (
            h.recurring = true
            AND MONTH(h.date) = MONTH(:date)
            AND DAY(h.date) = DAY(:date)
        )
        OR (
            h.recurring = false
            AND h.date = :date
        )
        """)
    Long countHolidaysOnDate(
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT
            a.employee.id,
            a.date,
            a.status
        FROM Attendance a
        WHERE a.date BETWEEN :startDate AND :endDate
          AND a.status IN ('On Leave', 'Leave')
        ORDER BY a.employee.id, a.date
        """)
    List<Object[]> findAttendanceWithLeaveStatus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // =========================================================
    // ATTENDANCE REPORT QUERIES
    // =========================================================

    @Query("""
        SELECT a
        FROM Attendance a
        JOIN FETCH a.employee e
        WHERE a.date BETWEEN :startDate AND :endDate
        ORDER BY e.employeeId, a.date
        """)
    List<Attendance> findAllForReportBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // =========================================================
    // LATE / ABSENT QUERIES
    // =========================================================

    @Query("""
        SELECT a
        FROM Attendance a
        WHERE a.date = :date
          AND a.status IN ('Late', 'Absent')
        """)
    List<Attendance> findLateAndAbsentByDate(
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT a
        FROM Attendance a
        WHERE a.date BETWEEN :startDate AND :endDate
          AND a.status IN ('Late', 'Absent')
        """)
    List<Attendance> findLateAndAbsentBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // =========================================================
    // CATEGORY-BASED QUERIES
    // =========================================================

    @Query("""
        SELECT
            e.id,
            COUNT(a),
            COALESCE(SUM(a.minimumHour), 0)
        FROM Attendance a
        JOIN a.employee e
        WHERE a.date BETWEEN :startDate AND :endDate
          AND a.status IN ('Present', 'Late', 'Overtime')
          AND e.category.name = :category
        GROUP BY e.id
        """)
    List<Object[]> summarizeAllPresentAttendanceBetweenDatesForCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category
    );

    @Query("""
        SELECT
            a.employee.id,
            a.date,
            a.minimumHour,
            a.status
        FROM Attendance a
        JOIN a.employee e
        WHERE a.date BETWEEN :startDate AND :endDate
          AND a.status IN (
              'Present',
              'Holiday Present',
              'Weekend Present',
              'On Leave'
          )
          AND a.minimumHour > 0
          AND e.category.name = :category
        ORDER BY a.employee.id, a.date
        """)
    List<Object[]> findValidAttendanceForPayrollForCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category
    );

    @Query("""
        SELECT
            a.employee.id,
            a.date,
            a.status
        FROM Attendance a
        JOIN a.employee e
        WHERE a.date BETWEEN :startDate AND :endDate
          AND a.status IN ('On Leave', 'Leave')
          AND e.category.name = :category
        ORDER BY a.employee.id, a.date
        """)
    List<Object[]> findAttendanceWithLeaveStatusForCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category
    );

    @Query("""
        SELECT DISTINCT
            a.employee.id,
            a.date
        FROM Attendance a
        JOIN a.employee e
        WHERE a.date BETWEEN :startDate AND :endDate
          AND e.category.name = :category
        """)
    List<Object[]> findAttendanceDatesByPeriodForCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category
    );

    @Query("""
        SELECT
            a.employee.id,
            a.date,
            SUM(a.minimumHour)
        FROM Attendance a
        JOIN a.employee e
        WHERE a.date BETWEEN :startDate AND :endDate
          AND e.category.name = :category
        GROUP BY a.employee.id, a.date
        """)
    List<Object[]> findDailyHoursByPeriodForCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") String category
    );

    @Query("""
        SELECT DISTINCT a.employee.id
        FROM Attendance a
        WHERE a.date BETWEEN :startDate AND :endDate
        """)
    List<Object[]> findEmployeeIdsInPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // =========================================================
    // PAGINATED ATTENDANCE DTO QUERY
    // =========================================================

    @Query(
            value = """
            SELECT new com.example.eac_backend_project.dto.AttendanceListDTO(
                a.id,
                a.date,
                a.checkIn,
                a.checkOut,
                a.minimumHour,
                a.overtime,
                a.standardHours,
                a.shift,
                a.workType,
                a.status,
                a.biometric,
                e.id,
                e.employeeId,
                e.firstName,
                e.lastName,
                c.id,
                c.name
            )
            FROM Attendance a
            JOIN a.employee e
            LEFT JOIN e.category c
            WHERE a.date BETWEEN :startDate AND :endDate
              AND (
                  :categoryId IS NULL
                  OR c.id = :categoryId
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
              )
            """,
            countQuery = """
            SELECT COUNT(a.id)
            FROM Attendance a
            JOIN a.employee e
            LEFT JOIN e.category c
            WHERE a.date BETWEEN :startDate AND :endDate
              AND (
                  :categoryId IS NULL
                  OR c.id = :categoryId
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
              )
            """
    )
    Page<AttendanceListDTO> findAttendancePage(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            Pageable pageable
    );
}