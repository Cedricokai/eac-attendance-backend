package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String employeeId;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String jobPosition;
    private String category;
    private Double minimumRate;
    private String workType;

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

    // ================= Allowance Breakdown (permanent employee setup) =================
    private Double rentAllowance = 0.0;
    private Double transportAllowance = 0.0;
    private Double clothingAllowance = 0.0;
    private Double otherAllowance = 0.0;

    // ============ Getters with Null Safety ============
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

    public Double getTotalAllowances() {
        return getRentAllowance() + getTransportAllowance() +
                getClothingAllowance() + getOtherAllowance();
    }

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
        this.jobPosition = jobPosition;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getMinimumRate() {
        return minimumRate;
    }

    public void setMinimumRate(Double minimumRate) {
        this.minimumRate = minimumRate;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public void setOvertimeRate(Double overtimeRate) {
        this.overtimeRate = overtimeRate;
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
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setBasicSalary(Double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public void setRentAllowance(Double rentAllowance) {
        this.rentAllowance = rentAllowance;
    }

    public void setTransportAllowance(Double transportAllowance) {
        this.transportAllowance = transportAllowance;
    }

    public void setClothingAllowance(Double clothingAllowance) {
        this.clothingAllowance = clothingAllowance;
    }

    public void setOtherAllowance(Double otherAllowance) {
        this.otherAllowance = otherAllowance;
    }
}
