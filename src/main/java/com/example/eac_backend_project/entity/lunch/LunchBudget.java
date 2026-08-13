package com.example.eac_backend_project.entity.lunch;

import com.example.eac_backend_project.entity.attendance.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lunch_budget")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LunchBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "budget_type", nullable = false)
    private String budgetType;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Employee department;
}