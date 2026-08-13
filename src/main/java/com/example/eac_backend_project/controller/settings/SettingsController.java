package com.example.eac_backend_project.controller.settings;

import com.example.eac_backend_project.Service.settings.SettingsService;
import com.example.eac_backend_project.dto.SystemSettingsDTO;
import com.example.eac_backend_project.entity.settings.*;
import com.example.eac_backend_project.repository.settings.LeaveSettingsRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private LeaveSettingsRepository leaveSettingsRepository;

    @GetMapping("/leave")
    public ResponseEntity<LeaveSettings> getLeaveSettings() {
        LeaveSettings settings = leaveSettingsRepository.findTopByOrderByIdDesc();
        if (settings == null) {
            settings = new LeaveSettings();
        }
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/leave")
    public ResponseEntity<LeaveSettings> updateLeaveSettings(@RequestBody LeaveSettings leaveSettings) {
        LeaveSettings existingSettings = leaveSettingsRepository.findTopByOrderByIdDesc();
        if (existingSettings != null) {
            leaveSettings.setId(existingSettings.getId());
        }
        LeaveSettings savedSettings = leaveSettingsRepository.save(leaveSettings);
        return ResponseEntity.ok(savedSettings);
    }

    @GetMapping("/loanSettings")
    public ResponseEntity<?> getLoanSettings() {
        try {
            List<LoanSettings> settings = settingsService.getAllLoanSettings();
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/loanSettings")
    public ResponseEntity<?> createLoanSettings(@RequestBody LoanSettings loanSettings) {
        try {
            LoanSettings savedLoanSettings = settingsService.createLoanSettings(loanSettings);
            return ResponseEntity.ok(savedLoanSettings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/loanSettings/{id}")
    public ResponseEntity<?> updateLoanSettings(@PathVariable Long id,
                                                @RequestBody LoanSettings loanSettings) {
        try {
            LoanSettings updatedLoanSettings = settingsService.updateLoanSettings(id, loanSettings);
            return ResponseEntity.ok(updatedLoanSettings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Categories Endpoints
    @GetMapping("/categories")
    public List<Category> getAllCategories() {
        return settingsService.getAllCategories();
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@Valid @RequestBody Category category) {
        if (category.getAnnualLeaveDays() != null &&
                (category.getAnnualLeaveDays() < 0 || category.getAnnualLeaveDays() > 365)) {
            return ResponseEntity.badRequest().body("Annual leave days must be between 0 and 365");
        }
        Category saved = settingsService.createCategory(category);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        try {
            Category updatedCategory = settingsService.updateCategory(id, category);
            return ResponseEntity.ok(updatedCategory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            settingsService.deleteCategory(id);
            return ResponseEntity.ok().body(Map.of("message", "Category deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Special Weekends Endpoints
    @GetMapping("/special-weekends")
    public List<SpecialWeekend> getAllSpecialWeekends() {
        return settingsService.getAllSpecialWeekends();
    }

    @PostMapping("/special-weekends")
    public ResponseEntity<?> createSpecialWeekend(@RequestBody SpecialWeekend specialWeekend) {
        try {
            SpecialWeekend savedSpecialWeekend = settingsService.createSpecialWeekend(specialWeekend);
            return ResponseEntity.ok(savedSpecialWeekend);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/special-weekends/{id}")
    public ResponseEntity<?> updateSpecialWeekend(@PathVariable Long id, @RequestBody SpecialWeekend specialWeekend) {
        try {
            SpecialWeekend updatedSpecialWeekend = settingsService.updateSpecialWeekend(id, specialWeekend);
            return ResponseEntity.ok(updatedSpecialWeekend);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/special-weekends/{id}")
    public ResponseEntity<?> deleteSpecialWeekend(@PathVariable Long id) {
        try {
            settingsService.deleteSpecialWeekend(id);
            return ResponseEntity.ok().body(Map.of("message", "Special weekend deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/holidays")
    public List<Holiday> getAllHolidays() {
        return settingsService.getAllHolidays();
    }

    @PostMapping("/holidays")
    public ResponseEntity<?> createHoliday(@RequestBody Holiday holiday) {
        try {
            Holiday savedHoliday = settingsService.createHoliday(holiday);
            return ResponseEntity.ok(savedHoliday);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/holidays/{id}")
    public ResponseEntity<?> updateHoliday(@PathVariable Long id, @RequestBody Holiday holiday) {
        try {
            Holiday updatedHoliday = settingsService.updateHoliday(id, holiday);
            return ResponseEntity.ok(updatedHoliday);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<?> deleteHoliday(@PathVariable Long id) {
        try {
            settingsService.deleteHoliday(id);
            return ResponseEntity.ok().body(Map.of("message", "Holiday deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/job-positions")
    public List<JobPosition> getAllJobPositions() {
        return settingsService.getAllJobPositions();
    }

    @PostMapping("/job-positions")
    public ResponseEntity<?> createJobPosition(@RequestBody JobPosition jobPosition) {
        try {
            JobPosition savedPosition = settingsService.createJobPosition(jobPosition);
            return ResponseEntity.ok(savedPosition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/job-positions/{id}")
    public ResponseEntity<?> updateJobPosition(@PathVariable Long id, @RequestBody JobPosition jobPosition) {
        try {
            JobPosition updatedPosition = settingsService.updateJobPosition(id, jobPosition);
            return ResponseEntity.ok(updatedPosition);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/job-positions/{id}")
    public ResponseEntity<?> deleteJobPosition(@PathVariable Long id) {
        try {
            settingsService.deleteJobPosition(id);
            return ResponseEntity.ok().body(Map.of("message", "Job position deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/system")
    public SystemSettings getSystemSettings() {
        return settingsService.getSystemSettings();
    }

    @PutMapping("/system")
    public ResponseEntity<?> updateSystemSettings(@RequestBody SystemSettingsDTO settingsDTO) {
        try {
            SystemSettings systemSettings = settingsService.updateSystemSettings(settingsDTO);
            return ResponseEntity.ok(systemSettings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // CORRECTED: Loan Notifications GET Endpoint
    @GetMapping("/loan-notifications")
    public ResponseEntity<?> getLoanNotificationSettings() {
        try {
            SystemSettings systemSettings = settingsService.getSystemSettings();
            Map<String, String> notificationSettings = new HashMap<>();
            notificationSettings.put("loanRequestRecipients",
                    systemSettings.getLoanRequestRecipients() != null ? systemSettings.getLoanRequestRecipients() : "");
            notificationSettings.put("loanApprovalRecipients",
                    systemSettings.getLoanApprovalRecipients() != null ? systemSettings.getLoanApprovalRecipients() : "");
            notificationSettings.put("loanRejectionRecipients",
                    systemSettings.getLoanRejectionRecipients() != null ? systemSettings.getLoanRejectionRecipients() : "");
            notificationSettings.put("loanConsolidationRecipients",
                    systemSettings.getLoanConsolidationRecipients() != null ? systemSettings.getLoanConsolidationRecipients() : "");
            return ResponseEntity.ok(notificationSettings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // CORRECTED: Loan Notifications PUT Endpoint - Uses DTO
    @PutMapping("/loan-notifications")
    public ResponseEntity<?> updateLoanNotificationSettings(@RequestBody Map<String, String> settings) {
        try {
            // First get the current system settings as DTO
            SystemSettingsDTO systemSettingsDTO = settingsService.getSystemSettingsDTO();

            // Update only the loan notification fields
            systemSettingsDTO.setLoanRequestRecipients(settings.get("loanRequestRecipients"));
            systemSettingsDTO.setLoanApprovalRecipients(settings.get("loanApprovalRecipients"));
            systemSettingsDTO.setLoanRejectionRecipients(settings.get("loanRejectionRecipients"));
            systemSettingsDTO.setLoanConsolidationRecipients(settings.get("loanConsolidationRecipients"));

            // Update using DTO (which matches what updateSystemSettings expects)
            SystemSettings updatedSettings = settingsService.updateSystemSettings(systemSettingsDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Loan notification settings updated successfully");
            response.put("settings", updatedSettings);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/check-holiday")
    public Map<String, Object> checkHoliday(@RequestParam String date) {
        LocalDate checkDate = LocalDate.parse(date);
        boolean isHoliday = settingsService.isHoliday(checkDate);
        Double multiplier = settingsService.getHolidayPayMultiplier(checkDate);

        Map<String, Object> response = new HashMap<>();
        response.put("isHoliday", isHoliday);
        response.put("payMultiplier", multiplier);
        response.put("date", checkDate.toString());

        return response;
    }

    @GetMapping("/check-weekend")
    public Map<String, Object> checkWeekend(@RequestParam String date) {
        LocalDate checkDate = LocalDate.parse(date);
        boolean isWeekend = settingsService.isWeekend(checkDate);

        Map<String, Object> response = new HashMap<>();
        response.put("isWeekend", isWeekend);
        response.put("date", checkDate.toString());
        response.put("dayOfWeek", checkDate.getDayOfWeek().toString());

        return response;
    }

    @GetMapping("/all")
    public Map<String, Object> getAllSettings() {
        Map<String, Object> allSettings = new HashMap<>();

        allSettings.put("system", settingsService.getSystemSettings());
        allSettings.put("holidays", settingsService.getAllHolidays());
        allSettings.put("jobPositions", settingsService.getAllJobPositions());
        allSettings.put("employeeCategories", settingsService.getEmployeeCategories());
        allSettings.put("weekendDays", settingsService.getWeekendDays());
        allSettings.put("categories", settingsService.getAllCategories());
        allSettings.put("specialWeekends", settingsService.getAllSpecialWeekends());
        allSettings.put("loanSettings", settingsService.getAllLoanSettings());

        return allSettings;
    }

    @GetMapping("/attendance-schedule")
    public ResponseEntity<AttendanceReportScheduleSettings> getAttendanceSchedule() {
        return ResponseEntity.ok(settingsService.getAttendanceScheduleSettings());
    }

    @PutMapping("/attendance-schedule")
    public ResponseEntity<AttendanceReportScheduleSettings> updateAttendanceSchedule(
            @RequestBody AttendanceReportScheduleSettings settings) {
        return ResponseEntity.ok(settingsService.updateAttendanceScheduleSettings(settings));
    }

    @GetMapping("/attendance-skip-categories")
    public ResponseEntity<Map<String, Set<Long>>> getSkipCategories() {
        Set<Long> skipCategoryIds = settingsService.getAllCategoryIdsToSkipForAutoAbsent();
        return ResponseEntity.ok(Map.of("skipCategoryIds", skipCategoryIds));
    }

    @GetMapping("/direct-purchase-notifications")
    public ResponseEntity<?> getDirectPurchaseNotificationSettings() {
        SystemSettings settings = settingsService.getSystemSettings();

        Map<String, Object> response = new HashMap<>();
        response.put(
                "accountantRecipients",
                Objects.toString(settings.getAccountantRecipients(), "")
        );
        response.put(
                "reviewerRecipients",
                Objects.toString(settings.getReviewerRecipients(), "")
        );
        response.put(
                "financeRecipients",
                Objects.toString(settings.getFinanceRecipients(), "")
        );
        response.put(
                "requesterNotificationEnabled",
                settings.getRequesterNotificationEnabled() == null ||
                        settings.getRequesterNotificationEnabled()
        );
        response.put(
                "accountantNotificationEnabled",
                settings.getAccountantNotificationEnabled() == null ||
                        settings.getAccountantNotificationEnabled()
        );
        response.put(
                "reviewerNotificationEnabled",
                settings.getReviewerNotificationEnabled() == null ||
                        settings.getReviewerNotificationEnabled()
        );
        response.put(
                "financeNotificationEnabled",
                settings.getFinanceNotificationEnabled() == null ||
                        settings.getFinanceNotificationEnabled()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/direct-purchase-notifications")
    public ResponseEntity<?> updateDirectPurchaseNotificationSettings(
            @RequestBody Map<String, Object> payload
    ) {
        SystemSettingsDTO dto = settingsService.getSystemSettingsDTO();

        dto.setAccountantRecipients(
                Objects.toString(payload.get("accountantRecipients"), "")
        );
        dto.setReviewerRecipients(
                Objects.toString(payload.get("reviewerRecipients"), "")
        );
        dto.setFinanceRecipients(
                Objects.toString(payload.get("financeRecipients"), "")
        );

        dto.setRequesterNotificationEnabled(
                asBoolean(
                        payload.get("requesterNotificationEnabled"),
                        true
                )
        );
        dto.setAccountantNotificationEnabled(
                asBoolean(
                        payload.get("accountantNotificationEnabled"),
                        true
                )
        );
        dto.setReviewerNotificationEnabled(
                asBoolean(
                        payload.get("reviewerNotificationEnabled"),
                        true
                )
        );
        dto.setFinanceNotificationEnabled(
                asBoolean(
                        payload.get("financeNotificationEnabled"),
                        true
                )
        );

        settingsService.updateSystemSettings(dto);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Direct Purchase notification settings updated"
                )
        );
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @GetMapping("/inventory-notifications")
    public ResponseEntity<?> getInventoryNotificationSettings() {
        try {
            SystemSettings settings = settingsService.getSystemSettings();

            Map<String, Object> response = new HashMap<>();

            // =====================================================
            // NORMAL INVENTORY RECIPIENTS
            // =====================================================
            response.put(
                    "inventoryRequestRecipients",
                    Objects.toString(settings.getInventoryRequestRecipients(), "")
            );
            response.put(
                    "inventoryPlannerRecipients",
                    Objects.toString(settings.getInventoryPlannerRecipients(), "")
            );
            response.put(
                    "inventoryProcurementRecipients",
                    Objects.toString(settings.getInventoryProcurementRecipients(), "")
            );
            response.put(
                    "inventoryStoreRecipients",
                    Objects.toString(settings.getInventoryStoreRecipients(), "")
            );
            response.put(
                    "inventoryStatusRecipients",
                    Objects.toString(settings.getInventoryStatusRecipients(), "")
            );

            // =====================================================
            // NORMAL INVENTORY TOGGLES
            // =====================================================
            response.put(
                    "inventoryRequesterNotificationEnabled",
                    settings.getInventoryRequesterNotificationEnabled() == null
                            || settings.getInventoryRequesterNotificationEnabled()
            );
            response.put(
                    "inventoryPlannerNotificationEnabled",
                    settings.getInventoryPlannerNotificationEnabled() == null
                            || settings.getInventoryPlannerNotificationEnabled()
            );
            response.put(
                    "inventoryProcurementNotificationEnabled",
                    settings.getInventoryProcurementNotificationEnabled() == null
                            || settings.getInventoryProcurementNotificationEnabled()
            );
            response.put(
                    "inventoryStoreNotificationEnabled",
                    settings.getInventoryStoreNotificationEnabled() == null
                            || settings.getInventoryStoreNotificationEnabled()
            );
            response.put(
                    "inventoryStatusNotificationEnabled",
                    settings.getInventoryStatusNotificationEnabled() == null
                            || settings.getInventoryStatusNotificationEnabled()
            );

            // =====================================================
            // PPE RECIPIENTS
            // =====================================================
            response.put(
                    "ppeRequestRecipients",
                    Objects.toString(settings.getPpeRequestRecipients(), "")
            );
            response.put(
                    "ppeProcurementRecipients",
                    Objects.toString(settings.getPpeProcurementRecipients(), "")
            );
            response.put(
                    "ppeStoreRecipients",
                    Objects.toString(settings.getPpeStoreRecipients(), "")
            );
            response.put(
                    "ppeStatusRecipients",
                    Objects.toString(settings.getPpeStatusRecipients(), "")
            );

            // =====================================================
            // PPE TOGGLES
            // =====================================================
            response.put(
                    "ppeRequesterNotificationEnabled",
                    settings.getPpeRequesterNotificationEnabled() == null
                            || settings.getPpeRequesterNotificationEnabled()
            );
            response.put(
                    "ppeProcurementNotificationEnabled",
                    settings.getPpeProcurementNotificationEnabled() == null
                            || settings.getPpeProcurementNotificationEnabled()
            );
            response.put(
                    "ppeStoreNotificationEnabled",
                    settings.getPpeStoreNotificationEnabled() == null
                            || settings.getPpeStoreNotificationEnabled()
            );
            response.put(
                    "ppeStatusNotificationEnabled",
                    settings.getPpeStatusNotificationEnabled() == null
                            || settings.getPpeStatusNotificationEnabled()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    @PutMapping("/inventory-notifications")
    public ResponseEntity<?> updateInventoryNotificationSettings(
            @RequestBody Map<String, Object> payload
    ) {
        try {
            SystemSettingsDTO dto = settingsService.getSystemSettingsDTO();

            // =====================================================
            // NORMAL INVENTORY RECIPIENTS
            // =====================================================
            dto.setInventoryRequestRecipients(
                    Objects.toString(
                            payload.get("inventoryRequestRecipients"),
                            ""
                    ).trim()
            );

            dto.setInventoryPlannerRecipients(
                    Objects.toString(
                            payload.get("inventoryPlannerRecipients"),
                            ""
                    ).trim()
            );

            dto.setInventoryProcurementRecipients(
                    Objects.toString(
                            payload.get("inventoryProcurementRecipients"),
                            ""
                    ).trim()
            );

            dto.setInventoryStoreRecipients(
                    Objects.toString(
                            payload.get("inventoryStoreRecipients"),
                            ""
                    ).trim()
            );

            dto.setInventoryStatusRecipients(
                    Objects.toString(
                            payload.get("inventoryStatusRecipients"),
                            ""
                    ).trim()
            );

            // =====================================================
            // NORMAL INVENTORY TOGGLES
            // =====================================================
            dto.setInventoryRequesterNotificationEnabled(
                    asBoolean(
                            payload.get("inventoryRequesterNotificationEnabled"),
                            true
                    )
            );

            dto.setInventoryPlannerNotificationEnabled(
                    asBoolean(
                            payload.get("inventoryPlannerNotificationEnabled"),
                            true
                    )
            );

            dto.setInventoryProcurementNotificationEnabled(
                    asBoolean(
                            payload.get("inventoryProcurementNotificationEnabled"),
                            true
                    )
            );

            dto.setInventoryStoreNotificationEnabled(
                    asBoolean(
                            payload.get("inventoryStoreNotificationEnabled"),
                            true
                    )
            );

            dto.setInventoryStatusNotificationEnabled(
                    asBoolean(
                            payload.get("inventoryStatusNotificationEnabled"),
                            true
                    )
            );

            // =====================================================
            // PPE RECIPIENTS
            // =====================================================
            dto.setPpeRequestRecipients(
                    Objects.toString(
                            payload.get("ppeRequestRecipients"),
                            ""
                    ).trim()
            );

            dto.setPpeProcurementRecipients(
                    Objects.toString(
                            payload.get("ppeProcurementRecipients"),
                            ""
                    ).trim()
            );

            dto.setPpeStoreRecipients(
                    Objects.toString(
                            payload.get("ppeStoreRecipients"),
                            ""
                    ).trim()
            );

            dto.setPpeStatusRecipients(
                    Objects.toString(
                            payload.get("ppeStatusRecipients"),
                            ""
                    ).trim()
            );

            // =====================================================
            // PPE TOGGLES
            // =====================================================
            dto.setPpeRequesterNotificationEnabled(
                    asBoolean(
                            payload.get("ppeRequesterNotificationEnabled"),
                            true
                    )
            );

            dto.setPpeProcurementNotificationEnabled(
                    asBoolean(
                            payload.get("ppeProcurementNotificationEnabled"),
                            true
                    )
            );

            dto.setPpeStoreNotificationEnabled(
                    asBoolean(
                            payload.get("ppeStoreNotificationEnabled"),
                            true
                    )
            );

            dto.setPpeStatusNotificationEnabled(
                    asBoolean(
                            payload.get("ppeStatusNotificationEnabled"),
                            true
                    )
            );

            SystemSettings updatedSettings =
                    settingsService.updateSystemSettings(dto);

            Map<String, Object> response = new HashMap<>();
            response.put(
                    "message",
                    "Inventory and PPE notification settings updated successfully"
            );
            response.put("settings", updatedSettings);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Failed to update notification settings"
                    )
            );
        }
    }
}