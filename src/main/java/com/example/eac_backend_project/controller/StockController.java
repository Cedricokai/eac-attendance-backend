package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.ProductService;
import com.example.eac_backend_project.dto.StockAdjustmentRequest;
import com.example.eac_backend_project.entity.inventory.Product;
import com.example.eac_backend_project.entity.inventory.StockHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "http://localhost:5173")
public class StockController {

    private final ProductService productService;

    @Autowired
    public StockController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/adjust")
    public ResponseEntity<?> adjustStock(@RequestBody StockAdjustmentRequest request, Principal principal) {
        try {
            String userEmail = principal.getName();
            request.setUserEmail(userEmail);

            Product updatedProduct = productService.adjustProductStock(request);

            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to adjust stock: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while adjusting stock");
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<StockHistory>> getAllStockHistory() {
        List<StockHistory> history = productService.getAllStockHistory();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history/{productId}")
    public ResponseEntity<List<StockHistory>> getStockHistoryByProduct(@PathVariable Long productId) {
        List<StockHistory> history = productService.getStockHistoryByProductId(productId);
        return ResponseEntity.ok(history);
    }
}