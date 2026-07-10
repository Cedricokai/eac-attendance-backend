package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.attendance.JobProduct;

public class JobProductDTO {
    private Long id;
    private Integer quantity;
    private String estimatedUsage;
    private ProductDTO product; // Create a simplified ProductDTO

    public JobProductDTO(JobProduct jobProduct) {
        this.id = jobProduct.getId();
        this.quantity = jobProduct.getQuantity();
        this.estimatedUsage = jobProduct.getEstimatedUsage();
        this.product = new ProductDTO(jobProduct.getProduct());
    }
}
