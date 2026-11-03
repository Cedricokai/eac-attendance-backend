package com.example.eac_backend_project.dto;

import java.time.LocalDateTime;

public class ItAssetDTO {

    private Long id;
    private String assetName;
    private String assetTag;
    private String category;
    private String specifications;
    private Boolean isPublic;
    private String location;

    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String employeeDepartment;
    private String employeePosition;
    private String employeePhone;
    private String employeeEmail;

    private LocalDateTime takenAt;
    private Boolean returned;
    private LocalDateTime returnedAt;
    private String status;
    private String conditionNotes;
    private String issuedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ItAssetDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSpecifications() { return specifications; }
    public void setSpecifications(String specifications) { this.specifications = specifications; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeDepartment() { return employeeDepartment; }
    public void setEmployeeDepartment(String employeeDepartment) { this.employeeDepartment = employeeDepartment; }

    public String getEmployeePosition() { return employeePosition; }
    public void setEmployeePosition(String employeePosition) { this.employeePosition = employeePosition; }

    public String getEmployeePhone() { return employeePhone; }
    public void setEmployeePhone(String employeePhone) { this.employeePhone = employeePhone; }

    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }

    public LocalDateTime getTakenAt() { return takenAt; }
    public void setTakenAt(LocalDateTime takenAt) { this.takenAt = takenAt; }

    public Boolean getReturned() { return returned; }
    public void setReturned(Boolean returned) { this.returned = returned; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConditionNotes() { return conditionNotes; }
    public void setConditionNotes(String conditionNotes) { this.conditionNotes = conditionNotes; }

    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

