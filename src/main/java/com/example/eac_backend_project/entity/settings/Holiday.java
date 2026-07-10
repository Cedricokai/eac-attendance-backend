package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "holidays")
public class Holiday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean recurring = true;

    @Column(name = "pay_multiplier")
    private Double payMultiplier = 1.5;

    // Constructors
    public Holiday() {}

    public Holiday(String name, LocalDate date, boolean recurring, Double payMultiplier) {
        this.name = name;
        this.date = date;
        this.recurring = recurring;
        this.payMultiplier = payMultiplier;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }

    public Double getPayMultiplier() { return payMultiplier; }
    public void setPayMultiplier(Double payMultiplier) { this.payMultiplier = payMultiplier; }
}