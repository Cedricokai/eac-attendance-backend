package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.inventory.CostCenter;
import com.example.eac_backend_project.entity.inventory.CostCenterProduct;
import com.example.eac_backend_project.entity.inventory.CostCenterProductStatus;
import com.example.eac_backend_project.entity.inventory.Product;
import com.example.eac_backend_project.repository.CostCenterProductRepository;
import com.example.eac_backend_project.repository.CostCenterRepository;
import com.example.eac_backend_project.repository.ProductRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CostCenterProductService {

    @Autowired
    private  CostCenterProductRepository costCenterProductRepository;

    @Autowired
    private  CostCenterRepository costCenterRepository;

    @Autowired
    private  ProductRepository productRepository;

    @Transactional
    public CostCenterProduct addProductToCostCenter(
            Long costCenterId,
            Long productId,
            Integer requestedQuantity,
            String estimatedUsage) {

        CostCenter costCenter = costCenterRepository.findById(costCenterId)
                .orElseThrow(() -> new RuntimeException("Cost center not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if product already exists in cost center
        CostCenterProduct existing = costCenterProductRepository
                .findByCostCenterIdAndProductId(costCenterId, productId);

        if (existing != null) {
            // Update existing
            existing.setRequestedQuantity(existing.getRequestedQuantity() + requestedQuantity);
            existing.setEstimatedUsage(estimatedUsage);
            existing.setRequestDate(LocalDateTime.now());
            existing.calculateTotalCost();
            return costCenterProductRepository.save(existing);
        } else {
            // Create new using constructor
            CostCenterProduct costCenterProduct = new CostCenterProduct();
            costCenterProduct.setCostCenter(costCenter);
            costCenterProduct.setProduct(product);
            costCenterProduct.setRequestedQuantity(requestedQuantity);
            costCenterProduct.setEstimatedUsage(estimatedUsage);
            costCenterProduct.setUnitCost(product.getUnitCost());
            costCenterProduct.setStatus(CostCenterProductStatus.PROCUREMENT_PENDING);
            costCenterProduct.setRequestDate(LocalDateTime.now());

            // Manually calculate total cost
            costCenterProduct.calculateTotalCost();

            return costCenterProductRepository.save(costCenterProduct);
        }
    }

    @Transactional
    public List<CostCenterProduct> addMultipleProductsToCostCenter(
            Long costCenterId,
            List<CostCenterProductRequest> requests) {

        CostCenter costCenter = costCenterRepository.findById(costCenterId)
                .orElseThrow(() -> new RuntimeException("Cost center not found"));

        return requests.stream()
                .map(request -> addProductToCostCenter(
                        costCenterId,
                        request.getProductId(),
                        request.getRequestedQuantity(),
                        request.getEstimatedUsage()))
                .toList();
    }

    public List<CostCenterProduct> getProductsByCostCenterId(Long costCenterId) {
        return costCenterProductRepository.findByCostCenterId(costCenterId);
    }

    public List<CostCenterProduct> getProductsByJobId(Long jobId) {
        return costCenterProductRepository.findByCostCenterJobId(jobId);
    }

    @Transactional
    public CostCenterProduct updateProductStatus(
            Long costCenterProductId,
            CostCenterProductStatus status,
            Integer approvedQuantity) {

        CostCenterProduct product = costCenterProductRepository.findById(costCenterProductId)
                .orElseThrow(() -> new RuntimeException("Cost center product not found"));

        product.setStatus(status);

        if (approvedQuantity != null) {
            product.setApprovedQuantity(approvedQuantity);
        }

        if (status == CostCenterProductStatus.APPROVED) {
            product.setApprovalDate(LocalDateTime.now());
        } else if (status == CostCenterProductStatus.ISSUED) {
            product.setIssueDate(LocalDateTime.now());
            product.setIssuedQuantity(product.getApprovedQuantity());
        }

        return costCenterProductRepository.save(product);
    }

    // In CostCenterProductService.java - Add this method
    public CostCenterProduct getCostCenterProductById(Long id) {
        return costCenterProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cost center product not found"));
    }

    // In CostCenterProductService.java

    public List<CostCenterProduct> getProductsByRequestId(Long requestId) {
        return costCenterProductRepository.findByInventoryRequestId(requestId);
    }

    // DTO for batch requests
    @Data
    public static class CostCenterProductRequest {
        private Long productId;
        private Integer requestedQuantity;
        private String estimatedUsage;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getRequestedQuantity() {
            return requestedQuantity;
        }

        public void setRequestedQuantity(Integer requestedQuantity) {
            this.requestedQuantity = requestedQuantity;
        }

        public String getEstimatedUsage() {
            return estimatedUsage;
        }

        public void setEstimatedUsage(String estimatedUsage) {
            this.estimatedUsage = estimatedUsage;
        }
    }
}