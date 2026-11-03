package com.example.eac_backend_project.entity.inventory;

import com.example.eac_backend_project.entity.attendance.Employee;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "it_assets")
public class ItAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(name = "asset_tag")
    private String assetTag;

    @Column(name = "category", nullable = false)
    private String category = "LAPTOP";

    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "location")
    private String location;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    @Column(name = "is_returned", nullable = false)
    private Boolean returned = false;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "condition_notes", columnDefinition = "TEXT")
    private String conditionNotes;

    @Column(name = "issued_by")
    private String issuedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isPublic == null) {
            this.isPublic = false;
        }
        if (this.returned == null) {
            this.returned = false;
        }
        if (this.takenAt == null && !Boolean.TRUE.equals(this.isPublic)) {
            this.takenAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.isPublic == null) {
            this.isPublic = false;
        }
        if (this.returned == null) {
            this.returned = false;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public Boolean getIsPublic() {
        return isPublic != null ? isPublic : false;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic != null ? isPublic : false;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public Boolean getReturned() {
        return returned != null ? returned : false;
    }

    public void setReturned(Boolean returned) {
        this.returned = returned != null ? returned : false;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public String getConditionNotes() {
        return conditionNotes;
    }

    public void setConditionNotes(String conditionNotes) {
        this.conditionNotes = conditionNotes;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

