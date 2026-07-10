package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.settings.Category;
import lombok.Data;

import java.util.Map;

@Data
public class EmployeeLeaveBalanceDTO {
    private Long employeeId;
    private String firstName;
    private String lastName;
    private String employeeCode;
    private String department;
    private Category category;
    private Integer annualBalance;
    private Integer usedDays;
    private Integer pendingDays;
    private Integer availableBalance;
    private Double usagePercentage;
    private Map<String, Integer> leaveTypeBreakdown;

    // Constructors
    public EmployeeLeaveBalanceDTO() {}

    public EmployeeLeaveBalanceDTO(Long employeeId, String firstName, String lastName,
                                   String employeeCode, String department, Category category,
                                   Integer annualBalance, Integer usedDays, Integer pendingDays,
                                   Integer availableBalance, Double usagePercentage) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.employeeCode = employeeCode;
        this.department = department;
        this.category = category;
        this.annualBalance = annualBalance;
        this.usedDays = usedDays;
        this.pendingDays = pendingDays;
        this.availableBalance = availableBalance;
        this.usagePercentage = usagePercentage;
    }



    public String getFullName() {
        return firstName + " " + lastName;
    }
}