package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.EmployeeService;
import com.example.eac_backend_project.Service.SmsService;
import com.example.eac_backend_project.dto.EmployeeBulkDTO;
import com.example.eac_backend_project.dto.EmployeeDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.settings.Category;
import com.example.eac_backend_project.repository.CategoryRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SmsService smsService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // Replace or update your existing getAllEmployees method in EmployeeController


    @PostMapping("/{id}/send-sms")
    public ResponseEntity<?> sendSmsToEmployee(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Message text is required"));
            }

            Employee employee = employeeService.getEmployeeById(id);
            String phone = employee.getPhone();
            if (phone == null || phone.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Employee does not have a phone number on file"));
            }

            boolean sent = smsService.sendSms(phone, message);
            if (sent) {
                // Log activity
                // activityLogService.logInfo("SMS sent to employee " + id, "EMPLOYEE", getCurrentUsername(), id);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "SMS sent to " + phone
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to send SMS"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send SMS: " + e.getMessage()));
        }
    }


    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees(
            @RequestParam(required = false) Boolean active
    ) {
        try {
            List<Employee> employees;
            if (active != null) {
                employees = active ?
                        employeeRepository.findByActiveTrue() :
                        employeeRepository.findByActiveFalse();
            } else {
                employees = employeeService.getAllEmployees();
            }

            System.out.println("Found " + employees.size() + " employees" +
                    (active != null ? " (active=" + active + ")" : ""));

            List<EmployeeDTO> employeeDTOs = employees.stream()
                    .map(employeeService::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(employeeDTOs);
        } catch (Exception e) {
            System.err.println("Error in getAllEmployees: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleEmployeeActive(@PathVariable Long id) {
        try {
            Employee employee = employeeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

            employee.setActive(!Boolean.TRUE.equals(employee.getActive()));
            Employee updatedEmployee = employeeRepository.save(employee);

            return ResponseEntity.ok(Map.of(
                    "message", "Employee status updated successfully",
                    "active", updatedEmployee.getActive(),
                    "employeeId", updatedEmployee.getId(),
                    "employeeName", updatedEmployee.getFirstName() + " " + updatedEmployee.getLastName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to toggle employee status", "message", e.getMessage()));
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> createEmployees(@RequestBody List<EmployeeBulkDTO> employees) {
        try {
            System.out.println("=== BULK IMPORT STARTED ===");
            System.out.println("Received " + employees.size() + " employees for bulk import");

            if (employees == null || employees.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No employees provided for import"));
            }

            List<Employee> entities = employees.stream().map(dto -> {
                Employee emp = new Employee();

                // Basic info
                emp.setFirstName(dto.getFirstName());
                emp.setLastName(dto.getLastName());
                emp.setEmail(dto.getEmail());
                emp.setPhone(dto.getPhone());
                emp.setJobPosition(dto.getJobPosition());

                // Category
                if (dto.getCategory() != null) {
                    Category category = categoryRepository.findByName(dto.getCategory()).orElse(null);
                    emp.setCategory(category);
                }

                // Rates and salary
                emp.setMinimumRate(dto.getMinimumRate() != null ? dto.getMinimumRate() : 0.0);
                emp.setHourlyRate(dto.getMinimumRate() != null ? dto.getMinimumRate() : 0.0);
                emp.setOvertimeRate(dto.getMinimumRate() != null ? dto.getMinimumRate() * 1.5 : 0.0);
                emp.setBasicSalary(dto.getBasicSalary() != null ? dto.getBasicSalary() : 0.0);

                // Work type
                emp.setWorkType(dto.getWorkType() != null ? dto.getWorkType() : "Regular");

                // Dates
                emp.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now());
                emp.setEndDate(dto.getEndDate());
                emp.setDateOfBirth(dto.getDateOfBirth());

                // Allowances
                emp.setRentAllowance(dto.getRentAllowance() != null ? dto.getRentAllowance() : 0.0);
                emp.setTransportAllowance(dto.getTransportAllowance() != null ? dto.getTransportAllowance() : 0.0);
                emp.setClothingAllowance(dto.getClothingAllowance() != null ? dto.getClothingAllowance() : 0.0);
                emp.setOtherAllowance(dto.getOtherAllowance() != null ? dto.getOtherAllowance() : 0.0);
                emp.setNssAllowance(dto.getNssAllowance() != null ? dto.getNssAllowance() : 0.0);

                // Personal details
                emp.setSsnitNumber(dto.getSsnitNumber());
                emp.setTinNumber(dto.getTinNumber());
                emp.setAccountNumber(dto.getAccountNumber());
                emp.setTagNumber(dto.getTagNumber());
                emp.setLocation(dto.getLocation());
                emp.setGhanaCard(dto.getGhanaCard());
                emp.setBank(dto.getBank());
                emp.setBankBranch(dto.getBankBranch());
                emp.setContactPerson(dto.getContactPerson());
                emp.setRelationship(dto.getRelationship());
                emp.setTownOfResidence(dto.getTownOfResidence());
                emp.setHouseNumber(dto.getHouseNumber());
                emp.setSpouse(dto.getSpouse());
                emp.setNumberOfChildren(dto.getNumberOfChildren() != null ? dto.getNumberOfChildren() : 0);
                emp.setAge(dto.getAge() != null ? dto.getAge() : 0);

                // Department (if you have this field in Employee entity)
                // emp.setDepartment(dto.getDepartment());

                // Set active status
                emp.setActive(true);

                return emp;
            }).collect(Collectors.toList());

            // Debug logging
            for (int i = 0; i < Math.min(3, entities.size()); i++) {
                Employee emp = entities.get(i);
                System.out.println("Employee " + (i+1) + ": " +
                        emp.getFirstName() + " " + emp.getLastName() +
                        " | Email: " + emp.getEmail() +
                        " | Position: " + emp.getJobPosition() +
                        " | Category: " + (emp.getCategory() != null ? emp.getCategory().getName() : "null") +
                        " | Allowances: R=" + emp.getRentAllowance() +
                        ", T=" + emp.getTransportAllowance() +
                        ", C=" + emp.getClothingAllowance() +
                        ", O=" + emp.getOtherAllowance());
            }

            List<Employee> saved = employeeService.saveEmployees(entities);

            System.out.println("=== BULK IMPORT COMPLETED ===");
            System.out.println("Successfully imported " + saved.size() + " employees");

            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            System.err.println("=== BULK IMPORT FAILED ===");
            System.err.println("Error in bulk import: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error", "Failed to save employees",
                            "message", e.getMessage(),
                            "details", e.getClass().getSimpleName()
                    ));
        }
    }

    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeDTO employeeDTO) {
        try {
            System.out.println("Creating single employee: " + employeeDTO.getFirstName() + " " + employeeDTO.getLastName());

            EmployeeDTO savedEmployee = employeeService.createEmployee(employeeDTO);
            return ResponseEntity.ok(savedEmployee);

        } catch (Exception e) {
            System.err.println("Error creating employee: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to create employee", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        try {
            employeeService.deleteEmployee(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to delete employee", "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody EmployeeDTO employeeDTO) {
        try {
            EmployeeDTO updatedEmployee = employeeService.updateEmployeeFromDTO(id, employeeDTO);
            return ResponseEntity.ok(updatedEmployee);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Employee not found", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update employee", "message", e.getMessage()));
        }
    }

    @PutMapping("/dto/{id}")
    public ResponseEntity<?> updateEmployeeWithDTO(@PathVariable Long id, @RequestBody EmployeeDTO employeeDTO) {
        try {
            EmployeeDTO savedEmployee = employeeService.updateEmployeeFromDTO(id, employeeDTO);
            return ResponseEntity.ok(savedEmployee);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update employee", "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployee(@PathVariable Long id) {
        try {
            Employee employee = employeeService.getEmployeeById(id);
            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get employee", "message", e.getMessage()));
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getEmployeeByEmail(@PathVariable String email) {
        try {
            Optional<Employee> employee = employeeRepository.findByEmail(email);
            return employee.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get employee by email", "message", e.getMessage()));
        }
    }

    @GetMapping("/bulk/health")
    public ResponseEntity<?> bulkImportHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "ready",
                "message", "Bulk import endpoint is available"
        ));
    }

    @GetMapping("/{employeeId}/job")
    public ResponseEntity<Job> getEmployeeJob(@PathVariable Long employeeId) {
        try {
            Employee employee = employeeService.getEmployeeById(employeeId);
            if (employee.getJob() != null) {
                return ResponseEntity.ok(employee.getJob());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}