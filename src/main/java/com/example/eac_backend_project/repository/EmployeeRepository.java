package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeId(String employeeId);

    @Override
    Optional<Employee> findById(Long id);

    List<Employee> findByActiveTrue();

    List<Employee> findByActiveFalse();

    Optional<Employee> findByIdAndActiveTrue(Long id);

    List<Employee> findByExcludeFromSsnitTrue();

    @Query("SELECT e FROM Employee e WHERE e.id NOT IN " +
            "(SELECT t.employee.id FROM Timesheet t WHERE t.date = :date AND t.status = 'APPROVED')")
    List<Employee> findEmployeesWithoutApprovedTimesheet(@Param("date") LocalDate date);

    @Query("SELECT e FROM Employee e WHERE e.id IN " +
            "(SELECT t.employee.id FROM Timesheet t WHERE t.date BETWEEN :startDate AND :endDate)")
    List<Employee> findEmployeesWithTimesheetsInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    long countByStartDate(LocalDate startDate);
    Optional<Employee> findByEmail(String email);


    List<Employee> findByJobId(Long jobId);


    List<Employee> findBySupervisedJob(Job job);

    List<Employee> findByCategoryName(String categoryName);

    Long countByActive(boolean b);

    Long countByJobNotNull();

    Long countByIsSupervisor(boolean b);



    // SIMPLIFIED: Find supervisors by department
    @Query("SELECT e FROM Employee e WHERE e.department = :department AND e.isSupervisor = true")
    List<Employee> findSupervisorsByDepartment(@Param("department") String department);

    // Find employees by role - SIMPLIFIED VERSION
    @Query("SELECT e FROM Employee e WHERE e.jobPosition = :jobPosition")
    List<Employee> findByJobPosition(@Param("jobPosition") String jobPosition);

    // SIMPLE method to find planners
    List<Employee> findByJobPositionContainingIgnoreCase(String position);


    // FIXED: Find supervisors by category
    @Query("SELECT e FROM Employee e WHERE e.category.id = :categoryId AND e.isSupervisor = true")
    List<Employee> findSupervisorsByCategory(@Param("categoryId") Long categoryId);

    // Alternative: Find supervisors by category name
    @Query("SELECT e FROM Employee e WHERE e.category.name = :categoryName AND e.isSupervisor = true")
    List<Employee> findSupervisorsByCategoryName(@Param("categoryName") String categoryName);

    // Find supervisors for a specific employee's category
    @Query("SELECT e FROM Employee e WHERE e.category.id = " +
            "(SELECT emp.category.id FROM Employee emp WHERE emp.id = :employeeId) " +
            "AND e.isSupervisor = true")
    List<Employee> findSupervisorsForEmployee(@Param("employeeId") Long employeeId);
    @Query("SELECT e FROM Employee e WHERE " +
            "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Employee> searchEmployees(@Param("searchTerm") String searchTerm);

    // Find employees who are supervisors for a specific job
    List<Employee> findByJobIdAndIsSupervisorTrue(Long jobId);

    List<Employee> findByIsSupervisorTrue();

    // Find all supervisors (employees with isSupervisor = true)
    @Query("SELECT e FROM Employee e WHERE e.isSupervisor = true")
    List<Employee> findAllSupervisors();

    @Query("""
    SELECT e FROM Employee e 
    WHERE e.job.id = :jobId 
    AND e.isSupervisor = true
""")
    List<Employee> findSupervisorsByJobId(@Param("jobId") Long jobId);


    // Find supervisors by category (if needed)
    @Query("SELECT e FROM Employee e WHERE e.category.id = :categoryId AND e.isSupervisor = true")
    List<Employee> findSupervisorsByCategoryId(@Param("categoryId") Long categoryId);

    // Find employees supervised by a specific supervisor
    @Query("SELECT e FROM Employee e WHERE e.supervisor.id = :supervisorId")
    List<Employee> findEmployeesBySupervisorId(@Param("supervisorId") Long supervisorId);



    List<Employee> findByGhanaCardIn(List<String> ghanaCards);
    List<Employee> findBySsnitNumberIn(List<String> ssnitNumbers);
    List<Employee> findByTinNumberIn(List<String> tinNumbers);
    List<Employee> findByTagNumberIn(List<String> tagNumbers);
    List<Employee> findByAccountNumberIn(List<String> accountNumbers);
    List<Employee> findByEmailIn(List<String> emails);

    @Query("SELECT e FROM Employee e WHERE " +
            "LOWER(CONCAT(e.firstName, ' ', e.lastName)) = LOWER(:fullName)")
    Optional<Employee> findByFullName(@Param("fullName") String fullName);

    // Find by firstName OR lastName with flexible matching
    @Query("SELECT e FROM Employee e WHERE " +
            "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Employee> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    // Find by exact firstName and lastName
    Optional<Employee> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);


    @Query("SELECT e FROM Employee e WHERE " +
            "LOWER(CONCAT(e.firstName, ' ', e.lastName)) = LOWER(:name)")
    Optional<Employee> findByNameCaseInsensitive(@Param("name") String name);

    @Query("SELECT e FROM Employee e WHERE e.active = true ORDER BY e.firstName, e.lastName")
    List<Employee> findAllActiveEmployees();


}
