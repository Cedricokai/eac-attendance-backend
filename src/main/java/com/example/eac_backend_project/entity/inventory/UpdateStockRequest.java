package com.example.eac_backend_project.entity.inventory;

public class UpdateStockRequest {
    private Long productId;
    private int newStock;

    public UpdateStockRequest() {
    }


    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getNewStock() {
        return newStock;
    }

    public void setNewStock(int newStock) {
        this.newStock = newStock;
    }
}
