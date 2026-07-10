package com.example.eac_backend_project.Service.billing;

import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.entity.attendance.Job;
import com.example.eac_backend_project.entity.billing.BillingNotification;
import com.example.eac_backend_project.repository.JobRepository;
import com.example.eac_backend_project.repository.billing.BillingNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BillingReminderScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BillingReminderScheduler.class);

    private final JobRepository jobRepository;
    private final BillingNotificationRepository billingNotificationRepository;
    private final EmailService emailService;

    public BillingReminderScheduler(
            JobRepository jobRepository,
            BillingNotificationRepository billingNotificationRepository,
            EmailService emailService
    ) {
        this.jobRepository = jobRepository;
        this.billingNotificationRepository = billingNotificationRepository;
        this.emailService = emailService;
    }

    /**
     * Runs daily at 1:00 AM to schedule reminders (after midnight processing)
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void scheduleAdvanceReminders() {
        logger.info("Starting to schedule advance reminders for all active jobs");

        // Get all jobs with billing reminders enabled AND with valid next billing date
        List<Job> activeJobs = jobRepository.findAllActiveJobsWithBillingReminderEnabled();

        // Filter jobs that actually have next billing date and emails
        List<Job> jobsToSchedule = activeJobs.stream()
                .filter(job -> job.getNextBillingDate() != null)
                .filter(job -> job.getBillingReminderEmails() != null && !job.getBillingReminderEmails().trim().isEmpty())
                .collect(Collectors.toList());

        logger.info("Found {} jobs with valid billing reminders enabled", jobsToSchedule.size());

        int scheduledCount = 0;

        for (Job job : jobsToSchedule) {
            List<String> recipients = parseRecipients(job.getBillingReminderEmails());
            if (recipients.isEmpty()) {
                logger.warn("Job {} has reminder enabled but no valid recipients", job.getName());
                continue;
            }

            LocalDate dueDate = job.getNextBillingDate();
            LocalDate today = LocalDate.now();

            // Cancel any existing pending notifications for this job
            int cancelled = billingNotificationRepository.cancelPendingNotificationsForJob(job.getId());
            if (cancelled > 0) {
                logger.debug("Cancelled {} existing pending notifications for job {}", cancelled, job.getName());
            }

            LocalTime reminderTime = LocalTime.of(
                    job.getReminderTimeHour() != null ? job.getReminderTimeHour() : 8,
                    job.getReminderTimeMinute() != null ? job.getReminderTimeMinute() : 0
            );

            // Schedule advance reminders based on job configuration
            if (Boolean.TRUE.equals(job.getEnableAdvanceReminders()) && job.getReminderDaysBefore() != null) {
                List<Integer> reminderDays = parseReminderDays(job.getReminderDaysBefore());

                for (int daysBefore : reminderDays) {
                    LocalDate scheduledDate = dueDate.minusDays(daysBefore);

                    // Only schedule if the scheduled date is today or in the future
                    if (!scheduledDate.isBefore(today)) {
                        for (String recipient : recipients) {
                            String noticeType = "ADVANCE_" + daysBefore + "_DAYS";

                            // Check if already scheduled
                            boolean alreadyScheduled = billingNotificationRepository
                                    .existsByJobIdAndScheduledDateAndNoticeTypeAndRecipientEmail(
                                            job.getId(), scheduledDate, noticeType, recipient
                                    );

                            if (!alreadyScheduled) {
                                BillingNotification notification = new BillingNotification(
                                        job.getId(), recipient, dueDate, noticeType,
                                        daysBefore, scheduledDate, reminderTime
                                );
                                billingNotificationRepository.save(notification);
                                scheduledCount++;
                                logger.debug("Scheduled {} day advance reminder for job {} to {} on {}",
                                        daysBefore, job.getName(), recipient, scheduledDate);
                            }
                        }
                    }
                }
            }

            // Schedule due date reminder (day of)
            for (String recipient : recipients) {
                boolean alreadyScheduled = billingNotificationRepository
                        .existsByJobIdAndScheduledDateAndNoticeTypeAndRecipientEmail(
                                job.getId(), dueDate, "DUE_TODAY", recipient
                        );

                if (!alreadyScheduled && !dueDate.isBefore(today)) {
                    BillingNotification notification = new BillingNotification(
                            job.getId(), recipient, dueDate, "DUE_TODAY",
                            0, dueDate, reminderTime
                    );
                    billingNotificationRepository.save(notification);
                    scheduledCount++;
                    logger.debug("Scheduled due date reminder for job {} to {} on {}", job.getName(), recipient, dueDate);
                }
            }

            // Schedule overdue reminders
            if (Boolean.TRUE.equals(job.getEnableOverdueReminders()) && job.getOverdueReminderDays() != null) {
                int overdueDays = job.getOverdueReminderDays();
                LocalDate overdueDate = dueDate.plusDays(overdueDays);

                for (String recipient : recipients) {
                    boolean alreadyScheduled = billingNotificationRepository
                            .existsByJobIdAndScheduledDateAndNoticeTypeAndRecipientEmail(
                                    job.getId(), overdueDate, "OVERDUE", recipient
                            );

                    if (!alreadyScheduled && !overdueDate.isBefore(today)) {
                        BillingNotification notification = new BillingNotification(
                                job.getId(), recipient, dueDate, "OVERDUE",
                                -overdueDays, overdueDate, reminderTime
                        );
                        billingNotificationRepository.save(notification);
                        scheduledCount++;
                        logger.debug("Scheduled overdue reminder for job {} to {} on {}", job.getName(), recipient, overdueDate);
                    }
                }
            }
        }

        logger.info("Scheduled {} new reminders", scheduledCount);
    }

    /**
     * Runs every hour to process notifications that need to be sent
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processScheduledNotifications() {
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now().withSecond(0).withNano(0);

        logger.debug("Checking for pending notifications - Date: {}, Time: {}", today, currentTime);

        List<BillingNotification> pendingNotifications =
                billingNotificationRepository.findPendingNotificationsScheduledForNow(today, currentTime);

        if (pendingNotifications.isEmpty()) {
            return;
        }

        logger.info("Found {} pending notifications to process", pendingNotifications.size());

        for (BillingNotification notification : pendingNotifications) {
            processNotification(notification);
        }
    }

    /**
     * Process a single notification - FIXED to use email templates
     */
    private void processNotification(BillingNotification notification) {
        try {
            logger.info("Processing notification {} for job {}", notification.getId(), notification.getJobId());

            // Get the job (may have been updated)
            Job job = jobRepository.findById(notification.getJobId()).orElse(null);
            if (job == null) {
                logger.warn("Job {} not found, cancelling notification", notification.getJobId());
                notification.setStatus("CANCELLED");
                notification.setErrorMessage("Job no longer exists");
                billingNotificationRepository.save(notification);
                return;
            }

            // Verify job still has billing reminders enabled
            if (!job.isBillingReminderEnabled()) {
                logger.info("Billing reminders disabled for job {}, cancelling notification", job.getName());
                notification.setStatus("CANCELLED");
                notification.setErrorMessage("Billing reminders disabled for this job");
                billingNotificationRepository.save(notification);
                return;
            }

            // Determine days until due for the email template
            int daysToDue = 0;
            if (notification.getNoticeType().startsWith("ADVANCE_")) {
                daysToDue = notification.getDaysBeforeDue();
            } else if (notification.getNoticeType().equals("DUE_TODAY")) {
                daysToDue = 0;
            } else if (notification.getNoticeType().equals("OVERDUE")) {
                daysToDue = -notification.getDaysBeforeDue();
            }

            // CRITICAL FIX: Use the template-based email method instead of raw HTML
            String subject = buildEmailSubject(job, notification);

            // This now uses your beautiful Thymeleaf templates!
            emailService.sendBillingReminderEmail(
                    notification.getRecipientEmail(),
                    subject,
                    job,
                    daysToDue
            );

            // Update notification status
            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);
            billingNotificationRepository.save(notification);

            logger.info("Successfully sent notification {} to {}", notification.getId(), notification.getRecipientEmail());

            // If this was a DUE_TODAY notification, update the job's next billing date
            if (notification.getNoticeType().equals("DUE_TODAY")) {
                updateNextBillingDate(job);
            }

        } catch (Exception e) {
            logger.error("Failed to send notification {}: {}", notification.getId(), e.getMessage(), e);

            notification.setRetryCount(notification.getRetryCount() + 1);

            // Max 3 retries
            if (notification.getRetryCount() >= 3) {
                notification.setStatus("FAILED");
                notification.setErrorMessage(e.getMessage());
            } else {
                // Keep as PENDING for retry
                notification.setStatus("PENDING");
                notification.setErrorMessage("Retry " + notification.getRetryCount() + ": " + e.getMessage());
            }

            billingNotificationRepository.save(notification);
        }
    }

    /**
     * Update the next billing date after a due reminder is sent
     */
    private void updateNextBillingDate(Job job) {
        LocalDate currentDue = job.getNextBillingDate();
        if (currentDue == null) return;

        LocalDate newDue = null;

        // PRIORITY 1: Anchor Day billing (specific day of month)
        if (job.isAnchorDayBilling()) {
            int anchorDay = job.getBillingAnchorDay();
            newDue = currentDue.plusMonths(1).withDayOfMonth(Math.min(anchorDay, currentDue.plusMonths(1).lengthOfMonth()));
            logger.info("Anchor day billing: next due on day {} → {}", anchorDay, newDue);
        }
        // PRIORITY 2: Bill on 15th next month (legacy)
        else if (job.isBillOn15NextMonth()) {
            newDue = currentDue.plusMonths(1).withDayOfMonth(15);
        }
        // PRIORITY 3: Regular billing cycle
        else if (job.getBillingCycle() != null) {
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

        // Check if job has ended
        if (job.getEndDate() != null && newDue != null && newDue.isAfter(job.getEndDate())) {
            newDue = null;
        }

        job.setNextBillingDate(newDue);
        jobRepository.save(job);
        logger.info("Updated next billing date for job {} from {} to {}",
                job.getName(), currentDue, newDue);
    }

    /**
     * Build email subject - FIXED to be cleaner
     */
    private String buildEmailSubject(Job job, BillingNotification notification) {
        String jobName = safe(job.getName());
        String clientName = safe(job.getClient());

        switch (notification.getNoticeType()) {
            case "OVERDUE":
                return String.format("⚠️ OVERDUE: Billing for %s (%s)", jobName, clientName);
            case "DUE_TODAY":
                return String.format("🔔 Billing Due Today: %s (%s)", jobName, clientName);
            default:
                if (notification.getNoticeType().startsWith("ADVANCE_")) {
                    int days = notification.getDaysBeforeDue();
                    return String.format("📅 Billing Reminder: %s (%s) - Due in %d day%s",
                            jobName, clientName, days, days > 1 ? "s" : "");
                }
                return String.format("Billing Reminder: %s (%s)", jobName, clientName);
        }
    }

    /**
     * Parse comma-separated reminder days
     */
    private List<Integer> parseReminderDays(String daysStr) {
        if (daysStr == null || daysStr.trim().isEmpty()) {
            return List.of(7, 3, 1);
        }
        try {
            return Arrays.stream(daysStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .distinct()
                    .sorted((a, b) -> Integer.compare(b, a))
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            logger.warn("Invalid reminder days format: {}, using defaults", daysStr);
            return List.of(7, 3, 1);
        }
    }

    /**
     * Parse comma-separated email recipients
     */
    private List<String> parseRecipients(String raw) {
        if (raw == null || raw.trim().isEmpty()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> s.contains("@"))  // Basic email validation
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Safe string handling
     */
    private String safe(String s) {
        return s == null ? "" : s;
    }
}