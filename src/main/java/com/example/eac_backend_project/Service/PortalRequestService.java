package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.AccountAccessRequestDTO;
import com.example.eac_backend_project.dto.SupportRequestDTO;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.utils.EmailRecipientHelper;
import com.example.eac_backend_project.entity.portal.AccountAccessRequest;
import com.example.eac_backend_project.entity.portal.SupportRequest;
import com.example.eac_backend_project.repository.AccountAccessRequestRepository;
import com.example.eac_backend_project.repository.SupportRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PortalRequestService {
    private static final Set<String> ACCOUNT_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "CANCELLED");
    private static final Set<String> SUPPORT_STATUSES = Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");
    private final AccountAccessRequestRepository accountRepository;
    private final SupportRequestRepository supportRepository;
    private final SettingsService settingsService;
    private final EmailRecipientHelper emailRecipientHelper;
    private final EmailService emailService;

    public PortalRequestService(AccountAccessRequestRepository accountRepository, SupportRequestRepository supportRepository,
                                SettingsService settingsService, EmailRecipientHelper emailRecipientHelper,
                                EmailService emailService) {
        this.accountRepository = accountRepository;
        this.supportRepository = supportRepository;
        this.settingsService = settingsService;
        this.emailRecipientHelper = emailRecipientHelper;
        this.emailService = emailService;
    }

    @Transactional
    public AccountAccessRequest submitAccountRequest(AccountAccessRequestDTO dto) {
        String employeeId = clean(dto.employeeId());
        String email = clean(dto.workEmail()).toLowerCase(Locale.ROOT);
        if (accountRepository.existsByEmployeeIdIgnoreCaseAndStatus(employeeId, "PENDING") || accountRepository.existsByWorkEmailIgnoreCaseAndStatus(email, "PENDING")) {
            throw new IllegalArgumentException("A pending account request already exists for this employee or email");
        }
        AccountAccessRequest request = new AccountAccessRequest();
        request.setReference(reference("ACC"));
        request.setFullName(clean(dto.fullName()));
        request.setEmployeeId(employeeId);
        request.setDepartment(clean(dto.department()));
        request.setWorkEmail(email);
        request.setMobile(clean(dto.mobile()));
        request.setAccessReason(clean(dto.accessReason()));
        AccountAccessRequest saved = accountRepository.save(request);
        SystemSettings settings = settingsService.getSystemSettings();
        notifyConfiguredRecipients(settings.getAccountRequestRecipients(),
                "New EAC account request — " + saved.getReference(),
                "A new account-access request is awaiting review.<br><br><strong>Reference:</strong> " + saved.getReference() + "<br><br>Open Portal Requests in the EAC Central Dashboard to review it.");
        return saved;
    }

    @Transactional
    public SupportRequest submitSupportRequest(SupportRequestDTO dto) {
        SupportRequest request = new SupportRequest();
        request.setReference(reference("IT"));
        request.setName(clean(dto.name()));
        request.setEmail(clean(dto.email()).toLowerCase(Locale.ROOT));
        request.setCategory(dto.category());
        request.setDescription(clean(dto.description()));
        request.setPage(dto.page() == null ? null : clean(dto.page()));
        SupportRequest saved = supportRepository.save(request);
        SystemSettings settings = settingsService.getSystemSettings();
        notifyConfiguredRecipients(settings.getSupportRequestRecipients(),
                "New EAC IT support ticket — " + saved.getReference(),
                "A new IT support request is awaiting review.<br><br><strong>Reference:</strong> " + saved.getReference() + "<br><strong>Category:</strong> " + saved.getCategory().replace('_', ' ') + "<br><br>Open Portal Requests in the EAC Central Dashboard to review it.");
        return saved;
    }

    public List<AccountAccessRequest> getAccountRequests() { return accountRepository.findAllByOrderByCreatedAtDesc(); }
    public List<SupportRequest> getSupportRequests() { return supportRepository.findAllByOrderByCreatedAtDesc(); }

    @Transactional
    public AccountAccessRequest reviewAccountRequest(Long id, String status, String notes, String reviewer) {
        AccountAccessRequest request = accountRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Account request not found"));
        request.setStatus(normalizeStatus(status, ACCOUNT_STATUSES));
        request.setReviewNotes(cleanNullable(notes));
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        return accountRepository.save(request);
    }

    @Transactional
    public SupportRequest updateSupportRequest(Long id, String status, String notes, String assignee) {
        SupportRequest request = supportRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Support request not found"));
        request.setStatus(normalizeStatus(status, SUPPORT_STATUSES));
        request.setResolutionNotes(cleanNullable(notes));
        request.setAssignedTo(cleanNullable(assignee));
        request.setUpdatedAt(LocalDateTime.now());
        return supportRepository.save(request);
    }

    private String normalizeStatus(String status, Set<String> allowed) {
        if (status == null) throw new IllegalArgumentException("Status is required");
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw new IllegalArgumentException("Invalid status: " + status);
        return normalized;
    }

    private String reference(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String cleanNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private void notifyConfiguredRecipients(String configuredRecipients, String subject, String html) {
        for (String recipient : emailRecipientHelper.parseEmailRecipients(configuredRecipients)) {
            try { emailService.sendCustomEmail(recipient, subject, html); }
            catch (Exception ignored) { /* Saving a request must not fail when SMTP is unavailable. */ }
        }
    }
}
