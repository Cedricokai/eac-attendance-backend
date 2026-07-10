package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Entity
@Data
@Table(name = "leave_settings")
public class LeaveSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "maternity_leave_months")
    private Integer maternityLeaveMonths = 3;

    @Column(name = "paternity_leave_months")
    private Integer paternityLeaveMonths = 1;

    @Column(name = "annual_leave_balance")
    private Integer annualLeaveBalance = 20;

    @ElementCollection
    @CollectionTable(name = "non_deductible_leave_types", joinColumns = @JoinColumn(name = "leave_settings_id"))
    @Column(name = "leave_type")
    private List<String> nonDeductibleLeaveTypes = Arrays.asList("Maternity", "Paternity", "Sick", "Study");

    // Constructors, getters, setters
    public LeaveSettings() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getMaternityLeaveMonths() { return maternityLeaveMonths; }
    public void setMaternityLeaveMonths(Integer maternityLeaveMonths) { this.maternityLeaveMonths = maternityLeaveMonths; }

    public Integer getPaternityLeaveMonths() { return paternityLeaveMonths; }
    public void setPaternityLeaveMonths(Integer paternityLeaveMonths) { this.paternityLeaveMonths = paternityLeaveMonths; }

    public List<String> getNonDeductibleLeaveTypes() { return nonDeductibleLeaveTypes; }
    public void setNonDeductibleLeaveTypes(List<String> nonDeductibleLeaveTypes) { this.nonDeductibleLeaveTypes = nonDeductibleLeaveTypes; }


}