package com.example.eac_backend_project.Service.settings;

import com.example.eac_backend_project.dto.SystemSettingsDTO;
import com.example.eac_backend_project.entity.settings.*;
import com.example.eac_backend_project.repository.settings.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Service
@Transactional
public class SettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private com.example.eac_backend_project.Service.HolidayAttendanceService holidayAttendanceService;

    @Autowired
    private AttendanceReportScheduleSettingsRepository scheduleRepository;

    @Autowired
    private JobPositionRepository jobPositionRepository;

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    @Autowired
    private CategorySettingsRepository categorySettingsRepository;

    @Autowired
    private SpecialWeekendRepository specialWeekendRepository;

    @Autowired
    private LoanSettingsRepository loanSettingsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public List<LoanSettings> getAllLoanSettings() {
        return loanSettingsRepository.findAll();
    }

    public LoanSettings createLoanSettings(LoanSettings loanSettings) {
        return loanSettingsRepository.save(loanSettings);
    }

    public LoanSettings updateLoanSettings(Long id, LoanSettings loanSettings) {
        LoanSettings existingLoanSettings = loanSettingsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LoanSettings already exist for: " + loanSettings.getMaximumLoanLimit()));

        existingLoanSettings.setMaximumLoanLimit(loanSettings.getMaximumLoanLimit());

        return loanSettingsRepository.save(existingLoanSettings);

    }

    // Category Methods
    public List<Category> getAllCategories() {
        return categorySettingsRepository.
                findAllByOrderByNameAsc();
    }



    public Category createCategory(Category category) {
        if (categorySettingsRepository.existsByName(category.getName())) {
            throw new RuntimeException("Category already exists: " + category.getName());
        }

        // Set default annual leave days if not provided
        if (category.getAnnualLeaveDays() == null) {
            category.setAnnualLeaveDays(20);
        }

        return categorySettingsRepository.save(category);
    }

    // In SettingsService.java - Update the updateCategory method

    public Category updateCategory(Long id, Category category) {
        Category existingCategory = categorySettingsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        existingCategory.setName(category.getName());
        existingCategory.setStandardRateHours(category.getStandardRateHours());
        existingCategory.setWorkStartTime(category.getWorkStartTime());
        if (category.getWeeklyOffDays() != null) existingCategory.setWeeklyOffDays(category.getWeeklyOffDays());
        existingCategory.setAbsenceCutoffTime(category.getAbsenceCutoffTime());
        existingCategory.setOvernightShift(Boolean.TRUE.equals(category.getOvernightShift()));
        existingCategory.setUseNonTaxableAllowances(category.getUseNonTaxableAllowances());
        existingCategory.setExcludeWeekendsFromLeave(category.getExcludeWeekendsFromLeave());

        if (category.getAnnualLeaveDays() != null) {
            existingCategory.setAnnualLeaveDays(category.getAnnualLeaveDays());
        } else {
            existingCategory.setAnnualLeaveDays(20);
        }

        existingCategory.setSkipAutoAbsent(category.getSkipAutoAbsent());
        existingCategory.setApplyWithholdingTax(category.getApplyWithholdingTax());
        existingCategory.setWeekendAttendanceAsOvertime(
                Boolean.TRUE.equals(category.getWeekendAttendanceAsOvertime()));

        // ========== NEW: Save approval routing fields ==========
        existingCategory.setSupervisorEmails(category.getSupervisorEmails());
        existingCategory.setPlannerEmails(category.getPlannerEmails());
        existingCategory.setHrEmails(category.getHrEmails());
        existingCategory.setUseCustomApprovers(category.getUseCustomApprovers() != null ? category.getUseCustomApprovers() : false);

        return categorySettingsRepository.save(existingCategory);
    }

    // Add a helper method to get category approvers
    public CategoryApprovers getCategoryApprovers(Long categoryId) {
        Category category = categorySettingsRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        CategoryApprovers approvers = new CategoryApprovers();

        if (category.getUseCustomApprovers() != null && category.getUseCustomApprovers()) {
            approvers.setUseCustomApprovers(true);
            approvers.setSupervisorEmails(parseEmailList(category.getSupervisorEmails()));
            approvers.setPlannerEmails(parseEmailList(category.getPlannerEmails()));
            approvers.setHrEmails(parseEmailList(category.getHrEmails()));
        } else {
            approvers.setUseCustomApprovers(false);
            SystemSettings settings = getSystemSettings();
            approvers.setSupervisorEmails(parseEmailList(settings.getLeaveRequestRecipients()));
            approvers.setPlannerEmails(parseEmailList(settings.getLeaveApprovalRecipients()));
            approvers.setHrEmails(parseEmailList(settings.getLeaveApprovalRecipients()));
        }

        return approvers;
    }

    private List<String> parseEmailList(String emailString) {
        if (emailString == null || emailString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(emailString.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());
    }

    // Inner class for approvers
    public static class CategoryApprovers {
        private boolean useCustomApprovers;
        private List<String> supervisorEmails;
        private List<String> plannerEmails;
        private List<String> hrEmails;

        // Getters and setters
        public boolean isUseCustomApprovers() { return useCustomApprovers; }
        public void setUseCustomApprovers(boolean useCustomApprovers) { this.useCustomApprovers = useCustomApprovers; }
        public List<String> getSupervisorEmails() { return supervisorEmails; }
        public void setSupervisorEmails(List<String> supervisorEmails) { this.supervisorEmails = supervisorEmails; }
        public List<String> getPlannerEmails() { return plannerEmails; }
        public void setPlannerEmails(List<String> plannerEmails) { this.plannerEmails = plannerEmails; }
        public List<String> getHrEmails() { return hrEmails; }
        public void setHrEmails(List<String> hrEmails) { this.hrEmails = hrEmails; }
    }

    public void deleteCategory(Long id) {
        // Check if category is used by any job positions
        List<JobPosition> positionsUsingCategory = jobPositionRepository.findByCategoryId(id);
        if (!positionsUsingCategory.isEmpty()) {
            throw new RuntimeException("Cannot delete category. It is being used by " + positionsUsingCategory.size() + " job position(s).");
        }
        categorySettingsRepository.deleteById(id);
    }

    // Special Weekend Methods
    public List<SpecialWeekend> getAllSpecialWeekends() {
        return specialWeekendRepository.findAllByOrderByDateAsc();
    }

    public SpecialWeekend createSpecialWeekend(SpecialWeekend specialWeekend) {
        if (specialWeekendRepository.existsByDate(specialWeekend.getDate())) {
            throw new RuntimeException("Special weekend already exists for date: " + specialWeekend.getDate());
        }
        return specialWeekendRepository.save(specialWeekend);
    }

    public SpecialWeekend updateSpecialWeekend(Long id, SpecialWeekend specialWeekend) {
        SpecialWeekend existingSpecialWeekend = specialWeekendRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special weekend not found with id: " + id));

        existingSpecialWeekend.setName(specialWeekend.getName());
        existingSpecialWeekend.setDate(specialWeekend.getDate());
        existingSpecialWeekend.setRateMultiplier(specialWeekend.getRateMultiplier());

        return specialWeekendRepository.save(existingSpecialWeekend);
    }

    public void deleteSpecialWeekend(Long id) {
        specialWeekendRepository.deleteById(id);
    }

    public boolean isSpecialWeekend(LocalDate date) {
        return specialWeekendRepository.existsByDate(date);
    }

    public Double getSpecialWeekendMultiplier(LocalDate date) {
        return specialWeekendRepository.findAll().stream()
                .filter(sw -> sw.getDate().equals(date))
                .findFirst()
                .map(SpecialWeekend::getRateMultiplier)
                .orElse(null);
    }

    // Job Position Methods
    public List<JobPosition> getAllJobPositions() {
        return jobPositionRepository.findAllByOrderByNameAsc();
    }

    public JobPosition createJobPosition(JobPosition jobPosition) {


        if (jobPositionRepository.existsByName(jobPosition.getName())) {
            throw new RuntimeException("Job position already exists: " + jobPosition.getName());
        }

        // Ensure grades are properly linked
        if (jobPosition.getGrades() != null) {
            for (JobGrade grade : jobPosition.getGrades()) {
                grade.setJobPosition(jobPosition);
            }
        }

        return jobPositionRepository.save(jobPosition);
    }

    public JobPosition updateJobPosition(Long id, JobPosition jobPosition) {
        JobPosition existingPosition = jobPositionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job position not found with id: " + id));

        existingPosition.setName(jobPosition.getName());
        existingPosition.setDescription(jobPosition.getDescription());
        existingPosition.setCategory(jobPosition.getCategory());
        existingPosition.setDepartment(jobPosition.getDepartment());   // <-- ADD THIS
        existingPosition.setBaseRate(jobPosition.getBaseRate());
        existingPosition.setStandardWorkHours(jobPosition.getStandardWorkHours());
        existingPosition.setGrades(jobPosition.getGrades());

        return jobPositionRepository.save(existingPosition);
    }

    // ADD THIS MISSING METHOD:
    public void deleteJobPosition(Long id) {
        JobPosition existingPosition = jobPositionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job position not found with id: " + id));

        // Optional: Check if this job position is being used by any employees
        // You would need to inject EmployeeRepository and add a method like:
        // List<Employee> employeesWithPosition = employeeRepository.findByJobPosition(existingPosition.getName());
        // if (!employeesWithPosition.isEmpty()) {
        //     throw new RuntimeException("Cannot delete job position. It is being used by " + employeesWithPosition.size() + " employee(s).");
        // }

        jobPositionRepository.deleteById(id);
    }

    public Optional<JobPosition> getJobPositionByName(String name) {
        return jobPositionRepository.findByName(name);
    }

    // Holiday Methods
    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAllByOrderByDateAsc();
    }

    public Holiday createHoliday(Holiday holiday) {
        if (holidayRepository.existsByDate(holiday.getDate())) {
            throw new RuntimeException("Holiday already exists for date: " + holiday.getDate());
        }
        Holiday saved = holidayRepository.save(holiday);
        holidayAttendanceService.holidaySaved(saved);
        return saved;
    }

    public Holiday updateHoliday(Long id, Holiday holiday) {
        Holiday existingHoliday = holidayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Holiday not found with id: " + id));

        existingHoliday.setName(holiday.getName());
        existingHoliday.setDate(holiday.getDate());
        existingHoliday.setRecurring(holiday.isRecurring());
        existingHoliday.setPayMultiplier(holiday.getPayMultiplier());

        Holiday saved = holidayRepository.save(existingHoliday);
        holidayAttendanceService.holidaySaved(saved);
        return saved;
    }

    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }

    public boolean isHoliday(LocalDate date) {
        // Check specific date
        if (holidayRepository.existsByDate(date)) {
            return true;
        }

        // Check recurring holidays (same month and day)
        List<Holiday> recurringHolidays = holidayRepository.findByRecurringTrue();
        return recurringHolidays.stream()
                .anyMatch(holiday -> {
                    LocalDate holidayDate = holiday.getDate();
                    return holidayDate.getMonth() == date.getMonth() &&
                            holidayDate.getDayOfMonth() == date.getDayOfMonth();
                });
    }

    // System Settings Methods
    public SystemSettings getSystemSettings() {
        return systemSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    SystemSettings defaultSettings = new SystemSettings();
                    return systemSettingsRepository.save(defaultSettings);
                });
    }

    public SystemSettings updateSystemSettings(SystemSettingsDTO settingsDTO) {
        SystemSettings existingSettings = getSystemSettings();

        if (settingsDTO.getLateArrivalTime() != null) {
            existingSettings.setLateArrivalTime(settingsDTO.getLateArrivalTime());
        }

        existingSettings.setHourlyRate(settingsDTO.getHourlyRate());
        existingSettings.setOvertimeHourlyRate(settingsDTO.getOvertimeHourlyRate());
        existingSettings.setStandardWorkHours(settingsDTO.getStandardWorkHours());
        existingSettings.setDoubleTimeOnSunday(settingsDTO.getDoubleTimeOnSunday());
        existingSettings.setTimeAndHalfAfter8Hours(settingsDTO.getTimeAndHalfAfter8Hours());
        existingSettings.setWeekendRate(settingsDTO.getWeekendRate());
        existingSettings.setHolidayRate(settingsDTO.getHolidayRate());
        existingSettings.setWithholdingTaxEnabled(settingsDTO.getWithholdingTaxEnabled());
        existingSettings.setWithholdingTaxRate(settingsDTO.getWithholdingTaxRate());

        if (settingsDTO.getDefaultOvertimeMultiplier() != null) {
            existingSettings.setDefaultOvertimeMultiplier(settingsDTO.getDefaultOvertimeMultiplier());
        }

        if (settingsDTO.getSundayOvertimeMultiplier() != null) {
            existingSettings.setSundayOvertimeMultiplier(settingsDTO.getSundayOvertimeMultiplier());
        }

        if (settingsDTO.getHolidayOvertimeMultiplier() != null) {
            existingSettings.setHolidayOvertimeMultiplier(settingsDTO.getHolidayOvertimeMultiplier());
        }

        if (settingsDTO.getEnableTimeAndHalfAfter8Hours() != null) {
            existingSettings.setEnableTimeAndHalfAfter8Hours(settingsDTO.getEnableTimeAndHalfAfter8Hours());
        }

        if (settingsDTO.getTimeAndHalfMultiplier() != null) {
            existingSettings.setTimeAndHalfMultiplier(settingsDTO.getTimeAndHalfMultiplier());
        }

        if (settingsDTO.getAccountantRecipients() != null) {
            existingSettings.setAccountantRecipients(settingsDTO.getAccountantRecipients());
        }
        if (settingsDTO.getReviewerRecipients() != null) {
            existingSettings.setReviewerRecipients(settingsDTO.getReviewerRecipients());
        }
        if (settingsDTO.getFinanceRecipients() != null) {
            existingSettings.setFinanceRecipients(settingsDTO.getFinanceRecipients());
        }
        if (settingsDTO.getRequesterNotificationEnabled() != null) {
            existingSettings.setRequesterNotificationEnabled(
                    settingsDTO.getRequesterNotificationEnabled()
            );
        }
        if (settingsDTO.getAccountantNotificationEnabled() != null) {
            existingSettings.setAccountantNotificationEnabled(
                    settingsDTO.getAccountantNotificationEnabled()
            );
        }
        if (settingsDTO.getReviewerNotificationEnabled() != null) {
            existingSettings.setReviewerNotificationEnabled(
                    settingsDTO.getReviewerNotificationEnabled()
            );
        }
        if (settingsDTO.getFinanceNotificationEnabled() != null) {
            existingSettings.setFinanceNotificationEnabled(
                    settingsDTO.getFinanceNotificationEnabled()
            );
        }

        // company fields
        if (settingsDTO.getCompanyName() != null) {
            existingSettings.setCompanyName(settingsDTO.getCompanyName());
        }
        if (settingsDTO.getCompanyEmail() != null) {
            existingSettings.setCompanyEmail(settingsDTO.getCompanyEmail());
        }
        if (settingsDTO.getCompanyPhone() != null) {
            existingSettings.setCompanyPhone(settingsDTO.getCompanyPhone());
        }
        if (settingsDTO.getCompanyAddress() != null) {
            existingSettings.setCompanyAddress(settingsDTO.getCompanyAddress());
        }

        // Billing reminders
        if (settingsDTO.getBillingReminderEmails() != null) {
            existingSettings.setBillingReminderEmails(settingsDTO.getBillingReminderEmails());
        }
        if (settingsDTO.getBillingReminderEnabled() != null) {
            existingSettings.setBillingReminderEnabled(settingsDTO.getBillingReminderEnabled());
        }
        if (settingsDTO.getAccountRequestRecipients() != null) {
            existingSettings.setAccountRequestRecipients(settingsDTO.getAccountRequestRecipients());
        }
        if (settingsDTO.getSupportRequestRecipients() != null) {
            existingSettings.setSupportRequestRecipients(settingsDTO.getSupportRequestRecipients());
        }
        if (settingsDTO.getPettyCashThreshold() != null && settingsDTO.getPettyCashThreshold().signum() >= 0) {
            existingSettings.setPettyCashThreshold(settingsDTO.getPettyCashThreshold());
        }
        if (settingsDTO.getPettyCashRequestRecipients() != null) {
            existingSettings.setPettyCashRequestRecipients(settingsDTO.getPettyCashRequestRecipients());
        }

        if (settingsDTO.getInventoryRequestRecipients() != null) {
            existingSettings.setInventoryRequestRecipients(
                    settingsDTO.getInventoryRequestRecipients()
            );
        }

        if (settingsDTO.getInventoryPlannerRecipients() != null) {
            existingSettings.setInventoryPlannerRecipients(
                    settingsDTO.getInventoryPlannerRecipients()
            );
        }

        if (settingsDTO.getInventoryProcurementRecipients() != null) {
            existingSettings.setInventoryProcurementRecipients(
                    settingsDTO.getInventoryProcurementRecipients()
            );
        }

        if (settingsDTO.getInventoryStoreRecipients() != null) {
            existingSettings.setInventoryStoreRecipients(
                    settingsDTO.getInventoryStoreRecipients()
            );
        }

        if (settingsDTO.getInventoryStatusRecipients() != null) {
            existingSettings.setInventoryStatusRecipients(
                    settingsDTO.getInventoryStatusRecipients()
            );
        }

        if (settingsDTO.getInventoryRequesterNotificationEnabled() != null) {
            existingSettings.setInventoryRequesterNotificationEnabled(
                    settingsDTO.getInventoryRequesterNotificationEnabled()
            );
        }

        if (settingsDTO.getInventoryPlannerNotificationEnabled() != null) {
            existingSettings.setInventoryPlannerNotificationEnabled(
                    settingsDTO.getInventoryPlannerNotificationEnabled()
            );
        }

        if (settingsDTO.getInventoryProcurementNotificationEnabled() != null) {
            existingSettings.setInventoryProcurementNotificationEnabled(
                    settingsDTO.getInventoryProcurementNotificationEnabled()
            );
        }

        if (settingsDTO.getInventoryStoreNotificationEnabled() != null) {
            existingSettings.setInventoryStoreNotificationEnabled(
                    settingsDTO.getInventoryStoreNotificationEnabled()
            );
        }

        if (settingsDTO.getInventoryStatusNotificationEnabled() != null) {
            existingSettings.setInventoryStatusNotificationEnabled(
                    settingsDTO.getInventoryStatusNotificationEnabled()
            );
        }

        // PPE notification recipient fields
        if (settingsDTO.getPpeRequestRecipients() != null) {
            existingSettings.setPpeRequestRecipients(
                    settingsDTO.getPpeRequestRecipients()
            );
        }

        if (settingsDTO.getPpeProcurementRecipients() != null) {
            existingSettings.setPpeProcurementRecipients(
                    settingsDTO.getPpeProcurementRecipients()
            );
        }

        if (settingsDTO.getPpeStoreRecipients() != null) {
            existingSettings.setPpeStoreRecipients(
                    settingsDTO.getPpeStoreRecipients()
            );
        }

        if (settingsDTO.getPpeStatusRecipients() != null) {
            existingSettings.setPpeStatusRecipients(
                    settingsDTO.getPpeStatusRecipients()
            );
        }

        // PPE notification toggle fields
        if (settingsDTO.getPpeRequesterNotificationEnabled() != null) {
            existingSettings.setPpeRequesterNotificationEnabled(
                    settingsDTO.getPpeRequesterNotificationEnabled()
            );
        }

        if (settingsDTO.getPpeProcurementNotificationEnabled() != null) {
            existingSettings.setPpeProcurementNotificationEnabled(
                    settingsDTO.getPpeProcurementNotificationEnabled()
            );
        }

        if (settingsDTO.getPpeStoreNotificationEnabled() != null) {
            existingSettings.setPpeStoreNotificationEnabled(
                    settingsDTO.getPpeStoreNotificationEnabled()
            );
        }

        if (settingsDTO.getPpeStatusNotificationEnabled() != null) {
            existingSettings.setPpeStatusNotificationEnabled(
                    settingsDTO.getPpeStatusNotificationEnabled()
            );
        }

        // Add these lines for loan notification fields (if not already present)
        if (settingsDTO.getLoanRequestRecipients() != null) {
            existingSettings.setLoanRequestRecipients(settingsDTO.getLoanRequestRecipients());
        }

        if (settingsDTO.getLoanApprovalRecipients() != null) {
            existingSettings.setLoanApprovalRecipients(settingsDTO.getLoanApprovalRecipients());
        }

        if (settingsDTO.getLoanRejectionRecipients() != null) {
            existingSettings.setLoanRejectionRecipients(settingsDTO.getLoanRejectionRecipients());
        }

        // ADD THIS NEW FIELD HANDLING
        if (settingsDTO.getLoanConsolidationRecipients() != null) {
            existingSettings.setLoanConsolidationRecipients(settingsDTO.getLoanConsolidationRecipients());
        }

        // Serialize JSON-like fields safely (avoid double encoding)
        try {
            // weekendDays
            if (settingsDTO.getWeekendDays() != null) {
                existingSettings.setWeekendDays(objectMapper.writeValueAsString(settingsDTO.getWeekendDays()));
            }

            // employeeCategories (fix)
            // employeeCategories (FULL FIX)
            if (settingsDTO.getEmployeeCategories() != null) {

                Object normalized = settingsDTO.getEmployeeCategories();

                try {

                    // Recursively unwrap nested JSON strings
                    while (normalized instanceof String s) {

                        s = s.trim();

                        // plain text category
                        if (!(s.startsWith("[")
                                || s.startsWith("{")
                                || s.startsWith("\""))) {

                            normalized = List.of(s);
                            break;
                        }

                        Object parsed = objectMapper.readValue(s, Object.class);

                        // stop infinite loops
                        if (Objects.equals(parsed, normalized)) {
                            break;
                        }

                        normalized = parsed;
                    }

                    // Final safe normalization
                    if (normalized instanceof List<?>) {

                        existingSettings.setEmployeeCategories(
                                objectMapper.writeValueAsString(normalized)
                        );

                    } else if (normalized instanceof String s) {

                        existingSettings.setEmployeeCategories(
                                objectMapper.writeValueAsString(List.of(s))
                        );

                    } else {

                        existingSettings.setEmployeeCategories(
                                objectMapper.writeValueAsString(normalized)
                        );
                    }

                } catch (Exception e) {

                    logger.error("Failed to normalize employeeCategories", e);

                    existingSettings.setEmployeeCategories("[]");
                }
            }
        } catch (Exception e) {
            logger.error("Error serializing settings fields", e);
        }

        // Debug: measure what will be stored
        String catJson = existingSettings.getEmployeeCategories();
        if (catJson != null) {
            int bytes = catJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            logger.warn("employeeCategories chars={}, bytes={}", catJson.length(), bytes);
            logger.warn("employeeCategories preview={}", catJson.substring(0, Math.min(300, catJson.length())));
        } else {
            logger.warn("employeeCategories is null");
        }

        catJson = existingSettings.getEmployeeCategories();

        if (catJson != null && catJson.length() > 100000) {
            throw new RuntimeException(
                    "employeeCategories payload exceeded safe size"
            );
        }

        return systemSettingsRepository.save(existingSettings);
    }


    public boolean isWeekend(LocalDate date) {
        try {
            int dayOfWeek = date.getDayOfWeek().getValue() % 7;
            List<Integer> weekendDays = getWeekendDays();
            return weekendDays.contains(dayOfWeek);
        } catch (Exception e) {
            logger.error("Error checking weekend", e);
            return false;
        }
    }

    public Double getHolidayPayMultiplier(LocalDate date) {
        if (!isHoliday(date)) {
            return null;
        }

        List<Holiday> holidays = getAllHolidays();
        return holidays.stream()
                .filter(holiday -> {
                    if (holiday.isRecurring()) {
                        LocalDate holidayDate = holiday.getDate();
                        return holidayDate.getMonth() == date.getMonth() &&
                                holidayDate.getDayOfMonth() == date.getDayOfMonth();
                    }
                    return holiday.getDate().equals(date);
                })
                .findFirst()
                .map(Holiday::getPayMultiplier)
                .orElse(getSystemSettings().getHolidayRate());
    }

    public List<String> getEmployeeCategories() {
        try {
            SystemSettings settings = getSystemSettings();
            if (settings.getEmployeeCategories() == null || settings.getEmployeeCategories().trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(settings.getEmployeeCategories(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            logger.error("Error parsing employee categories", e);
            return new ArrayList<>();
        }
    }

    public List<Integer> getWeekendDays() {
        try {
            SystemSettings settings = getSystemSettings();
            if (settings.getWeekendDays() == null || settings.getWeekendDays().trim().isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(settings.getWeekendDays(), new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            logger.error("Error parsing weekend days", e);
            return new ArrayList<>();
        }
    }
    public AttendanceReportScheduleSettings getAttendanceScheduleSettings() {
        return scheduleRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    AttendanceReportScheduleSettings defaultSettings = new AttendanceReportScheduleSettings();
                    defaultSettings.setAutoAbsentEnabled(true);
                    defaultSettings.setAutoAbsentRunTime(LocalTime.of(0, 30));
                    defaultSettings.setAutoAbsentSkipCategories(new ArrayList<>());
                    return scheduleRepository.save(defaultSettings);
                });
    }

    public AttendanceReportScheduleSettings updateAttendanceScheduleSettings(AttendanceReportScheduleSettings settings) {
        AttendanceReportScheduleSettings existing = getAttendanceScheduleSettings();

        // Update auto-absent specific fields
        existing.setAutoAbsentEnabled(settings.isAutoAbsentEnabled());
        if (settings.isAutoAbsentEnabled() && settings.getAutoAbsentCutoffTime() == null) {
            throw new IllegalArgumentException("Choose an absence cutoff time before enabling auto-absence.");
        }
        existing.setAutoAbsentCutoffTime(settings.getAutoAbsentCutoffTime());
        existing.setAutoAbsentSkipCategories(settings.getAutoAbsentSkipCategories());

        return scheduleRepository.save(existing);
    }

    public Set<Long> getAllCategoryIdsToSkipForAutoAbsent() {
        Set<Long> skipCategoryIds = new HashSet<>();

        // 1. Get categories with skipAutoAbsent flag = true (from Category entity)
        List<Category> flaggedSkipCategories = categorySettingsRepository.findBySkipAutoAbsentTrue();
        for (Category category : flaggedSkipCategories) {
            skipCategoryIds.add(category.getId());
        }

        // 2. Get global skip categories from schedule settings
        AttendanceReportScheduleSettings settings = getAttendanceScheduleSettings();
        if (settings.getAutoAbsentSkipCategories() != null) {
            skipCategoryIds.addAll(settings.getAutoAbsentSkipCategories());
        }

        return skipCategoryIds;
    }

    // Add this method to your SettingsService class
    public SystemSettingsDTO getSystemSettingsDTO() {
        SystemSettings settings = getSystemSettings();
        SystemSettingsDTO dto = new SystemSettingsDTO();

        // Copy all fields from entity to DTO
        dto.setHourlyRate(settings.getHourlyRate());
        dto.setOvertimeHourlyRate(settings.getOvertimeHourlyRate());
        dto.setStandardWorkHours(settings.getStandardWorkHours());
        dto.setDoubleTimeOnSunday(settings.getDoubleTimeOnSunday());
        dto.setTimeAndHalfAfter8Hours(settings.getTimeAndHalfAfter8Hours());
        dto.setWeekendRate(settings.getWeekendRate());
        dto.setHolidayRate(settings.getHolidayRate());
        dto.setAccountantRecipients(settings.getAccountantRecipients());
        dto.setReviewerRecipients(settings.getReviewerRecipients());
        dto.setFinanceRecipients(settings.getFinanceRecipients());
        dto.setRequesterNotificationEnabled(
                settings.getRequesterNotificationEnabled()
        );
        dto.setAccountantNotificationEnabled(
                settings.getAccountantNotificationEnabled()
        );
        dto.setReviewerNotificationEnabled(
                settings.getReviewerNotificationEnabled()
        );
        dto.setFinanceNotificationEnabled(
                settings.getFinanceNotificationEnabled()
        );

        // Handle weekendDays conversion from String to List<Integer>
        try {
            if (settings.getWeekendDays() != null && !settings.getWeekendDays().isEmpty()) {
                String[] days = settings.getWeekendDays().split(",");
                List<Integer> weekendDaysList = new ArrayList<>();
                for (String day : days) {
                    weekendDaysList.add(Integer.parseInt(day.trim()));
                }
                dto.setWeekendDays(weekendDaysList);
            } else {
                dto.setWeekendDays(Arrays.asList(0, 6));
            }
        } catch (Exception e) {
            dto.setWeekendDays(Arrays.asList(0, 6));
        }

        dto.setWithholdingTaxEnabled(settings.getWithholdingTaxEnabled());
        dto.setWithholdingTaxRate(settings.getWithholdingTaxRate());
        dto.setCompanyName(settings.getCompanyName());
        dto.setCompanyEmail(settings.getCompanyEmail());
        dto.setCompanyPhone(settings.getCompanyPhone());
        dto.setCompanyAddress(settings.getCompanyAddress());

        // Handle employeeCategories - parse JSON string to Object
        try {
            if (settings.getEmployeeCategories() != null && !settings.getEmployeeCategories().isEmpty()) {
                // Try to parse as List, fallback to string
                try {
                    List<String> categories = objectMapper.readValue(settings.getEmployeeCategories(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                    dto.setEmployeeCategories(categories);
                } catch (Exception e) {
                    dto.setEmployeeCategories(settings.getEmployeeCategories());
                }
            } else {
                dto.setEmployeeCategories(new ArrayList<>());
            }
        } catch (Exception e) {
            dto.setEmployeeCategories(new ArrayList<>());
        }

        dto.setBillingReminderEmails(settings.getBillingReminderEmails());
        dto.setBillingReminderEnabled(settings.getBillingReminderEnabled());
        dto.setLateArrivalTime(settings.getLateArrivalTime());
        dto.setAccountRequestRecipients(settings.getAccountRequestRecipients());
        dto.setSupportRequestRecipients(settings.getSupportRequestRecipients());
        dto.setPettyCashThreshold(settings.getPettyCashThreshold());
        dto.setPettyCashRequestRecipients(settings.getPettyCashRequestRecipients());

        dto.setInventoryRequestRecipients(
                settings.getInventoryRequestRecipients()
        );
        dto.setInventoryPlannerRecipients(
                settings.getInventoryPlannerRecipients()
        );
        dto.setInventoryProcurementRecipients(
                settings.getInventoryProcurementRecipients()
        );
        dto.setInventoryStoreRecipients(
                settings.getInventoryStoreRecipients()
        );
        dto.setInventoryStatusRecipients(
                settings.getInventoryStatusRecipients()
        );

        dto.setInventoryRequesterNotificationEnabled(
                settings.getInventoryRequesterNotificationEnabled()
        );
        dto.setInventoryPlannerNotificationEnabled(
                settings.getInventoryPlannerNotificationEnabled()
        );
        dto.setInventoryProcurementNotificationEnabled(
                settings.getInventoryProcurementNotificationEnabled()
        );
        dto.setInventoryStoreNotificationEnabled(
                settings.getInventoryStoreNotificationEnabled()
        );
        dto.setInventoryStatusNotificationEnabled(
                settings.getInventoryStatusNotificationEnabled()
        );

        // PPE notification recipients
        dto.setPpeRequestRecipients(
                settings.getPpeRequestRecipients()
        );
        dto.setPpeProcurementRecipients(
                settings.getPpeProcurementRecipients()
        );
        dto.setPpeStoreRecipients(
                settings.getPpeStoreRecipients()
        );
        dto.setPpeStatusRecipients(
                settings.getPpeStatusRecipients()
        );

        // PPE notification toggles
        dto.setPpeRequesterNotificationEnabled(
                settings.getPpeRequesterNotificationEnabled()
        );
        dto.setPpeProcurementNotificationEnabled(
                settings.getPpeProcurementNotificationEnabled()
        );
        dto.setPpeStoreNotificationEnabled(
                settings.getPpeStoreNotificationEnabled()
        );
        dto.setPpeStatusNotificationEnabled(
                settings.getPpeStatusNotificationEnabled()
        );

        // Leave and attendance notification fields
        dto.setLeaveRequestRecipients(settings.getLeaveRequestRecipients());
        dto.setAttendanceAlertRecipients(settings.getAttendanceAlertRecipients());
        dto.setLateArrivalRecipients(settings.getLateArrivalRecipients());
        dto.setOvertimeRequestRecipients(settings.getOvertimeRequestRecipients());
        dto.setAttendanceValidationRecipients(settings.getAttendanceValidationRecipients());
        dto.setLeaveApprovalRecipients(settings.getLeaveApprovalRecipients());

        // Overtime multiplier fields
        dto.setDefaultOvertimeMultiplier(settings.getDefaultOvertimeMultiplier());
        dto.setSundayOvertimeMultiplier(settings.getSundayOvertimeMultiplier());
        dto.setHolidayOvertimeMultiplier(settings.getHolidayOvertimeMultiplier());
        dto.setEnableTimeAndHalfAfter8Hours(settings.getEnableTimeAndHalfAfter8Hours());
        dto.setTimeAndHalfMultiplier(settings.getTimeAndHalfMultiplier());

        // Loan notification fields
        dto.setLoanRequestRecipients(settings.getLoanRequestRecipients());
        dto.setLoanApprovalRecipients(settings.getLoanApprovalRecipients());
        dto.setLoanRejectionRecipients(settings.getLoanRejectionRecipients());
        dto.setLoanConsolidationRecipients(settings.getLoanConsolidationRecipients());

        return dto;
    }


}
