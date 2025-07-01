package com.eacattendance.Controller;

import com.eacattendance.Exceptions.ResourceNotFoundException;
import com.eacattendance.Service.PayrollService;
import com.eacattendance.entity.PayrollPeriod;
import com.eacattendance.entity.PayrollRecord;
import com.eacattendance.repository.PayrollPeriodRepository;
import com.eacattendance.repository.PayrollRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
@CrossOrigin(origins = "http://localhost:5173")
public class PayrollController {
    private final PayrollService payrollService;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final PayrollRecordRepository payrollRecordRepository;

    public PayrollController(PayrollService payrollService, PayrollPeriodRepository payrollPeriodRepository, PayrollRecordRepository payrollRecordRepository) {
        this.payrollService = payrollService;
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.payrollRecordRepository = payrollRecordRepository;
    }

    @GetMapping("/periods")
    public List<PayrollPeriod> getAllPeriods() {
        return payrollService.getAllPeriods();
    }

    @PostMapping("/periods")
    public ResponseEntity<PayrollPeriod> createPeriod(@RequestBody PayrollPeriod period) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createPeriod(period));
    }

    @GetMapping
    public ResponseEntity<List<PayrollRecord>> getPayroll(@RequestParam Long periodId) {
        return ResponseEntity.ok(payrollService.getPayrollForPeriod(periodId));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generatePayroll(@RequestParam Long periodId) {
        payrollService.generatePayroll(periodId);
        return ResponseEntity.ok(Map.of("message", "Payroll generated"));
    }

    @PostMapping("/process")
    public ResponseEntity<?> processPayroll(@RequestParam Long periodId) {
        payrollService.processPayroll(periodId);
        return ResponseEntity.ok(Map.of("message", "Payroll processed"));
    }

    // Add to PayrollController.java

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPayrollSummary() {
        // Get the most recent processed payroll
        PayrollPeriod latestPeriod = payrollPeriodRepository.findTopByOrderByEndDateDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No payroll periods found"));

        List<PayrollRecord> records = payrollRecordRepository.findByPeriodId(latestPeriod.getId());

        double totalAmount = records.stream()
                .mapToDouble(r -> r.getRegularPay() + r.getOvertimePay())
                .sum();

        return ResponseEntity.ok(Map.of(
                "totalAmount", totalAmount,
                "employeeCount", records.size(),
                "periodName", latestPeriod.getName(),
                "periodDates", latestPeriod.getStartDate() + " to " + latestPeriod.getEndDate()
        ));
    }
}
