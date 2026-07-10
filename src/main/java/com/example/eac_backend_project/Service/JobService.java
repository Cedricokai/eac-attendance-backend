package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.attendance.JobAttachment;
import com.example.eac_backend_project.entity.attendance.JobProduct;
import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobAttachmentRepository attachmentRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JobProductRepository jobProductRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CostCenterService costCenterService;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Autowired
    private TimesheetService timesheetService;

    @Autowired
    private CostTransactionRepository costTransactionRepository;

    @Autowired
    private JobEmployeeRateService jobEmployeeRateService;   // NEW

    @Autowired
    private  JobAttachmentRepository jobAttachmentRepository;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private String getFrontendBaseUrl() {
        return "http://localhost:5173";
    }

    private static final String UPLOAD_DIR = "uploads/jobs/";

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

    public Job createJob(Job job) {
        if (job.getStandardWorkHours() == null) {
            job.setStandardWorkHours(8);
        }

        System.out.println("=== CREATING JOB WITH ===");
        System.out.println("billingReminderEnabled: " + job.isBillingReminderEnabled());
        System.out.println("billingReminderEmails: " + job.getBillingReminderEmails());
        System.out.println("enableAdvanceReminders: " + job.getEnableAdvanceReminders());
        System.out.println("reminderDaysBefore: " + job.getReminderDaysBefore());
        System.out.println("reminderTimeHour: " + job.getReminderTimeHour());
        System.out.println("reminderTimeMinute: " + job.getReminderTimeMinute());

        if (job.getEnableAdvanceReminders() == null) {
            job.setEnableAdvanceReminders(true);
        }
        if (job.getReminderDaysBefore() == null || job.getReminderDaysBefore().isEmpty()) {
            job.setReminderDaysBefore("7,3,1");
        }
        if (job.getReminderTimeHour() == null) {
            job.setReminderTimeHour(8);
        }
        if (job.getReminderTimeMinute() == null) {
            job.setReminderTimeMinute(0);
        }
        if (job.getEnableOverdueReminders() == null) {
            job.setEnableOverdueReminders(true);
        }
        if (job.getOverdueReminderDays() == null) {
            job.setOverdueReminderDays(1);
        }

        if (job.getBillingAnchorDay() != null && job.getBillingAnchorDay() > 0) {
            job.setBillOn15NextMonth(false);
            System.out.println("Anchor day billing enabled for day: " + job.getBillingAnchorDay());
        } else if (job.isBillOn15NextMonth()) {
            job.setBillingAnchorDay(null);
        }

        jobRepository.findByNameAndClient(job.getName(), job.getClient())
                .ifPresent(existingJob -> {
                    throw new RuntimeException("Job with name '" + job.getName() + "' for client '" + job.getClient() + "' already exists");
                });

        ensureNextBillingDate(job);

        if (job.isBillingReminderEnabled() && (job.getBillingReminderEmails() == null || job.getBillingReminderEmails().trim().isEmpty())) {
            System.out.println("WARNING: Billing reminder enabled but no emails provided. Disabling reminders.");
            job.setBillingReminderEnabled(false);
        }

        Job savedJob = jobRepository.save(job);

        System.out.println("✅ Job created with ID: " + savedJob.getId());
        System.out.println("   - Name: " + savedJob.getName());
        System.out.println("   - Reminder Enabled: " + savedJob.isBillingReminderEnabled());
        System.out.println("   - Reminder Emails: " + savedJob.getBillingReminderEmails());
        System.out.println("   - Next Billing Date: " + savedJob.getNextBillingDate());

        return savedJob;
    }

    public Job updateJob(Long id, Job jobDetails) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));

        System.out.println("=== UPDATING JOB ID: " + id + " ===");
        System.out.println("New billingReminderEnabled: " + jobDetails.isBillingReminderEnabled());
        System.out.println("New billingReminderEmails: " + jobDetails.getBillingReminderEmails());
        System.out.println("Current billingReminderEmails in DB: " + job.getBillingReminderEmails());

        if (jobDetails.getName() != null && jobDetails.getClient() != null) {
            jobRepository.findByNameAndClient(jobDetails.getName(), jobDetails.getClient())
                    .ifPresent(existingJob -> {
                        if (!existingJob.getId().equals(id)) {
                            throw new RuntimeException("Job with name '" + jobDetails.getName() + "' for client '" + jobDetails.getClient() + "' already exists");
                        }
                    });
        }

        if (jobDetails.getName() != null) job.setName(jobDetails.getName());
        if (jobDetails.getDescription() != null) job.setDescription(jobDetails.getDescription());
        if (jobDetails.getClient() != null) job.setClient(jobDetails.getClient());
        if (jobDetails.getStartDate() != null) job.setStartDate(jobDetails.getStartDate());
        if (jobDetails.getEndDate() != null) job.setEndDate(jobDetails.getEndDate());
        if (jobDetails.getBillingCycle() != null) job.setBillingCycle(jobDetails.getBillingCycle());
        if (jobDetails.getStatus() != null) job.setStatus(jobDetails.getStatus());
        if (jobDetails.getBudget() != null) job.setBudget(jobDetails.getBudget());
        if (jobDetails.getHourlyRate() != null) job.setHourlyRate(jobDetails.getHourlyRate());

        if (jobDetails.getStandardWorkHours() != null) {
            job.setStandardWorkHours(jobDetails.getStandardWorkHours());
        } else if (job.getStandardWorkHours() == null) {
            job.setStandardWorkHours(8);
        }

        if (jobDetails.getOvertimeMultiplier() != null) {
            job.setOvertimeMultiplier(jobDetails.getOvertimeMultiplier());
        }
       
        job.setBillOn15NextMonth(jobDetails.isBillOn15NextMonth());
        job.setBillingReminderEnabled(jobDetails.isBillingReminderEnabled());

        if (jobDetails.getBillingReminderEmails() != null) {
            job.setBillingReminderEmails(jobDetails.getBillingReminderEmails());
            System.out.println("📧 Updated billing reminder emails to: " + jobDetails.getBillingReminderEmails());
        } else {
            System.out.println("📧 Keeping existing billing reminder emails: " + job.getBillingReminderEmails());
        }

        if (jobDetails.getEnableAdvanceReminders() != null) {
            job.setEnableAdvanceReminders(jobDetails.getEnableAdvanceReminders());
        }
        if (jobDetails.getReminderDaysBefore() != null && !jobDetails.getReminderDaysBefore().isEmpty()) {
            job.setReminderDaysBefore(jobDetails.getReminderDaysBefore());
        }
        if (jobDetails.getReminderTimeHour() != null) {
            job.setReminderTimeHour(jobDetails.getReminderTimeHour());
        }
        if (jobDetails.getReminderTimeMinute() != null) {
            job.setReminderTimeMinute(jobDetails.getReminderTimeMinute());
        }
        if (jobDetails.getEnableOverdueReminders() != null) {
            job.setEnableOverdueReminders(jobDetails.getEnableOverdueReminders());
        }
        if (jobDetails.getOverdueReminderDays() != null) {
            job.setOverdueReminderDays(jobDetails.getOverdueReminderDays());
        }

        if (jobDetails.getBillingAnchorDay() != null) {
            job.setBillingAnchorDay(jobDetails.getBillingAnchorDay());
            if (jobDetails.getBillingAnchorDay() > 0) {
                job.setBillOn15NextMonth(false);
            }
        } else if (jobDetails.isBillOn15NextMonth()) {
            job.setBillOn15NextMonth(true);
            job.setBillingAnchorDay(null);
        }

        job.setNextBillingDate(computeInitialNextBillingDate(job));
        ensureNextBillingDate(job);

        Job savedJob = jobRepository.save(job);

        System.out.println("✅ Job updated - Reminder Enabled: " + savedJob.isBillingReminderEnabled());
        System.out.println("✅ Job updated - Reminder Emails: " + savedJob.getBillingReminderEmails());

        return savedJob;
    }

    // ==================== BILLING REMINDER METHODS ====================

    @Transactional
    public void sendManualBillingReminder(Long jobId) {
        Job job = getJobById(jobId).orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        if (!job.isBillingReminderEnabled()) {
            throw new RuntimeException("Billing reminders are not enabled for this job");
        }
        sendBillingDueReminder(job);
        System.out.println("📧 Manual billing reminder sent for job: " + job.getName());
    }

    @Transactional
    public void sendAllBillingRemindersManually() {
        List<Job> activeJobs = jobRepository.findAllActiveJobsWithBillingReminderEnabled();
        int sentCount = 0;
        for (Job job : activeJobs) {
            try {
                sendBillingDueReminder(job);
                sentCount++;
            } catch (Exception e) {
                System.err.println("Failed to send reminder for job " + job.getId() + ": " + e.getMessage());
            }
        }
        System.out.println("Manually sent billing reminders to " + sentCount + " jobs");
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkAndSendBillingReminders() {
        LocalDate today = LocalDate.now();
        System.out.println("Running billing reminder check for: " + today);
        List<Job> jobsDueToday = jobRepository.findByNextBillingDateAndBillingReminderEnabledTrue(today);
        if (jobsDueToday.isEmpty()) {
            System.out.println("No billing reminders due today.");
            return;
        }
        System.out.println("Found " + jobsDueToday.size() + " job(s) with billing due today.");
        for (Job job : jobsDueToday) {
            try {
                sendBillingDueReminder(job);
                LocalDate nextBillingDate = calculateNextBillingDate(job);
                job.setNextBillingDate(nextBillingDate);
                jobRepository.save(job);
                System.out.println("Sent billing reminder for job: " + job.getName() +
                        " (Next due: " + nextBillingDate + ")");
            } catch (Exception e) {
                System.err.println("Failed to process billing reminder for job " + job.getId() +
                        " (" + job.getName() + "): " + e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void checkAndSendOverdueBillingReminders() {
        LocalDate today = LocalDate.now();
        List<Job> overdueJobs = jobRepository.findOverdueBillingReminders(today);
        if (overdueJobs.isEmpty()) {
            return;
        }
        for (Job job : overdueJobs) {
            if (job.getNextBillingDate() != null && job.getNextBillingDate().isBefore(today)) {
                try {
                    sendBillingOverdueReminder(job);
                    System.out.println("Sent OVERDUE billing reminder for job: " + job.getName() +
                            " (Due: " + job.getNextBillingDate() + ")");
                } catch (Exception e) {
                    System.err.println("Failed to send overdue reminder for job " + job.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void checkAndSendScheduledReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();
        System.out.println("🔔 Running scheduled reminder check at: " + now);
        List<Job> activeJobs = jobRepository.findAllActiveJobsWithBillingReminderEnabled();
        for (Job job : activeJobs) {
            if (job.getNextBillingDate() == null) continue;
            boolean timeMatches = job.getReminderTimeHour() != null &&
                    job.getReminderTimeMinute() != null &&
                    job.getReminderTimeHour() == currentHour &&
                    Math.abs(job.getReminderTimeMinute() - currentMinute) <= 7;
            if (!timeMatches) continue;
            long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, job.getNextBillingDate());
            if (job.getEnableAdvanceReminders() != null && job.getEnableAdvanceReminders()) {
                String[] reminderDays = job.getReminderDaysBefore().split(",");
                for (String dayStr : reminderDays) {
                    try {
                        int days = Integer.parseInt(dayStr.trim());
                        if (daysUntilDue == days && days > 0) {
                            sendAdvanceReminder(job, days);
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid reminder day format: " + dayStr);
                    }
                }
            }
            if (daysUntilDue == 0) {
                sendBillingDueReminder(job);
            }
            if (job.getEnableOverdueReminders() != null && job.getEnableOverdueReminders() && daysUntilDue < 0) {
                int daysOverdue = Math.abs((int) daysUntilDue);
                if (daysOverdue == job.getOverdueReminderDays()) {
                    sendBillingOverdueReminder(job);
                }
            }
        }
    }

    private void sendAdvanceReminder(Job job, int daysUntilDue) {
        String recipients = job.getBillingReminderEmails();
        if (recipients == null || recipients.trim().isEmpty()) {
            System.out.println("No billing reminder emails configured for job: " + job.getName());
            return;
        }
        String[] emailList = recipients.split(",");
        String subject = "📅 Billing Reminder: " + job.getName() + " - Due in " + daysUntilDue + " days";
        String htmlContent = buildAdvanceReminderEmailHtml(job, daysUntilDue);
        for (String email : emailList) {
            String trimmedEmail = email.trim();
            if (!trimmedEmail.isEmpty()) {
                try {
                    emailService.sendCustomEmail(trimmedEmail, subject, htmlContent);
                    System.out.println("Sent " + daysUntilDue + "-day advance reminder to: " + trimmedEmail);
                } catch (Exception e) {
                    System.err.println("Failed to send advance reminder to " + trimmedEmail + ": " + e.getMessage());
                }
            }
        }
    }

    private String buildAdvanceReminderEmailHtml(Job job, int daysUntilDue) {
        String jobName = safe(job.getName());
        String client = safe(job.getClient());
        String dueDate = formatBillingDate(job.getNextBillingDate());
        String jobLink = getFrontendBaseUrl() + "/jobs/" + job.getId();
        return ""
                + "<div style='font-family: Arial, sans-serif; line-height:1.5;'>"
                + "<h2 style='margin:0 0 10px 0;'>📅 Advance Billing Reminder</h2>"
                + "<p>This is an advance reminder that billing will be due for the job below in <strong>" + daysUntilDue + " days</strong>:</p>"
                + "<ul>"
                + "<li><b>Job:</b> <a href='" + jobLink + "' style='color: #2563eb; text-decoration: none;'>" + escapeHtml(jobName) + "</a></li>"
                + "<li><b>Client:</b> " + escapeHtml(client) + "</li>"
                + "<li><b>Due Date:</b> " + dueDate + "</li>"
                + "</ul>"
                + "<div style='margin: 20px 0; padding: 15px; background-color: #f3f4f6; border-radius: 8px; text-align: center;'>"
                + "<a href='" + jobLink + "' style='display: inline-block; padding: 10px 20px; background-color: #2563eb; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>"
                + "🔗 View Job Details</a>"
                + "</div>"
                + "<p>Please begin preparing for this upcoming billing cycle.</p>"
                + "<p>Best regards,<br><strong>EAC Backend Team</strong></p>"
                + "</div>";
    }

    private void sendBillingDueReminder(Job job) {
        String recipients = job.getBillingReminderEmails();
        if (recipients == null || recipients.trim().isEmpty()) {
            System.out.println("No billing reminder emails configured for job: " + job.getName());
            return;
        }
        String[] emailList = recipients.split(",");
        String subject = "🔔 Billing Reminder: " + job.getName() + " - Due " + formatBillingDate(job.getNextBillingDate());
        String htmlContent = buildBillingDueEmailHtml(job);
        for (String email : emailList) {
            String trimmedEmail = email.trim();
            if (!trimmedEmail.isEmpty()) {
                try {
                    emailService.sendCustomEmail(trimmedEmail, subject, htmlContent);
                    System.out.println("Sent billing reminder to: " + trimmedEmail + " for job: " + job.getName());
                } catch (Exception e) {
                    System.err.println("Failed to send billing reminder to " + trimmedEmail + ": " + e.getMessage());
                }
            }
        }
    }

    private void sendBillingOverdueReminder(Job job) {
        String recipients = job.getBillingReminderEmails();
        if (recipients == null || recipients.trim().isEmpty()) {
            return;
        }
        String[] emailList = recipients.split(",");
        String subject = "⚠️ OVERDUE: Billing Reminder for " + job.getName() + " - Due " + formatBillingDate(job.getNextBillingDate());
        String htmlContent = buildBillingOverdueEmailHtml(job);
        for (String email : emailList) {
            String trimmedEmail = email.trim();
            if (!trimmedEmail.isEmpty()) {
                try {
                    emailService.sendCustomEmail(trimmedEmail, subject, htmlContent);
                } catch (Exception e) {
                    System.err.println("Failed to send overdue reminder to " + trimmedEmail + ": " + e.getMessage());
                }
            }
        }
    }

    private void notifyNewBillingRecipients(
            Job job,
            boolean oldEnabled, String oldEmails,
            boolean newEnabled, String newEmails
    ) {
        if (!newEnabled) return;
        Set<String> oldSet = parseRecipientSet(oldEmails);
        Set<String> newSet = parseRecipientSet(newEmails);
        Set<String> newlyAdded = new HashSet<>();
        if (!oldEnabled && newEnabled) {
            newlyAdded.addAll(newSet);
        } else {
            newlyAdded.addAll(newSet);
            newlyAdded.removeAll(oldSet);
        }
        if (newlyAdded.isEmpty()) return;
        for (String recipient : newlyAdded) {
            try {
                String subject = "You have been enlisted for billing reminders - " + safe(job.getName());
                String html = buildBillingEnlistedEmailHtml(job);
                emailService.sendCustomEmail(recipient, subject, html);
            } catch (Exception e) {
                System.err.println("Failed to send enlisted email to " + recipient + ": " + e.getMessage());
            }
        }
    }

    private Set<String> parseRecipientSet(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private String buildBillingEnlistedEmailHtml(Job job) {
        String jobName = safe(job.getName());
        String client = safe(job.getClient());
        return ""
                + "<div style='font-family: Arial, sans-serif; line-height:1.5;'>"
                + "<h2 style='margin:0 0 10px 0;'>Billing Reminder Enlistment</h2>"
                + "<p>You have been enlisted to receive billing reminders for the job below:</p>"
                + "<ul>"
                + "<li><b>Job:</b> " + escapeHtml(jobName) + "</li>"
                + "<li><b>Client:</b> " + escapeHtml(client) + "</li>"
                + "<li><b>Next Due Date:</b> " + (job.getNextBillingDate() != null ? job.getNextBillingDate() : "N/A") + "</li>"
                + "</ul>"
                + "<p>If you believe you received this in error, please contact your administrator.</p>"
                + "</div>";
    }

    private String buildBillingDueEmailHtml(Job job) {
        String jobName = safe(job.getName());
        String client = safe(job.getClient());
        String dueDate = formatBillingDate(job.getNextBillingDate());
        String billingCycle = job.getBillingCycle() != null ? job.getBillingCycle().toString() : "N/A";
        Double budget = job.getBudget();
        String budgetStr = budget != null ? String.format("₵%.2f", budget) : "Not specified";
        String jobLink = getFrontendBaseUrl() + "/jobs/" + job.getId();
        return ""
                + "<div style='font-family: Arial, sans-serif; line-height:1.5;'>"
                + "<h2 style='margin:0 0 10px 0;'>📅 Billing Reminder</h2>"
                + "<p>This is a friendly reminder that billing is due for the job below:</p>"
                + "<ul>"
                + "<li><b>Job:</b> <a href='" + jobLink + "' style='color: #2563eb; text-decoration: none;'>" + escapeHtml(jobName) + "</a></li>"
                + "<li><b>Client:</b> " + escapeHtml(client) + "</li>"
                + "<li><b>Billing Cycle:</b> " + billingCycle + "</li>"
                + "<li><b>Budget:</b> " + budgetStr + "</li>"
                + "<li><b>Due Date:</b> " + dueDate + "</li>"
                + "</ul>"
                + "<div style='margin: 20px 0; padding: 15px; background-color: #f3f4f6; border-radius: 8px; text-align: center;'>"
                + "<a href='" + jobLink + "' style='display: inline-block; padding: 10px 20px; background-color: #2563eb; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>"
                + "🔗 View Job Details</a>"
                + "</div>"
                + "<p>Please ensure the timesheet for this billing period is completed and ready for invoicing.</p>"
                + "<p>Best regards,<br><strong>EAC Backend Team</strong></p>"
                + "</div>";
    }

    private String buildBillingOverdueEmailHtml(Job job) {
        String jobName = safe(job.getName());
        String client = safe(job.getClient());
        String dueDate = formatBillingDate(job.getNextBillingDate());
        String jobLink = getFrontendBaseUrl() + "/jobs/" + job.getId();
        return ""
                + "<div style='font-family: Arial, sans-serif; line-height:1.5;'>"
                + "<h2 style='margin:0 0 10px 0; color: #dc2626;'>⚠️ OVERDUE Billing Notice</h2>"
                + "<p>The billing for the following job is now past due. Please take immediate action:</p>"
                + "<ul>"
                + "<li><b>Job:</b> <a href='" + jobLink + "' style='color: #2563eb; text-decoration: none;'>" + escapeHtml(jobName) + "</a></li>"
                + "<li><b>Client:</b> " + escapeHtml(client) + "</li>"
                + "<li><b>Due Date (Past):</b> " + dueDate + "</li>"
                + "</ul>"
                + "<div style='margin: 20px 0; padding: 15px; background-color: #fef2f2; border-radius: 8px; text-align: center; border: 1px solid #dc2626;'>"
                + "<a href='" + jobLink + "' style='display: inline-block; padding: 10px 20px; background-color: #dc2626; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>"
                + "🔗 View Job Details & Take Action</a>"
                + "</div>"
                + "<p>Please complete timesheet processing and generate invoice immediately.</p>"
                + "<p>Best regards,<br><strong>EAC Backend Team</strong></p>"
                + "</div>";
    }

    private String formatBillingDate(LocalDate date) {
        if (date == null) return "Not scheduled";
        return date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    private LocalDate calculateNextBillingDate(Job job) {
        if (job.getNextBillingDate() == null) {
            return computeInitialNextBillingDate(job);
        }
        LocalDate currentDue = job.getNextBillingDate();
        LocalDate newDue = null;
        if (job.isAnchorDayBilling()) {
            int anchorDay = job.getBillingAnchorDay();
            newDue = currentDue.plusMonths(1).withDayOfMonth(Math.min(anchorDay, currentDue.plusMonths(1).lengthOfMonth()));
            System.out.println("Anchor day billing: " + anchorDay + " → new due: " + newDue);
        } else if (job.isBillOn15NextMonth()) {
            newDue = currentDue.plusMonths(1).withDayOfMonth(15);
        } else if (job.getBillingCycle() != null) {
            switch (job.getBillingCycle()) {
                case WEEKLY:
                    newDue = currentDue.plusWeeks(1);
                    break;
                case BI_WEEKLY:
                    newDue = currentDue.plusWeeks(2);
                    break;
                case MONTHLY:
                    newDue = currentDue.plusMonths(1);
                    break;
                case QUARTERLY:
                    newDue = currentDue.plusMonths(3);
                    break;
                default:
                    newDue = currentDue.plusMonths(1);
            }
        } else {
            newDue = currentDue.plusMonths(1);
        }
        if (job.getEndDate() != null && newDue != null && newDue.isAfter(job.getEndDate())) {
            return null;
        }
        return newDue;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private LocalDate computeInitialNextBillingDate(Job job) {
        if (job.getStartDate() == null) {
            System.out.println("No start date, cannot compute next billing date");
            return null;
        }
        LocalDate start = job.getStartDate();
        LocalDate due = null;
        if (job.isAnchorDayBilling()) {
            int anchorDay = job.getBillingAnchorDay();
            LocalDate candidateDue = start.withDayOfMonth(Math.min(anchorDay, start.lengthOfMonth()));
            if (candidateDue.isBefore(start) || candidateDue.isEqual(start)) {
                due = start.plusMonths(1).withDayOfMonth(Math.min(anchorDay, start.plusMonths(1).lengthOfMonth()));
            } else {
                due = candidateDue;
            }
            System.out.println("Anchor day billing initial: anchorDay=" + anchorDay + ", start=" + start + ", due=" + due);
        } else if (job.isBillOn15NextMonth()) {
            due = start.plusMonths(1).withDayOfMonth(15);
            System.out.println("Bill on 15th next month: " + due);
        } else if (job.getBillingCycle() != null) {
            switch (job.getBillingCycle()) {
                case WEEKLY:
                    due = start.plusWeeks(1);
                    break;
                case BI_WEEKLY:
                    due = start.plusWeeks(2);
                    break;
                case MONTHLY:
                    due = start.plusMonths(1);
                    break;
                case QUARTERLY:
                    due = start.plusMonths(3);
                    break;
                default:
                    due = start.plusMonths(1);
            }
            System.out.println("Calculated due date based on " + job.getBillingCycle() + ": " + due);
        } else {
            due = start.plusMonths(1);
        }
        return clampToEndDate(job, due);
    }

    @Transactional
    public void scheduleNotificationsForJob(Long jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null || !job.isBillingReminderEnabled()) {
            return;
        }
        System.out.println("Manual scheduling requested for job: " + job.getName());
    }

    private LocalDate clampToEndDate(Job job, LocalDate due) {
        if (due == null) return null;
        if (job.getEndDate() != null && due.isAfter(job.getEndDate())) return null;
        return due;
    }

    private void ensureNextBillingDate(Job job) {
        if (job.getNextBillingDate() == null) {
            job.setNextBillingDate(computeInitialNextBillingDate(job));
        }
    }

    // ==================== DELETE JOB ====================

    @Transactional
    public void deleteJob(Long id, boolean deleteTimesheets) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));

        // Optionally delete timesheets
        if (deleteTimesheets) {
            timesheetService.deleteTimesheetsByJobId(id);
        }

        // Delete CostCenter and its transactions
        Optional<CostCenter> costCenterOpt = costCenterRepository.findByJob_Id(id);
        costCenterOpt.ifPresent(cc -> {
            costTransactionRepository.deleteByCostCenter(cc);
            costCenterRepository.delete(cc);
        });

        // Delete attachments
        List<JobAttachment> attachments = attachmentRepository.findByJobId(id);
        for (JobAttachment attachment : attachments) {
            try {
                fileStorageService.deleteFile(attachment.getFilePath());
            } catch (Exception e) {
                System.err.println("Failed to delete physical file: " + attachment.getFilePath());
            }
            attachmentRepository.delete(attachment);
        }

        // Clear supervisor references
        List<Employee> supervisors = employeeRepository.findBySupervisedJob(job);
        for (Employee supervisor : supervisors) {
            supervisor.setSupervisedJob(null);
            employeeRepository.save(supervisor);
        }

        // Delete job-specific rates
        jobEmployeeRateService.deleteRatesByJobId(id);   // NEW

        // Delete job
        jobRepository.delete(job);
    }

    // ==================== EMPLOYEE ASSIGNMENT ====================

    @Transactional
    public Employee assignEmployeeToJob(Long jobId, Long employeeId, Boolean isSupervisor) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        employee.setJob(job);
        employee.setIsSupervisor(Boolean.TRUE.equals(isSupervisor));

        if (Boolean.TRUE.equals(isSupervisor)) {
            if (job.getSupervisor() != null) {
                Employee currentSupervisor = job.getSupervisor();
                currentSupervisor.setIsSupervisor(false);
                employeeRepository.save(currentSupervisor);
            }
            job.setSupervisor(employee);
            jobRepository.save(job);
        }

        // Create default rates for this job/employee
        Double defaultNH = employee.getMinimumRate() != null ? employee.getMinimumRate() : 0.0;
        Double defaultOT = employee.getHourlyRate() != null ? employee.getHourlyRate() * 1.5 : 0.0;
        jobEmployeeRateService.saveRate(job, employee, defaultNH, defaultOT);   // NEW

        return employeeRepository.save(employee);
    }

    @Transactional
    public List<Employee> bulkAssignEmployeesToJob(Long jobId, List<Long> employeeIds, Long supervisorId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        List<Employee> assignedEmployees = new ArrayList<>();

        for (Long employeeId : employeeIds) {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));
            boolean isSupervisor = supervisorId != null && employeeId.equals(supervisorId);
            employee.setJob(job);
            employee.setIsSupervisor(isSupervisor);
            // Create default rates
            Double defaultNH = employee.getMinimumRate() != null ? employee.getMinimumRate() : 0.0;
            Double defaultOT = employee.getHourlyRate() != null ? employee.getHourlyRate() * 1.5 : 0.0;
            jobEmployeeRateService.saveRate(job, employee, defaultNH, defaultOT);   // NEW
            assignedEmployees.add(employeeRepository.save(employee));
        }

        if (supervisorId != null) {
            Employee supervisor = employeeRepository.findById(supervisorId)
                    .orElseThrow(() -> new RuntimeException("Supervisor employee not found with id: " + supervisorId));
            job.setSupervisor(supervisor);
            jobRepository.save(job);
        }

        return assignedEmployees;
    }

    @Transactional
    public Employee removeEmployeeFromJob(Long jobId, Long employeeId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        if (!job.equals(employee.getJob())) {
            throw new RuntimeException("Employee is not assigned to this job");
        }

        if (job.getSupervisor() != null && job.getSupervisor().getId().equals(employeeId)) {
            job.setSupervisor(null);
            jobRepository.save(job);
        }

        employee.setJob(null);
        employee.setIsSupervisor(false);

        // Optionally delete the rate for this job/employee
        jobEmployeeRateService.deleteRate(jobId, employeeId);   // NEW

        return employeeRepository.save(employee);
    }

    @Transactional
    public Job setJobSupervisor(Long jobId, Long employeeId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        if (!job.equals(employee.getJob())) {
            throw new RuntimeException("Employee must be assigned to the job before being set as supervisor");
        }

        if (job.getSupervisor() != null) {
            Employee currentSupervisor = job.getSupervisor();
            currentSupervisor.setIsSupervisor(false);
            employeeRepository.save(currentSupervisor);
        }

        job.setSupervisor(employee);
        employee.setIsSupervisor(true);

        employeeRepository.save(employee);
        return jobRepository.save(job);
    }

    @Transactional
    public Job removeJobSupervisor(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        if (job.getSupervisor() != null) {
            Employee supervisor = job.getSupervisor();
            supervisor.setIsSupervisor(false);
            employeeRepository.save(supervisor);
            job.setSupervisor(null);
            jobRepository.save(job);
        }
        return job;
    }

    public List<Employee> getJobEmployees(Long jobId) {
        return employeeRepository.findByJobId(jobId);
    }

    public List<Job> getJobsByStatus(Job.JobStatus status) {
        return jobRepository.findByStatus(status);
    }

    public List<Job> searchJobs(String searchTerm) {
        return jobRepository.searchJobs(searchTerm);
    }

    public List<Job> getActiveJobs() {
        return jobRepository.findByStatus(Job.JobStatus.ACTIVE);
    }

    // ==================== ATTACHMENTS ====================

    public JobAttachment addAttachment(Long jobId, MultipartFile file, String description, LocalDate documentDate) throws IOException {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

        String filePath = fileStorageService.storeFile(file, "jobs/" + jobId);

        JobAttachment attachment = new JobAttachment();
        attachment.setJob(job);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFilePath(filePath);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setDescription(description);
        attachment.setDocumentDate(documentDate);

        return jobAttachmentRepository.save(attachment);
    }

    public List<JobAttachment> getJobAttachments(Long jobId) {
        return attachmentRepository.findByJobId(jobId);
    }

    public void deleteAttachment(Long attachmentId) {
        JobAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));
        fileStorageService.deleteFile(attachment.getFilePath());
        attachmentRepository.delete(attachment);
    }

    public JobAttachment getAttachmentById(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));
    }

    // ==================== JOB STATUS & STATS ====================

    public Job updateJobStatus(Long id, Job.JobStatus status) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
        job.setStatus(status);
        return jobRepository.save(job);
    }

    public Long getActiveJobsCount() {
        return jobRepository.countActiveJobs();
    }

    public List<Job> getJobsWithBudget() {
        return jobRepository.findJobsWithBudget();
    }

    public Job findById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
    }

    // ==================== JOB PRODUCTS ====================

    public List<JobProduct> getJobProducts(Long jobId) {
        return jobProductRepository.findByJobId(jobId);
    }

    @Transactional
    public JobProduct addProductToJob(Long jobId, Long productId, Integer quantity, String estimatedUsage) {
        try {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
            if (quantity > product.getStock()) {
                throw new RuntimeException("Requested quantity (" + quantity + ") exceeds available stock (" + product.getStock() + ") for product: " + product.getName());
            }
            Optional<JobProduct> existingAssignment = jobProductRepository.findByJobIdAndProductId(jobId, productId);
            if (existingAssignment.isPresent()) {
                JobProduct jp = existingAssignment.get();
                int oldQuantity = jp.getQuantity();
                int quantityDiff = quantity - oldQuantity;
                jp.setQuantity(quantity);
                jp.setEstimatedUsage(estimatedUsage);
                JobProduct saved = jobProductRepository.save(jp);
                if (quantityDiff != 0) {
                    updateCostCenterForProduct(job, product, quantityDiff, "Updated product assignment for job");
                }
                return saved;
            }
            JobProduct jp = new JobProduct(job, product, quantity, estimatedUsage);
            JobProduct saved = jobProductRepository.save(jp);
            updateCostCenterForProduct(job, product, quantity, "Assigned product to job");
            return saved;
        } catch (Exception e) {
            throw e;
        }
    }

    public void removeProductFromJob(Long jobProductId) {
        jobProductRepository.deleteById(jobProductId);
    }

    public void updateProductQuantity(Long jobProductId, Integer quantity) {
        JobProduct jobProduct = jobProductRepository.findById(jobProductId)
                .orElseThrow(() -> new RuntimeException("Job product assignment not found"));
        if (quantity > jobProduct.getProduct().getStock()) {
            throw new RuntimeException("Requested quantity (" + quantity + ") exceeds available stock (" +
                    jobProduct.getProduct().getStock() + ") for product: " + jobProduct.getProduct().getName());
        }
        jobProduct.setQuantity(quantity);
        jobProductRepository.save(jobProduct);
    }

    // ==================== COST CENTER ====================

    private User getCurrentUser() {
        try {
            return userService.getCurrentUser();
        } catch (Exception e) {
            try {
                Optional<User> adminUser = userRepository.findByRole(SystemRole.ROLE_ADMIN)
                        .stream()
                        .findFirst();
                if (adminUser.isPresent()) {
                    return adminUser.get();
                }
                return userRepository.findFirstByOrderByIdAsc().orElse(null);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private void updateCostCenterForProduct(Job job, Product product, Integer quantity, String action) {
        try {
            Optional<CostCenter> costCenterOpt = costCenterRepository.findByJob_Id(job.getId());
            if (!costCenterOpt.isPresent()) {
                return;
            }
            CostCenter costCenter = costCenterOpt.get();
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return;
            }
            Double unitCost = product.getUnitCost() != null ? product.getUnitCost() : 0.0;
            Double totalCost = quantity * unitCost;
            if (costCenter.getSpentAmount() == null) {
                costCenter.setSpentAmount(0.0);
            }
            costCenter.setSpentAmount(costCenter.getSpentAmount() + totalCost);
            costCenterRepository.save(costCenter);

            CostTransaction transaction = new CostTransaction();
            transaction.setTransactionNumber("TRX-" + UUID.randomUUID().toString().substring(0, 8));
            transaction.setCostCenter(costCenter);
            transaction.setJob(job);
            transaction.setProduct(product);
            transaction.setTransactionType(TransactionType.PRODUCT_ASSIGNMENT);
            transaction.setAmount(totalCost);
            transaction.setDescription(String.format("%s: %d units of %s to job: %s", action, quantity, product.getName(), job.getName()));
            transaction.setIssuedBy(currentUser);
            transaction.setQuantity(quantity);
            transaction.setUnitCost(unitCost);
            transaction.setTransactionDate(LocalDateTime.now());
            costTransactionRepository.save(transaction);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public Job createJob(Job job, User createdBy) {
        Job savedJob = jobRepository.save(job);
        costCenterService.createCostCenterForJob(savedJob, createdBy);
        return savedJob;
    }
}