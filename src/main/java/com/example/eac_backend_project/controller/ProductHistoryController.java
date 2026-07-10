package com.example.eac_backend_project.controller;


import com.example.eac_backend_project.entity.inventory.ActionType;
import com.example.eac_backend_project.entity.inventory.Product;
import com.example.eac_backend_project.entity.inventory.ProductHistory;
import com.example.eac_backend_project.repository.ProductHistoryRepository;
import com.example.eac_backend_project.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/product-history")
public class ProductHistoryController {

    private final ProductHistoryRepository productHistoryRepository;
    private final ProductRepository productRepository;

    @Autowired
    public ProductHistoryController(ProductHistoryRepository productHistoryRepository, ProductRepository productRepository) {
        this.productHistoryRepository = productHistoryRepository;
        this.productRepository = productRepository;
    }

    @PostMapping("/log")
    public ResponseEntity<String> logProductHistory(
            @RequestParam Long productId,
            @RequestParam ActionType actionType,
            @RequestParam String userEmail
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        ProductHistory history = new ProductHistory();
        history.setProductId(product.getId());
        history.setActionType(actionType);
        history.setActionDate(LocalDateTime.now());
        history.setUserEmail(userEmail);
        history.setDescription("The Action " + actionType.name() + " was performed on the product'" + product.getName() + "'");

        productHistoryRepository.save(history);

        return ResponseEntity.ok("Product history logged successfully.");
    }

    @GetMapping
    public ResponseEntity<List<ProductHistory>> getAllProductHistory() {
        List<ProductHistory> history = productHistoryRepository.findAll();
        return ResponseEntity.ok(history);
    }
}


