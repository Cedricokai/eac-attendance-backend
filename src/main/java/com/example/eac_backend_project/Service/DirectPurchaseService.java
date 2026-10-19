package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.Service.settings.WorkflowService;
import com.example.eac_backend_project.dto.DirectPurchaseApproverDTO;
import com.example.eac_backend_project.dto.DirectPurchaseItemDTO;
import com.example.eac_backend_project.dto.DirectPurchaseRequestDTO;
import com.example.eac_backend_project.dto.DirectPurchaseResponseDTO;
import com.example.eac_backend_project.entity.directpurchase.DirectPurchase;
import com.example.eac_backend_project.entity.directpurchase.DirectPurchaseItem;
import com.example.eac_backend_project.entity.directpurchase.DirectPurchaseStatus;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.entity.settings.WorkflowDefinition;
import com.example.eac_backend_project.entity.settings.WorkflowStep;
import com.example.eac_backend_project.repository.DirectPurchaseRepository;
import com.example.eac_backend_project.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DirectPurchaseService {

    private static final String ENTITY_TYPE = "DIRECT_PURCHASE";

    private final DirectPurchaseRepository repository;
    private final WorkflowService workflowService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final SettingsService settingsService;

    public DirectPurchaseService(
            DirectPurchaseRepository repository,
            WorkflowService workflowService,
            EmailService emailService,
            UserRepository userRepository,
            SettingsService settingsService
    ) {
        this.repository = repository;
        this.workflowService = workflowService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.settingsService = settingsService;
    }

    private String generateRequestNumber() {
        String datePart = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        long sequence = repository.count() + 1;

        return "DP-" + datePart + "-" + String.format("%04d", sequence);
    }

    private Authentication requireAuthentication() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("You are not authenticated.");
        }

        return authentication;
    }

    private User getCurrentUser() {
        Authentication authentication = requireAuthentication();
        String login = authentication.getName();

        return userRepository.findByEmail(login)
                .or(() -> userRepository.findByUserName(login))
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user account was not found."
                ));
    }

    private Set<String> getCurrentRoles() {
        Authentication authentication = requireAuthentication();

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeRole)
                .collect(Collectors.toSet());
    }

    private String normalizeRole(String role) {
        if (role == null) return "";

        String normalized = role.trim().toUpperCase(Locale.ROOT);

        return normalized.startsWith("ROLE_")
                ? normalized.substring(5)
                : normalized;
    }

    private void requireRole(String requiredRoles) {
        if (requiredRoles == null || requiredRoles.isBlank()) {
            return;
        }

        Set<String> currentRoles = getCurrentRoles();

        boolean permitted = Arrays.stream(requiredRoles.split(","))
                .map(String::trim)
                .map(this::normalizeRole)
                .anyMatch(currentRoles::contains);

        if (!permitted) {
            throw new AccessDeniedException(
                    "You do not have permission for this action. Required role(s): " +
                            requiredRoles
            );
        }
    }

    private User requireEnabledUser(Long userId, String expectedRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + userId
                ));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("Selected user is disabled.");
        }

        String actualRole = user.getRole() == null
                ? ""
                : normalizeRole(user.getRole().getName());

        if (!normalizeRole(expectedRole).equals(actualRole)) {
            throw new IllegalArgumentException(
                    "Selected user does not have the " +
                            normalizeRole(expectedRole) + " role."
            );
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Selected user does not have an email address."
            );
        }

        return user;
    }

    private List<User> findEnabledUsersByRole(String role) {
        String normalized = normalizeRole(role);

        LinkedHashMap<Long, User> users = new LinkedHashMap<>();

        for (String roleName : List.of(normalized, "ROLE_" + normalized)) {
            userRepository.findByRoleNameAndEnabled(roleName)
                    .forEach(user -> users.put(user.getId(), user));
        }

        return new ArrayList<>(users.values());
    }

    public List<DirectPurchaseApproverDTO> getApprovers(String role) {
        return findEnabledUsersByRole(role).stream()
                .map(user -> new DirectPurchaseApproverDTO(
                        user.getId(),
                        user.getName(),
                        user.getUserName(),
                        user.getEmail(),
                        user.getRole() == null ? null : user.getRole().getName()
                ))
                .sorted(Comparator.comparing(
                        DirectPurchaseApproverDTO::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .collect(Collectors.toList());
    }

    private DirectPurchaseResponseDTO toResponseDTO(DirectPurchase purchase) {
        InventoryFinanceAccess.require(purchase.getInventoryRequestId());
        DirectPurchaseResponseDTO dto = new DirectPurchaseResponseDTO();

        dto.setInventoryRequestId(purchase.getInventoryRequestId());
        dto.setInventoryRequestNumber(purchase.getInventoryRequestNumber());
        dto.setCreatedByUserId(purchase.getCreatedByUserId());
        dto.setCreatedByName(purchase.getCreatedByName());
        dto.setId(purchase.getId());
        dto.setRequestNumber(purchase.getRequestNumber());
        dto.setRequestDate(purchase.getRequestDate());

        dto.setRequester(purchase.getRequester());
        dto.setRequesterUserId(purchase.getRequesterUserId());
        dto.setRequesterEmail(purchase.getRequesterEmail());

        dto.setAccountant(purchase.getAccountant());
        dto.setAccountantUserId(purchase.getAccountantUserId());
        dto.setAccountantEmail(purchase.getAccountantEmail());

        dto.setNotes(purchase.getNotes());
        dto.setStatus(purchase.getStatus());
        dto.setTotalAmount(purchase.getTotalAmount());

        dto.setItems(purchase.getItems().stream()
                .map(item -> {
                    DirectPurchaseItemDTO itemDTO =
                            new DirectPurchaseItemDTO();

                    itemDTO.setId(item.getId());
                    itemDTO.setDescription(item.getDescription());
                    itemDTO.setQuantity(item.getQuantity());
                    itemDTO.setUnitPrice(item.getUnitPrice());
                    itemDTO.setTotal(item.getTotal());

                    return itemDTO;
                })
                .collect(Collectors.toList()));

        try {
            WorkflowDefinition workflow =
                    workflowService.findMatchingWorkflow(
                            ENTITY_TYPE,
                            purchase
                    );

            WorkflowStep pendingStep =
                    getPendingActionStep(workflow, purchase.getStatus());

            if (pendingStep != null) {
                dto.setCurrentStepName(pendingStep.getStepName());
                dto.setCurrentRequiredRole(pendingStep.getRequiredRole());
            }
        } catch (Exception ignored) {
            dto.setCurrentStepName(null);
            dto.setCurrentRequiredRole(null);
        }

        return dto;
    }

    private WorkflowStep getPendingActionStep(
            WorkflowDefinition workflow,
            DirectPurchaseStatus status
    ) {
        if (workflow == null || workflow.getSteps() == null) {
            return null;
        }

        List<WorkflowStep> orderedSteps = workflow.getSteps().stream()
                .sorted(Comparator.comparingInt(WorkflowStep::getStepOrder))
                .collect(Collectors.toList());

        if (status == DirectPurchaseStatus.PENDING) {
            return orderedSteps.isEmpty() ? null : orderedSteps.get(0);
        }

        WorkflowStep completedStep =
                workflowService.getStepByStatus(
                        workflow,
                        status.name()
                );

        if (completedStep == null || completedStep.isFinalStep()) {
            return null;
        }

        return orderedSteps.stream()
                .filter(step ->
                        step.getStepOrder() >
                                completedStep.getStepOrder()
                )
                .findFirst()
                .orElse(null);
    }

    private DirectPurchaseStatus nextStatus(
            WorkflowDefinition workflow,
            DirectPurchaseStatus currentStatus
    ) {
        WorkflowStep pendingStep =
                getPendingActionStep(workflow, currentStatus);

        if (pendingStep == null ||
                pendingStep.getTargetStatus() == null) {
            throw new IllegalStateException(
                    "No next workflow step is defined for status: " +
                            currentStatus
            );
        }

        try {
            return DirectPurchaseStatus.valueOf(
                    pendingStep.getTargetStatus()
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Workflow target status is not supported: " +
                            pendingStep.getTargetStatus()
            );
        }
    }

    private String appendNote(
            String existing,
            String heading,
            String note,
            User actor
    ) {
        if (note == null || note.isBlank()) {
            return existing;
        }

        String actorName = actor == null
                ? "Unknown user"
                : actor.getName();

        String entry = heading + " by " + actorName + ": " +
                note.trim();

        return existing == null || existing.isBlank()
                ? entry
                : existing + "\n" + entry;
    }

    @Transactional
    public DirectPurchaseResponseDTO createRequest(
            DirectPurchaseRequestDTO dto
    ) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one item is required."
            );
        }

        return createForInventory(dto, getCurrentUser(), null, null);
    }

    @Transactional
    public DirectPurchaseResponseDTO createForInventory(DirectPurchaseRequestDTO dto, User requester, Long inventoryId, String inventoryNumber) {
        User actor = getCurrentUser();
        User accountant = requireEnabledUser(
                dto.getAccountantUserId(),
                "ACCOUNTANT"
        );

        DirectPurchase purchase = new DirectPurchase();

        purchase.setInventoryRequestId(inventoryId);
        purchase.setInventoryRequestNumber(inventoryNumber);
        purchase.setCreatedByUserId(actor.getId());
        purchase.setCreatedByName(actor.getName());
        purchase.setRequestNumber(generateRequestNumber());
        purchase.setRequestDate(
                dto.getRequestDate() == null
                        ? LocalDate.now()
                        : dto.getRequestDate()
        );

        purchase.setRequester(requester.getName());
        purchase.setRequesterUserId(requester.getId());
        purchase.setRequesterEmail(requester.getEmail());

        purchase.setAccountant(accountant.getName());
        purchase.setAccountantUserId(accountant.getId());
        purchase.setAccountantEmail(accountant.getEmail());

        purchase.setNotes(dto.getNotes());
        purchase.setStatus(DirectPurchaseStatus.PENDING);

        for (DirectPurchaseItemDTO itemDTO : dto.getItems()) {
            if (itemDTO.getDescription() == null ||
                    itemDTO.getDescription().isBlank()) {
                throw new IllegalArgumentException(
                        "Every item must have a description."
                );
            }

            int quantity = itemDTO.getQuantity() == null
                    ? 1
                    : itemDTO.getQuantity();

            double unitPrice = itemDTO.getUnitPrice() == null
                    ? 0.0
                    : itemDTO.getUnitPrice();

            if (quantity <= 0 || unitPrice < 0) {
                throw new IllegalArgumentException(
                        "Item quantity must be above zero and unit price cannot be negative."
                );
            }

            DirectPurchaseItem item =
                    new DirectPurchaseItem();

            item.setDescription(itemDTO.getDescription().trim());
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.calculateTotal();

            purchase.addItem(item);
        }

        purchase.recalculateTotal();

        DirectPurchase saved = repository.save(purchase);
        DirectPurchaseResponseDTO response = toResponseDTO(saved);

        notifyAccountantsOfNewRequest(response, accountant);

        return response;
    }

    public List<DirectPurchaseResponseDTO> getAllRequests() {
        return repository.findAll().stream()
                .filter(p -> InventoryFinanceAccess.visible(p.getInventoryRequestId()))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public DirectPurchaseResponseDTO getRequestById(Long id) {
        DirectPurchase purchase = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Direct purchase request not found with id: " +
                                id
                ));

        return toResponseDTO(purchase);
    }

    @Transactional
    public DirectPurchaseResponseDTO approveRequest(
            Long id,
            String notes
    ) {
        DirectPurchase purchase = requireStatus(
                id,
                DirectPurchaseStatus.PENDING
        );

        WorkflowDefinition workflow =
                workflowService.findMatchingWorkflow(
                        ENTITY_TYPE,
                        purchase
                );

        WorkflowStep actionStep =
                getPendingActionStep(
                        workflow,
                        purchase.getStatus()
                );

        if (actionStep == null) {
            throw new IllegalStateException(
                    "Accountant approval step was not found."
            );
        }

        requireRole(actionStep.getRequiredRole());

        User actor = getCurrentUser();

        purchase.setStatus(
                nextStatus(workflow, purchase.getStatus())
        );

        purchase.setNotes(
                appendNote(
                        purchase.getNotes(),
                        "Accountant approval",
                        notes,
                        actor
                )
        );

        DirectPurchaseResponseDTO response =
                toResponseDTO(repository.save(purchase));

        notifyRoleOfStep(response, "REVIEWER", notes);

        return response;
    }

    @Transactional
    public DirectPurchaseResponseDTO reviewerApprove(
            Long id,
            String notes
    ) {
        DirectPurchase purchase = requireStatus(
                id,
                DirectPurchaseStatus.ACCOUNTANT_APPROVED
        );

        WorkflowDefinition workflow =
                workflowService.findMatchingWorkflow(
                        ENTITY_TYPE,
                        purchase
                );

        WorkflowStep actionStep =
                getPendingActionStep(
                        workflow,
                        purchase.getStatus()
                );

        if (actionStep == null) {
            throw new IllegalStateException(
                    "Reviewer approval step was not found."
            );
        }

        requireRole(actionStep.getRequiredRole());

        User actor = getCurrentUser();

        purchase.setStatus(
                nextStatus(workflow, purchase.getStatus())
        );

        purchase.setNotes(
                appendNote(
                        purchase.getNotes(),
                        "Reviewer approval",
                        notes,
                        actor
                )
        );

        DirectPurchaseResponseDTO response =
                toResponseDTO(repository.save(purchase));

        notifyRoleOfStep(response, "FINANCE", notes);

        return response;
    }

    @Transactional
    public DirectPurchaseResponseDTO payRequest(
            Long id,
            String notes
    ) {
        DirectPurchase purchase = requireStatus(
                id,
                DirectPurchaseStatus.REVIEWER_APPROVED
        );

        WorkflowDefinition workflow =
                workflowService.findMatchingWorkflow(
                        ENTITY_TYPE,
                        purchase
                );

        WorkflowStep actionStep =
                getPendingActionStep(
                        workflow,
                        purchase.getStatus()
                );

        if (actionStep == null) {
            throw new IllegalStateException(
                    "Finance payment step was not found."
            );
        }

        requireRole(actionStep.getRequiredRole());

        User actor = getCurrentUser();

        DirectPurchaseStatus target =
                nextStatus(workflow, purchase.getStatus());

        if (target != DirectPurchaseStatus.PAID) {
            throw new IllegalStateException(
                    "Finance workflow must end with PAID."
            );
        }

        purchase.setStatus(target);

        purchase.setNotes(
                appendNote(
                        purchase.getNotes(),
                        "Payment",
                        notes,
                        actor
                )
        );

        DirectPurchaseResponseDTO response =
                toResponseDTO(repository.save(purchase));

        notifyPaid(response, notes);

        return response;
    }

    @Transactional
    public DirectPurchaseResponseDTO rejectRequest(
            Long id,
            String notes
    ) {
        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException(
                    "A rejection reason is required."
            );
        }

        DirectPurchase purchase = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Request not found with id: " + id
                ));

        if (purchase.getStatus() == DirectPurchaseStatus.PAID) {
            throw new IllegalStateException(
                    "A paid request cannot be rejected."
            );
        }

        if (purchase.getStatus() == DirectPurchaseStatus.REJECTED) {
            throw new IllegalStateException(
                    "This request is already rejected."
            );
        }

        WorkflowDefinition workflow =
                workflowService.findMatchingWorkflow(
                        ENTITY_TYPE,
                        purchase
                );

        WorkflowStep actionStep =
                getPendingActionStep(
                        workflow,
                        purchase.getStatus()
                );

        if (actionStep == null) {
            throw new IllegalStateException(
                    "No rejection step is available for the current status."
            );
        }

        requireRole(actionStep.getRequiredRole());

        User actor = getCurrentUser();

        purchase.setStatus(DirectPurchaseStatus.REJECTED);
        purchase.setNotes(
                appendNote(
                        purchase.getNotes(),
                        "Rejection",
                        notes,
                        actor
                )
        );

        DirectPurchaseResponseDTO response =
                toResponseDTO(repository.save(purchase));

        notifyRequesterOfRejection(response, notes);

        return response;
    }

    private DirectPurchase requireStatus(
            Long id,
            DirectPurchaseStatus expected
    ) {
        DirectPurchase purchase = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Request not found with id: " + id
                ));

        if (purchase.getStatus() != expected) {
            throw new IllegalStateException(
                    "Expected status " + expected +
                            " but current status is " +
                            purchase.getStatus()
            );
        }

        return purchase;
    }

    @Transactional
    public void deleteRequest(Long id) {
        DirectPurchase purchase = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Direct purchase request not found with id: " +
                                id
                ));

        if (purchase.getStatus() != DirectPurchaseStatus.PENDING &&
                purchase.getStatus() != DirectPurchaseStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only pending or rejected requests can be deleted."
            );
        }

        if(purchase.getInventoryRequestId()!=null) throw new IllegalStateException("Reject linked purchases to retain the inventory audit trail");
        repository.delete(purchase);
    }

    private void notifyAccountantsOfNewRequest(
            DirectPurchaseResponseDTO response,
            User selectedAccountant
    ) {
        SystemSettings settings =
                settingsService.getSystemSettings();

        if (!Boolean.TRUE.equals(
                defaultTrue(
                        settings.getAccountantNotificationEnabled()
                )
        )) {
            return;
        }

        LinkedHashSet<String> recipients =
                new LinkedHashSet<>();

        addEmail(recipients, selectedAccountant.getEmail());

        addConfiguredEmails(
                recipients,
                settings.getAccountantRecipients()
        );

        for (String email : recipients) {
            emailService.sendDirectPurchaseRequestCreated(
                    response,
                    email,
                    "ACCOUNTANT"
            );
        }
    }

    private void notifyRoleOfStep(
            DirectPurchaseResponseDTO response,
            String role,
            String notes
    ) {
        SystemSettings settings =
                settingsService.getSystemSettings();

        boolean enabled = switch (normalizeRole(role)) {
            case "REVIEWER" -> Boolean.TRUE.equals(
                    defaultTrue(
                            settings.getReviewerNotificationEnabled()
                    )
            );
            case "FINANCE" -> Boolean.TRUE.equals(
                    defaultTrue(
                            settings.getFinanceNotificationEnabled()
                    )
            );
            default -> true;
        };

        if (!enabled) return;

        LinkedHashSet<String> recipients =
                new LinkedHashSet<>();

        findEnabledUsersByRole(role).stream()
                .map(User::getEmail)
                .forEach(email -> addEmail(recipients, email));

        if ("REVIEWER".equals(normalizeRole(role))) {
            addConfiguredEmails(
                    recipients,
                    settings.getReviewerRecipients()
            );
        }

        if ("FINANCE".equals(normalizeRole(role))) {
            addConfiguredEmails(
                    recipients,
                    settings.getFinanceRecipients()
            );
        }

        for (String email : recipients) {
            emailService.sendDirectPurchaseStepApproved(
                    response,
                    email,
                    normalizeRole(role),
                    notes
            );
        }
    }

    private void notifyRequesterOfRejection(
            DirectPurchaseResponseDTO response,
            String reason
    ) {
        SystemSettings settings =
                settingsService.getSystemSettings();

        if (!Boolean.TRUE.equals(
                defaultTrue(
                        settings.getRequesterNotificationEnabled()
                )
        )) {
            return;
        }

        if (response.getRequesterEmail() != null &&
                !response.getRequesterEmail().isBlank()) {
            emailService.sendDirectPurchaseRejected(
                    response,
                    response.getRequesterEmail(),
                    reason
            );
        }
    }

    private void notifyPaid(
            DirectPurchaseResponseDTO response,
            String notes
    ) {
        SystemSettings settings =
                settingsService.getSystemSettings();

        String requesterEmail =
                Boolean.TRUE.equals(
                        defaultTrue(
                                settings.getRequesterNotificationEnabled()
                        )
                )
                        ? response.getRequesterEmail()
                        : null;

        String accountantEmail =
                Boolean.TRUE.equals(
                        defaultTrue(
                                settings.getAccountantNotificationEnabled()
                        )
                )
                        ? response.getAccountantEmail()
                        : null;

        if ((requesterEmail == null ||
                requesterEmail.isBlank()) &&
                (accountantEmail == null ||
                        accountantEmail.isBlank())) {
            return;
        }

        /*
         * Existing EmailService method requires a requester address.
         * When requester notification is disabled but accountant notification
         * is enabled, use accountant as the primary recipient and omit CC.
         */
        if (requesterEmail == null ||
                requesterEmail.isBlank()) {
            requesterEmail = accountantEmail;
            accountantEmail = null;
        }

        emailService.sendDirectPurchasePaid(
                response,
                requesterEmail,
                accountantEmail,
                notes
        );
    }

    private Boolean defaultTrue(Boolean value) {
        return value == null ? Boolean.TRUE : value;
    }

    private void addConfiguredEmails(
            Set<String> recipients,
            String raw
    ) {
        if (raw == null || raw.isBlank()) return;

        Arrays.stream(raw.split("[,;\\n]"))
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .forEach(email -> addEmail(recipients, email));
    }

    private void addEmail(
            Set<String> recipients,
            String email
    ) {
        if (email == null || email.isBlank()) return;
        recipients.add(email.trim().toLowerCase(Locale.ROOT));
    }
}
