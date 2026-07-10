package com.example.eac_backend_project.entity.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "outgoing")
public class Outgoing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String code;
    private Integer version;

    @Column(name = "quantity_moved")
    @JsonProperty("quantityMoved")
    private Integer quantityMoved;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String description;
    private String location;

    @Column(nullable = false, name = "user_name")
    private String userName;

    @Column(name = "requested_by")
    private String requestedBy;

    private String ppe;
    private String projectName;
    private String department;
    private String notes;
    private Integer stock;

    @Column(name = "transfer_date")
    private LocalDateTime transferDate;

    @Column(name = "movement_date")
    private LocalDateTime movementDate;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "asset_id")
    private Long assetId;
    private String job;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type")
    private OutgoingRecordType recordType;

    @Column(name = "request_number")
    private String requestNumber;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "status")
    private String status;

    @Column(name = "kitchen_store", nullable = true)
    private Boolean kitchenStore;

    @Column(name = "returnable_after_use", nullable = true)
    private Boolean returnableAfterUse;

    @Column(name = "return_status", nullable = true)
    private String returnStatus;

    @Column(name = "returned_date", nullable = true)
    private LocalDateTime returnedDate;

    @Column(name = "expected_return_date", nullable = true)
    private LocalDateTime expectedReturnDate;

    @Column(name = "store_type", nullable = true)
    private String storeType;

    @Column(name = "return_notes" , nullable = true)
    private String returnNotes;

    public String getReturnNotes() {
        return returnNotes;
    }

    public void setReturnNotes(String returnNotes) {
        this.returnNotes = returnNotes;
    }

    public Boolean getKitchenStore() {
        return kitchenStore;
    }

    public void setKitchenStore(Boolean kitchenStore) {
        this.kitchenStore = kitchenStore;
    }

    public Boolean getReturnableAfterUse() {
        return returnableAfterUse;
    }

    public void setReturnableAfterUse(Boolean returnableAfterUse) {
        this.returnableAfterUse = returnableAfterUse;
    }

    public String getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(String returnStatus) {
        this.returnStatus = returnStatus;
    }

    public LocalDateTime getReturnedDate() {
        return returnedDate;
    }

    public void setReturnedDate(LocalDateTime returnedDate) {
        this.returnedDate = returnedDate;
    }

    public LocalDateTime getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public void setExpectedReturnDate(LocalDateTime expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public Outgoing() {}

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getQuantityMoved() {
        return quantityMoved;
    }

    public void setQuantityMoved(Integer quantityMoved) {
        this.quantityMoved = quantityMoved;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getPpe() {
        return ppe;
    }

    public void setPpe(String ppe) {
        this.ppe = ppe;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDateTime getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDateTime transferDate) {
        this.transferDate = transferDate;
    }

    public LocalDateTime getMovementDate() {
        return movementDate;
    }

    public void setMovementDate(LocalDateTime movementDate) {
        this.movementDate = movementDate;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public OutgoingRecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(OutgoingRecordType recordType) {
        this.recordType = recordType;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}