package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "comparison_disparities")
@Data
@NoArgsConstructor
public class ComparisonDisparity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "comparison_id", nullable = false)
    private Comparison comparison;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "employee_id")
    private String employeeId;

    private LocalDate date;

    @Column(name = "hours_value")
    private Double hoursValue;

    @Column(name = "code_value", length = 10)
    private String codeValue;

    @Column(name = "expected_hours")
    private Double expectedHours;

    @Column(name = "disparity_type", length = 50)
    private String disparityType;

    @Column(length = 500)
    private String notes;
}