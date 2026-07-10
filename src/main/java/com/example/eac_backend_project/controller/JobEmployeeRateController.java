package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.JobEmployeeRateService;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.JobEmployeeRate;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job-employee-rates")
@RequiredArgsConstructor
public class JobEmployeeRateController {

    private final JobEmployeeRateService rateService;
    private final JobRepository jobRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<JobEmployeeRate>> getRatesByJob(@PathVariable Long jobId) {
        List<JobEmployeeRate> rates = rateService.getRatesByJobId(jobId);
        return ResponseEntity.ok(rates);
    }

    @PostMapping("/job/{jobId}")
    public ResponseEntity<?> saveRates(
            @PathVariable Long jobId,
            @RequestBody List<Map<String, Object>> ratesData) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        for (Map<String, Object> data : ratesData) {
            Long employeeId = ((Number) data.get("employeeId")).longValue();
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
            Double normalRate = ((Number) data.get("normalHourRate")).doubleValue();
            Double overtimeRate = ((Number) data.get("overtimeRate")).doubleValue();
            rateService.saveRate(job, employee, normalRate, overtimeRate);
        }
        return ResponseEntity.ok().build();
    }
}