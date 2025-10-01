package com.example.eac_backend_project.Controller;


import com.example.eac_backend_project.Service.EmployeeService;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/employee")
@CrossOrigin(origins = "http://localhost:5173")  // Remove extra space in URL
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @PostMapping
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        Employee savedEmployee = employeeService.createEmployee(employee);
        return ResponseEntity.ok(savedEmployee);
    }


    @PostMapping("/bulk")
    public ResponseEntity<List<Employee>> createEmployees(@RequestBody List<Employee> employees) {
        try {
            List<Employee> savedEmployees = employeeService.saveEmployees(employees);
            return ResponseEntity.ok(savedEmployees);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id ,@RequestBody Employee updatedEmployee ) {
        try {
            Employee savedEmployee = employeeService.updateEmployee(id, updatedEmployee);
            return ResponseEntity.ok(savedEmployee);
        } catch (RuntimeException e) {
            return  ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployee(@PathVariable Long id) {
        Employee employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Employee> getEmployeeByEmail(@PathVariable String email) {
        Optional<Employee> employee = employeeRepository.findByEmail(email);
        return employee.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
