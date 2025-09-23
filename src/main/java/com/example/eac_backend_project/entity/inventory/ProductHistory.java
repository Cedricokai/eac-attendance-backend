package com.example.eac_backend_project.entity.inventory;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
@Entity
@Table(name = "product_history")
public class ProductHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long productId;

    private Long assetsId;
    
    @Enumerated(EnumType.STRING)
    private ActionType actionType;
    
    private LocalDateTime actionDate;
    
    private String userEmail;
    
    private String description;

    public void setId(Long id) {
        this.id = id;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setAssetsId(Long assetsId) {
        this.assetsId = assetsId;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public void setActionDate(LocalDateTime actionDate) {
        this.actionDate = actionDate;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Getters and setters
}


    

