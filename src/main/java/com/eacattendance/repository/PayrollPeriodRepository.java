package com.eacattendance.repository;

import com.eacattendance.entity.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {
    Optional<PayrollPeriod> findTopByOrderByEndDateDesc();
}
