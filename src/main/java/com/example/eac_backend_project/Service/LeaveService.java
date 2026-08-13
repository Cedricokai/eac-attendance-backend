package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.email.EmailNotificationService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.dto.EmployeeLeaveBalanceDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.entity.attendance.Overview;
import com.example.eac_backend_project.entity.settings.LeaveSettings;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.LeaveRepository;
import com.example.eac_backend_project.repository.OverviewRepository;
import com.example.eac_backend_project.repository.UserRepository;
import com.example.eac_backend_project.repository.settings.LeaveSettingsRepository;
import com.example.eac_backend_project.utils.EmailRecipientHelper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OverviewRepository overviewRepository;

    @Autowired
    private LeaveSettingsRepository leaveSettingsRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private EmailRecipientHelper emailRecipientHelper;

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public Leave createLeave(Leave leave) {
        if (leave == null) {
            throw new IllegalArgumentException("Leave request is required");
        }

        if (leave.getEmployee() == null || leave.getEmployee().getId() == null) {
            throw new IllegalArgumentException("Employee is required");
        }

        Employee employee = employeeRepository.findById(leave.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + leave.getEmployee().getId()
                ));

        validateLeaveRequest(leave, employee);

        leave.setEmployee(employee);
        leave.setStatus("Pending");
        leave.setSupervisorStatus("Pending");
        leave.setPlannerStatus("Pending");
        leave.setHrStatus("Pending");
        leave.setCreatedAt(LocalDateTime.now());
        leave.setIsDeducted(false);
        leave.setDeductedDays(0);

        Leave savedLeave = leaveRepository.save(leave);

        sendLeaveCreationNotifications(savedLeave);

        return savedLeave;
    }


    /**
     * Sends the employee confirmation and routes the approval email to the
     * supervisor assigned to the employee's job.
     *
     * Fallback order:
     * 1. Job supervisor
     * 2. Employee's directly assigned supervisor
     * 3. Another supervisor assigned to the same job
     * 4. Category supervisor emails
     * 5. Global leave-request recipients
     * 6. Enabled ROLE_SUPERVISOR users
     */
    private void sendLeaveCreationNotifications(Leave leave) {
        if (leave == null || leave.getEmployee() == null) {
            return;
        }

        // Employee submission confirmation.
        try {
            if (normalizeEmail(leave.getEmployee().getEmail()) != null) {
                emailService.sendLeaveRequestNotification(leave);
            }
        } catch (Exception exception) {
            System.err.println(
                    "Leave was saved, but employee confirmation failed. Leave ID: "
                            + leave.getId() + ", error: " + exception.getMessage()
            );
        }

        // Approval notification to the correct supervisor.
        try {
            SupervisorRecipient supervisor =
                    resolveSupervisorRecipient(leave.getEmployee());

            if (supervisor != null &&
                    supervisor.email() != null &&
                    !supervisor.email().isBlank()) {

                emailService.sendLeaveRequestToSupervisor(
                        leave,
                        supervisor.email(),
                        supervisor.name()
                );

                System.out.println(
                        "Leave ID " + leave.getId()
                                + " routed to supervisor "
                                + supervisor.name()
                                + " <" + supervisor.email() + ">"
                );
                return;
            }

            sendLeaveRequestToFallbackRecipients(leave);

        } catch (Exception exception) {
            System.err.println(
                    "Leave was saved, but supervisor routing failed. Leave ID: "
                            + leave.getId() + ", error: " + exception.getMessage()
            );

            try {
                sendLeaveRequestToFallbackRecipients(leave);
            } catch (Exception fallbackException) {
                System.err.println(
                        "Fallback leave notification also failed. Leave ID: "
                                + leave.getId() + ", error: "
                                + fallbackException.getMessage()
                );
            }
        }
    }

    private SupervisorRecipient resolveSupervisorRecipient(Employee employee) {
        if (employee == null) {
            return null;
        }

        // Priority 1: supervisor assigned to the employee's job.
        Job employeeJob = employee.getJob();

        if (employeeJob != null) {
            SupervisorRecipient jobRecipient =
                    buildSupervisorRecipient(employeeJob.getSupervisor());

            if (jobRecipient != null) {
                return jobRecipient;
            }
        }

        // Priority 2: supervisor directly assigned to the employee.
        SupervisorRecipient directRecipient =
                buildSupervisorRecipient(employee.getSupervisor());

        if (directRecipient != null) {
            return directRecipient;
        }

        // Priority 3: any employee marked as supervisor on the same job.
        if (employeeJob != null && employeeJob.getId() != null) {
            List<Employee> jobSupervisors =
                    employeeRepository.findSupervisorsByJobId(employeeJob.getId());

            if (jobSupervisors != null) {
                for (Employee supervisor : jobSupervisors) {
                    SupervisorRecipient recipient =
                            buildSupervisorRecipient(supervisor);

                    if (recipient != null) {
                        return recipient;
                    }
                }
            }
        }

        return null;
    }

    private SupervisorRecipient buildSupervisorRecipient(Employee supervisor) {
        if (supervisor == null || supervisor.getId() == null) {
            return null;
        }

        String supervisorName = buildEmployeeName(supervisor);

        // Prefer the email stored directly on the Employee record.
        String employeeEmail = normalizeEmail(supervisor.getEmail());

        if (employeeEmail != null) {
            return new SupervisorRecipient(
                    supervisor.getId(),
                    supervisorName,
                    employeeEmail
            );
        }

        // Otherwise, use an enabled User account linked to that employee.
        List<User> linkedUsers =
                userRepository.findByEmployee_Id(supervisor.getId());

        if (linkedUsers != null) {
            for (User user : linkedUsers) {
                if (!Boolean.TRUE.equals(user.getEnabled())) {
                    continue;
                }

                String userEmail = normalizeEmail(user.getEmail());

                if (userEmail != null) {
                    String resolvedName =
                            user.getName() != null && !user.getName().isBlank()
                                    ? user.getName().trim()
                                    : supervisorName;

                    return new SupervisorRecipient(
                            supervisor.getId(),
                            resolvedName,
                            userEmail
                    );
                }
            }
        }

        return null;
    }

    private void sendLeaveRequestToFallbackRecipients(Leave leave) {
        if (leave == null || leave.getEmployee() == null) {
            return;
        }

        SystemSettings settings = settingsService.getSystemSettings();
        Set<String> recipients = new LinkedHashSet<>();
        Employee employee = leave.getEmployee();

        // Category-specific supervisor recipients.
        if (employee.getCategory() != null &&
                Boolean.TRUE.equals(employee.getCategory().getUseCustomApprovers())) {

            addNormalizedRecipients(
                    recipients,
                    employee.getCategory().getSupervisorEmails()
            );
        }

        // Global leave-request recipients.
        if (recipients.isEmpty()) {
            addNormalizedRecipients(
                    recipients,
                    settings.getLeaveRequestRecipients()
            );
        }

        // Final fallback: all enabled supervisor user accounts.
        if (recipients.isEmpty()) {
            List<User> supervisorUsers =
                    userRepository.findByRoleNameAndEnabled("ROLE_SUPERVISOR");

            if (supervisorUsers != null) {
                for (User user : supervisorUsers) {
                    String email = normalizeEmail(user.getEmail());
                    if (email != null) {
                        recipients.add(email);
                    }
                }
            }
        }

        if (recipients.isEmpty()) {
            System.err.println(
                    "No supervisor or fallback recipient found for leave ID: "
                            + leave.getId()
            );
            return;
        }

        for (String recipient : recipients) {
            emailService.sendLeaveRequestToSupervisor(
                    leave,
                    recipient,
                    "Leave Approver"
            );
        }

        System.out.println(
                "Leave ID " + leave.getId()
                        + " sent to fallback recipients: " + recipients
        );
    }

    private void addNormalizedRecipients(
            Set<String> recipients,
            String configuredEmails
    ) {
        List<String> parsed =
                emailRecipientHelper.parseEmailRecipients(configuredEmails);

        if (parsed == null) {
            return;
        }

        for (String email : parsed) {
            String normalized = normalizeEmail(email);
            if (normalized != null) {
                recipients.add(normalized);
            }
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String cleaned = email.trim();

        if (cleaned.isEmpty() || !cleaned.contains("@")) {
            return null;
        }

        return cleaned.toLowerCase(Locale.ROOT);
    }

    private String buildEmployeeName(Employee employee) {
        if (employee == null) {
            return "Supervisor";
        }

        String firstName =
                employee.getFirstName() == null
                        ? ""
                        : employee.getFirstName().trim();

        String lastName =
                employee.getLastName() == null
                        ? ""
                        : employee.getLastName().trim();

        String fullName = (firstName + " " + lastName).trim();

        return fullName.isEmpty() ? "Supervisor" : fullName;
    }

    private record SupervisorRecipient(
            Long employeeId,
            String name,
            String email
    ) {
    }

    private void validateLeaveRequest(Leave leave, Employee employee) {
        if (leave.getLeaveType() == null || leave.getLeaveType().isBlank()) {
            throw new IllegalArgumentException("Leave type is required");
        }

        if (leave.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }

        if (leave.getEndDate() == null) {
            throw new IllegalArgumentException("End date is required");
        }

        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        boolean excludeWeekends = shouldExcludeWeekends(employee);

        /*
         * true  = exclude weekends, so a normal leave cannot start/end on a weekend.
         * false = include weekends, so Saturday/Sunday are allowed and counted.
         */
        if (excludeWeekends && !isLongTermFamilyLeave(leave.getLeaveType())) {
            if (isWeekend(leave.getStartDate())) {
                throw new IllegalArgumentException(
                        "This employee's category excludes weekends from leave. " +
                                "Start date must be a weekday."
                );
            }

            if (isWeekend(leave.getEndDate())) {
                throw new IllegalArgumentException(
                        "This employee's category excludes weekends from leave. " +
                                "End date must be a weekday."
                );
            }
        }

        if (isLeaveTypeDeductible(leave.getLeaveType())) {
            int requestedDays = calculateLeaveDays(
                    leave.getStartDate(),
                    leave.getEndDate(),
                    employee
            );

            int availableDays = getAvailableLeaveDays(employee);

            if (requestedDays > availableDays) {
                throw new IllegalArgumentException(
                        "Insufficient leave balance. Available: " +
                                availableDays + ", requested: " + requestedDays
                );
            }
        }
    }

    // =========================================================
    // ATTACHMENTS
    // =========================================================

    @Transactional
    public Leave uploadAttachment(Long leaveId, MultipartFile file) throws IOException {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();

        if (!isValidFileType(contentType)) {
            throw new IllegalArgumentException(
                    "Invalid file type. Only PDF, JPG and PNG files are allowed."
            );
        }

        if (file.getSize() > 5L * 1024L * 1024L) {
            throw new IllegalArgumentException("File size must be less than 5MB");
        }

        leave.setAttachmentFileName(file.getOriginalFilename());
        leave.setAttachmentFileType(contentType);
        leave.setAttachmentFileSize(file.getSize());
        leave.setAttachmentFileContent(file.getBytes());

        return leaveRepository.save(leave);
    }

    public Leave getLeaveWithAttachment(Long leaveId) {
        return leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
    }

    public boolean hasAttachment(Long leaveId) {
        return leaveRepository.findById(leaveId)
                .map(Leave::hasAttachment)
                .orElse(false);
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (
                "application/pdf".equalsIgnoreCase(contentType) ||
                        "image/jpeg".equalsIgnoreCase(contentType) ||
                        "image/jpg".equalsIgnoreCase(contentType) ||
                        "image/png".equalsIgnoreCase(contentType)
        );
    }

    // =========================================================
    // READ
    // =========================================================

    @Transactional
    public List<Leave> getAllLeave() {
        List<Leave> leaves = leaveRepository.findAllWithEmployeeAndCategory();

        // Force access while the transaction is active.
        for (Leave leave : leaves) {
            if (leave.getEmployee() != null && leave.getEmployee().getCategory() != null) {
                leave.getEmployee().getCategory().getId();
                leave.getEmployee().getCategory().getExcludeWeekendsFromLeave();
                leave.getEmployee().getCategory().getAnnualLeaveDays();
            }
        }

        return leaves;
    }

    public List<Leave> getLeavesByEmployeeId(Long employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }

    public List<Leave> getLeaveHistoryByEmployeeId(Long employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }

    public Leave getLeaveById(Long leaveId) {
        return leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave not found with id: " + leaveId
                ));
    }

    public List<Leave> getSupervisorPending() {
        return leaveRepository.findBySupervisorStatus("Pending");
    }

    public List<Leave> getPlannerPending() {
        return leaveRepository.findByPlannerStatus("Pending");
    }

    public List<Leave> getHrPending() {
        return leaveRepository.findByHrStatus("Pending");
    }

    // =========================================================
    // APPROVAL WORKFLOW
    // =========================================================

    @Transactional
    public Leave supervisorApprove(Long leaveId, String feedback) {
        Leave leave = getLeaveById(leaveId);

        if (!"Pending".equalsIgnoreCase(leave.getSupervisorStatus())) {
            throw new IllegalStateException("Supervisor already acted on this request");
        }

        leave.setSupervisorStatus("Approved");
        leave.setSupervisorFeedback(safeFeedback(feedback));
        leave.setSupervisorActionDate(LocalDateTime.now());

        // Keep overall status descriptive while waiting for Planner.
        leave.setStatus("Supervisor_Approved");

        Leave updatedLeave = leaveRepository.save(leave);

        try {
            emailService.sendSupervisorApprovalNotification(updatedLeave, safeFeedback(feedback));
        } catch (Exception e) {
            System.err.println("Failed to send supervisor approval email: " + e.getMessage());
        }

        return updatedLeave;
    }

    @Transactional
    public Leave plannerApprove(Long leaveId, String feedback) {
        Leave leave = getLeaveById(leaveId);

        if (!"Approved".equalsIgnoreCase(leave.getSupervisorStatus())) {
            throw new IllegalStateException("Planner cannot act until Supervisor approves");
        }

        if (!"Pending".equalsIgnoreCase(leave.getPlannerStatus())) {
            throw new IllegalStateException("Planner already acted on this request");
        }

        leave.setPlannerStatus("Approved");
        leave.setPlannerFeedback(safeFeedback(feedback));
        leave.setPlannerActionDate(LocalDateTime.now());
        leave.setHrStatus("Pending");
        leave.setStatus("Planner_Approved");

        Leave updatedLeave = leaveRepository.save(leave);

        try {
            emailService.sendPlannerApprovalNotification(updatedLeave, safeFeedback(feedback));
        } catch (Exception e) {
            System.err.println("Failed to send planner approval email: " + e.getMessage());
        }

        return updatedLeave;
    }

    @Transactional
    public Leave hrApprove(Long leaveId, String feedback) {
        Leave leave = getLeaveById(leaveId);

        if (!"Approved".equalsIgnoreCase(leave.getPlannerStatus())) {
            throw new IllegalStateException("HR cannot act until Planner approves");
        }

        if ("Approved".equalsIgnoreCase(leave.getHrStatus()) &&
                "Approved".equalsIgnoreCase(leave.getStatus())) {
            return leave;
        }

        leave.setHrStatus("Approved");
        leave.setHrFeedback(safeFeedback(feedback));
        leave.setHrActionDate(LocalDateTime.now());
        leave.setStatus("Approved");

        deductLeaveDays(leave);

        Leave updatedLeave = leaveRepository.save(leave);

        try {
            emailService.sendHrApprovalNotification(updatedLeave, safeFeedback(feedback));
        } catch (Exception e) {
            System.err.println("Failed to send HR approval email: " + e.getMessage());
        }

        return updatedLeave;
    }

    @Transactional
    public Leave rejectLeave(Long leaveId, String role, String feedback) {
        Leave leave = getLeaveById(leaveId);
        String normalizedRole = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        String normalizedFeedback = safeFeedback(feedback);

        switch (normalizedRole) {
            case "supervisor" -> {
                if (!"Pending".equalsIgnoreCase(leave.getSupervisorStatus())) {
                    throw new IllegalStateException("Supervisor already acted on this request");
                }

                leave.setSupervisorStatus("Rejected");
                leave.setSupervisorFeedback(normalizedFeedback);
                leave.setSupervisorActionDate(LocalDateTime.now());
            }

            case "planner" -> {
                if (!"Approved".equalsIgnoreCase(leave.getSupervisorStatus())) {
                    throw new IllegalStateException(
                            "Planner cannot reject before Supervisor approves"
                    );
                }

                if (!"Pending".equalsIgnoreCase(leave.getPlannerStatus())) {
                    throw new IllegalStateException("Planner already acted on this request");
                }

                leave.setPlannerStatus("Rejected");
                leave.setPlannerFeedback(normalizedFeedback);
                leave.setPlannerActionDate(LocalDateTime.now());
            }

            case "hr" -> {
                if (!"Approved".equalsIgnoreCase(leave.getPlannerStatus())) {
                    throw new IllegalStateException(
                            "HR cannot reject before Planner approves"
                    );
                }

                if (!"Pending".equalsIgnoreCase(leave.getHrStatus())) {
                    throw new IllegalStateException("HR already acted on this request");
                }

                leave.setHrStatus("Rejected");
                leave.setHrFeedback(normalizedFeedback);
                leave.setHrActionDate(LocalDateTime.now());
            }

            default -> throw new IllegalArgumentException(
                    "Invalid approval role. Expected supervisor, planner or hr."
            );
        }

        leave.setStatus("Rejected");

        Leave updatedLeave = leaveRepository.save(leave);

        try {
            emailService.sendLeaveRejectionNotification(
                    updatedLeave,
                    normalizedRole,
                    normalizedFeedback
            );
        } catch (Exception e) {
            System.err.println("Failed to send rejection email: " + e.getMessage());
        }

        return updatedLeave;
    }

    // =========================================================
    // BULK ACTIONS
    // =========================================================

    @Transactional
    public Map<String, Object> bulkApproveLeaves(List<Long> leaveIds, String feedback) {
        List<Leave> approvedLeaves = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String normalizedFeedback = safeFeedback(feedback);

        if (leaveIds == null || leaveIds.isEmpty()) {
            return bulkResult("approvedLeaves", approvedLeaves, errors);
        }

        for (Long leaveId : leaveIds) {
            try {
                Leave leave = getLeaveById(leaveId);

                if ("Approved".equalsIgnoreCase(leave.getStatus())) {
                    errors.add("Leave ID " + leaveId + " is already approved");
                    continue;
                }

                if (!"Pending".equalsIgnoreCase(leave.getSupervisorStatus())) {
                    errors.add(
                            "Leave ID " + leaveId +
                                    " cannot be bulk-approved at this stage"
                    );
                    continue;
                }

                leave.setSupervisorStatus("Approved");
                leave.setSupervisorFeedback(normalizedFeedback);
                leave.setSupervisorActionDate(LocalDateTime.now());

                leave.setPlannerStatus("Approved");
                leave.setPlannerFeedback(normalizedFeedback);
                leave.setPlannerActionDate(LocalDateTime.now());

                leave.setHrStatus("Approved");
                leave.setHrFeedback(normalizedFeedback);
                leave.setHrActionDate(LocalDateTime.now());

                leave.setStatus("Approved");

                deductLeaveDays(leave);

                Leave approvedLeave = leaveRepository.save(leave);
                approvedLeaves.add(approvedLeave);

                try {
                    emailService.sendBulkApprovalNotification(
                            approvedLeave,
                            normalizedFeedback
                    );
                } catch (Exception emailError) {
                    System.err.println(
                            "Failed to send bulk approval email for leave ID " +
                                    leaveId + ": " + emailError.getMessage()
                    );
                }
            } catch (Exception e) {
                errors.add(
                        "Failed to approve leave ID " + leaveId + ": " + e.getMessage()
                );
            }
        }

        return bulkResult("approvedLeaves", approvedLeaves, errors);
    }

    @Transactional
    public Map<String, Object> bulkRejectLeaves(List<Long> leaveIds, String feedback) {
        List<Leave> rejectedLeaves = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String normalizedFeedback = safeFeedback(feedback);

        if (leaveIds == null || leaveIds.isEmpty()) {
            return bulkResult("rejectedLeaves", rejectedLeaves, errors);
        }

        for (Long leaveId : leaveIds) {
            try {
                Leave leave = getLeaveById(leaveId);

                if ("Rejected".equalsIgnoreCase(leave.getStatus())) {
                    errors.add("Leave ID " + leaveId + " is already rejected");
                    continue;
                }

                leave.setSupervisorStatus("Rejected");
                leave.setSupervisorFeedback(normalizedFeedback);
                leave.setSupervisorActionDate(LocalDateTime.now());

                leave.setPlannerStatus("Rejected");
                leave.setPlannerFeedback(normalizedFeedback);
                leave.setPlannerActionDate(LocalDateTime.now());

                leave.setHrStatus("Rejected");
                leave.setHrFeedback(normalizedFeedback);
                leave.setHrActionDate(LocalDateTime.now());

                leave.setStatus("Rejected");

                Leave rejectedLeave = leaveRepository.save(leave);
                rejectedLeaves.add(rejectedLeave);

                try {
                    emailService.sendLeaveRejectionNotification(
                            rejectedLeave,
                            "supervisor",
                            normalizedFeedback
                    );
                } catch (Exception emailError) {
                    System.err.println(
                            "Failed to send rejection email for leave ID " +
                                    leaveId + ": " + emailError.getMessage()
                    );
                }
            } catch (Exception e) {
                errors.add(
                        "Failed to reject leave ID " + leaveId + ": " + e.getMessage()
                );
            }
        }

        return bulkResult("rejectedLeaves", rejectedLeaves, errors);
    }

    private Map<String, Object> bulkResult(
            String dataKey,
            List<Leave> records,
            List<String> errors
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", records.size());
        result.put(dataKey, records);
        result.put("errors", errors);
        return result;
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void deleteLeave(Long leaveId) {
        Leave leave = getLeaveById(leaveId);

        if ("Approved".equalsIgnoreCase(leave.getStatus()) &&
                Boolean.TRUE.equals(leave.getIsDeducted())) {
            throw new IllegalStateException(
                    "Cannot delete an approved leave whose days have already been deducted."
            );
        }

        emailNotificationService.deleteByLeaveId(leaveId);
        leaveRepository.delete(leave);
    }

    @Transactional
    public Map<String, Object> bulkDeleteLeaves(List<Long> leaveIds) {
        List<Long> deletedIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (leaveIds == null || leaveIds.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("deletedCount", 0);
            emptyResult.put("deletedIds", deletedIds);
            emptyResult.put("errors", errors);
            return emptyResult;
        }

        for (Long leaveId : leaveIds) {
            try {
                Leave leave = getLeaveById(leaveId);

                if ("Approved".equalsIgnoreCase(leave.getStatus()) &&
                        Boolean.TRUE.equals(leave.getIsDeducted())) {
                    errors.add(
                            "Cannot delete approved leave ID " + leaveId +
                                    " because its days have already been deducted"
                    );
                    continue;
                }

                emailNotificationService.deleteByLeaveId(leaveId);
                leaveRepository.delete(leave);
                deletedIds.add(leaveId);
            } catch (Exception e) {
                errors.add(
                        "Failed to delete leave ID " + leaveId + ": " + e.getMessage()
                );
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedIds.size());
        result.put("deletedIds", deletedIds);
        result.put("errors", errors);
        return result;
    }

    // =========================================================
    // LEAVE BALANCE AND DAY CALCULATION
    // =========================================================

    private boolean shouldExcludeWeekends(Employee employee) {
        if (employee == null || employee.getCategory() == null) {
            return true;
        }

        Boolean setting = employee.getCategory().getExcludeWeekendsFromLeave();

        // Null safely defaults to excluding weekends.
        return setting == null || setting;
    }

    private boolean isWeekend(LocalDate date) {
        if (date == null) {
            return false;
        }

        DayOfWeek day = date.getDayOfWeek();

        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private boolean isLongTermFamilyLeave(String leaveType) {
        String normalized = normalizeLeaveType(leaveType);

        return "maternity".equals(normalized) || "paternity".equals(normalized);
    }

    private int calculateLeaveDays(
            LocalDate startDate,
            LocalDate endDate,
            Employee employee
    ) {
        if (startDate == null || endDate == null) {
            return 0;
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        boolean excludeWeekends = shouldExcludeWeekends(employee);
        int days = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            if (!excludeWeekends || !isWeekend(currentDate)) {
                days++;
            }

            currentDate = currentDate.plusDays(1);
        }

        return days;
    }

    /*
     * Kept for compatibility with existing code that may refer to this method name.
     */
    private int calculateBusinessDays(
            LocalDate startDate,
            LocalDate endDate,
            Employee employee
    ) {
        return calculateLeaveDays(startDate, endDate, employee);
    }

    private int getGlobalAnnualLeaveBalance() {
        LeaveSettings settings = leaveSettingsRepository.findTopByOrderByIdDesc();

        return settings != null && settings.getAnnualLeaveBalance() != null
                ? Math.max(settings.getAnnualLeaveBalance(), 0)
                : 20;
    }

    private int getEmployeeAnnualLeaveBalance(Employee employee) {
        if (employee != null &&
                employee.getCategory() != null &&
                employee.getCategory().getAnnualLeaveDays() != null) {
            return Math.max(employee.getCategory().getAnnualLeaveDays(), 0);
        }

        return getGlobalAnnualLeaveBalance();
    }

    private int getAvailableLeaveDays(Employee employee) {
        if (employee == null || employee.getId() == null) {
            return 0;
        }

        int annualBalance = getEmployeeAnnualLeaveBalance(employee);
        int usedDays = getUsedLeaveDays(employee.getId());

        return Math.max(annualBalance - usedDays, 0);
    }

    private void deductLeaveDays(Leave leave) {
        if (leave == null) {
            throw new IllegalArgumentException("Leave request is required");
        }

        if (!isLeaveTypeDeductible(leave.getLeaveType())) {
            leave.setIsDeducted(false);
            leave.setDeductedDays(0);
            return;
        }

        if (Boolean.TRUE.equals(leave.getIsDeducted())) {
            return;
        }

        if (leave.getEmployee() == null || leave.getEmployee().getId() == null) {
            throw new IllegalArgumentException("Employee is missing from leave request");
        }

        Employee employee = employeeRepository.findById(leave.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + leave.getEmployee().getId()
                ));

        int requestedDays = calculateLeaveDays(
                leave.getStartDate(),
                leave.getEndDate(),
                employee
        );

        int annualBalance = getEmployeeAnnualLeaveBalance(employee);
        int usedDays = getUsedLeaveDays(employee.getId());
        int availableDays = Math.max(annualBalance - usedDays, 0);

        if (requestedDays > availableDays) {
            throw new IllegalStateException(
                    "Insufficient leave balance. Available: " +
                            availableDays + ", requested: " + requestedDays
            );
        }

        leave.setEmployee(employee);
        leave.setIsDeducted(true);
        leave.setDeductedDays(requestedDays);
        leave.setDeductionDate(LocalDate.now());
    }

    public int getUsedLeaveDays(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId
                ));

        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.withDayOfYear(now.lengthOfYear());

        List<Leave> employeeLeaves = leaveRepository.findByEmployeeId(employeeId);

        int totalUsedDays = 0;

        for (Leave leave : employeeLeaves) {
            if (!"Approved".equalsIgnoreCase(leave.getStatus())) {
                continue;
            }

            if (!isLeaveTypeDeductible(leave.getLeaveType())) {
                continue;
            }

            if (!overlaps(leave.getStartDate(), leave.getEndDate(), startOfYear, endOfYear)) {
                continue;
            }

            LocalDate effectiveStart = laterDate(leave.getStartDate(), startOfYear);
            LocalDate effectiveEnd = earlierDate(leave.getEndDate(), endOfYear);

            totalUsedDays += calculateLeaveDays(
                    effectiveStart,
                    effectiveEnd,
                    employee
            );
        }

        return totalUsedDays;
    }

    public int getPendingLeaveDays(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId
                ));

        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.withDayOfYear(now.lengthOfYear());

        List<Leave> employeeLeaves = leaveRepository.findByEmployeeId(employeeId);

        int totalPendingDays = 0;

        for (Leave leave : employeeLeaves) {
            if (!isPendingWorkflowStatus(leave)) {
                continue;
            }

            if (!isLeaveTypeDeductible(leave.getLeaveType())) {
                continue;
            }

            if (!overlaps(leave.getStartDate(), leave.getEndDate(), startOfYear, endOfYear)) {
                continue;
            }

            LocalDate effectiveStart = laterDate(leave.getStartDate(), startOfYear);
            LocalDate effectiveEnd = earlierDate(leave.getEndDate(), endOfYear);

            totalPendingDays += calculateLeaveDays(
                    effectiveStart,
                    effectiveEnd,
                    employee
            );
        }

        return totalPendingDays;
    }

    private boolean isPendingWorkflowStatus(Leave leave) {
        if (leave == null || "Rejected".equalsIgnoreCase(leave.getStatus()) ||
                "Approved".equalsIgnoreCase(leave.getStatus())) {
            return false;
        }

        String status = leave.getStatus();

        return status == null ||
                "Pending".equalsIgnoreCase(status) ||
                "Supervisor_Approved".equalsIgnoreCase(status) ||
                "Planner_Approved".equalsIgnoreCase(status);
    }

    private boolean isLeaveTypeDeductible(String leaveType) {
        if (leaveType == null || leaveType.isBlank()) {
            return true;
        }

        LeaveSettings settings = leaveSettingsRepository.findTopByOrderByIdDesc();

        if (settings == null || settings.getNonDeductibleLeaveTypes() == null) {
            return true;
        }

        String normalizedRequestedType = normalizeLeaveType(leaveType);

        for (String nonDeductibleType : settings.getNonDeductibleLeaveTypes()) {
            if (normalizeLeaveType(nonDeductibleType).equals(normalizedRequestedType)) {
                return false;
            }
        }

        return true;
    }

    private String normalizeLeaveType(String leaveType) {
        if (leaveType == null) {
            return "";
        }

        return leaveType
                .trim()
                .replaceAll("(?i)\\s+leave$", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private boolean overlaps(
            LocalDate recordStart,
            LocalDate recordEnd,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        if (recordStart == null || recordEnd == null) {
            return false;
        }

        return !recordStart.isAfter(periodEnd) && !recordEnd.isBefore(periodStart);
    }

    private LocalDate laterDate(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalDate earlierDate(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    public List<EmployeeLeaveBalanceDTO> getAllEmployeesLeaveBalances() {
        List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();
        List<EmployeeLeaveBalanceDTO> balances = new ArrayList<>();

        for (Employee employee : activeEmployees) {
            int usedDays = getUsedLeaveDays(employee.getId());
            int pendingDays = getPendingLeaveDays(employee.getId());
            int annualBalance = getEmployeeAnnualLeaveBalance(employee);
            int available = Math.max(annualBalance - usedDays, 0);

            double percentage = annualBalance > 0
                    ? usedDays * 100.0 / annualBalance
                    : 0;

            Map<String, Integer> breakdown = getLeaveTypeBreakdown(employee.getId());

            EmployeeLeaveBalanceDTO dto = new EmployeeLeaveBalanceDTO(
                    employee.getId(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getEmployeeId(),
                    employee.getDepartment(),
                    employee.getCategory(),
                    annualBalance,
                    usedDays,
                    pendingDays,
                    available,
                    Math.round(percentage * 100.0) / 100.0
            );

            dto.setLeaveTypeBreakdown(breakdown);
            balances.add(dto);
        }

        return balances;
    }

    private Map<String, Integer> getLeaveTypeBreakdown(Long employeeId) {
        Map<String, Integer> breakdown = new HashMap<>();

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId
                ));

        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.withDayOfYear(now.lengthOfYear());

        List<Leave> employeeLeaves = leaveRepository.findByEmployeeId(employeeId);

        for (Leave leave : employeeLeaves) {
            if (!"Approved".equalsIgnoreCase(leave.getStatus())) {
                continue;
            }

            if (!overlaps(leave.getStartDate(), leave.getEndDate(), startOfYear, endOfYear)) {
                continue;
            }

            LocalDate effectiveStart = laterDate(leave.getStartDate(), startOfYear);
            LocalDate effectiveEnd = earlierDate(leave.getEndDate(), endOfYear);

            int days = calculateLeaveDays(
                    effectiveStart,
                    effectiveEnd,
                    employee
            );

            String leaveTypeKey = leave.getLeaveType() == null ||
                    leave.getLeaveType().isBlank()
                    ? "Other"
                    : leave.getLeaveType().replaceAll("(?i)\\s+leave$", "").trim();

            breakdown.merge(leaveTypeKey, days, Integer::sum);
        }

        return breakdown;
    }

    // =========================================================
    // OVERVIEW SYNCHRONIZATION
    // =========================================================

    @SuppressWarnings("unused")
    private void updateOverviewWithLeave(Leave leave) {
        if (leave == null ||
                leave.getEmployee() == null ||
                leave.getEmployee().getId() == null ||
                leave.getStartDate() == null ||
                leave.getEndDate() == null) {
            return;
        }

        List<Overview> overviews = overviewRepository.findByEmployeeIdAndDateBetween(
                leave.getEmployee().getId(),
                leave.getStartDate(),
                leave.getEndDate()
        );

        for (Overview overview : overviews) {
            overview.setLeave(leave);
            overview.setStatus("On Leave");
        }

        overviewRepository.saveAll(overviews);
    }

    private String safeFeedback(String feedback) {
        return feedback == null ? "" : feedback.trim();
    }
}
