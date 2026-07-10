package com.example.eac_backend_project.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class VehicleDTO {
    private Long id;
    private String name;
    private String type;
    private String dvla;
    private String status;
    private String vehicleLicenseNumber;  // Add this
    private LocalDate vehicleLicenseExpiry;  // Add this (instead of exp)
    private LocalDate insuranceExpiry;  // Add this
    private String vin;  // Add this
    private String fuelType;
    private Double maxFuelCapacity;
    private LocalDate lastService;
    private LocalDate nextService;
    private String assignedDriver;
    private Integer year;
    private String color;
    private Double currentMileage;
    private Double nextServiceMileage;
    private String notes;

    // Include only attachment information without circular reference
    private List<Long> attachmentIds = new ArrayList<>();
    private List<String> attachmentFileNames = new ArrayList<>();
    private Integer attachmentCount = 0;

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

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
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

    public List<Long> getAttachmentIds() {
        return attachmentIds;
    }

    public void setAttachmentIds(List<Long> attachmentIds) {
        this.attachmentIds = attachmentIds;
    }

    public List<String> getAttachmentFileNames() {
        return attachmentFileNames;
    }

    public void setAttachmentFileNames(List<String> attachmentFileNames) {
        this.attachmentFileNames = attachmentFileNames;
    }

    public Integer getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }
}