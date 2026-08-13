package com.example.eac_backend_project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectPurchaseApproverDTO {
    private Long id;
    private String name;
    private String userName;
    private String email;
    private String role;
}
