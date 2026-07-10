package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.CostCenterProductService;
import com.example.eac_backend_project.entity.inventory.CostCenterProduct;
import com.example.eac_backend_project.entity.inventory.CostCenterProductStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cost-center-products")
@RequiredArgsConstructor
public class CostCenterProductController {

    @Autowired
    private CostCenterProductService costCenterProductService;

    @PostMapping("/cost-center/{costCenterId}")
    public ResponseEntity<CostCenterProduct> addProductToCostCenter(
            @PathVariable Long costCenterId,
            @RequestBody CostCenterProductService.CostCenterProductRequest request) {

        CostCenterProduct product = costCenterProductService.addProductToCostCenter(
                costCenterId,
                request.getProductId(),
                request.getRequestedQuantity(),
                request.getEstimatedUsage());

        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PostMapping("/cost-center/{costCenterId}/batch")
    public ResponseEntity<List<CostCenterProduct>> addMultipleProductsToCostCenter(
            @PathVariable Long costCenterId,
            @RequestBody List<CostCenterProductService.CostCenterProductRequest> requests) {

        List<CostCenterProduct> products = costCenterProductService
                .addMultipleProductsToCostCenter(costCenterId, requests);

        return ResponseEntity.status(HttpStatus.CREATED).body(products);
    }

    @GetMapping("/cost-center/{costCenterId}")
    public ResponseEntity<List<CostCenterProduct>> getProductsByCostCenter(
            @PathVariable Long costCenterId) {

        List<CostCenterProduct> products = costCenterProductService
                .getProductsByCostCenterId(costCenterId);

        return ResponseEntity.ok(products);
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<CostCenterProduct>> getProductsByJob(
            @PathVariable Long jobId) {

        List<CostCenterProduct> products = costCenterProductService
                .getProductsByJobId(jobId);

        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CostCenterProduct> updateProductStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {

        CostCenterProduct product = costCenterProductService.updateProductStatus(
                id, request.getStatus(), request.getApprovedQuantity());

        return ResponseEntity.ok(product);
    }

    // In CostCenterProductController.java - Add this endpoint
    @GetMapping("/{id}")
    public ResponseEntity<CostCenterProduct> getCostCenterProductById(@PathVariable Long id) {
        CostCenterProduct product = costCenterProductService.getCostCenterProductById(id);
        return ResponseEntity.ok(product);
    }

    // In CostCenterProductController.java - Add this endpoint
    @GetMapping("/request/{requestId}")
    public ResponseEntity<List<CostCenterProduct>> getProductsByRequestId(@PathVariable Long requestId) {
        List<CostCenterProduct> products = costCenterProductService.getProductsByRequestId(requestId);
        return ResponseEntity.ok(products);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {
        private CostCenterProductStatus status;
        private Integer approvedQuantity;

        public CostCenterProductStatus getStatus() {
            return status;
        }

        public void setStatus(CostCenterProductStatus status) {
            this.status = status;
        }

        public Integer getApprovedQuantity() {
            return approvedQuantity;
        }

        public void setApprovedQuantity(Integer approvedQuantity) {
            this.approvedQuantity = approvedQuantity;
        }
    }

}