package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.PayrollRecord;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    // Existing methods
    List<PayrollRecord> findByPeriodId(Long periodId);

    List<PayrollRecord> findByEmployeeIdOrderByPeriodStartDateDesc(Long employeeId);

    Optional<PayrollRecord> findByPeriodIdAndEmployeeId(Long periodId, Long employeeId);

    void deleteByPeriodId(Long periodId);

    // NEW METHOD: Find payroll records by employee ID and period date range
    @Query("SELECT pr FROM PayrollRecord pr WHERE pr.employee.id = :employeeId AND pr.period.startDate BETWEEN :startDate AND :endDate ORDER BY pr.period.startDate DESC")
    List<PayrollRecord> findByEmployeeIdAndPeriodStartDateBetween(@Param("employeeId") Long employeeId,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);

    // Additional useful methods you might need:

    @Query("SELECT pr FROM PayrollRecord pr WHERE pr.appliedCreditIds IS NOT NULL AND pr.appliedCreditIds LIKE %:creditId%")
    List<PayrollRecord> findRecordsByAppliedCreditId(@Param("creditId") String creditId);

    // Find payroll records by status
    List<PayrollRecord> findByStatus(String status);

    // Find payroll records by employee and status
    List<PayrollRecord> findByEmployeeIdAndStatus(Long employeeId, String status);

    // Find payroll records by period ID and status
    List<PayrollRecord> findByPeriodIdAndStatus(Long periodId, String status);

    // Check if payroll record exists for employee in period
    boolean existsByEmployeeIdAndPeriodId(Long employeeId, Long periodId);

    // Find payroll records with gross salary greater than specified amount
    @Query("SELECT pr FROM PayrollRecord pr WHERE pr.grossSalary > :minSalary")
    List<PayrollRecord> findByGrossSalaryGreaterThan(@Param("minSalary") Double minSalary);

    // Get total payroll amount for a period
    @Query("SELECT COALESCE(SUM(pr.netSalary), 0) FROM PayrollRecord pr WHERE pr.period.id = :periodId")
    Double getTotalNetSalaryByPeriodId(@Param("periodId") Long periodId);

    // Get payroll records with leave days
    @Query("SELECT pr FROM PayrollRecord pr WHERE pr.leaveDays > 0 AND pr.period.id = :periodId")
    List<PayrollRecord> findWithLeaveDaysByPeriodId(@Param("periodId") Long periodId);

    // Get payroll records by employee category
    @Query("SELECT pr FROM PayrollRecord pr WHERE pr.employee.category = :category AND pr.period.id = :periodId")
    List<PayrollRecord> findByEmployeeCategoryAndPeriodId(@Param("category") String category,
                                                          @Param("periodId") Long periodId);

    // Get latest payroll record for an employee
    @Query("SELECT pr FROM PayrollRecord pr WHERE pr.employee.id = :employeeId ORDER BY pr.period.endDate DESC LIMIT 1")
    Optional<PayrollRecord> findLatestByEmployeeId(@Param("employeeId") Long employeeId);

    // Add to PayrollRecordRepository.java


    // FIXED: Use Object[] instead of DTO instantiation
    @Query("SELECT " +
            "SUM(pr.netSalary) as totalNetSalary, " +
            "SUM(pr.payeTax) as totalTax, " +
            "SUM(pr.ssnitEmployee) as totalSsnitEmployee, " +
            "SUM(pr.ssnitEmployer) as totalSsnitEmployer, " +
            "SUM(pr.grossSalary) as totalGrossSalary, " +
            "COUNT(pr) as employeeCount, " +
            "FUNCTION('DATE_FORMAT', pr.period.endDate, '%Y-%m') as yearMonth, " +
            "pr.period.name as periodName, " +
            "CAST(pr.period.id as string) as periodId " +
            "FROM PayrollRecord pr " +
            "WHERE pr.period.category = :category " +
            "GROUP BY FUNCTION('DATE_FORMAT', pr.period.endDate, '%Y-%m'), pr.period.name, pr.period.id " +
            "ORDER BY pr.period.endDate DESC")
    List<Object[]> getMonthlyAggregatesByCategory(@Param("category") String category);

    @Query("SELECT DISTINCT FUNCTION('date_format', p.endDate, '%Y-%m') as month " +
            "FROM PayrollRecord pr " +
            "JOIN pr.period p " +
            "ORDER BY month DESC")
    List<String> findAllMonthsWithRecords();

    @Query("SELECT DISTINCT pr.period.category FROM PayrollRecord pr WHERE pr.period.category IS NOT NULL")
    List<String> findAllCategoriesWithRecords();

    // Add to PayrollRecordRepository.java

    @Query("SELECT pr FROM PayrollRecord pr WHERE pr.employee.category.name = :category ORDER BY pr.period.endDate DESC")
    List<PayrollRecord> findByEmployeeCategory(@Param("category") String category);

    /**
     * Count payroll records by period ID
     */
    @Query("SELECT COUNT(p) FROM PayrollRecord p WHERE p.period.id = :periodId")
    int countByPeriodId(@Param("periodId") Long periodId);

}