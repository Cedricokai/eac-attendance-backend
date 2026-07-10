// CostCenterController.java
package com.example.eac_backend_project.controller;


import com.example.eac_backend_project.Service.CostCenterService;
import com.example.eac_backend_project.Service.JobService;
import com.example.eac_backend_project.Service.UserService;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.inventory.CostCenter;
import com.example.eac_backend_project.entity.inventory.CostTransaction;
import com.example.eac_backend_project.entity.inventory.Product;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.CostCenterRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.JobRepository;
import com.example.eac_backend_project.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cost-centers")
@RequiredArgsConstructor
public class CostCenterController {
    private final CostCenterService costCenterService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private CostCenterRepository costCenterRepository;


    // Add this POST endpoint if missing
    @PostMapping
    public ResponseEntity<CostCenter> createCostCenter(
            @Valid @RequestBody CostCenter costCenter,
            @AuthenticationPrincipal User user) {

        System.out.println("=== Creating Cost Center ===");
        System.out.println("Job ID from request: " + costCenter.getJobId());

        costCenter.setCreatedBy(user);
        costCenter.setSpentAmount(0.0); // Initialize spent amount to 0

        if (costCenter.getStartDate() == null) {
            costCenter.setStartDate(LocalDateTime.now());
        }

        // IMPORTANT: If jobId is provided, fetch and set the job
        if (costCenter.getJobId() != null) {
            try {
                // You need to inject JobRepository to fetch the job
                Job job = jobRepository.findById(costCenter.getJobId())
                        .orElseThrow(() -> new RuntimeException("Job not found with id: " + costCenter.getJobId()));
                costCenter.setJob(job);
                System.out.println("Linked job: " + job.getName() + " to cost center");
            } catch (Exception e) {
                System.err.println("Failed to link job to cost center: " + e.getMessage());
            }
        }

        CostCenter savedCostCenter = costCenterService.save(costCenter);
        System.out.println("Cost center saved with ID: " + savedCostCenter.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCostCenter);
    }

    // Also add a GET endpoint to get all cost centers if missing
    @GetMapping
    public ResponseEntity<List<CostCenter>> getAllCostCenters() {
        List<CostCenter> costCenters = costCenterService.findAll();
        return ResponseEntity.ok(costCenters);
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<CostCenter> getCostCenterByJob(@PathVariable Long jobId) {
        CostCenter costCenter = costCenterService.getCostCenterByJobId(jobId);
        return ResponseEntity.ok(costCenter);
    }

    @GetMapping("/{costCenterId}/transactions")
    public ResponseEntity<List<CostTransaction>> getCostCenterTransactions(
            @PathVariable Long costCenterId) {
        List<CostTransaction> transactions =
                costCenterService.getTransactionsByCostCenterId(costCenterId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/job/{jobId}/transactions")
    public ResponseEntity<List<CostTransaction>> getJobTransactions(@PathVariable Long jobId) {
        List<CostTransaction> transactions =
                costCenterService.getTransactionsByJobId(jobId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/job/{jobId}/total-spent")
    public ResponseEntity<Map<String, Object>> getJobTotalSpent(@PathVariable Long jobId) {
        Double totalSpent = costCenterService.getTotalSpentByJob(jobId);
        CostCenter costCenter = costCenterService.getCostCenterByJobId(jobId);

        return ResponseEntity.ok(Map.of(
                "totalSpent", totalSpent,
                "budget", costCenter.getBudget(),
                "remaining", costCenter.getRemainingBudget(),
                "utilizationPercentage", costCenter.getUtilizationPercentage()
        ));
    }

    @PutMapping("/{costCenterId}/budget")
    public ResponseEntity<CostCenter> updateBudget(
            @PathVariable Long costCenterId,
            @RequestBody Map<String, Double> request,
            @AuthenticationPrincipal User user) {
        Double newBudget = request.get("budget");
        CostCenter updated = costCenterService.updateBudget(costCenterId, newBudget);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{costCenterId}/manual-transaction")
    public ResponseEntity<CostTransaction> addManualTransaction(
            @PathVariable Long costCenterId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User user) {
        Double amount = (Double) request.get("amount");
        String description = (String) request.get("description");

        CostTransaction transaction = costCenterService.recordManualTransaction(
                costCenterId, amount, description, user);

        return ResponseEntity.ok(transaction);
    }

    // Add this method to your existing CostCenterController
    @PostMapping("/product-issue")
    public ResponseEntity<?> recordProductIssue(
            @RequestBody ProductIssueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User user = userService.getCurrentUser();

            // Get cost center by job
            CostCenter costCenter = costCenterService.getCostCenterByJobId(request.getJobId());
            if (costCenter == null) {
                return ResponseEntity.badRequest().body("Cost center not found for job: " + request.getJobId());
            }

            // Get job
            Job job = jobService.findById(request.getJobId());

            // Get product
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Get employee (issued to)
            Employee employee = employeeRepository.findById(request.getIssuedToEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            // Record the transaction
            CostTransaction transaction = costCenterService.recordProductIssue(
                    costCenter,
                    job,
                    product,
                    employee,
                    user,
                    request.getQuantity(),
                    request.getUnitCost()
            );

            return ResponseEntity.ok(transaction);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error recording product issue: " + e.getMessage());
        }
    }

    // Request DTO
    public static class ProductIssueRequest {
        private Long jobId;
        private Long productId;
        private String productName;
        private Integer quantity;
        private Double unitCost;
        private Long issuedToEmployeeId;
        private String issuedToEmployeeName;
        private String requestNumber;
        private String description;

        public Long getJobId() {
            return jobId;
        }

        public void setJobId(Long jobId) {
            this.jobId = jobId;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getUnitCost() {
            return unitCost;
        }

        public void setUnitCost(Double unitCost) {
            this.unitCost = unitCost;
        }

        public Long getIssuedToEmployeeId() {
            return issuedToEmployeeId;
        }

        public void setIssuedToEmployeeId(Long issuedToEmployeeId) {
            this.issuedToEmployeeId = issuedToEmployeeId;
        }

        public String getIssuedToEmployeeName() {
            return issuedToEmployeeName;
        }

        public void setIssuedToEmployeeName(String issuedToEmployeeName) {
            this.issuedToEmployeeName = issuedToEmployeeName;
        }

        public String getRequestNumber() {
            return requestNumber;
        }

        public void setRequestNumber(String requestNumber) {
            this.requestNumber = requestNumber;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        // Getters and setters
    }

    // Add this method to your CostCenterController
    @PutMapping("/{id}")
    public ResponseEntity<CostCenter> updateCostCenter(
            @PathVariable Long id,
            @Valid @RequestBody CostCenter updatedCostCenter,
            @AuthenticationPrincipal User user) {

        System.out.println("=== Updating Cost Center ID: " + id + " ===");
        System.out.println("Received spentAmount: " + updatedCostCenter.getSpentAmount());
        System.out.println("Received budget: " + updatedCostCenter.getBudget());

        // Find existing cost center
        CostCenter existingCostCenter = costCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cost center not found with id: " + id));

        // Update fields
        if (updatedCostCenter.getName() != null) {
            existingCostCenter.setName(updatedCostCenter.getName());
        }
        if (updatedCostCenter.getDescription() != null) {
            existingCostCenter.setDescription(updatedCostCenter.getDescription());
        }
        if (updatedCostCenter.getType() != null) {
            existingCostCenter.setType(updatedCostCenter.getType());
        }
        if (updatedCostCenter.getStatus() != null) {
            existingCostCenter.setStatus(updatedCostCenter.getStatus());
        }
        if (updatedCostCenter.getBudget() != null) {
            existingCostCenter.setBudget(updatedCostCenter.getBudget());
        }

        // CRITICAL: Update spentAmount
        if (updatedCostCenter.getSpentAmount() != null) {
            System.out.println("Updating spentAmount from " + existingCostCenter.getSpentAmount() + " to " + updatedCostCenter.getSpentAmount());
            existingCostCenter.setSpentAmount(updatedCostCenter.getSpentAmount());
        }

        if (updatedCostCenter.getStartDate() != null) {
            existingCostCenter.setStartDate(updatedCostCenter.getStartDate());
        }
        if (updatedCostCenter.getEndDate() != null) {
            existingCostCenter.setEndDate(updatedCostCenter.getEndDate());
        }

        // Save the updated cost center
        CostCenter savedCostCenter = costCenterRepository.save(existingCostCenter);

        System.out.println("Successfully updated cost center. New spentAmount: " + savedCostCenter.getSpentAmount());

        return ResponseEntity.ok(savedCostCenter);
    }

}