package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.LeaveService;
import com.example.eac_backend_project.Service.email.EmailNotificationService;
import com.example.eac_backend_project.dto.EmployeeLeaveBalanceDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.LeaveRepository;
import com.example.eac_backend_project.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/leave")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class LeaveController {

    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        logger.info("Test endpoint hit successfully");
        return ResponseEntity.ok("Leave Controller is working! All endpoints are accessible.");
    }

    @GetMapping
    public ResponseEntity<List<Leave>> getAllLeaves() {
        try {
            logger.info("Fetching all leaves");
            List<Leave> leaves = leaveService.getAllLeave();
            logger.info("Found {} leaves", leaves.size());
            return ResponseEntity.ok(leaves);
        } catch (Exception e) {
            logger.error("Error fetching all leaves: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyLeaveRequests() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Get user by username
            User user = userRepository.findByUserName(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            String email = user.getEmail();

            // Find employee by email
            Employee employee = employeeRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found for email: " + email));

            List<Leave> myRequests = leaveService.getLeavesByEmployeeId(employee.getId());
            logger.info("Found {} leave requests for employee: {}", myRequests.size(), employee.getEmployeeId());

            return ResponseEntity.ok(myRequests);
        } catch (Exception e) {
            logger.error("Error fetching my leave requests: {}", e.getMessage(), e);
            // Return empty list instead of throwing
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping
    public ResponseEntity<?> createLeave(@RequestBody Leave leave) {
        try {
            logger.info("Creating new leave request");
            Leave savedLeave = leaveService.createLeave(leave);
            return ResponseEntity.ok(savedLeave);
        } catch (Exception e) {
            logger.error("Error creating leave: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error creating leave request: " + e.getMessage());
        }
    }

    @PostMapping("/{leaveId}/attachment")
    public ResponseEntity<?> uploadAttachment(
            @PathVariable Long leaveId,
            @RequestParam("file") MultipartFile file) {

        try {
            logger.info("Uploading attachment for leave ID: {}", leaveId);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            Leave updatedLeave = leaveService.uploadAttachment(leaveId, file);
            return ResponseEntity.ok(updatedLeave);

        } catch (IOException e) {
            logger.error("Error uploading file for leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid file for leave {}: {}", leaveId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error uploading attachment for leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error uploading attachment: " + e.getMessage());
        }
    }

    @PostMapping("/public-approve")
    public ResponseEntity<?> publicApproveLeave(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== PUBLIC APPROVE ENDPOINT HIT ===");

            Long leaveId = Long.valueOf(request.get("leaveId").toString());
            String token = (String) request.get("token");
            String feedback = (String) request.get("feedback");

            // Verify leave exists
            Leave leave = leaveService.getLeaveById(leaveId);

            // Verify token matches
            if (leave.getApprovalToken() == null || !leave.getApprovalToken().equals(token)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid or expired approval token"
                ));
            }

            // Check if already approved
            if ("Approved".equals(leave.getStatus())) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Leave already approved"
                ));
            }

            // Process approval
            Map<String, Object> result = leaveService.bulkApproveLeaves(
                    List.of(leaveId),
                    feedback != null ? feedback : "Approved via email one-click"
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Leave approved successfully"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{leaveId}/attachment")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long leaveId) {
        try {
            logger.info("Downloading attachment for leave ID: {}", leaveId);
            Leave leave = leaveService.getLeaveWithAttachment(leaveId);

            if (!leave.hasAttachment()) {
                logger.warn("No attachment found for leave ID: {}", leaveId);
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(leave.getAttachmentFileType()));
            headers.setContentDispositionFormData("attachment", leave.getAttachmentFileName());
            headers.setContentLength(leave.getAttachmentFileSize());

            logger.info("Successfully downloaded attachment for leave ID: {}", leaveId);
            return new ResponseEntity<>(leave.getAttachmentFileContent(), headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error downloading attachment for leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{leaveId}/attachment/image")
    public ResponseEntity<byte[]> getAttachmentImage(@PathVariable Long leaveId) {
        try {
            logger.info("Fetching image attachment for leave ID: {}", leaveId);
            Leave leave = leaveService.getLeaveWithAttachment(leaveId);

            if (!leave.hasAttachment()) {
                logger.warn("No attachment found for leave ID: {}", leaveId);
                return ResponseEntity.notFound().build();
            }

            String fileType = leave.getAttachmentFileType();
            if (!fileType.startsWith("image/")) {
                logger.warn("Attachment for leave {} is not an image: {}", leaveId, fileType);
                return ResponseEntity.badRequest().body("Attachment is not an image".getBytes());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(fileType));
            headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
            headers.setContentLength(leave.getAttachmentFileSize());

            logger.info("Successfully returned image for leave ID: {}", leaveId);
            return new ResponseEntity<>(leave.getAttachmentFileContent(), headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error fetching image for leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{leaveId}/attachment-info")
    public ResponseEntity<?> getAttachmentInfo(@PathVariable Long leaveId) {
        try {
            logger.info("Getting attachment info for leave ID: {}", leaveId);
            Leave leave = leaveService.getLeaveWithAttachment(leaveId);

            if (!leave.hasAttachment()) {
                logger.info("No attachment found for leave ID: {}", leaveId);
                Map<String, Object> noAttachmentResponse = new HashMap<>();
                noAttachmentResponse.put("hasAttachment", false);
                noAttachmentResponse.put("message", "No attachment found for this leave request");
                return ResponseEntity.ok(noAttachmentResponse);
            }

            Map<String, Object> attachmentInfo = new HashMap<>();
            attachmentInfo.put("fileName", leave.getAttachmentFileName());
            attachmentInfo.put("fileType", leave.getAttachmentFileType());
            attachmentInfo.put("fileSize", leave.getAttachmentFileSize());
            attachmentInfo.put("fileSizeFormatted", formatFileSize(leave.getAttachmentFileSize()));
            attachmentInfo.put("hasAttachment", true);
            attachmentInfo.put("isImage", leave.getAttachmentFileType().startsWith("image/"));
            attachmentInfo.put("isPdf", "application/pdf".equals(leave.getAttachmentFileType()));

            return ResponseEntity.ok(attachmentInfo);
        } catch (Exception e) {
            logger.error("Error getting attachment info for leave {}: {}", leaveId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("hasAttachment", false);
            errorResponse.put("message", "Leave request not found or error retrieving attachment info");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Leave>> getLeavesByEmployee(@PathVariable Long employeeId) {
        try {
            logger.info("Fetching leaves for employee ID: {}", employeeId);
            List<Leave> leaves = leaveService.getLeavesByEmployeeId(employeeId);
            logger.info("Found {} leaves for employee ID: {}", leaves.size(), employeeId);
            return ResponseEntity.ok(leaves);
        } catch (Exception e) {
            logger.error("Error fetching leaves for employee {}: {}", employeeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/supervisor")
    public ResponseEntity<List<Leave>> getSupervisorPendingRequests() {
        try {
            logger.info("Fetching supervisor pending requests");
            List<Leave> pendingRequests = leaveService.getSupervisorPending();
            logger.info("Found {} pending requests for supervisor", pendingRequests.size());
            return ResponseEntity.ok(pendingRequests);
        } catch (Exception e) {
            logger.error("Error fetching supervisor pending requests: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/supervisor/pending")
    public ResponseEntity<List<Leave>> getSupervisorPending() {
        try {
            logger.info("Fetching supervisor pending leaves (alternative endpoint)");
            List<Leave> pendingLeaves = leaveService.getSupervisorPending();
            return ResponseEntity.ok(pendingLeaves);
        } catch (Exception e) {
            logger.error("Error fetching supervisor pending leaves: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/supervisor/approve/{leaveId}")
    public ResponseEntity<?> supervisorApprove(
            @PathVariable Long leaveId,
            @RequestBody(required = false) SupervisorDecisionRequest request) {

        try {
            logger.info("Supervisor approving leave ID: {}", leaveId);
            String feedback = request != null ? request.getFeedback() : "";
            Leave updatedLeave = leaveService.supervisorApprove(leaveId, feedback);
            return ResponseEntity.ok(updatedLeave);
        } catch (Exception e) {
            logger.error("Error supervisor approving leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error approving leave: " + e.getMessage());
        }
    }

    @PostMapping("/{leaveId}/supervisor/approve")
    public ResponseEntity<?> supervisorApproveAlternative(
            @PathVariable Long leaveId,
            @RequestBody(required = false) String feedback) {

        try {
            logger.info("Supervisor approving leave ID: {} (alternative endpoint)", leaveId);
            Leave updatedLeave = leaveService.supervisorApprove(leaveId, feedback != null ? feedback : "");
            return ResponseEntity.ok(updatedLeave);
        } catch (Exception e) {
            logger.error("Error supervisor approving leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error approving leave: " + e.getMessage());
        }
    }

    @PostMapping("/supervisor/reject/{leaveId}")
    public ResponseEntity<Leave> supervisorReject(
            @PathVariable Long leaveId,
            @RequestBody SupervisorDecisionRequest request) {
        try {
            logger.info("Supervisor rejecting leave ID: {}", leaveId);
            Leave updatedLeave = leaveService.rejectLeave(leaveId, "supervisor", request.getFeedback());
            return ResponseEntity.ok(updatedLeave);
        } catch (Exception e) {
            logger.error("Error supervisor rejecting leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/planner")
    public ResponseEntity<List<Leave>> getPlannerPendingRequests() {
        try {
            logger.info("Fetching planner pending requests");
            List<Leave> pendingRequests = leaveService.getPlannerPending();
            logger.info("Found {} pending requests for planner", pendingRequests.size());
            return ResponseEntity.ok(pendingRequests);
        } catch (Exception e) {
            logger.error("Error fetching planner pending requests: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/planner/pending")
    public ResponseEntity<List<Leave>> getPlannerPending() {
        try {
            logger.info("Fetching planner pending leaves (alternative endpoint)");
            List<Leave> pendingLeaves = leaveService.getPlannerPending();
            return ResponseEntity.ok(pendingLeaves);
        } catch (Exception e) {
            logger.error("Error fetching planner pending leaves: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/planner/approve/{leaveId}")
    public ResponseEntity<Leave> plannerApprove(
            @PathVariable Long leaveId,
            @RequestBody PlannerDecisionRequest request) {
        try {
            logger.info("Planner approving leave ID: {}", leaveId);
            Leave updatedLeave = leaveService.plannerApprove(leaveId, request.getFeedback());
            return ResponseEntity.ok(updatedLeave);
        } catch (Exception e) {
            logger.error("Error planner approving leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{leaveId}/planner/approve")
    public ResponseEntity<?> plannerApproveAlternative(
            @PathVariable Long leaveId,
            @RequestBody(required = false) String feedback) {

        try {
            logger.info("Planner approving leave ID: {} (alternative endpoint)", leaveId);
            Leave updatedLeave = leaveService.plannerApprove(leaveId, feedback != null ? feedback : "");
            return ResponseEntity.ok(updatedLeave);
        } catch (Exception e) {
            logger.error("Error planner approving leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error approving leave: " + e.getMessage());
        }
    }

    @GetMapping("/hr")
    public ResponseEntity<List<Leave>> getHrPendingRequests() {
        try {
            logger.info("Fetching HR pending requests");
            List<Leave> pendingRequests = leaveService.getHrPending();
            logger.info("Found {} pending requests for HR", pendingRequests.size());
            return ResponseEntity.ok(pendingRequests);
        } catch (Exception e) {
            logger.error("Error fetching HR pending requests: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/hr/pending")
    public ResponseEntity<List<Leave>> getHrPending() {
        try {
            logger.info("Fetching HR pending leaves (alternative endpoint)");
            List<Leave> pendingLeaves = leaveService.getHrPending();
            return ResponseEntity.ok(pendingLeaves);
        } catch (Exception e) {
            logger.error("Error fetching HR pending leaves: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/hr/approve/{leaveId}")
    public ResponseEntity<Leave> hrApprove(
            @PathVariable Long leaveId,
            @RequestBody HrDecisionRequest request) {
        try {
            logger.info("HR approving leave ID: {}", leaveId);
            Leave updatedLeave = leaveService.hrApprove(leaveId, request.getFeedback());
            return ResponseEntity.ok(updatedLeave);
        } catch (Exception e) {
            logger.error("Error HR approving leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{leaveId}/hr/approve")
    public ResponseEntity<?> hrApproveAlternative(
            @PathVariable Long leaveId,
            @RequestBody(required = false) String feedback) {

        try {
            logger.info("HR approving leave ID: {} (alternative endpoint)", leaveId);
            Leave updatedLeave = leaveService.hrApprove(leaveId, feedback != null ? feedback : "");
            return ResponseEntity.ok(updatedLeave);
        } catch (Exception e) {
            logger.error("Error HR approving leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error approving leave: " + e.getMessage());
        }
    }

    @GetMapping("/check-exists")
    public ResponseEntity<Boolean> checkLeaveExists(
            @RequestParam Long employeeId,
            @RequestParam String date) {
        LocalDate leaveDate = LocalDate.parse(date);
        boolean exists = leaveRepository.existsByEmployeeIdAndDate(employeeId, leaveDate);
        return ResponseEntity.ok(exists);
    }

    @PostMapping("/reject/{leaveId}")
    public ResponseEntity<Leave> rejectLeave(
            @PathVariable Long leaveId,
            @RequestBody RejectRequest request) {
        try {
            logger.info("Rejecting leave ID: {} by role: {}", leaveId, request.getRole());
            Leave updatedLeave = leaveService.rejectLeave(leaveId, request.getRole(), request.getFeedback());
            return ResponseEntity.ok(updatedLeave);
        } catch (Exception e) {
            logger.error("Error rejecting leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Bulk approve leaves
    @PostMapping("/bulk/approve")
    public ResponseEntity<?> bulkApproveLeaves(@RequestBody BulkActionRequest request) {
        try {
            if (request.getLeaveIds() == null || request.getLeaveIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No leave IDs provided"
                ));
            }

            Map<String, Object> result = leaveService.bulkApproveLeaves(
                    request.getLeaveIds(),
                    request.getFeedback() != null ? request.getFeedback() : "Bulk approved"
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully processed " + result.get("processedCount") + " leave requests",
                    "processedCount", result.get("processedCount"),
                    "errors", result.get("errors")
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error processing bulk approval: " + e.getMessage()
            ));
        }
    }

    // Bulk reject leaves
    @PostMapping("/bulk/reject")
    public ResponseEntity<?> bulkRejectLeaves(@RequestBody BulkActionRequest request) {
        try {
            if (request.getLeaveIds() == null || request.getLeaveIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No leave IDs provided"
                ));
            }

            Map<String, Object> result = leaveService.bulkRejectLeaves(
                    request.getLeaveIds(),
                    request.getFeedback() != null ? request.getFeedback() : "Bulk rejected"
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully processed " + result.get("processedCount") + " leave requests",
                    "processedCount", result.get("processedCount"),
                    "errors", result.get("errors")
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error processing bulk rejection: " + e.getMessage()
            ));
        }
    }

    // Delete single leave
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long id) {
        try {
            leaveService.deleteLeave(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Leave request deleted successfully"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error deleting leave: " + e.getMessage()
            ));
        }
    }

    // Bulk delete leaves
    @DeleteMapping("/bulk/delete")
    public ResponseEntity<?> bulkDeleteLeaves(@RequestBody BulkActionRequest request) {
        try {
            if (request.getLeaveIds() == null || request.getLeaveIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No leave IDs provided"
                ));
            }

            Map<String, Object> result = leaveService.bulkDeleteLeaves(request.getLeaveIds());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully deleted " + result.get("deletedCount") + " leave requests",
                    "deletedCount", result.get("deletedCount"),
                    "errors", result.get("errors")
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error processing bulk delete: " + e.getMessage()
            ));
        }
    }

    // Add this DTO class inside your controller
    public static class BulkActionRequest {
        private List<Long> leaveIds;
        private String feedback;

        public List<Long> getLeaveIds() { return leaveIds; }
        public void setLeaveIds(List<Long> leaveIds) { this.leaveIds = leaveIds; }
        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }

    @GetMapping("/admin/employee-balances")
    public ResponseEntity<List<EmployeeLeaveBalanceDTO>> getAllEmployeeLeaveBalances() {
        try {
            logger.info("Fetching leave balances for all active employees");
            List<EmployeeLeaveBalanceDTO> balances = leaveService.getAllEmployeesLeaveBalances();
            return ResponseEntity.ok(balances);
        } catch (Exception e) {
            logger.error("Error fetching employee leave balances: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{leaveId}/reject")
    public ResponseEntity<?> rejectLeaveAlternative(
            @PathVariable Long leaveId,
            @RequestParam String role,
            @RequestBody(required = false) String feedback) {

        try {
            logger.info("Rejecting leave ID: {} by role: {} (alternative endpoint)", leaveId, role);
            Leave updatedLeave = leaveService.rejectLeave(leaveId, role, feedback != null ? feedback : "");
            return ResponseEntity.ok(updatedLeave);
        } catch (Exception e) {
            logger.error("Error rejecting leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error rejecting leave: " + e.getMessage());
        }
    }

    // In LeaveController.java - Add this new public endpoint
    // Add this method to your LeaveController.java

    @PostMapping("/public/bulk-approve/{token}")
    public ResponseEntity<?> publicBulkApprove(
            @PathVariable String token,
            @RequestBody BulkActionRequest request) {

        try {
            logger.info("Public bulk approval request with token for leave IDs: {}", request.getLeaveIds());

            if (request.getLeaveIds() == null || request.getLeaveIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No leave IDs provided"
                ));
            }

            Long leaveId = request.getLeaveIds().get(0);
            Leave leave = leaveService.getLeaveById(leaveId);

            // Verify the token
            if (leave.getApprovalToken() == null || !leave.getApprovalToken().equals(token)) {
                logger.warn("Invalid approval token for leave ID: {}", leaveId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Invalid or expired approval token"
                ));
            }

            // Check if token is expired
            if (!leave.isTokenValid()) {
                logger.warn("Expired approval token for leave ID: {}", leaveId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Approval link has expired. Please request a new one."
                ));
            }

            // Check if already approved
            if ("Approved".equals(leave.getStatus())) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Leave already approved",
                        "alreadyApproved", true
                ));
            }

            // Process bulk approval
            Map<String, Object> result = leaveService.bulkApproveLeaves(
                    request.getLeaveIds(),
                    request.getFeedback() != null ? request.getFeedback() : "Approved via email link"
            );

            // Invalidate the token after use (optional but recommended)
            leave.setApprovalToken(null);
            leave.setTokenExpiryDate(null);
            leaveRepository.save(leave);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Leave approved successfully!",
                    "processedCount", result.get("processedCount"),
                    "errors", result.get("errors")
            ));

        } catch (Exception e) {
            logger.error("Error in public bulk approval: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error processing approval: " + e.getMessage()
            ));
        }
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public static class SupervisorDecisionRequest {
        private String feedback;

        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }

    public static class PlannerDecisionRequest {
        private String feedback;

        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }

    public static class HrDecisionRequest {
        private String feedback;

        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }

    public static class RejectRequest {
        private String role;
        private String feedback;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }
}