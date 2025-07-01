package com.eacattendance.repository;

import com.eacattendance.entity.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {
    List<PayrollRecord> findByPeriodId(Long periodId);

    void deleteByPeriodId(Long periodId);

}
