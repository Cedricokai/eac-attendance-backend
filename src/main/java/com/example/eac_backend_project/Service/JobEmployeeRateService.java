package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.JobEmployeeRate;
import com.example.eac_backend_project.repository.JobEmployeeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobEmployeeRateService {

    private final JobEmployeeRateRepository rateRepository;

    public List<JobEmployeeRate> getRatesByJobId(Long jobId) {
        return rateRepository.findByJobId(jobId);
    }

    public JobEmployeeRate getRate(Long jobId, Long employeeId) {
        return rateRepository.findByJobIdAndEmployeeId(jobId, employeeId).orElse(null);
    }

    @Transactional
    public JobEmployeeRate saveRate(Job job, Employee employee, Double normalRate, Double overtimeRate) {
        JobEmployeeRate rate = rateRepository.findByJobIdAndEmployeeId(job.getId(), employee.getId())
                .orElse(new JobEmployeeRate());
        rate.setJob(job);
        rate.setEmployee(employee);
        rate.setNormalHourRate(normalRate);
        rate.setOvertimeRate(overtimeRate);
        return rateRepository.save(rate);
    }

    @Transactional
    public JobEmployeeRate saveRate(Job job, Employee employee, Double normalRate, Double overtimeRate,
                                   Double payrollRate, java.time.LocalDate from, java.time.LocalDate to) {
        if (payrollRate != null && (!Double.isFinite(payrollRate) || payrollRate < 0 || from == null))
            throw new IllegalArgumentException("Effective payroll rate requires a non-negative rate and start date");
        if (to != null && (from == null || to.isBefore(from)))
            throw new IllegalArgumentException("Effective end date must be on or after start date");
        JobEmployeeRate rate = saveRate(job, employee, normalRate, overtimeRate);
        rate.setEffectivePayrollRate(payrollRate);
        rate.setEffectivePayrollRateFrom(from);
        rate.setEffectivePayrollRateTo(to);
        return rateRepository.save(rate);
    }

    @Transactional
    public void deleteRate(Long jobId, Long employeeId) {
        rateRepository.deleteByJobIdAndEmployeeId(jobId, employeeId);
    }

    @Transactional
    public void deleteRatesByJobId(Long jobId) {
        rateRepository.deleteByJobId(jobId);
    }
}