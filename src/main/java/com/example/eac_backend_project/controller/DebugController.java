package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/employee/{id}")
    public ResponseEntity<?> getEmployeeDebugInfo(@PathVariable Long id) {
        try {
            Employee employee = employeeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("id", employee.getId());
            debugInfo.put("name", employee.getFirstName() + " " + employee.getLastName());
            debugInfo.put("basicSalary", employee.getBasicSalary());
            debugInfo.put("minimumRate", employee.getMinimumRate());
            debugInfo.put("rentAllowance", employee.getRentAllowance());
            debugInfo.put("transportAllowance", employee.getTransportAllowance());
            debugInfo.put("clothingAllowance", employee.getClothingAllowance());
            debugInfo.put("otherAllowance", employee.getOtherAllowance());

            // Calculate derived values
            double hourlyFromBasic = employee.getBasicSalary() != null ?
                    employee.getBasicSalary() / (22 * 8) : 0.0;
            debugInfo.put("hourlyRateFromBasic", hourlyFromBasic);
            debugInfo.put("effectiveHourlyRate",
                    employee.getMinimumRate() != null ? employee.getMinimumRate() : hourlyFromBasic);

            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}