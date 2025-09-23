package com.example.eac_backend_project.Controller;

import com.example.eac_backend_project.Service.ProductService;
import com.example.eac_backend_project.entity.inventory.MoveRequest;
import com.example.eac_backend_project.entity.inventory.Product;
import com.example.eac_backend_project.entity.inventory.UpdateStockRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Get all products
    @GetMapping
    public List<Product> getProducts() {
        return productService.getAllProducts();
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Product> addProduct(@RequestBody Product product, Principal principal) {
        String userEmail = principal.getName(); // Get the authenticated user's email
        Product savedProduct = productService.addProduct(product, userEmail);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }


    // Update an existing product (Admin only)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product updatedProduct, Principal principal) {
        try {
            String userEmail = principal.getName();
            Product savedProduct = productService.updateProduct(id, updatedProduct, userEmail);
            return ResponseEntity.ok(savedProduct);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, Principal principal) {
        String userEmail = principal.getName();
        productService.deleteProduct(id, userEmail);
        return ResponseEntity.noContent().build();
    }

    // Search products (Public access)
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String query) {
        List<Product> products = productService.searchProducts(query);
        if (products.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    // Move products to outgoing (Admin only)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/outgoing")
    public ResponseEntity<String> moveProductsToOutgoing(@RequestBody MoveRequest moveRequest) {
        try {
            productService.moveProductsToOutgoing(moveRequest.getProductIds(), moveRequest.getLocation(), moveRequest.getRequestedBy());
            return new ResponseEntity<>("Products moved to outgoing successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to move products to outgoing: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/update-stock")
    public ResponseEntity<String> updateStock(@RequestBody UpdateStockRequest update, Principal principal) {
        try {
            String userEmail = principal.getName();
            productService.updateProductStock(update.getProductId(), update.getNewStock(), userEmail);
            return ResponseEntity.ok("Stock updated successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Failed to update stock: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    public long getTotalNumberOfProducts() {
        return productService.getTotalNumberOfProducts();
    }

    @GetMapping("/total-stock")
    public ResponseEntity<Integer> getTotalStock() {
        int totalStock = productService.getTotalStock();
        return ResponseEntity.ok(totalStock);
    }

}
