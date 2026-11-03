package com.example.eac_backend_project.controller.inventory;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.EmployeeIssuedItem;
import com.example.eac_backend_project.repository.EmployeeIssuedItemRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/employee-issued-items")
@RequiredArgsConstructor
public class EmployeeIssuedItemsController {

    private final EmployeeIssuedItemRepository issuedItemRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllIssuedItems() {
        List<EmployeeIssuedItem> items = issuedItemRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> result = items.stream().map(this::wrapRecordWithDue).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/employees")
    public ResponseEntity<List<Map<String, Object>>> getEmployeesForIssuedItems() {
        List<Employee> employees = employeeRepository.findAll();
        List<Map<String, Object>> list = employees.stream().map(emp -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", emp.getId());
            String fullName = ((emp.getFirstName() != null ? emp.getFirstName() : "") + " " + (emp.getLastName() != null ? emp.getLastName() : "")).trim();
            map.put("name", !fullName.isEmpty() ? fullName : "Employee");
            map.put("employeeId", emp.getEmployeeId() != null ? emp.getEmployeeId() : "");
            map.put("department", emp.getDepartment() != null ? emp.getDepartment() : "");
            return map;
        }).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/policies")
    public ResponseEntity<List<Map<String, Object>>> getReplacementPolicies() {
        List<Map<String, Object>> policies = List.of(
                createPolicy(1L, "Safety Boots", true, 1, "YEARS"),
                createPolicy(2L, "Safety Helmet", true, 2, "YEARS"),
                createPolicy(3L, "Safety Goggles", true, 6, "MONTHS"),
                createPolicy(4L, "Reflective Vest", true, 1, "YEARS"),
                createPolicy(5L, "Hand Gloves", true, 3, "MONTHS"),
                createPolicy(6L, "Ear Protection", true, 1, "YEARS")
        );
        return ResponseEntity.ok(policies);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Map<String, Object>>> createBatch(@RequestBody Map<String, List<Map<String, Object>>> request) {
        List<Map<String, Object>> itemsData = request.getOrDefault("items", Collections.emptyList());
        List<EmployeeIssuedItem> savedItems = new ArrayList<>();

        for (Map<String, Object> itemData : itemsData) {
            Long empId = itemData.get("employeeId") != null ? Long.valueOf(itemData.get("employeeId").toString()) : null;
            String employeeName = "Employee";
            String employeeNumber = "";

            if (empId != null) {
                Optional<Employee> empOpt = employeeRepository.findById(empId);
                if (empOpt.isPresent()) {
                    Employee emp = empOpt.get();
                    String fullName = ((emp.getFirstName() != null ? emp.getFirstName() : "") + " " + (emp.getLastName() != null ? emp.getLastName() : "")).trim();
                    if (!fullName.isEmpty()) {
                        employeeName = fullName;
                    }
                    employeeNumber = emp.getEmployeeId();
                }
            }

            String issueDate = String.valueOf(itemData.getOrDefault("issueDate", LocalDate.now().toString()));
            String ppeType = itemData.get("ppeType") != null ? String.valueOf(itemData.get("ppeType")) : null;
            String category = String.valueOf(itemData.getOrDefault("category", "PPE"));

            String replacementDueDate = null;
            if ("PPE".equalsIgnoreCase(category)) {
                try {
                    LocalDate parsedIssue = LocalDate.parse(issueDate);
                    int monthsToAdd = 12;
                    if (ppeType != null) {
                        String lower = ppeType.toLowerCase();
                        if (lower.contains("glove")) monthsToAdd = 3;
                        else if (lower.contains("goggle")) monthsToAdd = 6;
                        else if (lower.contains("helmet")) monthsToAdd = 24;
                    }
                    replacementDueDate = parsedIssue.plusMonths(monthsToAdd).toString();
                } catch (Exception ignored) {
                    replacementDueDate = LocalDate.now().plusYears(1).toString();
                }
            }

            Integer quantity = 1;
            if (itemData.get("quantity") != null) {
                try {
                    quantity = Integer.valueOf(itemData.get("quantity").toString());
                } catch (Exception ignored) {}
            }

            EmployeeIssuedItem item = EmployeeIssuedItem.builder()
                    .employeeId(empId)
                    .employeeName(employeeName)
                    .employeeNumber(employeeNumber)
                    .category(category)
                    .itemName(String.valueOf(itemData.getOrDefault("itemName", "Item")))
                    .ppeType(ppeType)
                    .quantity(quantity)
                    .serialNumber(itemData.get("serialNumber") != null ? String.valueOf(itemData.get("serialNumber")) : null)
                    .receiptReference(itemData.get("receiptReference") != null ? String.valueOf(itemData.get("receiptReference")) : null)
                    .issueDate(issueDate)
                    .replacementDueDate(replacementDueDate)
                    .status("ISSUED")
                    .notes(itemData.get("notes") != null ? String.valueOf(itemData.get("notes")) : null)
                    .issuedBy(itemData.get("issuedBy") != null ? String.valueOf(itemData.get("issuedBy")) : "Admin")
                    .acknowledgement(itemData.get("acknowledgement") != null ? String.valueOf(itemData.get("acknowledgement")) : null)
                    .acknowledgementDate(itemData.get("acknowledgementDate") != null ? String.valueOf(itemData.get("acknowledgementDate")) : null)
                    .build();

            savedItems.add(issuedItemRepository.save(item));
        }

        List<Map<String, Object>> result = savedItems.stream().map(this::wrapRecordWithDue).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<Map<String, Object>>> getItemHistory(@PathVariable Long id) {
        Optional<EmployeeIssuedItem> itemOpt = issuedItemRepository.findById(id);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        EmployeeIssuedItem item = itemOpt.get();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("id", 1);
        event.put("action", "ISSUED");
        event.put("date", item.getIssueDate() != null ? item.getIssueDate() : LocalDate.now().toString());
        event.put("notes", "Issued to " + item.getEmployeeName() + " by " + item.getIssuedBy());
        return ResponseEntity.ok(List.of(event));
    }

    @PostMapping("/{id}/action")
    public ResponseEntity<Map<String, Object>> handleAction(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Optional<EmployeeIssuedItem> itemOpt = issuedItemRepository.findById(id);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        EmployeeIssuedItem item = itemOpt.get();
        String action = String.valueOf(body.getOrDefault("action", "NOTE")).toUpperCase();
        String notes = body.get("notes") != null ? String.valueOf(body.get("notes")) : "";

        if ("RETURN".equalsIgnoreCase(action)) {
            item.setStatus("RETURNED");
        } else if ("REPLACE".equalsIgnoreCase(action)) {
            item.setStatus("REPLACED");
        }

        if (!notes.isBlank()) {
            item.setNotes((item.getNotes() != null ? item.getNotes() + " | " : "") + notes);
        }

        EmployeeIssuedItem updated = issuedItemRepository.save(item);
        return ResponseEntity.ok(wrapRecordWithDue(updated));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateItem(@PathVariable Long id, @RequestBody EmployeeIssuedItem update) {
        Optional<EmployeeIssuedItem> itemOpt = issuedItemRepository.findById(id);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        EmployeeIssuedItem item = itemOpt.get();
        if (update.getItemName() != null) item.setItemName(update.getItemName());
        if (update.getCategory() != null) item.setCategory(update.getCategory());
        if (update.getStatus() != null) item.setStatus(update.getStatus());
        if (update.getNotes() != null) item.setNotes(update.getNotes());
        if (update.getQuantity() != null) item.setQuantity(update.getQuantity());

        EmployeeIssuedItem updated = issuedItemRepository.save(item);
        return ResponseEntity.ok(wrapRecordWithDue(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (issuedItemRepository.existsById(id)) {
            issuedItemRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private Map<String, Object> wrapRecordWithDue(EmployeeIssuedItem item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("record", item);

        Map<String, Object> due = new LinkedHashMap<>();
        if (List.of("RETURNED", "REPLACED").contains(item.getStatus())) {
            due.put("label", "Closed");
            due.put("status", "CLOSED");
        } else if (item.getReplacementDueDate() == null || item.getReplacementDueDate().isBlank()) {
            due.put("label", "Not scheduled");
            due.put("status", "NOT_SCHEDULED");
        } else {
            try {
                LocalDate dueDate = LocalDate.parse(item.getReplacementDueDate());
                long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
                due.put("days", days);
                if (days < 0) {
                    due.put("label", Math.abs(days) + " days overdue");
                    due.put("status", "OVERDUE");
                } else if (days == 0) {
                    due.put("label", "Due today");
                    due.put("status", "DUE_TODAY");
                } else if (days <= 30) {
                    due.put("label", days + " days remaining");
                    due.put("status", "DUE_SOON");
                } else {
                    due.put("label", days + " days remaining");
                    due.put("status", "ON_TRACK");
                }
            } catch (Exception e) {
                due.put("label", "Not scheduled");
                due.put("status", "NOT_SCHEDULED");
            }
        }
        map.put("due", due);
        return map;
    }

    private Map<String, Object> createPolicy(Long id, String ppeType, boolean active, int count, String unit) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("category", "PPE");
        map.put("ppeType", ppeType);
        map.put("active", active);
        map.put("intervalCount", count);
        map.put("intervalUnit", unit);
        return map;
    }
}
