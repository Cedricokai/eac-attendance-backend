package com.example.eac_backend_project.Service;


import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.repository.CostCenterRepository;
import com.example.eac_backend_project.repository.CostTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CostCenterService {
    private final CostCenterRepository costCenterRepository;
    private final CostTransactionRepository costTransactionRepository;


    @Transactional
    public CostCenter createCostCenterForJob(Job job, User createdBy) {
        // Generate unique code for cost center
        String code = "CC-" + job.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);

        // Use job budget if available, otherwise set default
        Double budget = job.getBudget() != null ? job.getBudget() : 100000.0;

        CostCenter costCenter = new CostCenter();
        costCenter.setCode(code);
        costCenter.setName(job.getName() + " Cost Center");
        costCenter.setDescription("Cost center for job: " + job.getName());
        costCenter.setType(CostCenterType.PROJECT);
        costCenter.setStatus(CostCenterStatus.ACTIVE);
        costCenter.setBudget(budget);
        costCenter.setSpentAmount(0.0);
        costCenter.setJob(job);
        costCenter.setCreatedBy(createdBy);
        costCenter.setStartDate(LocalDateTime.now());

        if (job.getEndDate() != null) {
            costCenter.setEndDate(job.getEndDate().atStartOfDay());
        }

        return costCenterRepository.save(costCenter);
    }

    @Transactional
    public CostTransaction recordProductIssue(
            CostCenter costCenter,
            Job job,
            Product product,
            Employee issuedTo,
            User issuedBy,
            Integer quantity,
            Double unitCost
    ) {
        // Calculate total cost
        Double totalCost = quantity * unitCost;

        // Update cost center spent amount
        costCenter.addSpentAmount(totalCost);
        costCenterRepository.save(costCenter);

        // Create transaction record
        CostTransaction transaction = new CostTransaction();
        transaction.setTransactionNumber("TRX-" + UUID.randomUUID().toString().substring(0, 8));
        transaction.setCostCenter(costCenter);
        transaction.setJob(job);
        transaction.setProduct(product);
        transaction.setTransactionType(TransactionType.PRODUCT_ISSUE);
        transaction.setAmount(totalCost);
        transaction.setDescription(
                String.format("Issued %d units of %s to %s %s",
                        quantity, product.getName(),
                        issuedTo.getFirstName(), issuedTo.getLastName())
        );
        transaction.setIssuedTo(issuedTo);
        transaction.setIssuedBy(issuedBy);
        transaction.setQuantity(quantity);
        transaction.setUnitCost(unitCost);

        return costTransactionRepository.save(transaction);
    }

    @Transactional
    public CostTransaction recordManualTransaction(
            Long costCenterId,
            Double amount,
            String description,
            User user
    ) {
        CostCenter costCenter = costCenterRepository.findById(costCenterId)
                .orElseThrow(() -> new RuntimeException("Cost center not found"));

        // Update cost center spent amount
        costCenter.addSpentAmount(amount);
        costCenterRepository.save(costCenter);

        // Create transaction record
        CostTransaction transaction = new CostTransaction();
        transaction.setTransactionNumber("TRX-MAN-" + UUID.randomUUID().toString().substring(0, 8));
        transaction.setCostCenter(costCenter);
        transaction.setJob(costCenter.getJob());
        transaction.setTransactionType(TransactionType.MANUAL_ADJUSTMENT);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setIssuedBy(user);

        return costTransactionRepository.save(transaction);
    }

    public CostCenter getCostCenterByJobId(Long jobId) {
        return costCenterRepository.findByJob_Id(jobId)
                .orElseThrow(() -> new RuntimeException("Cost center not found for job: " + jobId));
    }

    public List<CostTransaction> getTransactionsByJobId(Long jobId) {
        return costTransactionRepository.findByJobId(jobId);
    }

    public List<CostTransaction> getTransactionsByCostCenterId(Long costCenterId) {
        return costTransactionRepository.findByCostCenterId(costCenterId);
    }

    @Transactional
    public CostCenter updateBudget(Long costCenterId, Double newBudget) {
        CostCenter costCenter = costCenterRepository.findById(costCenterId)
                .orElseThrow(() -> new RuntimeException("Cost center not found"));

        if (newBudget < costCenter.getSpentAmount()) {
            throw new RuntimeException("New budget cannot be less than spent amount");
        }

        costCenter.setBudget(newBudget);
        costCenterRepository.save(costCenter);
        return costCenter;
    }

    public Double getTotalSpentByJob(Long jobId) {
        CostCenter costCenter = getCostCenterByJobId(jobId);
        return costCenter.getSpentAmount();
    }

    // Add this method if missing
    public CostCenter save(CostCenter costCenter) {
        return costCenterRepository.save(costCenter);
    }

    public List<CostCenter> findAll() {
        return costCenterRepository.findAll();
    }

    @Transactional
    public CostCenter updateCostCenter(Long id, CostCenter updatedCostCenter) {
        CostCenter existingCostCenter = costCenterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cost center not found with id: " + id));

        // Update only non-null fields
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
        if (updatedCostCenter.getSpentAmount() != null) {
            existingCostCenter.setSpentAmount(updatedCostCenter.getSpentAmount());
        }
        if (updatedCostCenter.getStartDate() != null) {
            existingCostCenter.setStartDate(updatedCostCenter.getStartDate());
        }
        if (updatedCostCenter.getEndDate() != null) {
            existingCostCenter.setEndDate(updatedCostCenter.getEndDate());
        }

        return costCenterRepository.save(existingCostCenter);
    }

    public Optional<CostCenter> findById(Long id) {
        return costCenterRepository.findById(id);
    }
}