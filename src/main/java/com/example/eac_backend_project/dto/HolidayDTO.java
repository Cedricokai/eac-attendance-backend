package com.example.eac_backend_project.dto;

import java.time.LocalDate;

public class HolidayDTO {
    private Long id;
    private LocalDate date;
    private String name;
    private Boolean recurring;
    private Double payMultiplier;

    // Constructors, getters, and setters
    public HolidayDTO() {}

    public HolidayDTO(Long id, LocalDate date, String name, Boolean recurring, Double payMultiplier) {
        this.id = id;
        this.date = date;
        this.name = name;
        this.recurring = recurring;
        this.payMultiplier = payMultiplier;
    }

    // Getters and setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getRecurring() { return recurring; }
    public void setRecurring(Boolean recurring) { this.recurring = recurring; }

    public Double getPayMultiplier() { return payMultiplier; }
    public void setPayMultiplier(Double payMultiplier) { this.payMultiplier = payMultiplier; }

    public boolean isRecurring() { return Boolean.TRUE.equals(recurring); }
}
