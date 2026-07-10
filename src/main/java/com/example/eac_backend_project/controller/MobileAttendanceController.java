package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.AttendanceService;
import com.example.eac_backend_project.Service.EmployeeNameMatchingService;
import com.example.eac_backend_project.dto.BiometricRecordDTO;
import com.example.eac_backend_project.entity.attendance.Attendance;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/ONEPASS")
public class MobileAttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeNameMatchingService nameMatchingService;
    private final ObjectMapper objectMapper;

    // Store recent biometric attempts (in-memory cache)
    private final Map<String, Map<String, Object>> recentAttempts = Collections.synchronizedMap(
            new LinkedHashMap<String, Map<String, Object>>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
                    return size() > MAX_ATTEMPTS;
                }
            }
    );
    private static final int MAX_ATTEMPTS = 100;

    // ========== SOLUTION 1: DUPLICATE DETECTION & BLOCKING ==========
    // Cache to track recently processed/failed records
    private final Map<String, Long> processedRecordCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> failedAttemptCounts = new ConcurrentHashMap<>();
    private final Set<String> blockedUserIds = ConcurrentHashMap.newKeySet();
    private static final long DUPLICATE_WINDOW_MS = 300000; // 5 minutes
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long BLOCK_DURATION_MS = 3600000; // 1 hour block

    // Scheduled executor for cache cleanup
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public MobileAttendanceController(AttendanceService attendanceService,
                                      EmployeeNameMatchingService nameMatchingService,
                                      ObjectMapper objectMapper) {
        this.attendanceService = attendanceService;
        this.nameMatchingService = nameMatchingService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // Schedule cache cleanup every 30 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupCache();
            } catch (Exception e) {
                System.err.println("Error during scheduled cache cleanup: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.MINUTES);

        System.out.println("✅ MobileAttendanceController initialized with duplicate detection and auto-blocking");
    }

    /**
     * Generate a unique fingerprint for a biometric record to detect duplicates
     */
    private String generateRecordFingerprint(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder();

        // Use userId if available (most reliable)
        Object userId = payload.get("userId");
        if (userId != null) {
            sb.append("userId:").append(userId.toString());
        }

        // Add timestamp from device
        String time = (String) payload.get("time");
        if (time != null) {
            sb.append("|time:").append(time);
        }

        // Add user name if available
        String name = extractEmployeeName(payload);
        if (name != null) {
            sb.append("|name:").append(name);
        }

        // Add sign type
        String signType = (String) payload.get("signType");
        if (signType != null) {
            sb.append("|signType:").append(signType);
        }

        return sb.toString();
    }

    /**
     * Check if this record has been processed recently
     */
    private boolean isDuplicateRecord(String fingerprint) {
        Long lastProcessed = processedRecordCache.get(fingerprint);
        if (lastProcessed != null) {
            long timeSinceLast = System.currentTimeMillis() - lastProcessed;
            if (timeSinceLast < DUPLICATE_WINDOW_MS) {
                System.out.println("⚠️ DUPLICATE DETECTED - Record processed " + timeSinceLast/1000 + " seconds ago");
                return true;
            }
        }
        return false;
    }

    /**
     * Block a user permanently or temporarily
     */
    private void blockUser(String userId, String reason) {
        if (userId != null && !userId.isEmpty()) {
            blockedUserIds.add(userId);
            // Store when they were blocked for auto-unblock
            processedRecordCache.put("block_time_" + userId, System.currentTimeMillis());
            System.out.println("🚫 User blocked: " + userId + " - " + reason);
        }
    }

    /**
     * Check if user is blocked
     */
    private boolean isUserBlocked(Map<String, Object> payload) {
        Object userId = payload.get("userId");
        if (userId != null) {
            String userIdStr = userId.toString();
            if (blockedUserIds.contains(userIdStr)) {
                // Check if block has expired
                Long blockTime = processedRecordCache.get("block_time_" + userIdStr);
                if (blockTime != null && (System.currentTimeMillis() - blockTime) > BLOCK_DURATION_MS) {
                    // Auto-unblock after duration
                    blockedUserIds.remove(userIdStr);
                    processedRecordCache.remove("block_time_" + userIdStr);
                    System.out.println("✅ User auto-unblocked after timeout: " + userIdStr);
                    return false;
                }
                System.out.println("🚫 BLOCKED: User " + userId + " is blocked");
                return true;
            }
        }
        return false;
    }

    /**
     * Clean up old cache entries
     */
    private void cleanupCache() {
        long now = System.currentTimeMillis();
        int beforeSize = processedRecordCache.size();
        int beforeFailedSize = failedAttemptCounts.size();

        // Clean processed record cache
        processedRecordCache.entrySet().removeIf(entry -> {
            // Keep block timestamps longer
            if (entry.getKey().startsWith("block_time_")) {
                return (now - entry.getValue()) > BLOCK_DURATION_MS;
            }
            // Regular entries expire after duplicate window
            return (now - entry.getValue()) > DUPLICATE_WINDOW_MS;
        });

        // Clean failed attempt counts (keep for 1 hour)
        failedAttemptCounts.entrySet().removeIf(entry -> {
            // We don't store timestamps for failed attempts, so we'll use a different approach
            // For simplicity, we'll clean based on a separate timestamp map
            Long lastAttempt = processedRecordCache.get("fail_time_" + entry.getKey());
            return lastAttempt != null && (now - lastAttempt) > 3600000;
        });

        System.out.println("🧹 Cache cleanup completed - Removed " +
                (beforeSize - processedRecordCache.size()) + " processed entries, " +
                (beforeFailedSize - failedAttemptCounts.size()) + " failed attempts");
    }

    @PostMapping("/UPLOAD_RECORD_NOW")
    public ResponseEntity<Map<String, Object>> receiveBiometricAttendance(
            @RequestBody Map<String, Object> payload) {

        // Generate unique ID for this attempt
        String attemptId = String.valueOf(System.currentTimeMillis());

        // ========== SOLUTION 2: DUPLICATE & BLOCKING CHECKS (AT THE VERY BEGINNING) ==========

        // Check if user is blocked
        if (isUserBlocked(payload)) {
            System.out.println("🚫 REQUEST REJECTED: User is blocked - Returning 200 to stop device retries");
            // Return 200 to stop device from retrying, but with a "blocked" message
            return ResponseEntity.ok(Map.of(
                    "result", "blocked",
                    "code", 200,
                    "message", "User is temporarily blocked - please contact administrator"
            ));
        }

        // Generate fingerprint to check for duplicates
        String fingerprint = generateRecordFingerprint(payload);

        // Extract user info for logging
        String employeeName = extractEmployeeName(payload);
        Object userId = payload.get("userId");
        String timeStr = (String) payload.get("time");

        // Check for duplicate within time window
        if (isDuplicateRecord(fingerprint)) {
            System.out.println("⚠️ DUPLICATE REQUEST REJECTED - User: " + employeeName +
                    ", Time: " + timeStr + ", UserId: " + userId);

            // Return 200 to stop device from retrying, but indicate it was skipped
            return ResponseEntity.ok(Map.of(
                    "result", "skipped",
                    "code", 200,
                    "message", "Duplicate request - already processed",
                    "attemptId", attemptId
            ));
        }

        // Store that we've seen this fingerprint
        processedRecordCache.put(fingerprint, System.currentTimeMillis());

        // Check if employee exists in database BEFORE processing
        Optional<Employee> employeeOpt = nameMatchingService.findEmployeeByDeviceName(employeeName);

        // Track failed attempts for non-existent employees
        if (!employeeOpt.isPresent()) {
            String failKey = userId != null ? userId.toString() : employeeName;
            if (failKey != null && !failKey.isEmpty()) {
                int failCount = failedAttemptCounts.getOrDefault(failKey, 0) + 1;
                failedAttemptCounts.put(failKey, failCount);
                // Store the time of this failed attempt
                processedRecordCache.put("fail_time_" + failKey, System.currentTimeMillis());

                System.out.println("❌ Failed attempt " + failCount + "/" + MAX_FAILED_ATTEMPTS +
                        " for user: " + failKey + " (" + employeeName + ")");

                // Auto-block after max failed attempts
                if (failCount >= MAX_FAILED_ATTEMPTS) {
                    if (userId != null) {
                        blockUser(userId.toString(), "Employee not found after " + failCount + " attempts: " + employeeName);

                        // Return 200 to stop device retries
                        return ResponseEntity.ok(Map.of(
                                "result", "blocked",
                                "code", 200,
                                "message", "User has been blocked after multiple failed attempts - contact administrator",
                                "attemptId", attemptId
                        ));
                    }
                }
            }
        }

        // Create base attempt record early with null-safe initialization
        Map<String, Object> attemptRecord = new ConcurrentHashMap<>();

        try {
            // ========== COMPREHENSIVE LOGGING ==========
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📱 BIOMETRIC DEVICE REQUEST RECEIVED");
            System.out.println("=".repeat(80));

            // Log timestamp
            System.out.println("⏰ Server Time: " + LocalDateTime.now());

            // Log all top-level fields in the payload
            System.out.println("\n📦 PAYLOAD CONTENTS:");
            System.out.println("-".repeat(40));
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Map) {
                    System.out.println("  🔹 " + key + " (Map):");
                    Map<?, ?> mapValue = (Map<?, ?>) value;
                    for (Map.Entry<?, ?> mapEntry : mapValue.entrySet()) {
                        System.out.println("      - " + mapEntry.getKey() + ": " + mapEntry.getValue());
                    }
                } else if (value instanceof List) {
                    System.out.println("  🔹 " + key + " (List): " + value);
                } else {
                    System.out.println("  🔹 " + key + ": " + value);
                }
            }

            // Extract and log employee name specifically
            System.out.println("\n👤 EMPLOYEE INFORMATION:");
            System.out.println("-".repeat(40));
            System.out.println("  Extracted name from device: '" + employeeName + "'");

            // Log all possible places where name might be
            Object userObj = payload.get("user");
            if (userObj instanceof Map) {
                Map<String, Object> userMap = (Map<String, Object>) userObj;
                System.out.println("  User object fields: " + userMap.keySet());
                if (userMap.containsKey("name")) {
                    System.out.println("  user.name = '" + userMap.get("name") + "'");
                }
                if (userMap.containsKey("employeeName")) {
                    System.out.println("  user.employeeName = '" + userMap.get("employeeName") + "'");
                }
                if (userMap.containsKey("fullName")) {
                    System.out.println("  user.fullName = '" + userMap.get("fullName") + "'");
                }
            }

            String userJson = (String) payload.get("userJson");
            if (userJson != null) {
                System.out.println("  userJson present (length: " + userJson.length() + ")");
                System.out.println("  userJson preview: " + userJson.substring(0, Math.min(100, userJson.length())) + "...");
            }

            // Log timestamp and signOn
            System.out.println("\n⏱️ TIME INFORMATION:");
            System.out.println("-".repeat(40));
            Boolean isSignOn = (Boolean) payload.get("signOn");
            String signType = (String) payload.get("signType");

            System.out.println("  time: " + timeStr);
            System.out.println("  signOn: " + isSignOn);
            System.out.println("  signType: " + signType);

            // Initialize attempt record with basic info
            attemptRecord.put("id", attemptId);
            attemptRecord.put("timestamp", LocalDateTime.now().toString());
            attemptRecord.put("deviceTimestamp", timeStr != null ? timeStr : "unknown");
            attemptRecord.put("action", isSignOn != null && isSignOn ? "SIGN ON" : "SIGN OFF");
            attemptRecord.put("deviceName", employeeName != null ? employeeName : "unknown");
            attemptRecord.put("signType", signType != null ? signType : "unknown");

            // Validate required fields
            if (employeeName == null || employeeName.isEmpty()) {
                attemptRecord.put("status", "error");
                attemptRecord.put("errorMessage", "Could not extract employee name from device data");
                storeRecentAttempt(attemptId, attemptRecord);

                System.out.println("\n❌ ERROR: Could not extract employee name");
                return ResponseEntity.badRequest().body(Map.of(
                        "result", "error",
                        "code", 400,
                        "message", "Could not extract employee name from device data"
                ));
            }

            if (timeStr == null || timeStr.isEmpty()) {
                attemptRecord.put("status", "error");
                attemptRecord.put("errorMessage", "Missing timestamp in device data");
                storeRecentAttempt(attemptId, attemptRecord);

                System.out.println("\n❌ ERROR: Missing timestamp");
                return ResponseEntity.badRequest().body(Map.of(
                        "result", "error",
                        "code", 400,
                        "message", "Missing timestamp in device data"
                ));
            }

            // Try to find employee (we already did this above, but let's do it again for the flow)
            System.out.println("\n🔍 DATABASE LOOKUP:");
            System.out.println("-".repeat(40));

            if (!employeeOpt.isPresent()) {
                // Get list of similar names for suggestion
                List<String> similarNames = nameMatchingService.findSimilarEmployeeNames(employeeName);

                System.out.println("  ❌ No employee found with name: '" + employeeName + "'");
                if (!similarNames.isEmpty()) {
                    System.out.println("  💡 Did you mean: " + String.join(", ", similarNames));
                }

                attemptRecord.put("status", "error");
                attemptRecord.put("errorMessage", "Employee not found: '" + employeeName + "'");
                attemptRecord.put("suggestedNames", similarNames);
                storeRecentAttempt(attemptId, attemptRecord);

                return ResponseEntity.status(404).body(Map.of(
                        "result", "error",
                        "code", 404,
                        "message", "Employee not found: '" + employeeName + "'",
                        "suggestedNames", similarNames
                ));
            }

            Employee employee = employeeOpt.get();
            System.out.println("  ✅ Found employee in database:");
            System.out.println("     ID: " + employee.getId());
            System.out.println("     Name: " + employee.getFirstName() + " " + employee.getLastName());
            System.out.println("     Email: " + employee.getEmail());

            // Reset failed attempt count if employee exists (successful authentication)
            String failKey = userId != null ? userId.toString() : employeeName;
            if (failKey != null) {
                failedAttemptCounts.remove(failKey);
                processedRecordCache.remove("fail_time_" + failKey);
                // If user was blocked but now exists in DB, unblock them
                if (userId != null && blockedUserIds.contains(userId.toString())) {
                    blockedUserIds.remove(userId.toString());
                    processedRecordCache.remove("block_time_" + userId.toString());
                    System.out.println("✅ User auto-unblocked after successful authentication: " + userId);
                }
            }

            // Check if employee is active
            if (employee.getActive() == null || !employee.getActive()) {
                attemptRecord.put("status", "error");
                attemptRecord.put("errorMessage", "Employee is inactive in the system");
                attemptRecord.put("employeeId", employee.getId());
                attemptRecord.put("employeeDbName", employee.getFirstName() + " " + employee.getLastName());
                storeRecentAttempt(attemptId, attemptRecord);

                return ResponseEntity.status(403).body(Map.of(
                        "result", "error",
                        "code", 403,
                        "message", "Employee '" + employeeName + "' is inactive in the system"
                ));
            }

            // Parse timestamp
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(timeStr, formatter);

            // Create BiometricRecordDTO
            BiometricRecordDTO dto = new BiometricRecordDTO();
            dto.setEmployeeId(employee.getId());
            dto.setEmployeeName(employeeName);
            dto.setTimestamp(dateTime);
            dto.setAction(isSignOn != null && isSignOn ? "SIGN ON" : "SIGN OFF");
            dto.setAuthenticationMethod(signType != null ? signType : "fingerprint");

            // Process using your existing service
            List<BiometricRecordDTO> records = new ArrayList<>();
            records.add(dto);

            List<Attendance> saved = attendanceService.processBiometricRecords(records);

            System.out.println("\n✅ RECORD PROCESSED SUCCESSFULLY");
            System.out.println("   Saved attendance records: " + saved.size());

            // Store successful attempt
            attemptRecord.put("status", "success");
            attemptRecord.put("employeeId", employee.getId());
            attemptRecord.put("employeeDbName", employee.getFirstName() + " " + employee.getLastName());
            attemptRecord.put("employeeCode", employee.getEmployeeId());
            attemptRecord.put("recordsCreated", saved.size());
            storeRecentAttempt(attemptId, attemptRecord);

            System.out.println("\n" + "=".repeat(80) + "\n");

            return ResponseEntity.ok(Map.of(
                    "result", "success",
                    "code", 200,
                    "message", "Attendance recorded successfully",
                    "attemptId", attemptId
            ));

        } catch (Exception e) {
            System.err.println("\n❌ ERROR PROCESSING BIOMETRIC RECORD:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();

            // Store error information safely
            try {
                if (attemptRecord != null) {
                    attemptRecord.put("status", "error");
                    attemptRecord.put("errorMessage", e.getMessage());
                    attemptRecord.put("errorType", e.getClass().getSimpleName());
                    attemptRecord.put("timestamp", LocalDateTime.now().toString());
                    storeRecentAttempt(attemptId, attemptRecord);
                } else {
                    // Create new error record if attemptRecord is null
                    Map<String, Object> errorRecord = new ConcurrentHashMap<>();
                    errorRecord.put("id", attemptId);
                    errorRecord.put("timestamp", LocalDateTime.now().toString());
                    errorRecord.put("status", "error");
                    errorRecord.put("errorMessage", e.getMessage());
                    errorRecord.put("errorType", e.getClass().getSimpleName());
                    storeRecentAttempt(attemptId, errorRecord);
                }
            } catch (Exception storeError) {
                System.err.println("⚠️ Could not store error record: " + storeError.getMessage());
            }

            System.out.println("=".repeat(80) + "\n");

            return ResponseEntity.status(500).body(Map.of(
                    "result", "error",
                    "code", 500,
                    "message", "Server error: " + e.getMessage()
            ));
        }
    }

    private String extractEmployeeName(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }

        // Method 1: Try to get from 'user' object
        Object userObj = payload.get("user");
        if (userObj instanceof Map) {
            Map<String, Object> userMap = (Map<String, Object>) userObj;
            Object name = userMap.get("name");
            if (name != null && !name.toString().trim().isEmpty()) {
                return name.toString().trim();
            }
            // Try alternative field names
            name = userMap.get("employeeName");
            if (name != null && !name.toString().trim().isEmpty()) {
                return name.toString().trim();
            }
            name = userMap.get("fullName");
            if (name != null && !name.toString().trim().isEmpty()) {
                return name.toString().trim();
            }
        }

        // Method 2: Try to parse userJson
        String userJson = (String) payload.get("userJson");
        if (userJson != null && !userJson.isEmpty()) {
            try {
                Map<String, Object> userData = objectMapper.readValue(userJson,
                        new TypeReference<Map<String, Object>>() {});
                Object name = userData.get("name");
                if (name != null && !name.toString().trim().isEmpty()) {
                    return name.toString().trim();
                }
                // Try alternative field names in JSON
                name = userData.get("employeeName");
                if (name != null && !name.toString().trim().isEmpty()) {
                    return name.toString().trim();
                }
                name = userData.get("fullName");
                if (name != null && !name.toString().trim().isEmpty()) {
                    return name.toString().trim();
                }
            } catch (Exception e) {
                System.out.println("Failed to parse userJson: " + e.getMessage());
            }
        }

        // Method 3: Try direct fields in payload
        Object directName = payload.get("employeeName");
        if (directName != null && !directName.toString().trim().isEmpty()) {
            return directName.toString().trim();
        }

        Object name = payload.get("name");
        if (name != null && !name.toString().trim().isEmpty()) {
            return name.toString().trim();
        }

        return null;
    }

    // Store recent attempt in memory with proper null checks
    private void storeRecentAttempt(String id, Map<String, Object> attempt) {
        if (id == null || id.trim().isEmpty()) {
            System.err.println("⚠️ Cannot store attempt: id is null or empty");
            return;
        }

        if (attempt == null) {
            System.err.println("⚠️ Cannot store attempt for id " + id + ": attempt record is null");
            return;
        }

        try {
            recentAttempts.put(id, attempt);
        } catch (Exception e) {
            System.err.println("❌ Error storing recent attempt: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get recent biometric attempts for frontend display
    @GetMapping("/recent-attempts")
    public ResponseEntity<List<Map<String, Object>>> getRecentBiometricAttempts() {
        try {
            List<Map<String, Object>> attempts = new ArrayList<>(recentAttempts.values());
            // Sort by timestamp descending (newest first)
            attempts.sort((a, b) -> {
                String timeA = (String) a.get("timestamp");
                String timeB = (String) b.get("timestamp");
                if (timeA == null || timeB == null) return 0;
                return timeB.compareTo(timeA);
            });
            return ResponseEntity.ok(attempts);
        } catch (Exception e) {
            System.err.println("Error retrieving recent attempts: " + e.getMessage());
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    // RETRY ATTEMPT ENDPOINT
    @PostMapping("/retry-attempt")
    public ResponseEntity<Map<String, Object>> retryBiometricAttempt(@RequestBody Map<String, Object> retryPayload) {
        String retryId = UUID.randomUUID().toString();

        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🔄 RETRY BIOMETRIC ATTEMPT REQUEST RECEIVED");
            System.out.println("=".repeat(80));

            String selectedEmployeeName = (String) retryPayload.get("selectedEmployeeName");
            String originalAction = (String) retryPayload.get("originalAction");
            String originalTimestamp = (String) retryPayload.get("originalTimestamp");
            String signType = (String) retryPayload.get("signType");

            System.out.println("   Selected employee: " + selectedEmployeeName);
            System.out.println("   Action: " + originalAction);
            System.out.println("   Timestamp: " + originalTimestamp);
            System.out.println("   Sign Type: " + signType);

            // Validate required fields
            if (selectedEmployeeName == null || selectedEmployeeName.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Employee name is required"
                ));
            }

            if (originalTimestamp == null || originalTimestamp.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Timestamp is required"
                ));
            }

            // Find the employee by selected name
            Optional<Employee> employeeOpt = nameMatchingService.findEmployeeByDeviceName(selectedEmployeeName);

            if (!employeeOpt.isPresent()) {
                System.out.println("❌ Employee not found: " + selectedEmployeeName);
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Employee not found: " + selectedEmployeeName
                ));
            }

            Employee employee = employeeOpt.get();
            System.out.println("✅ Found employee: " + employee.getFirstName() + " " + employee.getLastName());

            // Check if employee is active
            if (employee.getActive() == null || !employee.getActive()) {
                System.out.println("❌ Employee is inactive");
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Employee is inactive in the system"
                ));
            }

            // Parse timestamp - handle different formats
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss");
            LocalDateTime dateTime;
            try {
                dateTime = LocalDateTime.parse(originalTimestamp, formatter);
            } catch (Exception e) {
                // Try alternative format
                try {
                    DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    dateTime = LocalDateTime.parse(originalTimestamp, altFormatter);
                } catch (Exception e2) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "Invalid timestamp format: " + originalTimestamp
                    ));
                }
            }

            // Create biometric record DTO
            BiometricRecordDTO dto = new BiometricRecordDTO();
            dto.setEmployeeId(employee.getId());
            dto.setEmployeeName(selectedEmployeeName);
            dto.setTimestamp(dateTime);
            dto.setAction(originalAction != null ? originalAction : "SIGN ON");
            dto.setAuthenticationMethod(signType != null ? signType : "fingerprint");

            // Process the record
            List<BiometricRecordDTO> records = new ArrayList<>();
            records.add(dto);

            List<Attendance> saved = attendanceService.processBiometricRecords(records);

            // Store successful retry
            Map<String, Object> retryRecord = new ConcurrentHashMap<>();
            retryRecord.put("id", retryId);
            retryRecord.put("timestamp", LocalDateTime.now().toString());
            retryRecord.put("status", "success");
            retryRecord.put("action", "RETRY");
            retryRecord.put("employeeId", employee.getId());
            retryRecord.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
            retryRecord.put("recordsCreated", saved.size());
            storeRecentAttempt(retryId, retryRecord);

            System.out.println("✅ RETRY SUCCESSFUL - Saved " + saved.size() + " records");
            System.out.println("=".repeat(80) + "\n");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Attendance recorded successfully",
                    "employeeCode", employee.getEmployeeId(),
                    "employeeName", employee.getFirstName() + " " + employee.getLastName(),
                    "recordsCreated", saved.size()
            ));

        } catch (Exception e) {
            System.err.println("\n❌ Retry failed: " + e.getMessage());
            e.printStackTrace();

            // Store error retry record
            try {
                Map<String, Object> errorRecord = new ConcurrentHashMap<>();
                errorRecord.put("id", retryId);
                errorRecord.put("timestamp", LocalDateTime.now().toString());
                errorRecord.put("status", "error");
                errorRecord.put("action", "RETRY");
                errorRecord.put("errorMessage", e.getMessage());
                storeRecentAttempt(retryId, errorRecord);
            } catch (Exception storeError) {
                System.err.println("Could not store retry error record: " + storeError.getMessage());
            }

            System.out.println("=".repeat(80) + "\n");

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Server error: " + e.getMessage()
            ));
        }
    }

    // Optional: Endpoint to clear recent attempts (for testing/admin purposes)
    @DeleteMapping("/clear-attempts")
    public ResponseEntity<Map<String, Object>> clearRecentAttempts() {
        try {
            recentAttempts.clear();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Recent attempts cleared successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error clearing attempts: " + e.getMessage()
            ));
        }
    }

    // ========== ADMIN ENDPOINTS FOR BLOCK MANAGEMENT ==========

    // Unblock a specific user
    @PostMapping("/unblock-user")
    public ResponseEntity<Map<String, Object>> unblockUser(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        if (userId != null && !userId.isEmpty()) {
            boolean removed = blockedUserIds.remove(userId);
            if (removed) {
                processedRecordCache.remove("block_time_" + userId);
                System.out.println("✅ User unblocked by admin: " + userId);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "User unblocked successfully"
                ));
            }
        }
        return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "User not found in blocked list or invalid userId"
        ));
    }

    // Get list of currently blocked users
    @GetMapping("/blocked-users")
    public ResponseEntity<Map<String, Object>> getBlockedUsers() {
        List<String> blockedList = new ArrayList<>(blockedUserIds);
        Map<String, Object> response = new HashMap<>();
        response.put("count", blockedList.size());
        response.put("users", blockedList);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    // Get statistics about the blocking system
    @GetMapping("/blocking-stats")
    public ResponseEntity<Map<String, Object>> getBlockingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("blockedUsersCount", blockedUserIds.size());
        stats.put("cachedRecordsCount", processedRecordCache.size());
        stats.put("failedAttemptsCount", failedAttemptCounts.size());
        stats.put("duplicateWindowSeconds", DUPLICATE_WINDOW_MS / 1000);
        stats.put("maxFailedAttempts", MAX_FAILED_ATTEMPTS);
        stats.put("blockDurationHours", BLOCK_DURATION_MS / 3600000);
        stats.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(stats);
    }
}