// LoanDeductionHistoryRepository.java
package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.LoanDeductionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanDeductionHistoryRepository extends JpaRepository<LoanDeductionHistory, Long> {

    boolean existsByPayrollPeriodId(Long payrollPeriodId);

    boolean existsByLoanIdAndPayrollPeriodId(Long loanId, Long payrollPeriodId);

    List<LoanDeductionHistory> findByPayrollPeriodId(Long payrollPeriodId);

    List<LoanDeductionHistory> findByLoanId(Long loanId);

    @Query("SELECT SUM(l.deductionAmount) FROM LoanDeductionHistory l WHERE l.loanId = :loanId")
    Double getTotalDeductedForLoan(@Param("loanId") Long loanId);

    void deleteByPayrollPeriodId(Long payrollPeriodId);
}