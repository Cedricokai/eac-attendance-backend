package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(Job.JobStatus status);

    List<Job> findByClientContainingIgnoreCase(String client);

    List<Job> findByNameContainingIgnoreCase(String name);

    @Query("SELECT j FROM Job j WHERE j.startDate <= :date AND (j.endDate IS NULL OR j.endDate >= :date)")
    List<Job> findActiveJobsOnDate(@Param("date") LocalDate date);

    @Query("SELECT j FROM Job j WHERE " +
            "LOWER(j.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(j.client) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(j.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Job> searchJobs(@Param("search") String search);

    Optional<Job> findByNameAndClient(String name, String client);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = 'ACTIVE'")
    Long countActiveJobs();

    @Query("SELECT j FROM Job j WHERE j.budget IS NOT NULL AND j.budget > 0 ORDER BY j.budget DESC")
    List<Job> findJobsWithBudget();

    @Query("""
    SELECT j
    FROM Job j
    WHERE j.status = com.example.eac_backend_project.entity.attendance.Job$JobStatus.ACTIVE
      AND j.nextBillingDate IN (:today, :tomorrow)
""")
    List<Job> findActiveJobsDueOnEither(
            @Param("today") LocalDate today,
            @Param("tomorrow") LocalDate tomorrow
    );

    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE' AND j.billingReminderEnabled = true " +
            "AND j.nextBillingDate IS NOT NULL " +
            "AND j.billingReminderEmails IS NOT NULL AND j.billingReminderEmails != ''")
    List<Job> findAllActiveJobsWithBillingReminderEnabled();

    @Query("SELECT j FROM Job j WHERE j.nextBillingDate = :date AND j.billingReminderEnabled = true " +
            "AND j.billingReminderEmails IS NOT NULL AND j.billingReminderEmails != ''")
    List<Job> findByNextBillingDateAndBillingReminderEnabledTrue(@Param("date") LocalDate date);

    @Query("SELECT j FROM Job j WHERE j.nextBillingDate < :date AND j.billingReminderEnabled = true " +
            "AND j.enableOverdueReminders = true " +
            "AND j.billingReminderEmails IS NOT NULL AND j.billingReminderEmails != ''")
    List<Job> findOverdueBillingReminders(@Param("date") LocalDate date);

    @Modifying
    @Query(value = "DELETE FROM job_employee_assignment WHERE job_id = ?1", nativeQuery = true)
    void deleteEmployeeAssignmentsByJobId(Long jobId);
}