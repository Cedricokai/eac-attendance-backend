package com.example.eac_backend_project.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Long id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String jobPosition;
    private String jobGrade;
    private String category;
    private Double minimumRate;
    private String workType;
    private String ssnitAccountName;
    private String ssnitNumber;
    private String tinNumber;
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
    private String accountName;
    private String spouse;
    private Integer numberOfChildren;
    private Integer age;
    private Boolean usePositionRate;
    private Double basicSalary;
    private LocalDate startDate;
    private LocalDate endDate;
    private String accountNumber;
    private String emergencyContact;
    private String department;
    private Double rentAllowance;
    private Double transportAllowance;
    private Double clothingAllowance;
    private Double otherAllowance;
    private Double nssAllowance;
    private Long jobId;
    private String jobName;
    private Boolean isSupervisor;
    private Boolean active;
    private Boolean excludeFromSsnit;
    private Boolean applyWithholdingTax;
    private String tierTwoAccountName;
    private String tierTwoAccountNumber;
    private String baseNumber;

    public Boolean getExcludeFromSsnit() {
        return excludeFromSsnit;
    }

    public void setExcludeFromSsnit(Boolean excludeFromSsnit) {
        this.excludeFromSsnit = excludeFromSsnit != null ? excludeFromSsnit : false;
    }
}