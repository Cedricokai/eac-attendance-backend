package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.EmployeeDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.settings.Category;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.inventory.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JobRepository jobRepository,
                           CategoryRepository categoryRepository,
                           RoleRepository roleRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jobRepository = jobRepository;
        this.categoryRepository = categoryRepository;
        this.roleRepository = roleRepository;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "SYSTEM";
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public List<EmployeeDTO> getAllEmployeeDTOs() {
        return employeeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public Employee createEmployee(Employee employee) {
        setDefaultValues(employee);

        if (employee.getStartDate() == null) {
            employee.setStartDate(LocalDate.now());
        }

        if (employee.getEmployeeId() == null || employee.getEmployeeId().isEmpty()) {
            employee.setEmployeeId(generateEmployeeId(employee.getStartDate()));
        }

        Employee savedEmployee = employeeRepository.save(employee);
        createUserForEmployee(savedEmployee);
        return savedEmployee;
    }

    @Transactional
    public EmployeeDTO createEmployee(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();

        // --- Basic Info ---
        employee.setFirstName(employeeDTO.getFirstName());
        employee.setLastName(employeeDTO.getLastName());
        employee.setEmail(employeeDTO.getEmail());
        employee.setPhone(employeeDTO.getPhone());
        employee.setJobPosition(employeeDTO.getJobPosition());
        employee.setJobGrade(employeeDTO.getJobGrade());
        employee.setWorkType(employeeDTO.getWorkType());
        employee.setWeeklyOffDays(employeeDTO.getWeeklyOffDays());
        employee.setMinimumRate(employeeDTO.getMinimumRate());
        employee.setUsePositionRate(employeeDTO.getUsePositionRate() != null ? employeeDTO.getUsePositionRate() : false);

        // --- Category ---
        Category category = null;

        if (employeeDTO.getCategoryId() != null) {
            category = categoryRepository
                    .findById(employeeDTO.getCategoryId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Category not found with id: " +
                                            employeeDTO.getCategoryId()
                            )
                    );
        } else if (
                employeeDTO.getCategory() != null &&
                        !employeeDTO.getCategory().isBlank()
        ) {
            category = categoryRepository
                    .findByName(employeeDTO.getCategory().trim())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Category not found: " +
                                            employeeDTO.getCategory()
                            )
                    );
        }

        employee.setCategory(category);

        // --- Allowances & Salary ---
        employee.setBasicSalary(employeeDTO.getBasicSalary());
        employee.setRentAllowance(employeeDTO.getRentAllowance());
        employee.setTransportAllowance(employeeDTO.getTransportAllowance());
        employee.setClothingAllowance(employeeDTO.getClothingAllowance());
        employee.setOtherAllowance(employeeDTO.getOtherAllowance());
        employee.setNssAllowance(employeeDTO.getNssAllowance());

        // --- Banking & Accounts ---
        employee.setAccountName(employeeDTO.getAccountName());
        employee.setAccountNumber(employeeDTO.getAccountNumber());
        employee.setBank(employeeDTO.getBank());
        employee.setBankBranch(employeeDTO.getBankBranch());
        employee.setSsnitAccountName(employeeDTO.getSsnitAccountName());
        employee.setSsnitNumber(employeeDTO.getSsnitNumber());
        employee.setTinNumber(employeeDTO.getTinNumber());

        // --- Personal & Contact ---
        employee.setDateOfBirth(employeeDTO.getDateOfBirth());
        employee.setAge(employeeDTO.getAge());
        employee.setGhanaCard(employeeDTO.getGhanaCard());
        employee.setLocation(employeeDTO.getLocation());
        employee.setEmergencyContact(employeeDTO.getEmergencyContact());
        employee.setTagNumber(employeeDTO.getTagNumber());

        // --- Family & Residence ---
        employee.setContactPerson(employeeDTO.getContactPerson());
        employee.setRelationship(employeeDTO.getRelationship());
        employee.setTownOfResidence(employeeDTO.getTownOfResidence());
        employee.setHouseNumber(employeeDTO.getHouseNumber());
        employee.setSpouse(employeeDTO.getSpouse());
        employee.setNumberOfChildren(employeeDTO.getNumberOfChildren());

        // --- Employment Dates ---
        employee.setStartDate(employeeDTO.getStartDate() != null ? employeeDTO.getStartDate() : LocalDate.now());
        employee.setEndDate(employeeDTO.getEndDate());

        // --- Department & Others ---
        employee.setDepartment(employeeDTO.getDepartment());

        // --- Tax & SSNIT Flags ---
        employee.setApplyWithholdingTax(employeeDTO.getApplyWithholdingTax() != null ? employeeDTO.getApplyWithholdingTax() : false);
        employee.setExcludeFromSsnit(employeeDTO.getExcludeFromSsnit() != null ? employeeDTO.getExcludeFromSsnit() : false);

        // --- Tier Two Fields ---
        employee.setTierTwoAccountName(employeeDTO.getTierTwoAccountName());
        employee.setTierTwoAccountNumber(employeeDTO.getTierTwoAccountNumber());
        employee.setBaseNumber(employeeDTO.getBaseNumber());

        // --- Computed / Defaults ---
        setDefaultValues(employee); // this will fill hourlyRate, overtimeRate, etc.

        // --- Generate Employee ID if missing ---
        if (employee.getEmployeeId() == null || employee.getEmployeeId().isEmpty()) {
            employee.setEmployeeId(generateEmployeeId(employee.getStartDate()));
        }

        Employee savedEmployee = employeeRepository.save(employee);
        createUserForEmployee(savedEmployee);

        // LOG
        activityLogService.logSuccess(
                "Created employee: " + savedEmployee.getFirstName() + " " + savedEmployee.getLastName(),
                "EMPLOYEE", getCurrentUsername(), savedEmployee.getId()
        );

        return convertToDTO(savedEmployee);
    }

    @Transactional
    public List<Employee> saveEmployees(List<Employee> employees) {
        // ... duplicate check and batch save logic (unchanged) ...

        List<String> ghanaCardNumbers = new ArrayList<>();
        List<String> ssnitNumbers = new ArrayList<>();
        List<String> tinNumbers = new ArrayList<>();
        List<String> tagNumbers = new ArrayList<>();
        List<String> accountNumbers = new ArrayList<>();
        List<String> emails = new ArrayList<>();

        for (Employee emp : employees) {
            if (emp.getGhanaCard() != null && !emp.getGhanaCard().trim().isEmpty()) {
                ghanaCardNumbers.add(emp.getGhanaCard().trim());
            }
            if (emp.getSsnitNumber() != null && !emp.getSsnitNumber().trim().isEmpty()) {
                ssnitNumbers.add(emp.getSsnitNumber().trim());
            }
            if (emp.getTinNumber() != null && !emp.getTinNumber().trim().isEmpty()) {
                tinNumbers.add(emp.getTinNumber().trim());
            }
            if (emp.getTagNumber() != null && !emp.getTagNumber().trim().isEmpty()) {
                tagNumbers.add(emp.getTagNumber().trim());
            }
            if (emp.getAccountNumber() != null && !emp.getAccountNumber().trim().isEmpty()) {
                accountNumbers.add(emp.getAccountNumber().trim());
            }
            if (emp.getEmail() != null && !emp.getEmail().trim().isEmpty()) {
                emails.add(emp.getEmail().trim());
            }
        }

        List<Employee> existingDuplicates = new ArrayList<>();

        if (!ghanaCardNumbers.isEmpty()) {
            existingDuplicates.addAll(employeeRepository.findByGhanaCardIn(ghanaCardNumbers));
        }
        if (!ssnitNumbers.isEmpty()) {
            existingDuplicates.addAll(employeeRepository.findBySsnitNumberIn(ssnitNumbers));
        }
        if (!tinNumbers.isEmpty()) {
            existingDuplicates.addAll(employeeRepository.findByTinNumberIn(tinNumbers));
        }
        if (!tagNumbers.isEmpty()) {
            existingDuplicates.addAll(employeeRepository.findByTagNumberIn(tagNumbers));
        }
        if (!accountNumbers.isEmpty()) {
            existingDuplicates.addAll(employeeRepository.findByAccountNumberIn(accountNumbers));
        }
        if (!emails.isEmpty()) {
            existingDuplicates.addAll(employeeRepository.findByEmailIn(emails));
        }

        if (!existingDuplicates.isEmpty()) {
            Map<String, List<String>> duplicateMap = new HashMap<>();
            for (Employee existingEmp : existingDuplicates) {
                if (existingEmp.getGhanaCard() != null && ghanaCardNumbers.contains(existingEmp.getGhanaCard().trim())) {
                    duplicateMap.computeIfAbsent("Ghana Card", k -> new ArrayList<>()).add(existingEmp.getGhanaCard());
                }
                if (existingEmp.getSsnitNumber() != null && ssnitNumbers.contains(existingEmp.getSsnitNumber().trim())) {
                    duplicateMap.computeIfAbsent("SSNIT", k -> new ArrayList<>()).add(existingEmp.getSsnitNumber());
                }
                if (existingEmp.getTinNumber() != null && tinNumbers.contains(existingEmp.getTinNumber().trim())) {
                    duplicateMap.computeIfAbsent("TIN", k -> new ArrayList<>()).add(existingEmp.getTinNumber());
                }
                if (existingEmp.getTagNumber() != null && tagNumbers.contains(existingEmp.getTagNumber().trim())) {
                    duplicateMap.computeIfAbsent("Tag Number", k -> new ArrayList<>()).add(existingEmp.getTagNumber());
                }
                if (existingEmp.getAccountNumber() != null && accountNumbers.contains(existingEmp.getAccountNumber().trim())) {
                    duplicateMap.computeIfAbsent("Account Number", k -> new ArrayList<>()).add(existingEmp.getAccountNumber());
                }
                if (existingEmp.getEmail() != null && emails.contains(existingEmp.getEmail().trim())) {
                    duplicateMap.computeIfAbsent("Email", k -> new ArrayList<>()).add(existingEmp.getEmail());
                }
            }
            StringBuilder errorMessage = new StringBuilder("Duplicate entries found: ");
            for (Map.Entry<String, List<String>> entry : duplicateMap.entrySet()) {
                errorMessage.append(entry.getKey()).append(": ").append(String.join(", ", entry.getValue())).append("; ");
            }
            throw new DataIntegrityViolationException(errorMessage.toString());
        }

        employees.forEach(this::setDefaultValues);
        employees = generateEmployeeIdsForBatch(employees);

        try {
            List<Employee> savedEmployees = employeeRepository.saveAll(employees);
            for (Employee employee : savedEmployees) {
                try {
                    createUserForEmployee(employee);
                } catch (Exception e) {
                    System.err.println("Warning: Failed to create user for employee " +
                            employee.getFirstName() + " " + employee.getLastName() + ": " + e.getMessage());
                }
            }
            // LOG: Bulk import success
            activityLogService.logSuccess(
                    "Bulk imported " + savedEmployees.size() + " employees",
                    "EMPLOYEE", getCurrentUsername(), null
            );
            return savedEmployees;
        } catch (DataIntegrityViolationException e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("Duplicate entry")) {
                String duplicateValue = extractDuplicateValue(errorMsg);
                throw new DataIntegrityViolationException(
                        "Duplicate entry detected. The value '" + duplicateValue +
                                "' already exists in the database. Please check for duplicates in: " +
                                "Ghana Card, SSNIT, TIN, Tag Number, Account Number, or Email fields."
                );
            }
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save employees: " + e.getMessage(), e);
        }
    }

    private String extractDuplicateValue(String errorMessage) {
        int start = errorMessage.indexOf("'") + 1;
        int end = errorMessage.indexOf("'", start);
        if (start > 0 && end > start) {
            return errorMessage.substring(start, end);
        }
        return "unknown";
    }

    private void setDefaultValues(Employee employee) {
        if (employee.getMinimumRate() == null) employee.setMinimumRate(0.0);
        if (employee.getHourlyRate() == null) employee.setHourlyRate(employee.getMinimumRate());
        if (employee.getOvertimeRate() == null) employee.setOvertimeRate(employee.getHourlyRate() * 1.5);
        if (employee.getWorkType() == null) employee.setWorkType("Regular");
        if (employee.getJobPosition() == null) employee.setJobPosition("Employee");
        if (employee.getStartDate() == null) employee.setStartDate(LocalDate.now());
        if (employee.getBasicSalary() == null) employee.setBasicSalary(0.0);
        if (employee.getRentAllowance() == null) employee.setRentAllowance(0.0);
        if (employee.getTransportAllowance() == null) employee.setTransportAllowance(0.0);
        if (employee.getClothingAllowance() == null) employee.setClothingAllowance(0.0);
        if (employee.getOtherAllowance() == null) employee.setOtherAllowance(0.0);
        if (employee.getNssAllowance() == null) employee.setNssAllowance(0.0);
        if (employee.getPhone() == null) employee.setPhone("");
        if (employee.getEmail() == null || employee.getEmail().isBlank()) {
            String firstName = employee.getFirstName() != null ? employee.getFirstName().toLowerCase() : "employee";
            String lastName = employee.getLastName() != null ? employee.getLastName().toLowerCase() : "user";
            employee.setEmail(firstName + "." + lastName + "@company.com");
        }
    }

    private List<Employee> generateEmployeeIdsForBatch(List<Employee> employees) {
        Map<LocalDate, List<Employee>> employeesByDate = employees.stream()
                .collect(Collectors.groupingBy(emp -> emp.getStartDate() != null ? emp.getStartDate() : LocalDate.now()));
        List<Employee> employeesWithIds = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Employee>> entry : employeesByDate.entrySet()) {
            LocalDate startDate = entry.getKey();
            List<Employee> employeesForDate = entry.getValue();
            long existingCount = employeeRepository.countByStartDate(startDate);
            for (int i = 0; i < employeesForDate.size(); i++) {
                Employee employee = employeesForDate.get(i);
                if (employee.getEmployeeId() == null || employee.getEmployeeId().isEmpty()) {
                    long sequenceNumber = existingCount + i + 1;
                    String employeeId = generateSequentialEmployeeId(startDate, sequenceNumber);
                    employee.setEmployeeId(employeeId);
                }
                employeesWithIds.add(employee);
            }
        }
        return employeesWithIds;
    }

    private String generateEmployeeId(LocalDate startDate) {
        String datePart = startDate.format(DateTimeFormatter.ofPattern("yyMM"));
        long sequence = 1;
        String employeeId;
        do {
            String seqPart = String.format("%03d", sequence);
            employeeId = "E-" + datePart + "-" + seqPart;
            sequence++;
        } while (employeeRepository.existsByEmployeeId(employeeId));
        return employeeId;
    }

    private String generateSequentialEmployeeId(LocalDate startDate, long sequence) {
        String datePart = startDate.format(DateTimeFormatter.ofPattern("yyMM"));
        String seqPart = String.format("%03d", sequence);
        String employeeId = "E-" + datePart + "-" + seqPart;
        while (employeeRepository.existsByEmployeeId(employeeId)) {
            sequence++;
            seqPart = String.format("%03d", sequence);
            employeeId = "E-" + datePart + "-" + seqPart;
        }
        return employeeId;
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        List<User> linkedUsers = userRepository.findByEmployee_Id(employee.getId());
        for (User u : linkedUsers) {
            u.setEmployee(null);
        }
        userRepository.saveAll(linkedUsers);
        employeeRepository.delete(employee);
        // LOG: Employee deleted
        activityLogService.logWarning(
                "Deleted employee ID " + id,
                "EMPLOYEE", getCurrentUsername(), id
        );
    }

    @Transactional
    public void toggleEmployeeActive(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        employee.setActive(!Boolean.TRUE.equals(employee.getActive()));
        Employee saved = employeeRepository.save(employee);
        String action = saved.getActive() ? "activated" : "deactivated";
        // LOG: Employee status changed
        activityLogService.logInfo(
                action + " employee ID " + saved.getId(),
                "EMPLOYEE", getCurrentUsername(), saved.getId()
        );
    }

    private void regenerateEmployeeIdIfStartDateChanged(Employee existingEmployee, LocalDate newStartDate) {
        LocalDate oldStartDate = existingEmployee.getStartDate();
        if (newStartDate != null && !newStartDate.equals(oldStartDate)) {
            long count = employeeRepository.countByStartDate(newStartDate) + 1;
            String newEmployeeId = generateSequentialEmployeeId(newStartDate, count);
            existingEmployee.setEmployeeId(newEmployeeId);
        }
    }

    @Transactional
    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Optional<Employee> existingEmployeeOptional = employeeRepository.findById(id);
        if (existingEmployeeOptional.isPresent()) {
            Employee existingEmployee = existingEmployeeOptional.get();
            String oldEmail = existingEmployee.getEmail();

            if (updatedEmployee.getFirstName() != null) existingEmployee.setFirstName(updatedEmployee.getFirstName());
            if (updatedEmployee.getLastName() != null) existingEmployee.setLastName(updatedEmployee.getLastName());
            if (updatedEmployee.getEmail() != null) existingEmployee.setEmail(updatedEmployee.getEmail());
            if (updatedEmployee.getPhone() != null) existingEmployee.setPhone(updatedEmployee.getPhone());
            if (updatedEmployee.getJobPosition() != null) existingEmployee.setJobPosition(updatedEmployee.getJobPosition());
            if (updatedEmployee.getMinimumRate() != null) existingEmployee.setMinimumRate(updatedEmployee.getMinimumRate());
            if (updatedEmployee.getCategory() != null) existingEmployee.setCategory(updatedEmployee.getCategory());
            if (updatedEmployee.getWorkType() != null) existingEmployee.setWorkType(updatedEmployee.getWorkType());
            if (updatedEmployee.getRentAllowance() != null) existingEmployee.setRentAllowance(updatedEmployee.getRentAllowance());
            if (updatedEmployee.getTransportAllowance() != null) existingEmployee.setTransportAllowance(updatedEmployee.getTransportAllowance());
            if (updatedEmployee.getClothingAllowance() != null) existingEmployee.setClothingAllowance(updatedEmployee.getClothingAllowance());
            if (updatedEmployee.getOtherAllowance() != null) existingEmployee.setOtherAllowance(updatedEmployee.getOtherAllowance());
            if (updatedEmployee.getNssAllowance() != null) existingEmployee.setNssAllowance(updatedEmployee.getNssAllowance());
            if (updatedEmployee.getSsnitAccountName() != null) existingEmployee.setSsnitAccountName(updatedEmployee.getSsnitAccountName());
            if (updatedEmployee.getSsnitNumber() != null) existingEmployee.setSsnitNumber(updatedEmployee.getSsnitNumber());
            if (updatedEmployee.getTinNumber() != null) existingEmployee.setTinNumber(updatedEmployee.getTinNumber());
            if (updatedEmployee.getStartDate() != null) {
                regenerateEmployeeIdIfStartDateChanged(existingEmployee, updatedEmployee.getStartDate());
                existingEmployee.setStartDate(updatedEmployee.getStartDate());
            }
            if (updatedEmployee.getExcludeFromSsnit() != null) {
                existingEmployee.setExcludeFromSsnit(updatedEmployee.getExcludeFromSsnit());
            }
            if (updatedEmployee.getTierTwoAccountName() != null) existingEmployee.setTierTwoAccountName((updatedEmployee.getTierTwoAccountName()));
            if (updatedEmployee.getBaseNumber() != null) existingEmployee.setBaseNumber(updatedEmployee.getBaseNumber());
            if (updatedEmployee.getTierTwoAccountNumber() != null) existingEmployee.setTierTwoAccountNumber((updatedEmployee.getTierTwoAccountNumber()));
            if (updatedEmployee.getEndDate() != null) existingEmployee.setEndDate(updatedEmployee.getEndDate());
            if (updatedEmployee.getEmergencyContact() != null) existingEmployee.setEmergencyContact(updatedEmployee.getEmergencyContact());
            if (updatedEmployee.getAccountName() != null) existingEmployee.setAccountName(updatedEmployee.getAccountName());
            if (updatedEmployee.getBasicSalary() != null) existingEmployee.setBasicSalary(updatedEmployee.getBasicSalary());
            if (updatedEmployee.getAccountNumber() != null) existingEmployee.setAccountNumber(updatedEmployee.getAccountNumber());
            if (updatedEmployee.getTagNumber() != null) existingEmployee.setTagNumber(updatedEmployee.getTagNumber());
            if (updatedEmployee.getDateOfBirth() != null) existingEmployee.setDateOfBirth(updatedEmployee.getDateOfBirth());
            if (updatedEmployee.getLocation() != null) existingEmployee.setLocation(updatedEmployee.getLocation());
            if (updatedEmployee.getGhanaCard() != null) existingEmployee.setGhanaCard(updatedEmployee.getGhanaCard());
            if (updatedEmployee.getBank() != null) existingEmployee.setBank(updatedEmployee.getBank());
            if (updatedEmployee.getBankBranch() != null) existingEmployee.setBankBranch(updatedEmployee.getBankBranch());
            if (updatedEmployee.getContactPerson() != null) existingEmployee.setContactPerson(updatedEmployee.getContactPerson());
            if (updatedEmployee.getRelationship() != null) existingEmployee.setRelationship(updatedEmployee.getRelationship());
            if (updatedEmployee.getTownOfResidence() != null) existingEmployee.setTownOfResidence(updatedEmployee.getTownOfResidence());
            if (updatedEmployee.getHouseNumber() != null) existingEmployee.setHouseNumber(updatedEmployee.getHouseNumber());
            if (updatedEmployee.getSpouse() != null) existingEmployee.setSpouse(updatedEmployee.getSpouse());
            if (updatedEmployee.getNumberOfChildren() != 0) existingEmployee.setNumberOfChildren(updatedEmployee.getNumberOfChildren());
            if (updatedEmployee.getAge() != 0) existingEmployee.setAge(updatedEmployee.getAge());

            Employee savedEmployee = employeeRepository.save(existingEmployee);

            if (updatedEmployee.getEmail() != null && !oldEmail.equals(updatedEmployee.getEmail())) {
                updateUserForEmployee(savedEmployee, oldEmail);
            }

            // LOG: Employee updated
            activityLogService.logInfo(
                    "Updated employee ID " + savedEmployee.getId(),
                    "EMPLOYEE", getCurrentUsername(), savedEmployee.getId()
            );
            return savedEmployee;
        } else {
            throw new RuntimeException("Employee with ID " + id + " not found.");
        }
    }

    @Transactional
    public EmployeeDTO updateEmployeeFromDTO(Long id, EmployeeDTO employeeDTO) {
        Optional<Employee> existingEmployeeOptional = employeeRepository.findById(id);
        if (existingEmployeeOptional.isPresent()) {
            Employee existingEmployee = existingEmployeeOptional.get();
            String oldEmail = existingEmployee.getEmail();

            if (employeeDTO.getFirstName() != null) existingEmployee.setFirstName(employeeDTO.getFirstName());
            if (employeeDTO.getLastName() != null) existingEmployee.setLastName(employeeDTO.getLastName());
            if (employeeDTO.getEmail() != null) existingEmployee.setEmail(employeeDTO.getEmail());
            if (employeeDTO.getPhone() != null) existingEmployee.setPhone(employeeDTO.getPhone());
            if (employeeDTO.getJobPosition() != null) existingEmployee.setJobPosition(employeeDTO.getJobPosition());
            if (employeeDTO.getMinimumRate() != null) existingEmployee.setMinimumRate(employeeDTO.getMinimumRate());
            if (employeeDTO.getWorkType() != null) existingEmployee.setWorkType(employeeDTO.getWorkType());
            if (employeeDTO.getWeeklyOffDays() != null) existingEmployee.setWeeklyOffDays(employeeDTO.getWeeklyOffDays());
            if (employeeDTO.getBasicSalary() != null) existingEmployee.setBasicSalary(employeeDTO.getBasicSalary());
            if (employeeDTO.getRentAllowance() != null) existingEmployee.setRentAllowance(employeeDTO.getRentAllowance());
            if (employeeDTO.getTransportAllowance() != null) existingEmployee.setTransportAllowance(employeeDTO.getTransportAllowance());
            if (employeeDTO.getClothingAllowance() != null) existingEmployee.setClothingAllowance(employeeDTO.getClothingAllowance());
            if (employeeDTO.getOtherAllowance() != null) existingEmployee.setOtherAllowance(employeeDTO.getOtherAllowance());
            if (employeeDTO.getNssAllowance() != null) existingEmployee.setNssAllowance(employeeDTO.getNssAllowance());
            if (employeeDTO.getSsnitAccountName() != null) existingEmployee.setSsnitAccountName(employeeDTO.getSsnitAccountName());
            if (employeeDTO.getSsnitNumber() != null) existingEmployee.setSsnitNumber(employeeDTO.getSsnitNumber());
            if (employeeDTO.getTinNumber() != null) existingEmployee.setTinNumber(employeeDTO.getTinNumber());
            if (employeeDTO.getStartDate() != null) {
                regenerateEmployeeIdIfStartDateChanged(existingEmployee, employeeDTO.getStartDate());
                existingEmployee.setStartDate(employeeDTO.getStartDate());
            }
            if (employeeDTO.getApplyWithholdingTax() != null) {
                existingEmployee.setApplyWithholdingTax(employeeDTO.getApplyWithholdingTax());
            }
            if (employeeDTO.getJobGrade() != null) existingEmployee.setJobGrade(employeeDTO.getJobGrade());
            if (employeeDTO.getTierTwoAccountName() != null) existingEmployee.setTierTwoAccountName(employeeDTO.getTierTwoAccountName());
            if (employeeDTO.getBaseNumber() != null) existingEmployee.setBaseNumber(employeeDTO.getBaseNumber());
            if (employeeDTO.getTierTwoAccountNumber() != null) existingEmployee.setTierTwoAccountNumber(employeeDTO.getTierTwoAccountNumber());
            if (employeeDTO.getExcludeFromSsnit() != null) existingEmployee.setExcludeFromSsnit(employeeDTO.getExcludeFromSsnit());
            if (employeeDTO.getEndDate() != null) existingEmployee.setEndDate(employeeDTO.getEndDate());
            if (employeeDTO.getEmergencyContact() != null) existingEmployee.setEmergencyContact(employeeDTO.getEmergencyContact());
            if (employeeDTO.getAccountName() != null) existingEmployee.setAccountName(employeeDTO.getAccountName());
            if (employeeDTO.getAccountNumber() != null) existingEmployee.setAccountNumber(employeeDTO.getAccountNumber());
            if (employeeDTO.getTagNumber() != null) existingEmployee.setTagNumber(employeeDTO.getTagNumber());
            if (employeeDTO.getDateOfBirth() != null) existingEmployee.setDateOfBirth(employeeDTO.getDateOfBirth());
            if (employeeDTO.getLocation() != null) existingEmployee.setLocation(employeeDTO.getLocation());
            if (employeeDTO.getGhanaCard() != null) existingEmployee.setGhanaCard(employeeDTO.getGhanaCard());
            if (employeeDTO.getBank() != null) existingEmployee.setBank(employeeDTO.getBank());
            if (employeeDTO.getBankBranch() != null) existingEmployee.setBankBranch(employeeDTO.getBankBranch());
            if (employeeDTO.getContactPerson() != null) existingEmployee.setContactPerson(employeeDTO.getContactPerson());
            if (employeeDTO.getRelationship() != null) existingEmployee.setRelationship(employeeDTO.getRelationship());
            if (employeeDTO.getTownOfResidence() != null) existingEmployee.setTownOfResidence(employeeDTO.getTownOfResidence());
            if (employeeDTO.getHouseNumber() != null) existingEmployee.setHouseNumber(employeeDTO.getHouseNumber());
            if (employeeDTO.getSpouse() != null) existingEmployee.setSpouse(employeeDTO.getSpouse());
            if (employeeDTO.getNumberOfChildren() != null) existingEmployee.setNumberOfChildren(employeeDTO.getNumberOfChildren());
            if (employeeDTO.getAge() != null) existingEmployee.setAge(employeeDTO.getAge());
            if (employeeDTO.getDepartment() != null) existingEmployee.setDepartment(employeeDTO.getDepartment());
            if (employeeDTO.getActive() != null) existingEmployee.setActive(employeeDTO.getActive());
            if (employeeDTO.getIsSupervisor() != null) existingEmployee.setIsSupervisor(employeeDTO.getIsSupervisor());

            if (employeeDTO.getCategoryId() != null) {
                Category category = categoryRepository
                        .findById(employeeDTO.getCategoryId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found with id: " +
                                                employeeDTO.getCategoryId()
                                )
                        );

                existingEmployee.setCategory(category);

            } else if (
                    employeeDTO.getCategory() != null &&
                            !employeeDTO.getCategory().isBlank()
            ) {
                Category category = categoryRepository
                        .findByName(employeeDTO.getCategory().trim())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found: " +
                                                employeeDTO.getCategory()
                                )
                        );

                existingEmployee.setCategory(category);
            }
            if (employeeDTO.getUsePositionRate() != null && employeeDTO.getUsePositionRate() &&
                    employeeDTO.getJobPosition() != null && employeeDTO.getJobGrade() != null) {
                existingEmployee.setMinimumRate(employeeDTO.getMinimumRate());
            }

            Employee savedEmployee = employeeRepository.save(existingEmployee);

            if (employeeDTO.getEmail() != null && !oldEmail.equals(employeeDTO.getEmail())) {
                updateUserForEmployee(savedEmployee, oldEmail);
            }

            // LOG: Employee updated via DTO
            activityLogService.logInfo(
                    "Updated employee ID " + savedEmployee.getId(),
                    "EMPLOYEE", getCurrentUsername(), savedEmployee.getId()
            );
            return convertToDTO(savedEmployee);
        } else {
            throw new RuntimeException("Employee with ID " + id + " not found.");
        }
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee with ID " + id + " not found."));
    }

    public EmployeeDTO getEmployeeDTOById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee with ID " + id + " not found."));
        return convertToDTO(employee);
    }

    public Optional<Employee> getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }

    public List<Employee> getEmployeesWithoutApprovedTimesheet(LocalDate date) {
        return employeeRepository.findEmployeesWithoutApprovedTimesheet(date);
    }

    public List<Employee> getEmployeesWithTimesheetsInRange(LocalDate startDate, LocalDate endDate) {
        return employeeRepository.findEmployeesWithTimesheetsInRange(startDate, endDate);
    }

    private void createUserForEmployee(Employee employee) {
        try {
            if (employee.getEmail() == null || employee.getEmail().isBlank()) {
                System.out.println("Skipping user creation — no email for " + employee.getFirstName());
                return;
            }
            if (userRepository.findByEmail(employee.getEmail()).isPresent()) {
                System.out.println("User already exists for email: " + employee.getEmail());
                return;
            }
            String username = generateUsername(
                    Optional.ofNullable(employee.getFirstName()).orElse("user"),
                    Optional.ofNullable(employee.getLastName()).orElse(UUID.randomUUID().toString().substring(0, 5))
            );
            User user = new User();
            user.setName((employee.getFirstName() + " " + employee.getLastName()).trim());
            user.setUserName(username);
            user.setEmail(employee.getEmail());
            user.setPassword(passwordEncoder.encode("default123"));
            user.setRole(getRoleFromJobPosition(employee.getJobPosition()));
            user.setMobile("");
            user.setEmployee(employee);
            userRepository.save(user);
            System.out.println("✅ Created user: " + user.getUserName());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to create user for employee " + employee.getFirstName() + ": " + e.getMessage());
        }
    }

    private void updateUserForEmployee(Employee employee, String oldEmail) {
        Optional<User> existingUser = userRepository.findByEmail(oldEmail);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setName(employee.getFirstName() + " " + employee.getLastName());
            user.setEmail(employee.getEmail());
            user.setRole(getRoleFromJobPosition(employee.getJobPosition()));
            userRepository.save(user);
            System.out.println("Updated user record for employee: " + employee.getEmail());
        }
    }

    private String generateUsername(String firstName, String lastName) {
        String safeFirst = firstName != null ? firstName : "user";
        String safeLast = lastName != null ? lastName : UUID.randomUUID().toString().substring(0, 4);
        String baseUsername = (safeFirst + "." + safeLast).toLowerCase().replaceAll("[^a-z0-9.]", ".");
        String username = baseUsername;
        int counter = 1;
        while (userRepository.findByUserName(username).isPresent()) {
            username = baseUsername + counter;
            counter++;
        }
        return username;
    }

    private Role getRoleFromJobPosition(String jobPosition) {
        if (jobPosition == null) {
            return getDefaultRole();
        }
        String position = jobPosition.toLowerCase();
        String roleName;
        if (position.contains("admin")) {
            roleName = "ROLE_ADMIN";
        } else if (position.contains("supervisor")) {
            roleName = "ROLE_SUPERVISOR";
        } else if (position.contains("planner")) {
            roleName = "ROLE_PLANNER";
        } else if (position.contains("hr")) {
            roleName = "ROLE_HR";
        } else if (position.contains("inventory")) {
            roleName = "ROLE_INVENTORY";
        } else if (position.contains("procurement")) {
            roleName = "ROLE_PROCUREMENT_OFFICER";
        } else if (position.contains("transport")) {
            roleName = "ROLE_TRANSPORT";
        } else {
            roleName = "ROLE_CUSTOMER";
        }
        return roleRepository.findByName(roleName)
                .orElseGet(() -> createRole(roleName));
    }

    private Role getDefaultRole() {
        return roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> createRole("ROLE_CUSTOMER"));
    }

    private Role createRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        role.setSystem(roleName.equals("ROLE_ADMIN") || roleName.equals("ROLE_CUSTOMER"));
        return roleRepository.save(role);
    }

    @Transactional
    public Employee assignEmployeeToJob(Long employeeId, Long jobId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        if (jobId == null) {
            employee.setJob(null);
            employee.setIsSupervisor(false);
        } else {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            employee.setJob(job);
        }
        return employeeRepository.save(employee);
    }

    public EmployeeDTO convertToDTO(Employee employee) {
        if (employee == null) {
            return null;
        }

        EmployeeDTO dto = new EmployeeDTO();

        // =========================================================
        // BASIC EMPLOYEE INFORMATION
        // =========================================================

        dto.setId(employee.getId());
        dto.setWeeklyOffDays(employee.getWeeklyOffDays());
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setEmail(employee.getEmail());
        dto.setPhone(employee.getPhone());

        dto.setJobPosition(employee.getJobPosition());
        dto.setJobGrade(employee.getJobGrade());
        dto.setWorkType(employee.getWorkType());

        dto.setMinimumRate(employee.getMinimumRate());
        dto.setUsePositionRate(employee.getUsePositionRate());
        dto.setBasicSalary(employee.getBasicSalary());

        dto.setStartDate(employee.getStartDate());
        dto.setEndDate(employee.getEndDate());
        dto.setDateOfBirth(employee.getDateOfBirth());
        dto.setAge(employee.getAge());

        dto.setDepartment(employee.getDepartment());
        dto.setLocation(employee.getLocation());

        dto.setActive(employee.getActive());
        dto.setIsSupervisor(employee.getIsSupervisor());

        // =========================================================
        // CATEGORY INFORMATION
        // =========================================================

        Category category = employee.getCategory();

        if (category != null) {
            dto.setCategory(category.getName());
            dto.setCategoryId(category.getId());

            dto.setAnnualLeaveDays(
                    category.getAnnualLeaveDays() != null
                            ? category.getAnnualLeaveDays()
                            : 20
            );

            dto.setExcludeWeekendsFromLeave(
                    category.getExcludeWeekendsFromLeave() != null
                            ? category.getExcludeWeekendsFromLeave()
                            : true
            );
        } else {
            dto.setCategory(null);
            dto.setCategoryId(null);
            dto.setAnnualLeaveDays(20);

            // Safe fallback: exclude weekends when no category exists.
            dto.setExcludeWeekendsFromLeave(true);
        }

        // =========================================================
        // JOB INFORMATION
        // =========================================================

        if (employee.getJob() != null) {
            dto.setJobId(employee.getJob().getId());
            dto.setJobName(employee.getJob().getName());
        } else {
            dto.setJobId(null);
            dto.setJobName(null);
        }

        // =========================================================
        // BANKING AND ACCOUNT INFORMATION
        // =========================================================

        dto.setAccountName(employee.getAccountName());
        dto.setAccountNumber(employee.getAccountNumber());

        dto.setBank(employee.getBank());
        dto.setBankBranch(employee.getBankBranch());

        dto.setSsnitAccountName(employee.getSsnitAccountName());
        dto.setSsnitNumber(employee.getSsnitNumber());

        dto.setTinNumber(employee.getTinNumber());
        dto.setTagNumber(employee.getTagNumber());

        dto.setTierTwoAccountName(
                employee.getTierTwoAccountName()
        );

        dto.setTierTwoAccountNumber(
                employee.getTierTwoAccountNumber()
        );

        dto.setBaseNumber(employee.getBaseNumber());

        // =========================================================
        // PERSONAL INFORMATION
        // =========================================================

        dto.setGhanaCard(employee.getGhanaCard());
        dto.setEmergencyContact(employee.getEmergencyContact());

        dto.setContactPerson(employee.getContactPerson());
        dto.setRelationship(employee.getRelationship());

        dto.setTownOfResidence(employee.getTownOfResidence());
        dto.setHouseNumber(employee.getHouseNumber());

        dto.setSpouse(employee.getSpouse());
        dto.setNumberOfChildren(employee.getNumberOfChildren());

        // =========================================================
        // ALLOWANCES
        // =========================================================

        dto.setRentAllowance(employee.getRentAllowance());
        dto.setTransportAllowance(employee.getTransportAllowance());
        dto.setClothingAllowance(employee.getClothingAllowance());
        dto.setOtherAllowance(employee.getOtherAllowance());
        dto.setNssAllowance(employee.getNssAllowance());

        // =========================================================
        // TAX AND SSNIT FLAGS
        // =========================================================

        dto.setApplyWithholdingTax(
                employee.getApplyWithholdingTax() != null
                        ? employee.getApplyWithholdingTax()
                        : false
        );

        dto.setExcludeFromSsnit(
                employee.getExcludeFromSsnit() != null
                        ? employee.getExcludeFromSsnit()
                        : false
        );

        return dto;
    }

    public Employee findById(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
    }

    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee assignEmployeeToJobWithSupervisor(Long employeeId, Long jobId, Boolean isSupervisor) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        if (jobId == null) {
            employee.setJob(null);
            employee.setIsSupervisor(false);
        } else {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            employee.setJob(job);
            if (isSupervisor != null) {
                if (isSupervisor) {
                    List<Employee> currentSupervisors = employeeRepository.findSupervisorsByJobId(jobId);
                    for (Employee currentSupervisor : currentSupervisors) {
                        if (!currentSupervisor.getId().equals(employeeId)) {
                            currentSupervisor.setIsSupervisor(false);
                            employeeRepository.save(currentSupervisor);
                        }
                    }
                    employee.setIsSupervisor(true);
                    if (job.getSupervisor() == null || !job.getSupervisor().getId().equals(employeeId)) {
                        job.setSupervisor(employee);
                        jobRepository.save(job);
                    }
                } else {
                    employee.setIsSupervisor(false);
                    if (job.getSupervisor() != null && job.getSupervisor().getId().equals(employeeId)) {
                        job.setSupervisor(null);
                        jobRepository.save(job);
                    }
                }
            }
        }
        return employeeRepository.save(employee);
    }

    @Transactional
    public EmployeeDTO updateSupervisorStatus(Long employeeId, Long jobId, Boolean isSupervisor) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        if (isSupervisor != null) {
            employee.setIsSupervisor(isSupervisor);
            if (isSupervisor && jobId != null) {
                Job job = jobRepository.findById(jobId)
                        .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
                job.setSupervisor(employee);
                jobRepository.save(job);
            }
            if (!isSupervisor && jobId != null) {
                Job job = jobRepository.findById(jobId)
                        .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
                if (employee.equals(job.getSupervisor())) {
                    job.setSupervisor(null);
                    jobRepository.save(job);
                }
            }
        }
        Employee savedEmployee = employeeRepository.save(employee);
        return convertToDTO(savedEmployee);
    }

    @Transactional
    public void bulkAssignEmployeesToJob(List<Long> employeeIds, Long jobId) {
        Job job = null;
        if (jobId != null) {
            job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        }
        for (Long employeeId : employeeIds) {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
            employee.setJob(job);
            employeeRepository.save(employee);
        }
        // LOG: Bulk assign to job
        activityLogService.logInfo(
                "Bulk assigned " + employeeIds.size() + " employees to job ID " + jobId,
                "EMPLOYEE", getCurrentUsername(), null
        );
    }

    @Transactional
    public void bulkUpdateCategories(List<Long> employeeIds, String categoryName) {
        Category category = null;
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));
        }
        for (Long employeeId : employeeIds) {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
            employee.setCategory(category);
            employeeRepository.save(employee);
        }
        // LOG: Bulk update categories
        activityLogService.logInfo(
                "Bulk updated category of " + employeeIds.size() + " employees to " + categoryName,
                "EMPLOYEE", getCurrentUsername(), null
        );
    }

    public List<EmployeeDTO> searchEmployees(String searchTerm) {
        List<Employee> employees = employeeRepository.searchEmployees(searchTerm);
        return employees.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<EmployeeDTO> getEmployeesByCategory(String categoryName) {
        List<Employee> employees = employeeRepository.findByCategoryName(categoryName);
        return employees.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<EmployeeDTO> getEmployeesByJob(Long jobId) {
        List<Employee> employees = employeeRepository.findByJobId(jobId);
        return employees.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateEmployeeJobPositions(Map<Long, String> employeeJobPositions) {
        for (Map.Entry<Long, String> entry : employeeJobPositions.entrySet()) {
            Employee employee = employeeRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + entry.getKey()));
            employee.setJobPosition(entry.getValue());
            Optional<User> user = userRepository.findByEmail(employee.getEmail());
            if (user.isPresent()) {
                user.get().setRole(getRoleFromJobPosition(entry.getValue()));
                userRepository.save(user.get());
            }
            employeeRepository.save(employee);
        }
        // LOG: Bulk update job positions
        activityLogService.logInfo(
                "Updated job positions for " + employeeJobPositions.size() + " employees",
                "EMPLOYEE", getCurrentUsername(), null
        );
    }

    @Transactional
    public void updateEmployeeRates(Map<Long, Double> employeeRates) {
        for (Map.Entry<Long, Double> entry : employeeRates.entrySet()) {
            Employee employee = employeeRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + entry.getKey()));
            employee.setMinimumRate(entry.getValue());
            employee.setHourlyRate(entry.getValue());
            employee.setOvertimeRate(entry.getValue() * 1.5);
            employeeRepository.save(employee);
        }
        // LOG: Bulk update rates
        activityLogService.logInfo(
                "Updated rates for " + employeeRates.size() + " employees",
                "EMPLOYEE", getCurrentUsername(), null
        );
    }

    public Map<String, Long> getEmployeeStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalEmployees", employeeRepository.count());
        stats.put("activeEmployees", employeeRepository.countByActive(true));
        stats.put("employeesOnJob", employeeRepository.countByJobNotNull());
        stats.put("supervisors", employeeRepository.countByIsSupervisor(true));
        return stats;
    }

    @Transactional
    public void terminateEmployee(Long employeeId, LocalDate terminationDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
        employee.setEndDate(terminationDate);
        employee.setActive(false);
        employeeRepository.save(employee);
        Optional<User> user = userRepository.findByEmail(employee.getEmail());
        if (user.isPresent()) {
            user.get().setEnabled(false);
            userRepository.save(user.get());
        }
        // LOG: Employee terminated
        activityLogService.logWarning(
                "Terminated employee ID " + employeeId + " effective " + terminationDate,
                "EMPLOYEE", getCurrentUsername(), employeeId
        );
    }
}