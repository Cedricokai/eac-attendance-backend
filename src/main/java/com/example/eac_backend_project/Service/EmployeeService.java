package com.example.eac_backend_project.Service;


import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

        // Format date
        String datePart = employee.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Count existing employees for that date
        long count = employeeRepository.countByStartDate(employee.getStartDate()) + 1;

        // Format sequence as 3 digits
        String seqPart = String.format("%03d", count);

        // Build Employee ID
        String employeeId = "EAC-" + datePart + "-" + seqPart;
        employee.setEmployeeId(employeeId);

        return employeeRepository.save(employee);
    }


    // Save multiple employees
    public List<Employee> saveEmployees(List<Employee> employees) {
        return employeeRepository.saveAll(employees);
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

    // **New Method**
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
