package com.example.eac_backend_project.entity.transport;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "vehicles")
public class Vehicles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;

    @Column(name = "dvla", unique = true)
    private String dvla;  // License plate number

    @Column(name = "vehicle_license_number")
    private String vehicleLicenseNumber;  // Add this field

    @Column(name = "vehicle_license_expiry")
    private LocalDate vehicleLicenseExpiry;  // Change from 'exp' to this

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;  // Add this field

    @Column(name = "vin")
    private String vin;  // Add VIN field

    private String status;
    private String fuelType;
    private Double maxFuelCapacity;
    private LocalDate lastService;
    private LocalDate nextService;

    @Column(name = "driver")
    private String assignedDriver;

    private Integer year;
    private String color;
    private Double currentMileage;
    private Double nextServiceMileage;
    private String notes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDvla() {
        return dvla;
    }

    public void setDvla(String dvla) {
        this.dvla = dvla;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public Double getMaxFuelCapacity() {
        return maxFuelCapacity;
    }

    public void setMaxFuelCapacity(Double maxFuelCapacity) {
        this.maxFuelCapacity = maxFuelCapacity;
    }

    public LocalDate getLastService() {
        return lastService;
    }

    public void setLastService(LocalDate lastService) {
        this.lastService = lastService;
    }

    public LocalDate getNextService() {
        return nextService;
    }

    public void setNextService(LocalDate nextService) {
        this.nextService = nextService;
    }

    public String getAssignedDriver() {
        return assignedDriver;
    }

    public void setAssignedDriver(String assignedDriver) {
        this.assignedDriver = assignedDriver;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Double getCurrentMileage() {
        return currentMileage;
    }

    public void setCurrentMileage(Double currentMileage) {
        this.currentMileage = currentMileage;
    }

    public Double getNextServiceMileage() {
        return nextServiceMileage;
    }

    public void setNextServiceMileage(Double nextServiceMileage) {
        this.nextServiceMileage = nextServiceMileage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}