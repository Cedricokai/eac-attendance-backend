package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.email.EmailNotificationService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.dto.EmployeeLeaveBalanceDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.entity.attendance.Overview;
import com.example.eac_backend_project.entity.settings.LeaveSettings;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.LeaveRepository;
import com.example.eac_backend_project.repository.OverviewRepository;
import com.example.eac_backend_project.repository.settings.LeaveSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    // In LeaveService.java - Update createLeave method
    public Leave createLeave(Leave leave) {
        Employee employee = employeeRepository.findById(leave.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        leave.setEmployee(employee);
        leave.setStatus("Pending");
        leave.setSupervisorStatus("Pending");
        leave.setPlannerStatus("Pending");
        leave.setHrStatus("Pending");
        leave.setCreatedAt(LocalDateTime.now());

        Leave savedLeave = leaveRepository.save(leave);

        // Send email notifications
        try {
            // Send to all configured recipients (NEW)
            emailNotificationService.sendLeaveRequestNotifications(savedLeave);

            // Also send to the employee (existing)
            emailService.sendLeaveRequestNotification(savedLeave);
        } catch (Exception e) {
            System.err.println("Failed to send email notification: " + e.getMessage());
        }

        return savedLeave;
    }

    @Transactional
    public Leave uploadAttachment(Long leaveId, MultipartFile file) throws IOException {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        String contentType = file.getContentType();
        if (!isValidFileType(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only PDF, JPG, PNG files are allowed.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 5MB");
        }

        leave.setAttachmentFileName(file.getOriginalFilename());
        leave.setAttachmentFileType(file.getContentType());
        leave.setAttachmentFileSize(file.getSize());
        leave.setAttachmentFileContent(file.getBytes());

        return leaveRepository.save(leave);
    }

    public Leave getLeaveWithAttachment(Long leaveId) {
        return leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
    }

    public boolean hasAttachment(Long leaveId) {
        try {
            Leave leave = leaveRepository.findById(leaveId)
                    .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
            return leave.hasAttachment();
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/png")
        );
    }

    public List<Leave> getAllLeave() {
        List<Leave> leaves = leaveRepository.findAll();
        // Ensure category is loaded for each employee
        leaves.forEach(leave -> {
            if (leave.getEmployee() != null && leave.getEmployee().getCategory() != null) {
                // Force load the category
                leave.getEmployee().getCategory().getId();
            }
        });
        return leaves;
    }

    public List<Leave> getLeavesByEmployeeId(Long employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }

    public List<Leave> getSupervisorPending() {
        return leaveRepository.findBySupervisorStatus("Pending");
    }

    @Transactional
    public Leave plannerApprove(Long leaveId, String feedback) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if (!"Approved".equalsIgnoreCase(leave.getSupervisorStatus())) {
            throw new IllegalStateException("Planner cannot act until Supervisor approves");
        }
        if (!"Pending".equalsIgnoreCase(leave.getPlannerStatus())) {
            throw new IllegalStateException("Planner already acted on this request");
        }

        leave.setPlannerStatus("Approved");
        leave.setPlannerFeedback(feedback);
        leave.setPlannerActionDate(LocalDateTime.now());
        leave.setHrStatus("Pending");
        leave.setStatus("Planner_Approved");

        Leave updatedLeave = leaveRepository.save(leave);

        // Send email notification
        try {
            emailService.sendPlannerApprovalNotification(updatedLeave, feedback);
        } catch (Exception e) {
            System.err.println("Failed to send planner approval email: " + e.getMessage());
        }

        return updatedLeave;
    }

    public List<Leave> getPlannerPending() {
        return leaveRepository.findByPlannerStatus("Pending");
    }

    @Transactional
    public Leave hrApprove(Long leaveId, String feedback) {
        Leave leave = getLeaveById(leaveId);

        if (!"Approved".equalsIgnoreCase(leave.getPlannerStatus())) {
            throw new IllegalStateException("HR cannot act until Planner approves");
        }

        leave.setHrStatus("Approved");
        leave.setHrFeedback(feedback);
        leave.setHrActionDate(LocalDateTime.now());
        leave.setStatus("Approved");

        // ✅ DEDUCT HERE (final approval)
        deductLeaveDays(leave);

        Leave updatedLeave = leaveRepository.save(leave);

        // Send email notification
        try {
            emailService.sendHrApprovalNotification(updatedLeave, feedback);

        } catch (Exception e) {
            System.err.println("Failed to send HR approval email: " + e.getMessage());
        }

        return updatedLeave;
    }

    public List<Leave> getHrPending() {
        return leaveRepository.findByHrStatus("Pending");
    }

    @Transactional
    public Leave rejectLeave(Long leaveId, String role, String feedback) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if ("supervisor".equalsIgnoreCase(role)) {
            leave.setSupervisorStatus("Rejected");
            leave.setSupervisorFeedback(feedback);
            leave.setSupervisorActionDate(LocalDateTime.now());
        } else if ("planner".equalsIgnoreCase(role)) {
            if (!"Approved".equalsIgnoreCase(leave.getSupervisorStatus())) {
                throw new IllegalStateException("Planner cannot reject before Supervisor acts");
            }
            leave.setPlannerStatus("Rejected");
            leave.setPlannerFeedback(feedback);
            leave.setPlannerActionDate(LocalDateTime.now());
        } else if ("hr".equalsIgnoreCase(role)) {
            if (!"Approved".equalsIgnoreCase(leave.getPlannerStatus())) {
                throw new IllegalStateException("HR cannot reject before Planner acts");
            }
            leave.setHrStatus("Rejected");
            leave.setHrFeedback(feedback);
            leave.setHrActionDate(LocalDateTime.now());
        }

        leave.setStatus("Rejected");

        Leave updatedLeave = leaveRepository.save(leave);

        // Send email notification
        try {
            emailService.sendLeaveRejectionNotification(updatedLeave, role, feedback);
        } catch (Exception e) {
            System.err.println("Failed to send rejection email: " + e.getMessage());
        }

        return updatedLeave;
    }

    private void updateOverviewWithLeave(Leave leave) {
        LocalDate startDate = leave.getStartDate();
        LocalDate endDate = leave.getEndDate();
        Long employeeId = leave.getEmployee().getId();

        List<Overview> overviews = overviewRepository
                .findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);

        overviews.forEach(overview -> {
            overview.setLeave(leave);
            overview.setStatus("On Leave");
            overviewRepository.save(overview);
        });
    }

    // Add this method to deduct leave days when approved
    private void deductLeaveDays(Leave leave) {
        if (!isLeaveTypeDeductible(leave.getLeaveType())) {
            return;
        }

        Employee employee = leave.getEmployee();
        int businessDays = calculateBusinessDays(
                leave.getStartDate(),
                leave.getEndDate(),
                employee
        );

        int usedDays = getUsedLeaveDays(employee.getId());
        int totalAnnualBalance = getAnnualLeaveBalance();

        if (usedDays + businessDays > totalAnnualBalance) {
            throw new RuntimeException("Insufficient leave balance. Available: " +
                    (totalAnnualBalance - usedDays) + ", Requested: " + businessDays);
        }

        leave.setIsDeducted(true);
        leave.setDeductedDays(businessDays);
        leave.setDeductionDate(LocalDate.now());
        leaveRepository.save(leave);
    }

    private int getAnnualLeaveBalance() {
        LeaveSettings settings = leaveSettingsRepository.findTopByOrderByIdDesc();
        return settings != null && settings.getAnnualLeaveBalance() != null ? settings.getAnnualLeaveBalance() : 20; // Default
    }

    // Call deductLeaveDays in approve methods
    @Transactional
    public Leave supervisorApprove(Long leaveId, String feedback) {
        Leave leave = getLeaveById(leaveId);
        leave.setSupervisorStatus("Approved");
        leave.setSupervisorFeedback(feedback);
        leave.setSupervisorActionDate(LocalDateTime.now());

        Leave updatedLeave = leaveRepository.save(leave);

        // Send email notification
        try {
            emailService.sendSupervisorApprovalNotification(updatedLeave, feedback);
        } catch (Exception e) {
            System.err.println("Failed to send supervisor approval email: " + e.getMessage());
        }

        return updatedLeave;
    }

    public Leave getLeaveById(Long leaveId) {
        return leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));
    }

    public int getUsedLeaveDays(Long employeeId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.withDayOfYear(now.lengthOfYear());

        List<Leave> approvedLeaves = leaveRepository.findByEmployeeIdAndStatusAndStartDateBetween(
                employeeId, "Approved", startOfYear, endOfYear);

        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        int totalUsedDays = 0;
        for (Leave leave : approvedLeaves) {
            if (isLeaveTypeDeductible(leave.getLeaveType())) {
                int businessDays = calculateBusinessDays(
                        leave.getStartDate(),
                        leave.getEndDate(),
                        employee
                );
                totalUsedDays += businessDays;
            }
        }
        return totalUsedDays;
    }

    public int getPendingLeaveDays(Long employeeId) {
        List<Leave> pendingLeaves = leaveRepository.findByEmployeeIdAndStatus(
                employeeId, "Pending");

        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        int totalPendingDays = 0;
        for (Leave leave : pendingLeaves) {
            if (isLeaveTypeDeductible(leave.getLeaveType())) {
                int businessDays = calculateBusinessDays(
                        leave.getStartDate(),
                        leave.getEndDate(),
                        employee
                );
                totalPendingDays += businessDays;
            }
        }
        return totalPendingDays;
    }

    private boolean isLeaveTypeDeductible(String leaveType) {
        // Get non-deductible leave types from settings
        LeaveSettings settings = leaveSettingsRepository.findTopByOrderByIdDesc();
        if (settings == null) {
            return true; // Default: all leaves are deductible
        }
        List<String> nonDeductibleTypes = settings.getNonDeductibleLeaveTypes();
        if (nonDeductibleTypes == null) {
            return true;
        }

        // Remove " Leave" suffix for comparison
        String typeWithoutSuffix = leaveType.replace(" Leave", "");
        return !nonDeductibleTypes.contains(typeWithoutSuffix);
    }

    // Add these methods to LeaveService.java

    // Bulk approve leaves
    @Transactional
    public Map<String, Object> bulkApproveLeaves(List<Long> leaveIds, String feedback) {
        List<Leave> approvedLeaves = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Long leaveId : leaveIds) {
            try {
                Leave leave = getLeaveById(leaveId);

                // Check if already approved
                if ("Approved".equals(leave.getStatus())) {
                    errors.add("Leave ID " + leaveId + " is already approved");
                    continue;
                }

                // Check current status for proper workflow
                if (!"Pending".equals(leave.getSupervisorStatus())) {
                    errors.add("Leave ID " + leaveId + " cannot be approved at this stage (current status: " + leave.getStatus() + ")");
                    continue;
                }

                // Approve through the workflow
                leave.setSupervisorStatus("Approved");
                leave.setSupervisorFeedback(feedback);
                leave.setSupervisorActionDate(LocalDateTime.now());

                // If supervisor is the only approver, set other statuses
                leave.setPlannerStatus("Approved");
                leave.setPlannerFeedback(feedback);
                leave.setPlannerActionDate(LocalDateTime.now());
                leave.setHrStatus("Approved");
                leave.setHrFeedback(feedback);
                leave.setHrActionDate(LocalDateTime.now());
                leave.setStatus("Approved");

                // Deduct leave days
                deductLeaveDays(leave);

                approvedLeaves.add(leaveRepository.save(leave));

            } catch (Exception e) {
                errors.add("Failed to approve leave ID " + leaveId + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", approvedLeaves.size());
        result.put("approvedLeaves", approvedLeaves);
        result.put("errors", errors);
        return result;
    }

    // Bulk reject leaves
    @Transactional
    public Map<String, Object> bulkRejectLeaves(List<Long> leaveIds, String feedback) {
        List<Leave> rejectedLeaves = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Long leaveId : leaveIds) {
            try {
                Leave leave = getLeaveById(leaveId);

                if ("Rejected".equals(leave.getStatus())) {
                    errors.add("Leave ID " + leaveId + " is already rejected");
                    continue;
                }

                leave.setSupervisorStatus("Rejected");
                leave.setSupervisorFeedback(feedback);
                leave.setSupervisorActionDate(LocalDateTime.now());
                leave.setPlannerStatus("Rejected");
                leave.setPlannerFeedback(feedback);
                leave.setPlannerActionDate(LocalDateTime.now());
                leave.setHrStatus("Rejected");
                leave.setHrFeedback(feedback);
                leave.setHrActionDate(LocalDateTime.now());
                leave.setStatus("Rejected");

                rejectedLeaves.add(leaveRepository.save(leave));

                // Send rejection email
                try {
                    emailService.sendLeaveRejectionNotification(leave, "supervisor", feedback);
                } catch (Exception e) {
                    System.err.println("Failed to send rejection email for leave " + leaveId + ": " + e.getMessage());
                }

            } catch (Exception e) {
                errors.add("Failed to reject leave ID " + leaveId + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", rejectedLeaves.size());
        result.put("rejectedLeaves", rejectedLeaves);
        result.put("errors", errors);
        return result;
    }

    // Delete single leave
    @Transactional
    public void deleteLeave(Long leaveId) {
        Leave leave = getLeaveById(leaveId);

        // Check if leave is already approved - may need to reverse deduction
        if ("Approved".equals(leave.getStatus()) && leave.getIsDeducted()) {
            // Optionally reverse the deduction here
            // This would require adding logic to add back the deducted days
            throw new IllegalStateException("Cannot delete approved leave. Please adjust leave balance manually if needed.");
        }

        leaveRepository.delete(leave);
    }

    // Bulk delete leaves
    @Transactional
    public Map<String, Object> bulkDeleteLeaves(List<Long> leaveIds) {
        List<Long> deletedIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Long leaveId : leaveIds) {
            try {
                Leave leave = getLeaveById(leaveId);

                if ("Approved".equals(leave.getStatus()) && leave.getIsDeducted()) {
                    errors.add("Cannot delete approved leave ID " + leaveId + " (leave days already deducted)");
                    continue;
                }

                // FIRST: Delete email notifications associated with this leave
                emailNotificationService.deleteByLeaveId(leaveId);

                // THEN delete the leave
                leaveRepository.delete(leave);
                deletedIds.add(leaveId);

            } catch (Exception e) {
                errors.add("Failed to delete leave ID " + leaveId + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedIds.size());
        result.put("deletedIds", deletedIds);
        result.put("errors", errors);
        return result;
    }

    private int calculateBusinessDays(LocalDate startDate, LocalDate endDate, Employee employee) {
        int days = 0;
        LocalDate currentDate = startDate;

        // Check if this employee's category excludes weekends
        boolean excludeWeekends = true;
        if (employee != null && employee.getCategory() != null) {
            Boolean setting = employee.getCategory().getExcludeWeekendsFromLeave();
            excludeWeekends = setting == null ? true : setting;
        }

        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

            if (excludeWeekends) {
                if (!isWeekend) {
                    days++;
                }
            } else {
                days++;
            }
            currentDate = currentDate.plusDays(1);
        }
        return days;
    }

    public List<Leave> getLeaveHistoryByEmployeeId(Long employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }

    public List<EmployeeLeaveBalanceDTO> getAllEmployeesLeaveBalances() {
        List<Employee> activeEmployees = employeeRepository.findAllActiveEmployees();
        List<EmployeeLeaveBalanceDTO> balances = new ArrayList<>();

        for (Employee employee : activeEmployees) {
            int usedDays = getUsedLeaveDays(employee.getId());
            int pendingDays = getPendingLeaveDays(employee.getId());
            // FIX: Use employee's category annual days, not global settings
            int annualBalance = employee.getCategory() != null && employee.getCategory().getAnnualLeaveDays() != null
                    ? employee.getCategory().getAnnualLeaveDays()
                    : 20;
            int available = Math.max(0, annualBalance - usedDays);
            double percentage = annualBalance > 0 ? (usedDays * 100.0 / annualBalance) : 0;

            Map<String, Integer> breakdown = getLeaveTypeBreakdown(employee.getId());

            EmployeeLeaveBalanceDTO dto = new EmployeeLeaveBalanceDTO(
                    employee.getId(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getEmployeeId(),
                    employee.getDepartment(),
                    employee.getCategory(),
                    annualBalance,  // Now uses category's annual days
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
        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.withDayOfYear(now.lengthOfYear());

        List<Leave> approvedLeaves = leaveRepository.findByEmployeeIdAndStatusAndStartDateBetween(
                employeeId, "Approved", startOfYear, endOfYear);

        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        for (Leave leave : approvedLeaves) {
            // CHANGE: Include ALL leave types for display, not just deductible
            // Remove this condition or modify it:
            // if (isLeaveTypeDeductible(leave.getLeaveType())) {

            int days = calculateBusinessDays(
                    leave.getStartDate(),
                    leave.getEndDate(),
                    employee
            );
            String leaveTypeKey = leave.getLeaveType().replace(" Leave", "");
            breakdown.merge(leaveTypeKey, days, Integer::sum);
            // }
        }

        return breakdown;
    }
}
