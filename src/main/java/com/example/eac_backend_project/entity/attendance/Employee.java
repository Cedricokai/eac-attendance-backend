package com.example.eac_backend_project.entity.attendance;

import com.example.eac_backend_project.entity.settings.Category;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Data
@AllArgsConstructor
@Entity
@Table(name = "employee")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Employee {

    // Sunday=0 through Saturday=6; null/INHERIT uses the category for employees.
    @Column(name = "weekly_off_days")
    private String weeklyOffDays;

    public void setWeeklyOffDays(String value) {
        if (value != null && !value.equals("INHERIT") && !value.matches("[0-6](,[0-6])*|")) {
            throw new IllegalArgumentException("Invalid weekly off days");
        }
        this.weeklyOffDays = value;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String employeeId;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String phone;
    private String jobPosition;

    private String accountName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    private String ssnitAccountName;

    @Column(name = "apply_withholding_tax", columnDefinition = "boolean default false")
    private Boolean applyWithholdingTax = false;

    private Double minimumRate;
    private String workType;
    private String tagNumber;
    private LocalDate dateOfBirth;
    private String location;
    private String ghanaCard;
    private String bank;
    private String bankBranch;
    private String contactPerson;
    private String relationship;
    private String townOfResidence;
    private String houseNumber;
    private String spouse;
    private Integer numberOfChildren;
    private Integer age;

    private Boolean active;

    // Payroll fields
    private Double hourlyRate;
    private Double overtimeRate;

    @Column(name = "account_number", unique = true)
    private String accountNumber;

    @Column(name = "ssnit_number", unique = true)
    private String ssnitNumber;

    @Column(name = "tin_number", unique = true)
    private String tinNumber;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private Double basicSalary;

    @Column (name = "tier_two_account_name")
    private String tierTwoAccountName;

    @Column (name = "tier_two_account_number")
    private String tierTwoAccountNumber;

    @Column(name = "baseNumber")
    private String baseNumber;

    // Add this field with other boolean fields (near active, isSupervisor, etc.)
    @Column(name = "exclude_from_ssnit", columnDefinition = "boolean default false")
    private Boolean excludeFromSsnit = false;

    // Add getter and setter
    public Boolean getExcludeFromSsnit() {
        return excludeFromSsnit != null ? excludeFromSsnit : false;
    }

    public void setExcludeFromSsnit(Boolean excludeFromSsnit) {
        this.excludeFromSsnit = excludeFromSsnit != null ? excludeFromSsnit : false;
    }

    private String emergencyContact;
    private String department;
    private Boolean usePositionRate = false;
    private String jobGrade;

    public Employee() {
        // Initialize default values
        initializeDefaultValues();
    }

    // ================= Allowance Breakdown =================
    private Double rentAllowance = 0.0;
    private Double transportAllowance = 0.0;
    private Double clothingAllowance = 0.0;
    private Double otherAllowance = 0.0;
    private Double nssAllowance = 0.0;

    // Job relationship - Add JsonIgnore to break circular reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    @JsonIgnore
    @ToString.Exclude
    private Job job;

    // ADD THIS MISSING FIELD - Supervisor job relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervised_job_id")
    @JsonIgnore
    @ToString.Exclude
    private Job supervisedJob;

    // Supervisor relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    @JsonIgnore
    @ToString.Exclude
    private Employee supervisor;

    @OneToMany(mappedBy = "supervisor", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Employee> supervisedEmployees = new ArrayList<>();

    @Column(name = "is_supervisor")
    private Boolean isSupervisor = false;

    // REMOVE THE EXPLICIT NO-ARGS CONSTRUCTOR AND USE @NoArgsConstructor INSTEAD
    // The @NoArgsConstructor from Lombok will generate: public Employee() {}

    // ============ Constructor for flexible creation ============
    public Employee(String employeeId, String firstName, String lastName, String email) {
        // Initialize default values
        initializeDefaultValues();
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // Helper method to initialize default values
    private void initializeDefaultValues() {
        this.minimumRate = 0.0;
        this.hourlyRate = 0.0;
        this.overtimeRate = 0.0;
        this.basicSalary = 0.0;
        this.workType = "Regular";
        this.category = null;
        this.jobPosition = "Employee";
        this.startDate = LocalDate.now();
        this.numberOfChildren = 0;
        this.age = 0;
        this.rentAllowance = 0.0;
        this.transportAllowance = 0.0;
        this.clothingAllowance = 0.0;
        this.otherAllowance = 0.0;
        this.isSupervisor = false;
    }

    // Post-construct method to ensure defaults are set
    @PostLoad
    @PostPersist
    @PostUpdate
    public void ensureDefaults() {
        if (this.minimumRate == null) this.minimumRate = 0.0;
        if (this.hourlyRate == null) this.hourlyRate = 0.0;
        if (this.overtimeRate == null) this.overtimeRate = 0.0;
        if (this.basicSalary == null) this.basicSalary = 0.0;
        if (this.workType == null) this.workType = "Regular";
        if (this.category == null) this.category = null;
        if (this.jobPosition == null) this.jobPosition = "Employee";
        if (this.startDate == null) this.startDate = LocalDate.now();
        if (this.numberOfChildren == null) this.numberOfChildren = 0;
        if (this.age == null) this.age = 0;
        if (this.rentAllowance == null) this.rentAllowance = 0.0;
        if (this.transportAllowance == null) this.transportAllowance = 0.0;
        if (this.clothingAllowance == null) this.clothingAllowance = 0.0;
        if (this.otherAllowance == null) this.otherAllowance = 0.0;
        if (this.isSupervisor == null) this.isSupervisor = false;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setSupervisor(Boolean supervisor) {
        isSupervisor = supervisor;
    }

    // Supervisor job getter and setter
    public Job getSupervisedJob() {
        return supervisedJob;
    }

    public void setSupervisedJob(Job supervisedJob) {
        this.supervisedJob = supervisedJob;
    }

    // Supervisor getters and setters
    public Employee getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Employee supervisor) {
        this.supervisor = supervisor;
    }

    public List<Employee> getSupervisedEmployees() {
        return supervisedEmployees;
    }

    public void setSupervisedEmployees(List<Employee> supervisedEmployees) {
        this.supervisedEmployees = supervisedEmployees;
    }

    public Boolean getIsSupervisor() {
        return isSupervisor != null ? isSupervisor : false;
    }

    public void setIsSupervisor(Boolean isSupervisor) {
        this.isSupervisor = isSupervisor != null ? isSupervisor : false;
    }

    // Job getter and setter
    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    // ============ Getters with Null Safety ============
    public Double getMinimumRate() {
        return minimumRate != null ? minimumRate : 0.0;
    }

    public Double getHourlyRate() {
        return hourlyRate != null ? hourlyRate : 0.0;
    }

    public Double getOvertimeRate() {
        return overtimeRate != null ? overtimeRate : getHourlyRate() * 1.5;
    }

    public Double getBasicSalary() {
        return basicSalary != null ? basicSalary : 0.0;
    }

    public Double getRentAllowance() {
        return rentAllowance != null ? rentAllowance : 0.0;
    }

    public Double getTransportAllowance() {
        return transportAllowance != null ? transportAllowance : 0.0;
    }

    public Double getClothingAllowance() {
        return clothingAllowance != null ? clothingAllowance : 0.0;
    }

    public Double getOtherAllowance() {
        return otherAllowance != null ? otherAllowance : 0.0;
    }

    public Integer getNumberOfChildren() {
        return numberOfChildren != null ? numberOfChildren : 0;
    }

    public Integer getAge() {
        return age != null ? age : 0;
    }

    public Double getTotalAllowances() {
        return getRentAllowance() + getTransportAllowance() +
                getClothingAllowance() + getOtherAllowance();
    }

    // ============ Setters with Null Safety ============
    public void setMinimumRate(Double minimumRate) {
        this.minimumRate = minimumRate != null ? minimumRate : 0.0;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate != null ? hourlyRate : 0.0;
    }

    public void setOvertimeRate(Double overtimeRate) {
        this.overtimeRate = overtimeRate != null ? overtimeRate : 0.0;
    }

    public void setBasicSalary(Double basicSalary) {
        this.basicSalary = basicSalary != null ? basicSalary : 0.0;
    }

    public void setRentAllowance(Double rentAllowance) {
        this.rentAllowance = rentAllowance != null ? rentAllowance : 0.0;
    }

    public void setTransportAllowance(Double transportAllowance) {
        this.transportAllowance = transportAllowance != null ? transportAllowance : 0.0;
    }

    public void setClothingAllowance(Double clothingAllowance) {
        this.clothingAllowance = clothingAllowance != null ? clothingAllowance : 0.0;
    }

    public void setOtherAllowance(Double otherAllowance) {
        this.otherAllowance = otherAllowance != null ? otherAllowance : 0.0;
    }

    public void setNumberOfChildren(Integer numberOfChildren) {
        this.numberOfChildren = numberOfChildren != null ? numberOfChildren : 0;
    }

    public void setAge(Integer age) {
        this.age = age != null ? age : 0;
    }

    // Other getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJobPosition() {
        return jobPosition;
    }

    public void setJobPosition(String jobPosition) {
        this.jobPosition = jobPosition != null ? jobPosition : "Employee";
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType != null ? workType : "Regular";
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getSsnitNumber() {
        return ssnitNumber;
    }

    public void setSsnitNumber(String ssnitNumber) {
        this.ssnitNumber = ssnitNumber;
    }

    public String getTinNumber() {
        return tinNumber;
    }

    public void setTinNumber(String tinNumber) {
        this.tinNumber = tinNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate != null ? startDate : LocalDate.now();
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getTagNumber() {
        return tagNumber;
    }

    public void setTagNumber(String tagNumber) {
        this.tagNumber = tagNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGhanaCard() {
        return ghanaCard;
    }

    public void setGhanaCard(String ghanaCard) {
        this.ghanaCard = ghanaCard;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public void setBankBranch(String bankBranch) {
        this.bankBranch = bankBranch;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getTownOfResidence() {
        return townOfResidence;
    }

    public void setTownOfResidence(String townOfResidence) {
        this.townOfResidence = townOfResidence;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getSpouse() {
        return spouse;
    }

    public void setSpouse(String spouse) {
        this.spouse = spouse;
    }


}
