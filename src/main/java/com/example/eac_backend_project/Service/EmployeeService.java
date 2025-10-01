package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee createEmployee(Employee employee) {
        if (employee.getStartDate() == null) {
            employee.setStartDate(LocalDate.now());
        }

        // Generate employee ID if not provided
        if (employee.getEmployeeId() == null || employee.getEmployeeId().isEmpty()) {
            employee.setEmployeeId(generateEmployeeId(employee.getStartDate()));
        }

        return employeeRepository.save(employee);
    }

    // Save multiple employees with ID generation
    public List<Employee> saveEmployees(List<Employee> employees) {
        // Generate employee IDs for all employees in sequence
        employees = generateEmployeeIdsForBatch(employees);
        return employeeRepository.saveAll(employees);
    }

    // Enhanced method to handle batch ID generation
    private List<Employee> generateEmployeeIdsForBatch(List<Employee> employees) {
        // Group employees by start date
        Map<LocalDate, List<Employee>> employeesByDate = employees.stream()
                .collect(Collectors.groupingBy(emp ->
                        emp.getStartDate() != null ? emp.getStartDate() : LocalDate.now()
                ));

        List<Employee> employeesWithIds = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Employee>> entry : employeesByDate.entrySet()) {
            LocalDate startDate = entry.getKey();
            List<Employee> employeesForDate = entry.getValue();

            // Get count of existing employees for this date from database
            long existingCount = employeeRepository.countByStartDate(startDate);

            // Generate IDs for this batch - increment for each employee
            for (int i = 0; i < employeesForDate.size(); i++) {
                Employee employee = employeesForDate.get(i);
                if (employee.getEmployeeId() == null || employee.getEmployeeId().isEmpty()) {
                    // Calculate the sequence number: existing count + position in current batch
                    long sequenceNumber = existingCount + i + 1;
                    String employeeId = generateSequentialEmployeeId(startDate, sequenceNumber);
                    employee.setEmployeeId(employeeId);
                }
                employeesWithIds.add(employee);
            }
        }

        return employeesWithIds;
    }

    // Helper method to generate employee ID
    private String generateEmployeeId(LocalDate startDate) {
        // Format date
        String datePart = startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));

        // Count existing employees for that date
        long count = employeeRepository.countByStartDate(startDate) + 1;

        // Format sequence as 3 digits
        String seqPart = String.format("%03d", count);

        // Build Employee ID
        return "E-" + datePart + "-" + seqPart;
    }

    // New method for generating ID with specific sequence number
    private String generateSequentialEmployeeId(LocalDate startDate, long sequence) {
        String datePart = startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
        String seqPart = String.format("%03d", sequence);
        return "E-" + datePart + "-" + seqPart;
    }

    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
    }

    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Optional<Employee> existingEmployeeOptional = employeeRepository.findById(id);
        if (existingEmployeeOptional.isPresent()) {
            Employee existingEmployee = existingEmployeeOptional.get();

            existingEmployee.setFirstName(updatedEmployee.getFirstName());
            existingEmployee.setLastName(updatedEmployee.getLastName());
            existingEmployee.setEmail(updatedEmployee.getEmail());
            existingEmployee.setJobPosition(updatedEmployee.getJobPosition());
            existingEmployee.setMinimumRate(updatedEmployee.getMinimumRate());
            existingEmployee.setCategory(updatedEmployee.getCategory());
            existingEmployee.setWorkType(updatedEmployee.getWorkType());
            existingEmployee.setRentAllowance(updatedEmployee.getRentAllowance());
            existingEmployee.setTransportAllowance(updatedEmployee.getTransportAllowance());
            existingEmployee.setClothingAllowance(updatedEmployee.getClothingAllowance());
            existingEmployee.setOtherAllowance(updatedEmployee.getOtherAllowance());

            // Add the new fields for update
            existingEmployee.setSsnitNumber(updatedEmployee.getSsnitNumber());
            existingEmployee.setTinNumber(updatedEmployee.getTinNumber());
            existingEmployee.setStartDate(updatedEmployee.getStartDate());
            existingEmployee.setEndDate(updatedEmployee.getEndDate());
            existingEmployee.setBasicSalary(updatedEmployee.getBasicSalary());
            existingEmployee.setAccountNumber(updatedEmployee.getAccountNumber());

            Employee savedEmployee = employeeRepository.save(existingEmployee);

            return savedEmployee;
        } else {
            throw new RuntimeException("Employee with ID " + id + "not found.");
        }
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee with ID " + id + " not found."));
    }

    public List<Employee> getEmployeesWithoutApprovedTimesheet(LocalDate date) {
        return employeeRepository.findEmployeesWithoutApprovedTimesheet(date);
    }

    public List<Employee> getEmployeesWithTimesheetsInRange(LocalDate startDate, LocalDate endDate) {
        return employeeRepository.findEmployeesWithTimesheetsInRange(startDate, endDate);
    }
}