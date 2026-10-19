package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.Service.settings.WorkflowService;
import com.example.eac_backend_project.dto.InventoryRequestDTO;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.entity.settings.WorkflowDefinition;
import com.example.eac_backend_project.entity.settings.WorkflowStep;
import com.example.eac_backend_project.repository.CostCenterRepository;
import com.example.eac_backend_project.repository.InventoryRequestRepository;
import com.example.eac_backend_project.repository.JobRepository;
import com.example.eac_backend_project.repository.OutgoingRepository;
import com.example.eac_backend_project.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryRequestService {

    @org.springframework.beans.factory.annotation.Autowired private UserService inventoryUserService;
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.repository.InventoryPurchaseLineRepository purchaseLines;
    private final InventoryRequestRepository requestRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final OutgoingRepository outgoingRepository;
    private final CostCenterRepository costCenterRepository;
    private final JobRepository jobRepository;
    private final WorkflowService workflowService;
    private final EmailService emailService;

    public InventoryRequestService(
            InventoryRequestRepository requestRepository,
            ProductRepository productRepository,
            ProductService productService,
            OutgoingRepository outgoingRepository,
            CostCenterRepository costCenterRepository,
            JobRepository jobRepository,
            WorkflowService workflowService,
            EmailService emailService
    ) {
        this.requestRepository = requestRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.outgoingRepository = outgoingRepository;
        this.costCenterRepository = costCenterRepository;
        this.jobRepository = jobRepository;
        this.workflowService = workflowService;
        this.emailService = emailService;
    }

    // ============================================================
    // CREATE REQUEST
    // ============================================================

    @Transactional
    public InventoryRequest createRequest(
            InventoryRequestDTO dto,
            String userEmail
    ) {
        InventoryRequest request = new InventoryRequest();

        User origin = inventoryUserService.getCurrentUser();
        request.setRequestedBy(origin.getUserName());
        request.setFinanceFulfilment(true);
        request.setRequesterUserId(origin.getId());
        request.setRequesterName(origin.getName());
        request.setRequesterEmail(origin.getEmail());

        request.setDepartment(dto.getDepartment());
        request.setProjectName(dto.getProjectName());
        request.setJobDescription(dto.getJobDescription());
        request.setLocation(dto.getLocation());
        request.setNotes(dto.getNotes());
        request.setUrgency(dto.getUrgency());

        request.setPpeRequest(
                Boolean.TRUE.equals(dto.getPpeRequest())
        );

        request.setHasManualProducts(
                Boolean.TRUE.equals(dto.getHasManualProducts())
        );

        request.setContactPerson(dto.getContactPerson());
        request.setContactPhone(dto.getContactPhone());
        request.setRequestDate(LocalDateTime.now());
        request.setRequestNumber(generateRequestNumber());

        assignJob(request, dto);
        assignCostCenter(request, dto);
        addRequestItems(request, dto);

        determineInitialStatus(request);

        InventoryRequest savedRequest =
                requestRepository.save(request);

        sendCreatedNotificationSafely(
                savedRequest,
                userEmail
        );

        return savedRequest;
    }

    private void assignJob(
            InventoryRequest request,
            InventoryRequestDTO dto
    ) {
        if (dto.getJobId() != null) {
            Job job = jobRepository.findById(dto.getJobId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Job not found with id: " +
                                            dto.getJobId()
                            )
                    );

            request.setJob(job);
            request.setJobName(job.getName());
        } else {
            request.setJobName(dto.getJobName());
        }
    }

    private void assignCostCenter(
            InventoryRequest request,
            InventoryRequestDTO dto
    ) {
        if (dto.getCostCenterId() != null) {
            CostCenter costCenter =
                    costCenterRepository
                            .findById(dto.getCostCenterId())
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "CostCenter not found with id: " +
                                                    dto.getCostCenterId()
                                    )
                            );

            request.setCostCenter(costCenter);
            request.setCostCenterName(costCenter.getName());
            request.setCostCenterCode(costCenter.getCode());
        } else {
            request.setCostCenterName(dto.getCostCenterName());
            request.setCostCenterCode(dto.getCostCenterCode());
        }
    }

    private void addRequestItems(
            InventoryRequest request,
            InventoryRequestDTO dto
    ) {
        int totalQuantity = 0;
        double totalEstimatedCost = 0.0;

        if (dto.getItems() == null ||
                dto.getItems().isEmpty()) {
            request.setQuantity(0);
            return;
        }

        for (InventoryRequestDTO.RequestItemDTO itemDTO :
                dto.getItems()) {

            RequestItem item = new RequestItem();

            item.setInventoryRequest(request);

            item.setUnitCost(
                    itemDTO.getUnitCost() != null
                            ? itemDTO.getUnitCost()
                            : 0.0
            );

            item.setTotalCost(
                    itemDTO.getTotalCost() != null
                            ? itemDTO.getTotalCost()
                            : 0.0
            );

            if (itemDTO.getTotalCost() != null) {
                totalEstimatedCost +=
                        itemDTO.getTotalCost();
            }

            boolean inventoryItem =
                    !Boolean.FALSE.equals(
                            itemDTO.getIsInventory()
                    );

            if (!inventoryItem) {
                configureManualRequestItem(
                        request,
                        item,
                        itemDTO
                );
            } else {
                configureInventoryRequestItem(
                        request,
                        item,
                        itemDTO
                );
            }

            Integer requestedQuantity =
                    itemDTO.getQuantity() != null
                            ? itemDTO.getQuantity()
                            : 0;

            item.setRequestedQuantity(requestedQuantity);
            item.setApprovedQuantity(null);
            item.setIssuedQuantity(0);
            item.setNotes(itemDTO.getNotes());
            item.setEstimatedUsage(
                    itemDTO.getEstimatedUsage()
            );

            request.getItems().add(item);

            totalQuantity += requestedQuantity;
        }

        request.setQuantity(totalQuantity);

        if (totalEstimatedCost > 0) {
            request.setEstimatedCost(
                    totalEstimatedCost
            );
        }
    }

    private void configureManualRequestItem(
            InventoryRequest request,
            RequestItem item,
            InventoryRequestDTO.RequestItemDTO itemDTO
    ) {
        item.setProductId(null);
        item.setProductName(itemDTO.getProductName());
        item.setProductCode(null);
        item.setIsInventory(false);
        item.setCurrentStock(null);
        item.setStatus("MANUAL_NEEDS_PROCUREMENT");

        item.setJobId(
                request.getJob() != null
                        ? request.getJob().getId()
                        : null
        );

        item.setCostCenterId(
                request.getCostCenter() != null
                        ? request.getCostCenter().getId()
                        : null
        );
    }

    private void configureInventoryRequestItem(
            InventoryRequest request,
            RequestItem item,
            InventoryRequestDTO.RequestItemDTO itemDTO
    ) {
        if (itemDTO.getProductId() == null) {
            throw new IllegalArgumentException(
                    "Product ID is required for an inventory item"
            );
        }

        Product product =
                productRepository
                        .findById(itemDTO.getProductId())
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Product not found with id: " +
                                                itemDTO.getProductId()
                                )
                        );

        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductCode(product.getCode());
        item.setIsInventory(true);
        item.setCurrentStock(product.getStock());

        item.setJobId(
                request.getJob() != null
                        ? request.getJob().getId()
                        : null
        );

        item.setCostCenterId(
                request.getCostCenter() != null
                        ? request.getCostCenter().getId()
                        : null
        );
    }

    private void determineInitialStatus(
            InventoryRequest request
    ) {
        // New employee requests enter an unapproved queue; workflow target statuses are approval outcomes.
        request.setStatus(requiresProcurement(request) ? RequestStatus.PROCUREMENT_PENDING : RequestStatus.PENDING_STORE);
    }

    private String generateRequestNumber() {
        String timestamp =
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "yyyyMMddHHmmss"
                        )
                );

        String random =
                String.format(
                        "%04d",
                        new Random().nextInt(10000)
                );

        return "REQ-" + timestamp + "-" + random;
    }

    // ============================================================
    // APPROVAL
    // ============================================================

    @Transactional
    public InventoryRequest approveStep(
            Long requestId,
            String role,
            String approvedBy,
            String notes,
            Double estimatedCost,
            LocalDateTime deliveryDate
    ) {
        InventoryRequest request =
                getRequiredRequest(requestId);

        if(request.isFinanceFulfilment()) {
            requireFulfilmentApprovalRole(role);
            if("PROCUREMENT".equals(role) && request.getStatus()==RequestStatus.PROCUREMENT_PENDING) request.setStatus(RequestStatus.APPROVED_BY_PROCUREMENT);
            else if("STORE".equals(role) && Set.of(RequestStatus.PENDING_STORE,RequestStatus.STORE_REVIEW).contains(request.getStatus())) request.setStatus(RequestStatus.APPROVED_BY_STORE);
            else if("PLANNER".equals(role) && Set.of(RequestStatus.PENDING,RequestStatus.PENDING_PLANNER).contains(request.getStatus())) request.setStatus(requiresProcurement(request)?RequestStatus.PROCUREMENT_PENDING:RequestStatus.PENDING_STORE);
            else throw new IllegalArgumentException("Request is not awaiting this approval");
            for(RequestItem i:request.getItems())if(i.getApprovedQuantity()==null||i.getApprovedQuantity()==0)i.setApprovedQuantity(i.getRequestedQuantity());
            recordApproval(request,role,approvedBy,notes,estimatedCost,deliveryDate);
            InventoryRequest saved=requestRepository.save(request);sendStatusNotificationSafely(saved,role+"_APPROVED",approvedBy,notes);return saved;
        }

        WorkflowDefinition workflow =
                workflowService.findMatchingWorkflow(
                        "INVENTORY",
                        request
                );

        WorkflowStep currentStep = resolveActionStep(
                workflow,
                request.getStatus().name(),
                role
        );

        validateRoleForStep(
                currentStep,
                role,
                request
        );

        recordApproval(
                request,
                role,
                approvedBy,
                notes,
                estimatedCost,
                deliveryDate
        );

        WorkflowStep nextStep =
                workflowService.getNextStep(
                        workflow,
                        request.getStatus().name()
                );

        if (nextStep == null) {
            completeRequestByIssuing(
                    request,
                    approvedBy
            );
        } else {
            request.setStatus(
                    parseRequestStatus(
                            nextStep.getTargetStatus()
                    )
            );
        }

        updateAllItemStatuses(
                request,
                request.getStatus().name()
        );

        InventoryRequest savedRequest =
                requestRepository.save(request);

        sendStatusNotificationSafely(
                savedRequest,
                role + "_APPROVED",
                approvedBy,
                notes
        );

        return savedRequest;
    }

    private void recordApproval(
            InventoryRequest request,
            String role,
            String approvedBy,
            String notes,
            Double estimatedCost,
            LocalDateTime deliveryDate
    ) {
        switch (role) {
            case "PLANNER" -> {
                request.setPlannerApprovedBy(approvedBy);
                request.setPlannerApprovalDate(
                        LocalDateTime.now()
                );
                request.setPlannerNotes(notes);
            }

            case "PROCUREMENT" -> {
                request.setProcurementApprovedBy(
                        approvedBy
                );

                request.setProcurementApprovalDate(
                        LocalDateTime.now()
                );

                request.setProcurementNotes(notes);

                if (estimatedCost != null) {
                    request.setEstimatedCost(
                            estimatedCost
                    );
                }

                if (deliveryDate != null) {
                    request.setDeliveryDate(
                            deliveryDate
                    );
                }
            }

            case "STORE" -> {
                request.setStoreApprovedBy(approvedBy);
                request.setStoreApprovalDate(
                        LocalDateTime.now()
                );
                request.setStoreNotes(notes);
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported approval role: " + role
            );
        }
    }

    private void completeRequestByIssuing(
            InventoryRequest request,
            String approvedBy
    ) {
        // Approval authorizes fulfilment; an incomplete stock request must remain issuable in parts.
        if(request.isFinanceFulfilment() || request.getItems().stream().anyMatch(i -> i.getProductId()==null || productRepository.findById(i.getProductId()).map(p -> p.getStock() - (purchaseLines == null ? 0 : purchaseLines.reservedStock(p.getId())) < getApprovedOrRequestedQuantity(i)).orElse(true))) {
            request.setStatus(RequestStatus.APPROVED_BY_STORE);
            return;
        }
        issueItemsLogic(request, approvedBy);

        request.setStatus(RequestStatus.ISSUED);
        request.setIssuedDate(LocalDateTime.now());
        request.setIssuedBy(approvedBy);
    }

    @Transactional
    public InventoryRequest approveByPlanner(
            Long requestId,
            String approvedBy,
            String notes
    ) {
        return approveStep(
                requestId,
                "PLANNER",
                approvedBy,
                notes,
                null,
                null
        );
    }

    @Transactional
    public InventoryRequest approveByProcurement(
            Long requestId,
            String approvedBy,
            String notes,
            Double estimatedCost,
            LocalDateTime deliveryDate
    ) {
        InventoryRequest approvedRequest = approveStep(
                requestId,
                "PROCUREMENT",
                approvedBy,
                notes,
                estimatedCost,
                deliveryDate
        );

        // Procurement approval completes the procurement stage. Move the
        // request immediately into the configured Store queue so it appears
        // under Received > Manage Requests without a second manual action.
        if(approvedRequest.isFinanceFulfilment()) return approvedRequest;
        return sendToStore(
                approvedRequest.getId(),
                approvedBy
        );
    }

    @Transactional
    public InventoryRequest approveByStore(
            Long requestId,
            String approvedBy,
            String notes
    ) {
        return approveStep(
                requestId,
                "STORE",
                approvedBy,
                notes,
                null,
                null
        );
    }

    // ============================================================
    // REJECTION
    // ============================================================

    @Transactional
    public InventoryRequest rejectByPlanner(
            Long requestId,
            String rejectedBy,
            String notes
    ) {
        return rejectStep(
                requestId,
                "PLANNER",
                rejectedBy,
                notes
        );
    }

    @Transactional
    public InventoryRequest rejectByProcurement(
            Long requestId,
            String rejectedBy,
            String notes
    ) {
        return rejectStep(
                requestId,
                "PROCUREMENT",
                rejectedBy,
                notes
        );
    }

    @Transactional
    public InventoryRequest rejectByStore(
            Long requestId,
            String rejectedBy,
            String notes
    ) {
        return rejectStep(
                requestId,
                "STORE",
                rejectedBy,
                notes
        );
    }

    private InventoryRequest rejectStep(
            Long requestId,
            String role,
            String rejectedBy,
            String notes
    ) {
        InventoryRequest request =
                getRequiredRequest(requestId);

        if(request.isFinanceFulfilment()) {
            requireFulfilmentApprovalRole(role);
            boolean pending = switch(role) {
                case "STORE" -> Set.of(RequestStatus.PENDING_STORE,RequestStatus.STORE_REVIEW).contains(request.getStatus());
                case "PROCUREMENT" -> request.getStatus()==RequestStatus.PROCUREMENT_PENDING;
                case "PLANNER" -> Set.of(RequestStatus.PENDING,RequestStatus.PENDING_PLANNER).contains(request.getStatus());
                default -> false;
            };
            if(!pending) throw new IllegalArgumentException("Only a request awaiting this review can be rejected; return priced proposals through Accounts");
            RequestStatus rejected = switch(role) {case "STORE" -> RequestStatus.REJECTED_BY_STORE;case "PROCUREMENT" -> RequestStatus.REJECTED_BY_PROCUREMENT;default -> RequestStatus.REJECTED_BY_PLANNER;};
            request.setStatus(rejected);recordRejection(request,role,rejectedBy,notes);InventoryRequest saved=requestRepository.save(request);sendStatusNotificationSafely(saved,role+"_REJECTED",rejectedBy,notes);return saved;
        }

        WorkflowDefinition workflow =
                workflowService.findMatchingWorkflow(
                        "INVENTORY",
                        request
                );

        WorkflowStep currentStep = resolveActionStep(
                workflow,
                request.getStatus().name(),
                role
        );

        validateRoleForStep(
                currentStep,
                role,
                request
        );

        String rejectStatus =
                currentStep.getRejectStatus();

        if (rejectStatus == null ||
                rejectStatus.trim().isEmpty()) {
            throw new RuntimeException(
                    "This workflow step does not have a reject status"
            );
        }

        RequestStatus parsedRejectStatus =
                parseRequestStatus(rejectStatus);

        request.setStatus(parsedRejectStatus);

        recordRejection(
                request,
                role,
                rejectedBy,
                notes
        );

        updateAllItemStatuses(
                request,
                parsedRejectStatus.name()
        );

        InventoryRequest savedRequest =
                requestRepository.save(request);

        sendStatusNotificationSafely(
                savedRequest,
                role + "_REJECTED",
                rejectedBy,
                notes
        );

        return savedRequest;
    }

    private void recordRejection(
            InventoryRequest request,
            String role,
            String rejectedBy,
            String notes
    ) {
        switch (role) {
            case "PLANNER" -> {
                request.setPlannerApprovedBy(rejectedBy);
                request.setPlannerApprovalDate(
                        LocalDateTime.now()
                );
                request.setPlannerNotes(notes);
            }

            case "PROCUREMENT" -> {
                request.setProcurementApprovedBy(
                        rejectedBy
                );
                request.setProcurementApprovalDate(
                        LocalDateTime.now()
                );
                request.setProcurementNotes(notes);
            }

            case "STORE" -> {
                request.setStoreApprovedBy(rejectedBy);
                request.setStoreApprovalDate(
                        LocalDateTime.now()
                );
                request.setStoreNotes(notes);
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported rejection role: " + role
            );
        }
    }

    // ============================================================
    // SEND TO PROCUREMENT
    // ============================================================

    @Transactional
    public InventoryRequest sendToProcurement(
            Long requestId,
            String sentBy
    ) {
        InventoryRequest request =
                getRequiredRequest(requestId);

        WorkflowDefinition workflow =
                workflowService.findMatchingWorkflow(
                        "INVENTORY",
                        request
                );

        if (workflow == null ||
                workflow.getSteps() == null) {
            throw new RuntimeException(
                    "No workflow found for this request"
            );
        }

        WorkflowStep procurementStep =
                workflow.getSteps()
                        .stream()
                        .filter(step ->
                                roleContains(
                                        step.getRequiredRole(),
                                        "PROCUREMENT"
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No procurement step defined in workflow"
                                )
                        );

        request.setStatus(
                parseRequestStatus(
                        procurementStep.getTargetStatus()
                )
        );

        request.setProcurementApprovedBy(
                sentBy + " (sent to procurement)"
        );

        request.setProcurementApprovalDate(
                LocalDateTime.now()
        );

        updateAllItemStatuses(
                request,
                request.getStatus().name()
        );

        InventoryRequest savedRequest =
                requestRepository.save(request);

        sendStatusNotificationSafely(
                savedRequest,
                "SENT_TO_PROCUREMENT",
                sentBy,
                null
        );

        return savedRequest;
    }

    // ============================================================
    // SEND TO STORE
    // ============================================================

    @Transactional
    public InventoryRequest sendToStore(
            Long requestId,
            String sentBy
    ) {
        InventoryRequest request =
                getRequiredRequest(requestId);

        WorkflowDefinition workflow =
                workflowService.findMatchingWorkflow(
                        "INVENTORY",
                        request
                );

        if (workflow == null ||
                workflow.getSteps() == null) {
            throw new RuntimeException(
                    "No workflow found for this request"
            );
        }

        WorkflowStep storeStep =
                workflow.getSteps()
                        .stream()
                        .filter(step ->
                                roleContains(
                                        step.getRequiredRole(),
                                        "STORE"
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No store step defined in workflow"
                                )
                        );

        request.setStatus(
                parseRequestStatus(
                        storeStep.getTargetStatus()
                )
        );

        updateAllItemStatuses(
                request,
                request.getStatus().name()
        );

        InventoryRequest savedRequest =
                requestRepository.save(request);

        sendStatusNotificationSafely(
                savedRequest,
                "SENT_TO_STORE",
                sentBy,
                null
        );

        return savedRequest;
    }

    // ============================================================
    // CONFIRM STORE RECEIPT
    // ============================================================

    @Transactional
    public InventoryRequest confirmReceipt(
            Long requestId,
            Integer receivedQuantity,
            String receivedBy
    ) {
        InventoryRequest request =
                getRequiredRequest(requestId);

        if(request.isFinanceFulfilment()) throw new IllegalArgumentException("Use linked purchase receipt to record quantities and evidence");
        Set<RequestStatus> receiptAllowedStatuses =
                EnumSet.of(
                        RequestStatus.PROCURED,
                        RequestStatus.APPROVED_BY_STORE,
                        RequestStatus.STORE_REVIEW
                );

        if (!receiptAllowedStatuses.contains(
                request.getStatus()
        )) {
            throw new RuntimeException(
                    "Request is not ready to be received in store. " +
                            "Current status: " +
                            request.getStatus()
            );
        }

        for (RequestItem item : request.getItems()) {
            receiveItemIntoStore(item);
        }

        request.setStatus(
                RequestStatus.RECEIVED_IN_STORE
        );

        request.setReceivedQuantity(
                receivedQuantity != null
                        ? receivedQuantity
                        : calculateApprovedOrRequestedQuantity(request)
        );

        request.setReceivedDate(
                LocalDateTime.now()
        );

        request.setStoreApprovedBy(receivedBy);

        InventoryRequest savedRequest =
                requestRepository.save(request);

        sendStatusNotificationSafely(
                savedRequest,
                "RECEIVED_IN_STORE",
                receivedBy,
                null
        );

        return savedRequest;
    }

    private void receiveItemIntoStore(
            RequestItem item
    ) {
        if (item.getProductId() == null) {
            /*
             * A manual item has no linked Product record.
             * It cannot be added to stock until a Product
             * record has been created and linked.
             */
            item.setStatus("RECEIVED_MANUAL");
            return;
        }

        Product product =
                productRepository
                        .findForUpdate(item.getProductId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found: " +
                                                item.getProductId()
                                )
                        );

        int quantityToAdd =
                getApprovedOrRequestedQuantity(item);
        int currentStock = product.getStock();

        product.setStock(
                currentStock + quantityToAdd
        );

        productRepository.save(product);

        item.setStatus("RECEIVED");
    }

    // ============================================================
    // ISSUE ITEMS
    // ============================================================

    @Transactional
    public InventoryRequest issueItems(
            Long requestId,
            Integer issuedQuantity,
            String issuedBy
    ) {
        InventoryRequest request =
                getRequiredRequest(requestId);

        if (RequestStatus.ISSUED.equals(
                request.getStatus()
        )) {
            throw new RuntimeException(
                    "Request has already been issued"
            );
        }

        validateRequestCanBeIssued(request);

        issueItemsLogic(request, issuedBy);

        request.setStatus(RequestStatus.ISSUED);

        request.setIssuedQuantity(
                issuedQuantity != null
                        ? issuedQuantity
                        : calculateApprovedOrRequestedQuantity(request)
        );

        request.setIssuedDate(LocalDateTime.now());
        request.setIssuedBy(issuedBy);

        InventoryRequest savedRequest =
                requestRepository.save(request);

        sendStatusNotificationSafely(
                savedRequest,
                "ISSUED",
                issuedBy,
                null
        );

        return savedRequest;
    }

    private void validateRequestCanBeIssued(
            InventoryRequest request
    ) {
        WorkflowDefinition workflow =
                workflowService.findMatchingWorkflow(
                        "INVENTORY",
                        request
                );

        WorkflowStep currentStep = null;

        if (workflow != null &&
                request.getStatus() != null) {
            currentStep =
                    workflowService.getStepByStatus(
                            workflow,
                            request.getStatus().name()
                    );
        }

        boolean finalWorkflowStep =
                currentStep != null &&
                        currentStep.isFinalStep();

        boolean allowedStatus =
                RequestStatus.APPROVED_BY_STORE.equals(
                        request.getStatus()
                ) ||
                        RequestStatus.RECEIVED_IN_STORE.equals(
                                request.getStatus()
                        ) ||
                        RequestStatus.STORE_REVIEW.equals(
                                request.getStatus()
                        ) ||
                        RequestStatus.PROCURED.equals(
                                request.getStatus()
                        ) ||
                        RequestStatus.PENDING_STORE.equals(
                                request.getStatus()
                        );

        if (!finalWorkflowStep && !allowedStatus) {
            throw new RuntimeException(
                    "Request is not in a state that allows issuing items. " +
                            "Current status: " +
                            request.getStatus()
            );
        }
    }

    private void issueItemsLogic(
            InventoryRequest request,
            String issuedBy
    ) {
        if(request.isFinanceFulfilment()) throw new IllegalArgumentException("Use per-item fulfilment to issue this request");
        List<Outgoing> outgoingsToSave =
                new ArrayList<>();

        for (RequestItem item : request.getItems()) {
            if (item.getProductId() == null) {
                item.setStatus("ISSUED_MANUAL");
                continue;
            }

            Product product =
                    productRepository
                            .findForUpdate(item.getProductId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Product not found: " +
                                                    item.getProductId()
                                    )
                            );

            int quantityToIssue =
                    getApprovedOrRequestedQuantity(item);

            if ("ISSUED".equalsIgnoreCase(
                    item.getStatus()
            )) {
                throw new RuntimeException(
                        "Item " +
                                product.getName() +
                                " has already been issued"
                );
            }

            int currentStock = product.getStock() - (purchaseLines == null ? 0 : purchaseLines.reservedStock(product.getId()));

            if (currentStock < quantityToIssue) {
                throw new RuntimeException(
                        "Not enough stock for product " +
                                product.getName() +
                                ". Available: " +
                                currentStock +
                                ", required: " +
                                quantityToIssue
                );
            }

            product.setStock(
                    product.getStock() - quantityToIssue
            );

            productRepository.save(product);

            item.setIssuedQuantity(quantityToIssue);
            item.setStatus("ISSUED");

            Outgoing outgoing =
                    createOutgoingRecord(
                            request,
                            item,
                            product,
                            quantityToIssue,
                            issuedBy
                    );

            outgoingsToSave.add(outgoing);
        }

        if (!outgoingsToSave.isEmpty()) {
            outgoingRepository.saveAll(
                    outgoingsToSave
            );
        }
    }

    public Outgoing createOutgoingRecord(
            InventoryRequest request,
            RequestItem item,
            Product product,
            int quantityToIssue,
            String issuedBy
    ) {
        Outgoing outgoing = new Outgoing();

        outgoing.setProductId(product.getId());
        outgoing.setName(product.getName());
        outgoing.setCode(product.getCode());
        outgoing.setQuantityMoved(quantityToIssue);
        outgoing.setRequestedBy(
                request.getRequestedBy()
        );
        outgoing.setDepartment(
                request.getDepartment()
        );
        outgoing.setProjectName(
                request.getProjectName()
        );

        outgoing.setJob(
                request.getJobDescription() != null
                        ? request.getJobDescription()
                        : request.getJobName()
        );

        outgoing.setNotes(
                "Issued via request: " +
                        request.getRequestNumber()
        );

        outgoing.setMovementDate(
                LocalDateTime.now()
        );

        outgoing.setUserName(issuedBy);
        outgoing.setRecordType(
                OutgoingRecordType.ISSUED
        );
        outgoing.setStatus("ISSUED");
        outgoing.setRequestId(request.getId());
        outgoing.setRequestNumber(
                request.getRequestNumber()
        );
        outgoing.setEmployeeName(
                request.getRequestedBy()
        );
        outgoing.setEmployeeId(
                request.getRequestedBy()
        );

        return outgoing;
    }

    // ============================================================
    // EMAIL NOTIFICATION ROUTING
    // ============================================================

    /**
     * New request routing:
     *
     * PPE:
     *     PPE request recipients.
     *
     * Normal inventory with manual products:
     *     Inventory/manual procurement recipients.
     *
     * Normal stock inventory:
     *     Normal inventory recipients.
     */
    private void sendCreatedNotificationSafely(
            InventoryRequest request,
            String requesterEmail
    ) {
        try {
            if (isPpeRequest(request)) {
                emailService.sendPpeRequestCreated(
                        request,
                        requesterEmail
                );
            } else {
                emailService.sendInventoryRequestCreated(
                        request,
                        requesterEmail
                );
            }
        } catch (Exception exception) {
            /*
             * Email failure must not roll back a successfully
             * created inventory request.
             */
            System.err.println(
                    "Inventory request created, but email failed. " +
                            "Request: " +
                            request.getRequestNumber() +
                            ", error: " +
                            exception.getMessage()
            );
        }
    }

    /**
     * Status-change routing:
     *
     * PPE requests use PPE notification recipients.
     * Normal requests use normal inventory recipients.
     */
    private void sendStatusNotificationSafely(
            InventoryRequest request,
            String action,
            String performedBy,
            String notes
    ) {
        try {
            if (isPpeRequest(request)) {
                emailService.sendPpeStatusChanged(
                        request,
                        action,
                        performedBy,
                        notes
                );
            } else {
                emailService.sendInventoryStatusChanged(
                        request,
                        action,
                        performedBy,
                        notes
                );
            }
        } catch (Exception exception) {
            /*
             * Do not fail the inventory transaction because
             * SMTP or email configuration failed.
             */
            System.err.println(
                    "Inventory status updated, but email failed. " +
                            "Request: " +
                            request.getRequestNumber() +
                            ", action: " +
                            action +
                            ", error: " +
                            exception.getMessage()
            );
        }
    }

    private boolean isPpeRequest(
            InventoryRequest request
    ) {
        return Boolean.TRUE.equals(
                request.getPpeRequest()
        );
    }

    /**
     * Manual products require procurement, but they are not PPE
     * unless ppeRequest is explicitly true.
     */
    private boolean requiresProcurement(
            InventoryRequest request
    ) {
        return Boolean.TRUE.equals(
                request.getPpeRequest()
        ) ||
                Boolean.TRUE.equals(
                        request.getHasManualProducts()
                );
    }

    // ============================================================
    // GENERAL HELPERS
    // ============================================================

    private InventoryRequest getRequiredRequest(
            Long requestId
    ) {
        return requestRepository
                .findById(requestId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Inventory request not found with id: " +
                                        requestId
                        )
                );
    }

    private void validateRoleForStep(
            WorkflowStep currentStep,
            String role,
            InventoryRequest request
    ) {
        if (currentStep == null) {
            throw new RuntimeException(
                    "No workflow step was found for status: " +
                            request.getStatus()
            );
        }

        if (!roleContains(
                currentStep.getRequiredRole(),
                role
        )) {
            throw new RuntimeException(
                    "Role " +
                            role +
                            " is not authorized for status " +
                            request.getStatus() +
                            ". Required role(s): " +
                            currentStep.getRequiredRole()
            );
        }
    }

    private boolean roleContains(
            String configuredRoles,
            String requestedRole
    ) {
        if (configuredRoles == null ||
                requestedRole == null) {
            return false;
        }

        String normalizedRequestedRole =
                normalizeRole(requestedRole);

        return Arrays.stream(
                        configuredRoles.split(",")
                )
                .map(this::normalizeRole)
                .anyMatch(normalizedRequestedRole::equals);
    }

    private void requireFulfilmentApprovalRole(String role) {
        String authority = switch(role) {case "PROCUREMENT" -> "PROCUREMENT_OFFICER";case "STORE" -> "INVENTORY";default -> role;};
        var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || auth.getAuthorities().stream().noneMatch(a -> Arrays.asList("ROLE_ADMIN","ROLE_"+authority,"ROLE_"+role).contains(a.getAuthority()) || ("STORE".equals(role) && Arrays.asList("ROLE_STORE_OFFICER","ROLE_STOREKEEPER").contains(a.getAuthority())))) throw new org.springframework.security.access.AccessDeniedException("Approval permission is required");
    }

    private String normalizeRole(String role) {
        return role == null
                ? ""
                : role.trim()
                .toUpperCase(Locale.ROOT)
                .replace("ROLE_", "");
    }

    /**
     * Target statuses identify the step that produced them. If that step has
     * completed, the role assigned to the following step must be able to act
     * on the request. Role validation remains restricted to configured steps.
     */
    private WorkflowStep resolveActionStep(
            WorkflowDefinition workflow,
            String currentStatus,
            String requestedRole
    ) {
        WorkflowStep statusStep = workflowService.getStepByStatus(
                workflow,
                currentStatus
        );

        if (statusStep != null &&
                roleContains(statusStep.getRequiredRole(), requestedRole)) {
            return statusStep;
        }

        WorkflowStep nextStep = workflowService.getNextStep(
                workflow,
                currentStatus
        );

        if (nextStep != null &&
                roleContains(nextStep.getRequiredRole(), requestedRole)) {
            return nextStep;
        }

        return statusStep;
    }

    private RequestStatus parseRequestStatus(
            String value
    ) {
        if (value == null ||
                value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Request status cannot be empty"
            );
        }

        try {
            return RequestStatus.valueOf(
                    value.trim().replaceAll("[\\s-]+", "_").toUpperCase(
                            Locale.ROOT
                    )
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid inventory request status: " +
                            value,
                    exception
            );
        }
    }

    private void updateAllItemStatuses(
            InventoryRequest request,
            String status
    ) {
        if (request.getItems() == null) {
            return;
        }

        request.getItems().forEach(
                item -> item.setStatus(status)
        );
    }

    private int getApprovedOrRequestedQuantity(
            RequestItem item
    ) {
        if (item.getApprovedQuantity() != null &&
                item.getApprovedQuantity() > 0) {
            return item.getApprovedQuantity();
        }

        return item.getRequestedQuantity() != null
                ? item.getRequestedQuantity()
                : 0;
    }

    private int calculateApprovedOrRequestedQuantity(
            InventoryRequest request
    ) {
        if (request.getItems() == null) {
            return 0;
        }

        return request.getItems()
                .stream()
                .mapToInt(
                        this::getApprovedOrRequestedQuantity
                )
                .sum();
    }

    // ============================================================
    // QUERY METHODS
    // ============================================================

    public List<InventoryRequest> getRequestsForPlanner() {
        return requestRepository.findByStatus(
                RequestStatus.PENDING_PLANNER
        );
    }

    public List<InventoryRequest> getRequestsForProcurement() {
        return requestRepository.findAll()
                .stream()
                .filter(this::shouldDisplayForProcurement)
                .collect(Collectors.toList());
    }

    private boolean shouldDisplayForProcurement(
            InventoryRequest request
    ) {
        RequestStatus status = request.getStatus();

        if (status == null) {
            return false;
        }

        if(request.isFinanceFulfilment() && requiresProcurement(request) && Set.of(RequestStatus.APPROVED_BY_PROCUREMENT,RequestStatus.APPROVED_BY_STORE,RequestStatus.PARTIALLY_ISSUED,RequestStatus.ISSUED,RequestStatus.RECEIVED_IN_STORE).contains(status)) return true;

        // Keep successfully handed-off requests visible in Procurement's
        // Approved tab after they move into the Store review queue.
        if (status == RequestStatus.STORE_REVIEW) {
            return true;
        }

        boolean procurementRequest =
                requiresProcurement(request);

        if (procurementRequest) {
            return status ==
                    RequestStatus.PROCUREMENT_PENDING
                    ||
                    status ==
                            RequestStatus.APPROVED_BY_PLANNER
                    ||
                    status ==
                            RequestStatus.PROCURED
                    ||
                    status ==
                            RequestStatus.REJECTED_BY_PROCUREMENT
                    ||
                    status ==
                            RequestStatus.STORE_REVIEW;
        }

        return status ==
                RequestStatus.APPROVED_BY_PLANNER
                ||
                status ==
                        RequestStatus.PROCUREMENT_PENDING
                ||
                status ==
                        RequestStatus.PROCURED
                ||
                status ==
                        RequestStatus.REJECTED_BY_PROCUREMENT;
    }

    public List<InventoryRequest> getRequestsForStore() {
        Set<RequestStatus> storeStatuses =
                EnumSet.of(
                        RequestStatus.PARTIALLY_ISSUED,
                        RequestStatus.PENDING_STORE,
                        RequestStatus.STORE_REVIEW,
                        RequestStatus.APPROVED_BY_STORE,
                        RequestStatus.RECEIVED_IN_STORE
                );

        return requestRepository.findAll()
                .stream()
                .filter(request ->
                        request.getStatus() != null &&
                                storeStatuses.contains(
                                        request.getStatus()
                                )
                )
                .collect(Collectors.toList());
    }

    public List<InventoryRequest> getApprovedByProcurement() {
        return requestRepository.findByStatus(
                RequestStatus.PROCURED
        );
    }

    public List<InventoryRequest> getIssuedRequests() {
        return requestRepository.findByStatus(
                RequestStatus.ISSUED
        );
    }

    public List<InventoryRequest> getAllRequests() {
        return requestRepository.findAll();
    }

    public Optional<InventoryRequest> getRequestById(
            Long id
    ) {
        return requestRepository.findById(id);
    }

    public List<InventoryRequest> getRequestsByUser(
            String username
    ) {
        return requestRepository.findByRequestedBy(
                username
        );
    }
}
