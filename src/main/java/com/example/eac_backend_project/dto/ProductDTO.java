package com.example.eac_backend_project.dto;

import com.example.eac_backend_project.entity.inventory.Product;

public class ProductDTO {

    private Long id;
    private String name;
    private String code;
    private Integer stock;
    private String productType;

    public ProductDTO(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.code = product.getCode();
        this.stock = product.getStock();
        this.productType = product.getProductType();
    }
}
