package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
public class PayrollPeriod {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;

    @Column(name = "convert_excess_to_overtime", nullable = false)
    private boolean convertExcessToOvertime = true;

    @Column(name = "category")
    private String category; // Category periods only.
    @Column(columnDefinition = "varchar(20) default 'CATEGORY'")
    private String periodType = "CATEGORY";
    private Long projectId;
    private String projectName;
    @jakarta.persistence.Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isProjectPayroll() { return "PROJECT".equals(periodType); }

    @Column(name = "status")
    private String status = "Open"; // Open, Closed, Processing
}
