package com.example.eac_backend_project.repository;

 import com.example.eac_backend_project.entity.attendance.PayrollRecord;
 import org.springframework.data.jpa.repository.JpaRepository;

 import java.util.List;
 import java.util.Optional;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {
    List<PayrollRecord> findByPeriodId(Long periodId);

    void deleteByPeriodId(Long periodId);

    Optional<PayrollRecord> findByPeriodIdAndEmployeeId(Long periodId, Long employeeId);

}
