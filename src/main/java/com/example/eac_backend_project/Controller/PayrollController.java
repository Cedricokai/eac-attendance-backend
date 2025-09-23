package com.example.eac_backend_project.Controller;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.PayrollService;
import com.example.eac_backend_project.entity.attendance.PayrollPeriod;
import com.example.eac_backend_project.entity.attendance.PayrollRecord;
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

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
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
        try {
            payrollService.generatePayroll(periodId);
            return ResponseEntity.ok(Map.of("message", "Payroll generated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate payroll: " + e.getMessage()));
        }
    }

    @PostMapping("/process")
    public ResponseEntity<?> processPayroll(@RequestParam Long periodId) {
        try {
            payrollService.processPayroll(periodId);
            return ResponseEntity.ok(Map.of("message", "Payroll processed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process payroll: " + e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPayrollSummary() {
        try {
            return ResponseEntity.ok(payrollService.getPayrollSummary());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get payroll summary: " + e.getMessage()));
        }
    }
}