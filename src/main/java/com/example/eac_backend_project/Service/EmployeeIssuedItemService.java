package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.inventory.*;
import com.example.eac_backend_project.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class EmployeeIssuedItemService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Accra");

    public record Issue(
            Long employeeId,
            String itemName,
            String category,
            String ppeType,
            Integer quantity,
            String serialNumber,
            String receiptReference,
            LocalDate issueDate,
            String issuedBy,
            String notes,
            String acknowledgement,
            LocalDate acknowledgementDate,
            Long replacesId
    ) {}

    public record BatchIssue(List<Issue> items) {}

    public record Change(String action, LocalDate date, String notes) {}

    public record Policy(
            String ppeType,
            Integer intervalCount,
            String intervalUnit,
            boolean active
    ) {}

    public record View(
            EmployeeIssuedItem record,
            Long daysRemaining,
            String replacementStatus
    ) {}

    public record EmployeeOption(
            Long id,
            String name,
            String employeeNumber
    ) {}

    private final EmployeeIssuedItemRepository items;
    private final PpeReplacementPolicyRepository policies;
    private final IssuedItemEventRepository events;
    private final EmployeeRepository employees;

    public EmployeeIssuedItemService(
            EmployeeIssuedItemRepository items,
            PpeReplacementPolicyRepository policies,
            IssuedItemEventRepository events,
            EmployeeRepository employees
    ) {
        this.items = items;
        this.policies = policies;
        this.events = events;
        this.employees = employees;
    }

    public List<EmployeeOption> employees() {
        return employees.findAll()
                .stream()
                .map(e -> new EmployeeOption(
                        e.getId(),
                        ((e.getFirstName() == null ? "" : e.getFirstName())
                                + " "
                                + (e.getLastName() == null ? "" : e.getLastName())).trim(),
                        e.getEmployeeId()
                ))
                .toList();
    }

    public List<View> list() {
        return items.findAllByOrderByIssueDateDescIdDesc()
                .stream()
                .map(this::view)
                .toList();
    }

    public List<PpeReplacementPolicy> policies() {
        return policies.findAllByOrderByPpeTypeAsc();
    }

    public List<IssuedItemEvent> history(Long id) {
        return events.findByItemIdOrderByIdAsc(id);
    }

    public View view(EmployeeIssuedItem item) {
        Long days = item.getReplacementDueDate() == null
                ? null
                : ChronoUnit.DAYS.between(
                LocalDate.now(ZONE),
                item.getReplacementDueDate()
        );

        String due =
                !Set.of("ASSIGNED", "DAMAGED", "LOST").contains(item.getStatus())
                        ? "CLOSED"
                        : days == null
                        ? "NOT_SCHEDULED"
                        : days < 0
                        ? "OVERDUE"
                        : days == 0
                        ? "DUE_TODAY"
                        : days <= 30
                        ? "DUE_SOON"
                        : "ON_TRACK";

        return new View(item, days, due);
    }

    @Transactional
    public PpeReplacementPolicy savePolicy(Long id, Policy input) {
        if (input == null) {
            throw invalid("Enter a replacement rule.");
        }

        String type = required(input.ppeType(), "PPE type", 100);
        dueDate(LocalDate.now(ZONE), input.intervalCount(), input.intervalUnit());

        var duplicate = policies.findByPpeTypeIgnoreCase(type);

        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw invalid("A replacement rule already exists for this PPE type.");
        }

        var policy = id == null
                ? new PpeReplacementPolicy()
                : policies.findById(id)
                .orElseThrow(() -> invalid("Replacement rule not found."));

        if (id != null
                && !type.equals(policy.getPpeType())
                && items.existsByPpeTypeIgnoreCase(policy.getPpeType())) {
            throw invalid(
                    "This PPE type is already used in issue records. "
                            + "Add a new rule for a different type instead of renaming it."
            );
        }

        policy.setPpeType(type);
        policy.setIntervalCount(input.intervalCount());
        policy.setIntervalUnit(input.intervalUnit());
        policy.setActive(input.active());

        return policies.save(policy);
    }

    public static LocalDate dueDate(
            LocalDate issueDate,
            Integer count,
            String unit
    ) {
        if (issueDate == null) {
            throw invalid("Issue date is required.");
        }

        if (count == null
                || count < 1
                || count > 120
                || !("MONTHS".equals(unit) || "YEARS".equals(unit))) {
            throw invalid("Choose 1–120 months or years.");
        }

        return "YEARS".equals(unit)
                ? issueDate.plusYears(count)
                : issueDate.plusMonths(count);
    }

    /**
     * Keeps the original single-item endpoint working.
     */
    @Transactional
    public View issue(Issue input, String actor) {
        return issueInternal(input, actor);
    }

    /**
     * Transactional batch issuance.
     *
     * If ANY item fails validation or saving, Spring rolls the entire method back.
     * This prevents partial PPE sets from being recorded.
     */
    @Transactional
    public List<View> issueBatch(BatchIssue request, String actor) {
        if (request == null
                || request.items() == null
                || request.items().isEmpty()) {
            throw invalid("Add at least one item to issue.");
        }

        if (request.items().size() > 100) {
            throw invalid("A maximum of 100 items can be issued in one batch.");
        }

        Long employeeId = null;

        for (Issue item : request.items()) {
            if (item == null || item.employeeId() == null) {
                throw invalid("Every item must have an employee.");
            }

            if (employeeId == null) {
                employeeId = item.employeeId();
            } else if (!employeeId.equals(item.employeeId())) {
                throw invalid("All items in one batch must belong to the same employee.");
            }
        }

        List<View> saved = new ArrayList<>();

        for (Issue item : request.items()) {
            saved.add(issueInternal(item, actor));
        }

        return saved;
    }

    /**
     * Internal issuance method used by both single and batch issuance.
     * Transaction boundary is provided by the public calling method.
     */
    private View issueInternal(Issue input, String actor) {
        if (input == null || input.employeeId() == null) {
            throw invalid("Choose an employee.");
        }

        checkDate(input.issueDate());

        if (input.quantity() == null
                || input.quantity() < 1
                || input.quantity() > 100000) {
            throw invalid("Quantity must be between 1 and 100,000.");
        }

        String category = input.category() == null
                ? ""
                : input.category().trim().toUpperCase(Locale.ROOT);

        if (!Set.of("PPE", "TOOL", "UNIFORM", "EQUIPMENT", "OTHER")
                .contains(category)) {
            throw invalid("Choose an item category.");
        }

        var employee = employees.findById(input.employeeId())
                .orElseThrow(() -> invalid("Employee not found."));

        EmployeeIssuedItem previous = null;

        if (input.replacesId() != null) {
            previous = items.lockById(input.replacesId())
                    .orElseThrow(() -> invalid("Original issue not found."));

            if (!previous.getEmployeeId().equals(input.employeeId())
                    || Set.of("REPLACED", "RETURNED").contains(previous.getStatus())) {
                throw invalid("This issue cannot be replaced.");
            }

            if (input.issueDate().isBefore(previous.getIssueDate())) {
                throw invalid("Replacement cannot be dated before the original issue.");
            }

            String replacementPpeType =
                    "PPE".equals(category) ? input.ppeType() : null;

            if (!Objects.equals(previous.getCategory(), category)
                    || !Objects.equals(previous.getPpeType(), replacementPpeType)) {
                throw invalid(
                        "Replacement must use the same category and PPE type."
                );
            }

            if (!previous.getQuantity().equals(input.quantity())) {
                throw invalid(
                        "A replacement must cover the full original quantity. "
                                + "Record partial batches separately."
                );
            }
        }

        var item = new EmployeeIssuedItem();

        item.setEmployeeId(employee.getId());
        item.setEmployeeName(
                ((employee.getFirstName() == null ? "" : employee.getFirstName())
                        + " "
                        + (employee.getLastName() == null ? "" : employee.getLastName()))
                        .trim()
        );
        item.setEmployeeNumber(employee.getEmployeeId());

        item.setItemName(required(input.itemName(), "Item name", 200));
        item.setCategory(category);
        item.setQuantity(input.quantity());
        item.setIssueDate(input.issueDate());
        item.setIssuedBy(required(input.issuedBy(), "Issued by", 150));

        item.setSerialNumber(optional(input.serialNumber(), 150));
        item.setReceiptReference(optional(input.receiptReference(), 200));
        item.setNotes(optional(input.notes(), 2000));
        item.setAcknowledgement(optional(input.acknowledgement(), 200));

        if (item.getAcknowledgement() != null) {
            checkDate(input.acknowledgementDate());

            if (input.acknowledgementDate().isBefore(input.issueDate())) {
                throw invalid("Acknowledgement cannot be before the issue date.");
            }

            item.setAcknowledgementDate(input.acknowledgementDate());
        }

        if ("PPE".equals(category)) {
            String type = required(input.ppeType(), "PPE type", 100);

            var policy = policies.findByPpeTypeIgnoreCase(type)
                    .filter(PpeReplacementPolicy::isActive)
                    .orElseThrow(() ->
                            invalid(
                                    "Set an active replacement rule for this PPE type "
                                            + "in Settings first."
                            )
                    );

            item.setPpeType(policy.getPpeType());
            item.setReplacementIntervalCount(policy.getIntervalCount());
            item.setReplacementIntervalUnit(policy.getIntervalUnit());
            item.setReplacementDueDate(
                    dueDate(
                            input.issueDate(),
                            policy.getIntervalCount(),
                            policy.getIntervalUnit()
                    )
            );
        } else {
            item.setPpeType(null);
            item.setReplacementIntervalCount(null);
            item.setReplacementIntervalUnit(null);
            item.setReplacementDueDate(null);
        }

        item.setStatus("ASSIGNED");
        item.setStatusDate(input.issueDate());
        item.setRecordedBy(actor);
        item.setRecordedAt(Instant.now());
        item.setReplacesId(input.replacesId());

        items.saveAndFlush(item);

        event(
                item.getId(),
                "ISSUED",
                input.issueDate(),
                item.getNotes(),
                actor
        );

        if (item.getAcknowledgement() != null) {
            event(
                    item.getId(),
                    "ACKNOWLEDGED",
                    item.getAcknowledgementDate(),
                    item.getAcknowledgement(),
                    actor
            );
        }

        if (previous != null) {
            previous.setStatus("REPLACED");
            previous.setStatusDate(input.issueDate());
            previous.setReplacedById(item.getId());

            items.save(previous);

            event(
                    previous.getId(),
                    "REPLACED",
                    input.issueDate(),
                    "Replaced by issue #" + item.getId(),
                    actor
            );
        }

        return view(item);
    }

    @Transactional
    public View change(Long id, Change input, String actor) {
        if (input == null) {
            throw invalid("Enter a record update.");
        }

        var item = items.lockById(id)
                .orElseThrow(() -> invalid("Issue record not found."));

        checkDate(input.date());

        if (input.date().isBefore(item.getStatusDate())) {
            throw invalid(
                    "The update date cannot be before the latest recorded status date."
            );
        }

        if (!Set.of(
                "RETURNED",
                "DAMAGED",
                "LOST",
                "ACKNOWLEDGED",
                "NOTE"
        ).contains(input.action() == null ? "" : input.action())) {
            throw invalid("Choose a valid update.");
        }

        String notes = required(input.notes(), "Details", 2000);

        if (!"NOTE".equals(input.action())
                && Set.of("RETURNED", "REPLACED").contains(item.getStatus())) {
            throw invalid(
                    "This issue is already closed. Its history is preserved."
            );
        }

        if ("ACKNOWLEDGED".equals(input.action())) {
            item.setAcknowledgement(
                    required(input.notes(), "Acknowledgement", 200)
            );
            item.setAcknowledgementDate(input.date());
        } else if (!"NOTE".equals(input.action())) {
            if (input.action().equals(item.getStatus())) {
                throw invalid("This status is already recorded.");
            }

            item.setStatus(input.action());
            item.setStatusDate(input.date());
        }

        event(id, input.action(), input.date(), notes, actor);

        return view(items.save(item));
    }

    private void event(
            Long id,
            String action,
            LocalDate date,
            String notes,
            String actor
    ) {
        var event = new IssuedItemEvent();

        event.setItemId(id);
        event.setAction(action);
        event.setEffectiveDate(date);
        event.setRecordedBy(actor);
        event.setRecordedAt(Instant.now());
        event.setNotes(notes);

        events.save(event);
    }

    private static void checkDate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now(ZONE))) {
            throw invalid(
                    "Enter an issue or update date no later than today."
            );
        }
    }

    private static String required(
            String value,
            String name,
            int max
    ) {
        String result = optional(value, max);

        if (result == null) {
            throw invalid(name + " is required.");
        }

        return result;
    }

    private static String optional(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String result = value.trim();

        if (result.length() > max) {
            throw invalid("Text exceeds " + max + " characters.");
        }

        return result;
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
