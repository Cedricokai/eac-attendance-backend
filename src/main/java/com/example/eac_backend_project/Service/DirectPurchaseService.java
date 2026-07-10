package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Service.settings.WorkflowService;
import com.example.eac_backend_project.dto.DirectPurchaseItemDTO;
import com.example.eac_backend_project.dto.DirectPurchaseRequestDTO;
import com.example.eac_backend_project.dto.DirectPurchaseResponseDTO;
import com.example.eac_backend_project.entity.directpurchase.DirectPurchase;
import com.example.eac_backend_project.entity.directpurchase.DirectPurchaseItem;
import com.example.eac_backend_project.entity.directpurchase.DirectPurchaseStatus;
import com.example.eac_backend_project.entity.settings.WorkflowDefinition;
import com.example.eac_backend_project.entity.settings.WorkflowStep;
import com.example.eac_backend_project.repository.DirectPurchaseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DirectPurchaseService {

    private static final String ENTITY_TYPE = "DIRECT_PURCHASE";

    @Autowired
    private DirectPurchaseRepository repository;

    @Autowired
    private WorkflowService workflowService;

    // ------------------ Helper Methods ------------------

    private String generateRequestNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = repository.count() + 1;
        String seq = String.format("%04d", count);
        return "DP-" + datePart + "-" + seq;
    }

    private String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);
    }

    private DirectPurchaseResponseDTO toResponseDTO(DirectPurchase purchase) {
        DirectPurchaseResponseDTO dto = new DirectPurchaseResponseDTO();
        dto.setId(purchase.getId());
        dto.setRequestNumber(purchase.getRequestNumber());
        dto.setRequestDate(purchase.getRequestDate());
        dto.setRequester(purchase.getRequester());
        dto.setAccountant(purchase.getAccountant());
        dto.setNotes(purchase.getNotes());
        dto.setStatus(purchase.getStatus());
        dto.setTotalAmount(purchase.getTotalAmount());
        dto.setItems(purchase.getItems().stream().map(item -> {
            DirectPurchaseItemDTO itemDTO = new DirectPurchaseItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setDescription(item.getDescription());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setUnitPrice(item.getUnitPrice());
            itemDTO.setTotal(item.getTotal());
            return itemDTO;
        }).collect(Collectors.toList()));

        // Determine the required role for the current status
        try {
            WorkflowDefinition workflow = workflowService.findMatchingWorkflow(ENTITY_TYPE, purchase);
            WorkflowStep currentStep = workflowService.getStepByStatus(workflow, purchase.getStatus().name());
            if (currentStep != null && currentStep.getRequiredRole() != null) {
                dto.setCurrentRequiredRole(currentStep.getRequiredRole());
            } else {
                dto.setCurrentRequiredRole(null);
            }
        } catch (Exception e) {
            // If no workflow is found, no role is required
            dto.setCurrentRequiredRole(null);
        }

        return dto;
    }

    // ------------------ CRUD Operations ------------------

    @Transactional
    public DirectPurchaseResponseDTO createRequest(DirectPurchaseRequestDTO dto) {
        DirectPurchase purchase = new DirectPurchase();
        purchase.setRequestNumber(generateRequestNumber());
        purchase.setRequestDate(dto.getRequestDate() != null ? dto.getRequestDate() : LocalDate.now());
        purchase.setRequester(dto.getRequester());
        purchase.setAccountant(dto.getAccountant());
        purchase.setNotes(dto.getNotes());

        for (DirectPurchaseItemDTO itemDTO : dto.getItems()) {
            DirectPurchaseItem item = new DirectPurchaseItem();
            item.setDescription(itemDTO.getDescription());
            item.setQuantity(itemDTO.getQuantity() != null ? itemDTO.getQuantity() : 1);
            item.setUnitPrice(itemDTO.getUnitPrice() != null ? itemDTO.getUnitPrice() : 0.0);
            item.calculateTotal();
            purchase.addItem(item);
        }
        purchase.recalculateTotal();

        purchase.setStatus(DirectPurchaseStatus.PENDING);
        DirectPurchase saved = repository.save(purchase);

        WorkflowDefinition workflow = workflowService.findMatchingWorkflow(ENTITY_TYPE, saved);
        List<WorkflowStep> steps = workflow.getSteps();
        if (!steps.isEmpty()) {
            WorkflowStep firstStep = steps.get(0);
            saved.setStatus(DirectPurchaseStatus.valueOf(firstStep.getTargetStatus()));
            repository.save(saved);
        }

        return toResponseDTO(saved);
    }

    public List<DirectPurchaseResponseDTO> getAllRequests() {
        return repository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public DirectPurchaseResponseDTO getRequestById(Long id) {
        DirectPurchase purchase = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Direct purchase request not found with id: " + id));
        return toResponseDTO(purchase);
    }

    // ------------------ Workflow‑driven Actions ------------------

    @Transactional
    public DirectPurchaseResponseDTO advanceToNextStep(Long id, String notes) {
        DirectPurchase purchase = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + id));

        WorkflowDefinition workflow = workflowService.findMatchingWorkflow(ENTITY_TYPE, purchase);
        String currentStatus = purchase.getStatus().name();

        // Get current step
        WorkflowStep currentStep = workflowService.getStepByStatus(workflow, currentStatus);
        if (currentStep == null) {
            throw new IllegalStateException("No step defined for status: " + currentStatus);
        }

        // Role check
        String userRole = getCurrentUserRole();
        if (userRole == null) {
            throw new AccessDeniedException("You are not authenticated.");
        }
        if (!userRole.equals(currentStep.getRequiredRole())) {
            throw new AccessDeniedException(
                    "You do not have permission to approve this step. Required role: " + currentStep.getRequiredRole() +
                            ", Your role: " + userRole
            );
        }

        WorkflowStep nextStep = workflowService.getNextStep(workflow, currentStatus);
        if (nextStep == null) {
            throw new IllegalStateException("No further step defined for this workflow. Current status: " + currentStatus);
        }

        purchase.setStatus(DirectPurchaseStatus.valueOf(nextStep.getTargetStatus()));
        if (notes != null && !notes.isEmpty()) {
            purchase.setNotes((purchase.getNotes() != null ? purchase.getNotes() + "\n" : "") + "Step advanced: " + notes);
        }
        return toResponseDTO(repository.save(purchase));
    }

    @Transactional
    public DirectPurchaseResponseDTO rejectRequest(Long id, String notes) {
        DirectPurchase purchase = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + id));

        if (purchase.getStatus() == DirectPurchaseStatus.PAID) {
            throw new IllegalStateException("Cannot reject a request that is already PAID.");
        }

        // Optional: Check if the user is allowed to reject (same role as current step)
        WorkflowDefinition workflow = workflowService.findMatchingWorkflow(ENTITY_TYPE, purchase);
        WorkflowStep currentStep = workflowService.getStepByStatus(workflow, purchase.getStatus().name());
        if (currentStep != null) {
            String userRole = getCurrentUserRole();
            if (userRole == null || !userRole.equals(currentStep.getRequiredRole())) {
                throw new AccessDeniedException(
                        "You do not have permission to reject this request. Required role: " + currentStep.getRequiredRole()
                );
            }
        }

        purchase.setStatus(DirectPurchaseStatus.REJECTED);
        if (notes != null && !notes.isEmpty()) {
            purchase.setNotes((purchase.getNotes() != null ? purchase.getNotes() + "\n" : "") + "Rejection reason: " + notes);
        }
        return toResponseDTO(repository.save(purchase));
    }

    @Transactional
    public DirectPurchaseResponseDTO approveRequest(Long id, String notes) {
        return advanceToNextStep(id, notes);
    }

    @Transactional
    public DirectPurchaseResponseDTO reviewerApprove(Long id, String notes) {
        return advanceToNextStep(id, notes);
    }

    @Transactional
    public DirectPurchaseResponseDTO payRequest(Long id, String notes) {
        DirectPurchase purchase = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + id));

        if (purchase.getStatus() != DirectPurchaseStatus.REVIEWER_APPROVED) {
            throw new IllegalStateException("Request must be REVIEWER_APPROVED before payment. Current status: " + purchase.getStatus());
        }

        // Optional: Check if user has permission (usually the same role as the final step)
        WorkflowDefinition workflow = workflowService.findMatchingWorkflow(ENTITY_TYPE, purchase);
        WorkflowStep currentStep = workflowService.getStepByStatus(workflow, purchase.getStatus().name());
        if (currentStep != null) {
            String userRole = getCurrentUserRole();
            if (userRole == null || !userRole.equals(currentStep.getRequiredRole())) {
                throw new AccessDeniedException(
                        "You do not have permission to mark this request as paid. Required role: " + currentStep.getRequiredRole()
                );
            }
        }

        purchase.setStatus(DirectPurchaseStatus.PAID);
        if (notes != null && !notes.isEmpty()) {
            purchase.setNotes((purchase.getNotes() != null ? purchase.getNotes() + "\n" : "") + "Payment notes: " + notes);
        }
        return toResponseDTO(repository.save(purchase));
    }

    @Transactional
    public void deleteRequest(Long id) {
        DirectPurchase purchase = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Direct purchase request not found with id: " + id));

        if (purchase.getStatus() == DirectPurchaseStatus.PAID) {
            throw new IllegalStateException("Cannot delete a PAID request.");
        }
        repository.delete(purchase);
    }
}