package com.example.eac_backend_project.Service;


import com.example.eac_backend_project.entity.inventory.ActionType;
import com.example.eac_backend_project.entity.inventory.Outgoing;
import com.example.eac_backend_project.entity.inventory.Product;
import com.example.eac_backend_project.entity.inventory.ProductHistory;
import com.example.eac_backend_project.repository.OutgoingRepository;
import com.example.eac_backend_project.repository.ProductHistoryRepository;
import com.example.eac_backend_project.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OutgoingRepository outgoingRepository;
    private final ProductHistoryRepository productHistoryRepository;
    private final Logger logger = LoggerFactory.getLogger(ProductService.class);

    public ProductService(ProductRepository productRepository, OutgoingRepository outgoingRepository, ProductHistoryRepository productHistoryRepository) {
        this.productRepository = productRepository;
        this.outgoingRepository = outgoingRepository;
        this.productHistoryRepository = productHistoryRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product addProduct(Product product, String userEmail) {
        // Save the product
        Product savedProduct = productRepository.save(product);

        // Log the action in product history
        logProductHistory(savedProduct, ActionType.ADD, userEmail);

        // Log the action in the system log
        logger.info("Product added: {}", savedProduct.getName());

        return savedProduct;
    }

    public Product updateProduct(Long id, Product updatedProduct, String userEmail) {
        Optional<Product> existingProductOptional = productRepository.findById(id);
        if (existingProductOptional.isPresent()) {
            Product existingProduct = existingProductOptional.get();

            // Update fields
            existingProduct.setName(updatedProduct.getName());
            existingProduct.setCode(updatedProduct.getCode());
            existingProduct.setDescription(updatedProduct.getDescription());
            existingProduct.setStock(updatedProduct.getStock());
            existingProduct.setUserName(updatedProduct.getUserName());
            existingProduct.setProductType(updatedProduct.getProductType());

            // Save the updated product
            Product savedProduct = productRepository.save(existingProduct);

            // Log the action in product history
            logProductHistory(savedProduct, ActionType.EDIT, userEmail);

            return savedProduct;
        } else {
            throw new RuntimeException("Product with ID " + id + " not found.");
        }
    }

    public boolean existsById(Long id) {
        return productRepository.existsById(id);
    }

    public void deleteProduct(Long id, String userEmail) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();

            // Log the action before deleting
            logProductHistory(product, ActionType.DELETE, userEmail);

            // Delete the product
            productRepository.deleteById(id);
        } else {
            throw new RuntimeException("Product with ID " + id + " not found.");
        }
    }

    public List<Product> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(query, query);
    }

    @Transactional
    public void moveProductsToOutgoing(List<Long> productIds, String location, String requestedBy) {
        List<Product> productsToMove = productRepository.findAllById(productIds);
        logger.info("Products to move: {}", productsToMove);

        if (productsToMove.isEmpty()) {
            throw new IllegalArgumentException("No products found for the given IDs.");
        }

        productsToMove.forEach(product -> {
            product.setStock(product.getStock() - 1);
            productRepository.save(product);
        });

        List<Outgoing> outgoingItems = productsToMove.stream().map(product -> {
            Outgoing outgoing = new Outgoing();
            outgoing.setName(product.getName());
            outgoing.setCode(product.getCode());
            outgoing.setDescription(product.getDescription());
            outgoing.setMovementDate(product.getLastUpdatedDate());
            outgoing.setLocation(location);
            outgoing.setRequestedBy(requestedBy);
            outgoing.setUserName(product.getUserName());
            logger.info("Creating outgoing item: {}", outgoing);
            return outgoing;
        }).collect(Collectors.toList());

        outgoingRepository.saveAll(outgoingItems);
    }

    private void logProductHistory(Product product, ActionType actionType, String userEmail) {
        ProductHistory history = new ProductHistory();
        history.setProductId(product.getId());
        history.setActionType(actionType);
        history.setActionDate(LocalDateTime.now());
        history.setUserEmail(userEmail);
        history.setDescription("The Action '" + actionType.name() + "' was performed on the product '" + product.getName() + "'");

        // Save the history record
        productHistoryRepository.save(history);
    }

    @Transactional
    public Product updateProductStock(Long productId, int newStock, String userEmail) {
        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            int oldStock = product.getStock();
            product.setStock(newStock);
            productRepository.save(product);

            // Log product history
            logProductHistory(product, ActionType.EDIT, userEmail);

            logger.info("Stock updated for product {} from {} to {}", product.getName(), oldStock, newStock);
            return product;
        } else {
            throw new RuntimeException("Product with ID " + productId + " not found.");
        }
    }

    public long getTotalNumberOfProducts() {
        return productRepository.count();
    }

    public int getTotalStock() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .mapToInt(Product::getStock)
                .sum();
    }

}
