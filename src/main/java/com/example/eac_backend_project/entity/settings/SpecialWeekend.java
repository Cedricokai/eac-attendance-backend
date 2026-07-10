package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "special_weekends")
public class SpecialWeekend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "rate_multiplier")
    private Double rateMultiplier = 1.5;

    // Constructors
    public SpecialWeekend() {}

    public SpecialWeekend(String name, LocalDate date, Double rateMultiplier) {
        this.name = name;
        this.date = date;
        this.rateMultiplier = rateMultiplier;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Double getRateMultiplier() { return rateMultiplier; }
    public void setRateMultiplier(Double rateMultiplier) { this.rateMultiplier = rateMultiplier; }
}