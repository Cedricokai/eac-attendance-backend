package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {

    List<LoanRequest> findByProcessedBy(String processedBy);

    List<LoanRequest> findByStatus(String status);

    @Query("SELECT l FROM LoanRequest l WHERE l.employee.id = :employeeId " +
            "AND l.status = 'Approved' " +
            "AND l.isActive = true " +
            "AND (l.approvedAmount - COALESCE(l.paidAmount, 0)) > 0")
    List<LoanRequest> findActiveLoansByEmployee(@Param("employeeId") Long employeeId);

    @Query("SELECT l FROM LoanRequest l WHERE l.employee.id = :employeeId " +
            "AND l.status IN ('Approved', 'Aggregated') " +
            "ORDER BY l.requestDate DESC")
    List<LoanRequest> findAllLoansByEmployee(@Param("employeeId") Long employeeId);

    @Query("SELECT l FROM LoanRequest l WHERE l.employee.id = :employeeId " +
            "AND l.status = 'Approved' " +
            "AND l.isActive = true")
    List<LoanRequest> findApprovedActiveLoans(@Param("employeeId") Long employeeId);

    @Query("SELECT l FROM LoanRequest l WHERE l.previousLoanId = :loanId")
    List<LoanRequest> findLoansConsolidatedFrom(@Param("loanId") Long loanId);

    @Query("SELECT l FROM LoanRequest l WHERE l.status = 'Aggregated' AND l.employee.id = :employeeId")
    List<LoanRequest> findAggregatedLoansByEmployee(@Param("employeeId") Long employeeId);

    @Query("SELECT l FROM LoanRequest l WHERE l.isActive = true AND l.employee.id = :employeeId")
    Optional<LoanRequest> findCurrentActiveLoan(@Param("employeeId") Long employeeId);

    @Query("SELECT l FROM LoanRequest l WHERE l.isActive = true AND l.status = 'Approved' AND l.isAggregated = false AND (l.approvedAmount > l.paidAmount OR l.loanAmount > l.paidAmount)")
    List<LoanRequest> findActiveLoansForDeduction();

    @Query("SELECT l FROM LoanRequest l WHERE l.employee.id = :employeeId AND l.isActive = true AND l.status = 'Approved'")
    List<LoanRequest> findActiveApprovedLoansByEmployee(@Param("employeeId") Long employeeId);

    @Query("SELECT l FROM LoanRequest l WHERE l.employee.id = :employeeId AND l.isActive = true AND l.status = 'Approved' AND (l.isAggregated IS NULL OR l.isAggregated = false)")
    List<LoanRequest> findActiveNonAggregatedLoansByEmployee(@Param("employeeId") Long employeeId);

    @Query("SELECT l FROM LoanRequest l WHERE l.employee.id = :employeeId AND l.status = 'Completed' ORDER BY l.completedDate DESC")
    List<LoanRequest> findCompletedLoansByEmployee(@Param("employeeId") Long employeeId);

}