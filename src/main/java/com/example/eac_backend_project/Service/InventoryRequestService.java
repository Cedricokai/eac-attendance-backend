package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.InventoryRequestDTO;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.entity.settings.WorkflowDefinition;
import com.example.eac_backend_project.entity.settings.WorkflowStep;
import com.example.eac_backend_project.repository.*;
import com.example.eac_backend_project.Service.settings.WorkflowService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryRequestService {

    private final InventoryRequestRepository requestRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final OutgoingRepository outgoingRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkflowService workflowService;

    public InventoryRequestService(InventoryRequestRepository requestRepository,
                                   ProductRepository productRepository,
                                   ProductService productService,
                                   OutgoingRepository outgoingRepository) {
        this.requestRepository = requestRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.outgoingRepository = outgoingRepository;
    }

    // ==================== CREATE REQUEST (with workflow selection) ====================

    @Transactional
    public InventoryRequest createRequest(InventoryRequestDTO dto, String userEmail) {
        InventoryRequest request = new InventoryRequest();
        request.setRequestedBy(dto.getRequestedBy() != null ? dto.getRequestedBy() : userEmail);
        request.setDepartment(dto.getDepartment());
        request.setProjectName(dto.getProjectName());
        request.setJobDescription(dto.getJobDescription());
        request.setLocation(dto.getLocation());
        request.setNotes(dto.getNotes());
        request.setUrgency(dto.getUrgency());
        request.setPpeRequest(dto.getPpeRequest() != null ? dto.getPpeRequest() : false);
        request.setContactPerson(dto.getContactPerson());
        request.setContactPhone(dto.getContactPhone());
        request.setRequestDate(LocalDateTime.now());

        if (dto.getJobId() != null) {
            Job job = jobRepository.findById(dto.getJobId())
                    .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + dto.getJobId()));
            request.setJob(job);
            request.setJobName(job.getName());
        } else {
            request.setJobName(dto.getJobName());
        }

        if (dto.getCostCenterId() != null) {
            CostCenter costCenter = costCenterRepository.findById(dto.getCostCenterId())
                    .orElseThrow(() -> new EntityNotFoundException("CostCenter not found with id: " + dto.getCostCenterId()));
            request.setCostCenter(costCenter);
            request.setCostCenterName(costCenter.getName());
            request.setCostCenterCode(costCenter.getCode());
        } else {
            request.setCostCenterName(dto.getCostCenterName());
            request.setCostCenterCode(dto.getCostCenterCode());
        }

        request.setHasManualProducts(dto.getHasManualProducts() != null ? dto.getHasManualProducts() : false);
        request.setRequestNumber(generateRequestNumber());

        int totalQty = 0;
        Double totalEstimatedCost = 0.0;
        if (dto.getItems() != null) {
            for (InventoryRequestDTO.RequestItemDTO itemDTO : dto.getItems()) {
                RequestItem item = new RequestItem();
                item.setInventoryRequest(request);
                item.setUnitCost(itemDTO.getUnitCost() != null ? itemDTO.getUnitCost() : 0.0);
                item.setTotalCost(itemDTO.getTotalCost() != null ? itemDTO.getTotalCost() : 0.0);
                if (itemDTO.getUnitCost() != null && itemDTO.getTotalCost() != null) {
                    totalEstimatedCost += itemDTO.getTotalCost();
                }

                if (itemDTO.getIsInventory() != null && !itemDTO.getIsInventory()) {
                    item.setProductId(null);
                    item.setProductName(itemDTO.getProductName());
                    item.setProductCode(null);
                    item.setIsInventory(false);
                    item.setCurrentStock(null);
                    item.setStatus("MANUAL_NEEDS_PROCUREMENT");
                    item.setJobId(request.getJob() != null ? request.getJob().getId() : null);
                    item.setCostCenterId(request.getCostCenter() != null ? request.getCostCenter().getId() : null);
                } else {
                    Optional<Product> pOpt = productRepository.findById(itemDTO.getProductId());
                    Product product = pOpt.orElseThrow(() ->
                            new EntityNotFoundException("Product not found with id: " + itemDTO.getProductId()));
                    item.setProductId(itemDTO.getProductId());
                    item.setProductName(product.getName());
                    item.setProductCode(product.getCode());
                    item.setIsInventory(true);
                    item.setCurrentStock(product.getStock());
                    item.setJobId(request.getJob() != null ? request.getJob().getId() : null);
                    item.setCostCenterId(request.getCostCenter() != null ? request.getCostCenter().getId() : null);
                }
                item.setRequestedQuantity(itemDTO.getQuantity());
                item.setApprovedQuantity(0);
                item.setIssuedQuantity(0);
                item.setNotes(itemDTO.getNotes());
                item.setEstimatedUsage(itemDTO.getEstimatedUsage());

                request.getItems().add(item);
                totalQty += itemDTO.getQuantity() != null ? itemDTO.getQuantity() : 0;
            }
        }
        request.setQuantity(totalQty);
        if (totalEstimatedCost > 0) {
            request.setEstimatedCost(totalEstimatedCost);
        }

        // ========== DYNAMIC WORKFLOW: determine initial status ==========
        WorkflowDefinition workflow = workflowService.findMatchingWorkflow("INVENTORY", request);
        if (workflow.getSteps() != null && !workflow.getSteps().isEmpty()) {
            WorkflowStep firstStep = workflow.getSteps().get(0);
            request.setStatus(RequestStatus.valueOf(firstStep.getTargetStatus()));
        } else {
            if (Boolean.TRUE.equals(request.getPpeRequest()) ||
                    Boolean.TRUE.equals(request.getHasManualProducts())) {
                request.setStatus(RequestStatus.PROCUREMENT_PENDING);
            } else {
                request.setStatus(RequestStatus.PENDING_PLANNER);
            }
        }

        return requestRepository.save(request);
    }

    private String generateRequestNumber() {
        String prefix = "REQ";
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return prefix + "-" + timestamp + "-" + random;
    }

    // ==================== APPROVAL METHODS ====================

    @Transactional
    public InventoryRequest approveStep(Long requestId, String role, String approvedBy, String notes,
                                        Double estimatedCost, LocalDateTime deliveryDate) {
        InventoryRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        WorkflowDefinition workflow = workflowService.findMatchingWorkflow("INVENTORY", request);
        WorkflowStep currentStep = workflowService.getStepByStatus(workflow, request.getStatus().name());

        if (currentStep == null || !currentStep.getRequiredRole().equals(role)) {
            throw new RuntimeException("Not authorized or wrong step. Current status: " + request.getStatus());
        }

        // Record approval based on role
        switch (role) {
            case "PLANNER":
                request.setPlannerApprovedBy(approvedBy);
                request.setPlannerApprovalDate(LocalDateTime.now());
                request.setPlannerNotes(notes);
                break;
            case "PROCUREMENT":
                request.setProcurementApprovedBy(approvedBy);
                request.setProcurementApprovalDate(LocalDateTime.now());
                request.setProcurementNotes(notes);
                if (estimatedCost != null) request.setEstimatedCost(estimatedCost);
                if (deliveryDate != null) request.setDeliveryDate(deliveryDate);
                break;
            case "STORE":
                request.setStoreApprovedBy(approvedBy);
                request.setStoreApprovalDate(LocalDateTime.now());
                request.setStoreNotes(notes);
                break;
        }

        // Move to next step
        WorkflowStep nextStep = workflowService.getNextStep(workflow, request.getStatus().name());
        if (nextStep == null) {
            request.setStatus(RequestStatus.ISSUED);
            request.setIssuedDate(LocalDateTime.now());
            request.setIssuedBy(approvedBy);
            issueItemsLogic(request, approvedBy);
        } else {
            request.setStatus(RequestStatus.valueOf(nextStep.getTargetStatus()));
        }

        request.getItems().forEach(item -> item.setStatus(request.getStatus().name()));
        return requestRepository.save(request);
    }

    @Transactional
    public InventoryRequest approveByPlanner(Long requestId, String approvedBy, String notes) {
        return approveStep(requestId, "PLANNER", approvedBy, notes, null, null);
    }

    @Transactional
    public InventoryRequest approveByProcurement(Long requestId, String approvedBy, String notes,
                                                 Double estimatedCost, LocalDateTime deliveryDate) {
        return approveStep(requestId, "PROCUREMENT", approvedBy, notes, estimatedCost, deliveryDate);
    }

    @Transactional
    public InventoryRequest approveByStore(Long requestId, String approvedBy, String notes) {
        return approveStep(requestId, "STORE", approvedBy, notes, null, null);
    }

    // ==================== REJECTION METHODS ====================

    @Transactional
    public InventoryRequest rejectByPlanner(Long requestId, String rejectedBy, String notes) {
        return rejectStep(requestId, "PLANNER", rejectedBy, notes);
    }

    @Transactional
    public InventoryRequest rejectByProcurement(Long requestId, String rejectedBy, String notes) {
        return rejectStep(requestId, "PROCUREMENT", rejectedBy, notes);
    }

    @Transactional
    public InventoryRequest rejectByStore(Long requestId, String rejectedBy, String notes) {
        return rejectStep(requestId, "STORE", rejectedBy, notes);
    }

    private InventoryRequest rejectStep(Long requestId, String role, String rejectedBy, String notes) {
        InventoryRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        WorkflowDefinition workflow = workflowService.findMatchingWorkflow("INVENTORY", request);
        WorkflowStep currentStep = workflowService.getStepByStatus(workflow, request.getStatus().name());

        if (currentStep == null || !currentStep.getRequiredRole().equals(role)) {
            throw new RuntimeException("Not authorized or wrong step");
        }

        String rejectStatus = currentStep.getRejectStatus();
        if (rejectStatus == null || rejectStatus.isEmpty()) {
            throw new RuntimeException("This step does not have a defined reject status");
        }

        request.setStatus(RequestStatus.valueOf(rejectStatus));
        switch (role) {
            case "PLANNER":
                request.setPlannerApprovedBy(rejectedBy);
                request.setPlannerApprovalDate(LocalDateTime.now());
                request.setPlannerNotes(notes);
                break;
            case "PROCUREMENT":
                request.setProcurementApprovedBy(rejectedBy);
                request.setProcurementApprovalDate(LocalDateTime.now());
                request.setProcurementNotes(notes);
                break;
            case "STORE":
                request.setStoreApprovedBy(rejectedBy);
                request.setStoreApprovalDate(LocalDateTime.now());
                request.setStoreNotes(notes);
                break;
        }
        request.getItems().forEach(item -> item.setStatus(rejectStatus));
        return requestRepository.save(request);
    }

    // ==================== SEND TO PROCUREMENT / STORE ====================

    @Transactional
    public InventoryRequest sendToProcurement(Long requestId, String sentBy) {
        InventoryRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        WorkflowDefinition workflow = workflowService.findMatchingWorkflow("INVENTORY", request);
        Optional<WorkflowStep> procurementStep = workflow.getSteps().stream()
                .filter(step -> "PROCUREMENT".equals(step.getRequiredRole()))
                .findFirst();
        if (procurementStep.isPresent()) {
            request.setStatus(RequestStatus.valueOf(procurementStep.get().getTargetStatus()));
            request.setProcurementApprovedBy(sentBy + " (sent by planner)");
            request.setProcurementApprovalDate(LocalDateTime.now());
            request.getItems().forEach(i -> i.setStatus(request.getStatus().name()));
        } else {
            throw new RuntimeException("No procurement step defined in workflow");
        }
        return requestRepository.save(request);
    }

    @Transactional
    public InventoryRequest sendToStore(Long requestId, String sentBy) {
        InventoryRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        WorkflowDefinition workflow = workflowService.findMatchingWorkflow("INVENTORY", request);
        Optional<WorkflowStep> storeStep = workflow.getSteps().stream()
                .filter(step -> "STORE".equals(step.getRequiredRole()))
                .findFirst();
        if (storeStep.isPresent()) {
            request.setStatus(RequestStatus.valueOf(storeStep.get().getTargetStatus()));
            request.setStoreApprovedBy(sentBy + " (sent by planner)");
            request.setStoreApprovalDate(LocalDateTime.now());
            request.getItems().forEach(i -> i.setStatus(request.getStatus().name()));
        } else {
            throw new RuntimeException("No store step defined in workflow");
        }
        return requestRepository.save(request);
    }

    // ==================== STORE RECEIPT ====================

    @Transactional
    public InventoryRequest confirmReceipt(Long requestId, Integer receivedQuantity, String receivedBy) {
        InventoryRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        List<String> receiptAllowedStatuses = Arrays.asList("PROCURED", "APPROVED_BY_STORE", "STORE_REVIEW");
        if (!receiptAllowedStatuses.contains(request.getStatus().name())) {
            throw new RuntimeException("Request is not ready to be received in store. Current status: " + request.getStatus());
        }

        for (RequestItem item : request.getItems()) {
            if (item.getProductId() != null) {
                Optional<Product> pOpt = productRepository.findById(item.getProductId());
                if (pOpt.isPresent()) {
                    Product product = pOpt.get();
                    int toAdd = item.getApprovedQuantity() != null ? item.getApprovedQuantity() : item.getRequestedQuantity();
                    product.setStock(product.getStock() + toAdd);
                    productRepository.save(product);
                }
            }
            item.setStatus("RECEIVED");
        }

        request.setStatus(RequestStatus.RECEIVED_IN_STORE);
        request.setReceivedQuantity(receivedQuantity);
        request.setReceivedDate(LocalDateTime.now());
        request.setStoreApprovedBy(receivedBy);
        return requestRepository.save(request);
    }

    // ==================== ISSUE ITEMS ====================

    @Transactional
    public InventoryRequest issueItems(Long requestId, Integer issuedQuantity, String issuedBy) {
        InventoryRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (RequestStatus.ISSUED.equals(request.getStatus())) {
            throw new RuntimeException("Request has already been issued");
        }

        WorkflowDefinition workflow = workflowService.findMatchingWorkflow("INVENTORY", request);
        WorkflowStep currentStep = workflowService.getStepByStatus(workflow, request.getStatus().name());
        if (currentStep == null || !currentStep.isFinalStep()) {
            boolean okToIssue = RequestStatus.APPROVED_BY_STORE.equals(request.getStatus()) ||
                    RequestStatus.RECEIVED_IN_STORE.equals(request.getStatus()) ||
                    RequestStatus.STORE_REVIEW.equals(request.getStatus()) ||
                    RequestStatus.PROCURED.equals(request.getStatus());
            if (!okToIssue) {
                throw new RuntimeException("Request is not in a state that allows issuing items. Current status: " + request.getStatus());
            }
        }

        issueItemsLogic(request, issuedBy);
        request.setStatus(RequestStatus.ISSUED);
        request.setIssuedQuantity(issuedQuantity);
        request.setIssuedDate(LocalDateTime.now());
        request.setIssuedBy(issuedBy);
        return requestRepository.save(request);
    }

    private void issueItemsLogic(InventoryRequest request, String issuedBy) {
        List<Outgoing> outgoingsToSave = new ArrayList<>();
        for (RequestItem item : request.getItems()) {
            if (item.getProductId() == null) {
                item.setStatus("ISSUED_MANUAL");
                continue;
            }
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

            int qtyToIssue = (item.getApprovedQuantity() != null && item.getApprovedQuantity() > 0) ?
                    item.getApprovedQuantity() : item.getRequestedQuantity();

            if ("ISSUED".equals(item.getStatus())) {
                throw new RuntimeException("Item " + product.getName() + " has already been issued");
            }
            if (product.getStock() < qtyToIssue) {
                throw new RuntimeException("Not enough stock for product " + product.getName() +
                        ". Available: " + product.getStock() + ", required: " + qtyToIssue);
            }

            product.setStock(product.getStock() - qtyToIssue);
            productRepository.save(product);

            item.setIssuedQuantity(qtyToIssue);
            item.setStatus("ISSUED");

            Outgoing outgoing = new Outgoing();
            outgoing.setProductId(product.getId());
            outgoing.setName(product.getName());
            outgoing.setCode(product.getCode());
            outgoing.setQuantityMoved(qtyToIssue);
            outgoing.setRequestedBy(request.getRequestedBy());
            outgoing.setDepartment(request.getDepartment());
            outgoing.setProjectName(request.getProjectName());
            outgoing.setJob(request.getJobDescription());
            outgoing.setNotes("Issued via request: " + request.getRequestNumber());
            outgoing.setMovementDate(LocalDateTime.now());
            outgoing.setUserName(issuedBy);
            outgoing.setRecordType(OutgoingRecordType.ISSUED);
            outgoing.setStatus("ISSUED");
            outgoing.setRequestId(request.getId());
            outgoing.setRequestNumber(request.getRequestNumber());
            outgoing.setEmployeeName(request.getRequestedBy());
            outgoing.setEmployeeId(request.getRequestedBy());
            outgoingsToSave.add(outgoing);
        }
        outgoingRepository.saveAll(outgoingsToSave);
    }

    // ==================== QUERY METHODS ====================

    public List<InventoryRequest> getRequestsForPlanner() {
        return requestRepository.findByStatus(RequestStatus.PENDING_PLANNER);
    }

    public List<InventoryRequest> getRequestsForProcurement() {
        List<InventoryRequest> allRequests = requestRepository.findAll();
        return allRequests.stream()
                .filter(request ->
                        (Boolean.TRUE.equals(request.getPpeRequest()) &&
                                (request.getStatus() == RequestStatus.PROCUREMENT_PENDING ||
                                        request.getStatus() == RequestStatus.APPROVED_BY_PLANNER)) ||
                                (!Boolean.TRUE.equals(request.getPpeRequest()) &&
                                        (request.getStatus() == RequestStatus.APPROVED_BY_PLANNER ||
                                                request.getStatus() == RequestStatus.PENDING_STORE)) ||
                                request.getStatus() == RequestStatus.PROCURED ||
                                request.getStatus() == RequestStatus.REJECTED_BY_PROCUREMENT ||
                                request.getStatus() == RequestStatus.STORE_REVIEW
                )
                .collect(Collectors.toList());
    }

    public List<InventoryRequest> getRequestsForStore() {
        return requestRepository.findByStatus(RequestStatus.STORE_REVIEW);
    }

    public List<InventoryRequest> getApprovedByProcurement() {
        return requestRepository.findByStatus(RequestStatus.PROCURED);
    }

    public List<InventoryRequest> getIssuedRequests() {
        return requestRepository.findByStatus(RequestStatus.ISSUED);
    }

    public List<InventoryRequest> getAllRequests() {
        return requestRepository.findAll();
    }

    public Optional<InventoryRequest> getRequestById(Long id) {
        return requestRepository.findById(id);
    }

    public List<InventoryRequest> getRequestsByUser(String username) {
        return requestRepository.findByRequestedBy(username);
    }
}