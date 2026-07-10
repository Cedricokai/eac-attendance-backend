package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.FileStorageService;
import com.example.eac_backend_project.Service.JobService;
import com.example.eac_backend_project.dto.JobAttachmentDTO;
import com.example.eac_backend_project.dto.JobDTO;
import com.example.eac_backend_project.dto.JobProductDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.JobAttachment;
import com.example.eac_backend_project.entity.attendance.JobProduct;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.JobRepository;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService jobService;
    private final JobRepository jobRepository;
    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorageService;

    public JobController(JobService jobService, JobRepository jobRepository, EmployeeRepository employeeRepository, FileStorageService fileStorageService) {
        this.jobService = jobService;
        this.jobRepository = jobRepository;
        this.employeeRepository = employeeRepository;
        this.fileStorageService = fileStorageService;
    }

    // DTO for adding products to job
    public static class AddProductToJobRequest {
        private Long productId;
        private Integer quantity;
        private String estimatedUsage;

        public AddProductToJobRequest() {}

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getEstimatedUsage() {
            return estimatedUsage;
        }

        public void setEstimatedUsage(String estimatedUsage) {
            this.estimatedUsage = estimatedUsage;
        }
    }

    // DTO for assigning employees to job
    public static class AssignEmployeeRequest {
        private Long employeeId;
        private Boolean isSupervisor;

        public AssignEmployeeRequest() {}

        public Long getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(Long employeeId) {
            this.employeeId = employeeId;
        }

        public Boolean getIsSupervisor() {
            return isSupervisor;
        }

        public void setIsSupervisor(Boolean isSupervisor) {
            this.isSupervisor = isSupervisor;
        }
    }

    // DTO for bulk employee assignment
    public static class BulkAssignEmployeesRequest {
        private List<Long> employeeIds;
        private Long supervisorId;

        public BulkAssignEmployeesRequest() {}

        public List<Long> getEmployeeIds() {
            return employeeIds;
        }

        public void setEmployeeIds(List<Long> employeeIds) {
            this.employeeIds = employeeIds;
        }

        public Long getSupervisorId() {
            return supervisorId;
        }

        public void setSupervisorId(Long supervisorId) {
            this.supervisorId = supervisorId;
        }
    }

    // ==================== JOB CRUD ENDPOINTS ====================

    @GetMapping
    public ResponseEntity<List<JobDTO>> getAllJobs() {
        try {
            List<Job> jobs = jobService.getAllJobs();
            List<JobDTO> jobDTOs = jobs.stream()
                    .map(JobDTO::new)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(jobDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDTO> getJobById(@PathVariable Long id) {
        try {
            Optional<Job> job = jobService.getJobById(id);
            return job.map(j -> ResponseEntity.ok(new JobDTO(j)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody Job job) {
        try {
            Job createdJob = jobService.createJob(job);
            return ResponseEntity.status(HttpStatus.CREATED).body(new JobDTO(createdJob));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating job: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @RequestBody Job jobDetails) {
        try {
            System.out.println("=== UPDATE JOB REQUEST ===");
            System.out.println("Job ID: " + id);
            System.out.println("Job Details: " + jobDetails);

            System.out.println("Received update request for job " + id);
            System.out.println("billingReminderEmails in request: " + jobDetails.getBillingReminderEmails());
            System.out.println("--- INCOMING reminderTimeHour: " + jobDetails.getReminderTimeHour());
            System.out.println("--- INCOMING reminderTimeMinute: " + jobDetails.getReminderTimeMinute());

            Job updatedJob = jobService.updateJob(id, jobDetails);

            System.out.println("Job updated successfully: " + updatedJob.getId());
            return ResponseEntity.ok(new JobDTO(updatedJob));

        } catch (RuntimeException e) {
            System.out.println("RuntimeException: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating job: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id,
                                       @RequestParam(defaultValue = "false") boolean deleteTimesheets) {
        jobService.deleteJob(id, deleteTimesheets);
        return ResponseEntity.ok().build();
    }
    // ==================== EMPLOYEE ASSIGNMENT ENDPOINTS ====================

    // Assign a single employee to a job
    @PostMapping("/{jobId}/assign-employee")
    public ResponseEntity<?> assignEmployeeToJob(
            @PathVariable Long jobId,
            @RequestBody AssignEmployeeRequest request) {
        try {
            if (request.getEmployeeId() == null) {
                return ResponseEntity.badRequest().body("Employee ID is required");
            }

            Employee employee = jobService.assignEmployeeToJob(
                    jobId,
                    request.getEmployeeId(),
                    request.getIsSupervisor() != null ? request.getIsSupervisor() : false
            );

            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error assigning employee to job: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/send-billing-reminder")
    public ResponseEntity<?> sendBillingReminder(@PathVariable Long id) {
        try {
            jobService.sendManualBillingReminder(id);
            return ResponseEntity.ok(Map.of("message", "Billing reminder sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/send-all-billing-reminders")
    public ResponseEntity<?> sendAllBillingReminders() {
        jobService.sendAllBillingRemindersManually();
        return ResponseEntity.ok(Map.of("message", "All billing reminders sent"));
    }

    // Bulk assign multiple employees to a job
    @PostMapping("/{jobId}/assign-employees")
    public ResponseEntity<?> bulkAssignEmployeesToJob(
            @PathVariable Long jobId,
            @RequestBody BulkAssignEmployeesRequest request) {
        try {
            if (request.getEmployeeIds() == null || request.getEmployeeIds().isEmpty()) {
                return ResponseEntity.badRequest().body("Employee IDs are required");
            }

            List<Employee> assignedEmployees = jobService.bulkAssignEmployeesToJob(
                    jobId,
                    request.getEmployeeIds(),
                    request.getSupervisorId()
            );

            return ResponseEntity.ok(assignedEmployees);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error bulk assigning employees to job: " + e.getMessage());
        }
    }

    // Remove employee from job
    @DeleteMapping("/{jobId}/employees/{employeeId}")
    public ResponseEntity<?> removeEmployeeFromJob(
            @PathVariable Long jobId,
            @PathVariable Long employeeId) {
        try {
            Employee employee = jobService.removeEmployeeFromJob(jobId, employeeId);
            return ResponseEntity.ok(employee);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing employee from job: " + e.getMessage());
        }
    }

    // Set supervisor for job
    @PostMapping("/{jobId}/supervisor")
    public ResponseEntity<?> setJobSupervisor(
            @PathVariable Long jobId,
            @RequestBody Map<String, Long> request) {
        try {
            Long employeeId = request.get("employeeId");
            if (employeeId == null) {
                return ResponseEntity.badRequest().body("Employee ID is required");
            }

            Job job = jobService.setJobSupervisor(jobId, employeeId);
            return ResponseEntity.ok(new JobDTO(job));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error setting job supervisor: " + e.getMessage());
        }
    }

    // Remove supervisor from job
    @DeleteMapping("/{jobId}/supervisor")
    public ResponseEntity<?> removeJobSupervisor(@PathVariable Long jobId) {
        try {
            Job job = jobService.removeJobSupervisor(jobId);
            return ResponseEntity.ok(new JobDTO(job));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing job supervisor: " + e.getMessage());
        }
    }

    // Get assigned employees for a job
    @GetMapping("/{jobId}/employees")
    public ResponseEntity<?> getJobEmployees(@PathVariable Long jobId) {
        try {
            List<Employee> employees = jobService.getJobEmployees(jobId);
            return ResponseEntity.ok(employees);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching job employees: " + e.getMessage());
        }
    }

    // ==================== JOB MANAGEMENT ENDPOINTS ====================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Job>> getJobsByStatus(@PathVariable Job.JobStatus status) {
        try {
            List<Job> jobs = jobService.getJobsByStatus(status);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Job>> searchJobs(@RequestParam String q) {
        try {
            List<Job> jobs = jobService.searchJobs(q);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<Job>> getActiveJobs() {
        try {
            List<Job> jobs = jobService.getActiveJobs();
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // In JobController.java

    @PostMapping("/{id}/attachments")
    public ResponseEntity<?> addAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("documentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate documentDate) {
        try {
            JobAttachment attachment = jobService.addAttachment(id, file, description, documentDate);
            return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
        } catch (IOException e) {
            // Log the error
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving file: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading attachment: " + e.getMessage());
        }
    }

    @GetMapping("/debug/all")
    public ResponseEntity<?> debugAllJobs() {
        try {
            List<Job> allJobs = jobRepository.findAll();

            System.out.println("=== DEBUG: ALL JOBS IN DATABASE ===");
            for (Job job : allJobs) {
                System.out.println("Job ID: " + job.getId() + ", Name: " + job.getName() + ", Client: " + job.getClient());
            }
            System.out.println("Total jobs: " + allJobs.size());

            Map<String, Object> response = new HashMap<>();
            response.put("total", allJobs.size());
            response.put("jobs", allJobs.stream()
                    .map(job -> Map.of(
                            "id", job.getId(),
                            "name", job.getName(),
                            "client", job.getClient(),
                            "status", job.getStatus()
                    ))
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<JobAttachmentDTO>> getAttachments(@PathVariable Long id) {
        try {
            List<JobAttachment> attachments = jobService.getJobAttachments(id);
            List<JobAttachmentDTO> attachmentDTOs = attachments.stream()
                    .map(JobAttachmentDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(attachmentDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long attachmentId) {
        try {
            jobService.deleteAttachment(attachmentId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting attachment: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateJobStatus(@PathVariable Long id, @RequestParam Job.JobStatus status) {
        try {
            Job updatedJob = jobService.updateJobStatus(id, status);
            return ResponseEntity.ok(updatedJob);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating job status: " + e.getMessage());
        }
    }

    @GetMapping("/stats/active-count")
    public ResponseEntity<Long> getActiveJobsCount() {
        try {
            Long count = jobService.getActiveJobsCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/with-budget")
    public ResponseEntity<List<Job>> getJobsWithBudget() {
        try {
            List<Job> jobs = jobService.getJobsWithBudget();
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== JOB PRODUCTS ENDPOINTS ====================

    @GetMapping("/{jobId}/products")
    public ResponseEntity<List<JobProductDTO>> getJobProducts(@PathVariable Long jobId) {
        try {
            List<JobProduct> jobProducts = jobService.getJobProducts(jobId);
            List<JobProductDTO> dtos = jobProducts.stream()
                    .map(JobProductDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{jobId}/products")
    public ResponseEntity<?> addProductToJob(
            @PathVariable Long jobId,
            @RequestBody AddProductToJobRequest request) {
        try {
            if (request.getProductId() == null || request.getQuantity() == null) {
                return ResponseEntity.badRequest().body("Product ID and quantity are required");
            }

            JobProduct jobProduct = jobService.addProductToJob(
                    jobId,
                    request.getProductId(),
                    request.getQuantity(),
                    request.getEstimatedUsage()
            );

            // Return DTO instead of entity
            JobProductDTO dto = new JobProductDTO(jobProduct);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding product to job: " + e.getMessage());
        }
    }

    @DeleteMapping("/products/{jobProductId}")
    public ResponseEntity<?> removeProductFromJob(@PathVariable Long jobProductId) {
        try {
            jobService.removeProductFromJob(jobProductId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing product from job: " + e.getMessage());
        }
    }

    @PutMapping("/products/{jobProductId}/quantity")
    public ResponseEntity<?> updateProductQuantity(
            @PathVariable Long jobProductId,
            @RequestParam Integer quantity) {
        try {
            jobService.updateProductQuantity(jobProductId, quantity);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating product quantity: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<?> getJobWithDetails(@PathVariable Long id) {
        try {
            Optional<Job> job = jobService.getJobById(id);
            if (job.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<JobProduct> products = jobService.getJobProducts(id);
            List<JobAttachment> attachments = jobService.getJobAttachments(id);

            JobDetailsResponse response = new JobDetailsResponse(job.get(), products, attachments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching job details: " + e.getMessage());
        }
    }

    // Response DTO for job details
    public static class JobDetailsResponse {
        private Job job;
        private List<JobProduct> products;
        private List<JobAttachment> attachments;

        public JobDetailsResponse(Job job, List<JobProduct> products, List<JobAttachment> attachments) {
            this.job = job;
            this.products = products;
            this.attachments = attachments;
        }

        public Job getJob() {
            return job;
        }

        public void setJob(Job job) {
            this.job = job;
        }

        public List<JobProduct> getProducts() {
            return products;
        }

        public void setProducts(List<JobProduct> products) {
            this.products = products;
        }

        public List<JobAttachment> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<JobAttachment> attachments) {
            this.attachments = attachments;
        }
    }

    // Add this method to JobController
    @GetMapping("/attachments/{attachmentId}/preview")
    public ResponseEntity<?> previewAttachment(@PathVariable Long attachmentId) {
        try {
            // Get attachment from database
            JobAttachment attachment = jobService.getAttachmentById(attachmentId);
            if (attachment == null) {
                return ResponseEntity.notFound().build();
            }

            // Load file as resource
            Resource resource = fileStorageService.loadFileAsResource(attachment.getFilePath());

            // Determine content type
            String contentType = attachment.getFileType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = fileStorageService.getFileMimeType(attachment.getFilePath());
            }
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }

            // Check if file can be previewed in browser
            boolean canPreview = canPreviewInBrowser(contentType);

            if (canPreview) {
                // For previewable files, return with inline disposition
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")
                        .body(resource);
            } else {
                // For non-previewable files, force download
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")
                        .body(resource);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error previewing file: " + e.getMessage());
        }
    }

    // Helper method to determine if file can be previewed in browser
    private boolean canPreviewInBrowser(String contentType) {
        if (contentType == null) return false;

        return contentType.contains("application/pdf") ||
                contentType.contains("image/") ||
                contentType.contains("text/") ||
                contentType.contains("application/json") ||
                contentType.contains("application/xml");
    }
}