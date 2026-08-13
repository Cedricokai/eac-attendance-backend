package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Exceptions.AttendanceRequestException;
import com.example.eac_backend_project.dto.AttendanceRequestCreateDTO;
import com.example.eac_backend_project.dto.AttendanceRequestResponseDTO;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.AttendanceRepository;
import com.example.eac_backend_project.repository.AttendanceRequestRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.Service.email.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AttendanceRequestService {

    private static final Set<String> FULL_REVIEW_ROLES = Set.of("ROLE_ADMIN", "ROLE_HR");
    private static final Set<String> DELEGATED_SUBMISSION_ROLES = Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_SUPERVISOR");

    private final AttendanceRequestRepository requestRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserService userService;
    private final EmailService emailService;

    public AttendanceRequestService(
            AttendanceRequestRepository requestRepository,
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            UserService userService,
            EmailService emailService
    ) {
        this.requestRepository = requestRepository;
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.userService = userService;
        this.emailService = emailService;
    }

    @Transactional
    public AttendanceRequestResponseDTO create(AttendanceRequestCreateDTO input) {
        AttendanceRequestResponseDTO result = createRequest(input);
        scheduleReviewEmail(List.of(result), result.requestedBy());
        return result;
    }

    private AttendanceRequestResponseDTO createRequest(AttendanceRequestCreateDTO input) {
        User actor = currentUser();
        Employee employee = employeeRepository.findById(input.employeeId())
                .orElseThrow(() -> new AttendanceRequestException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (Boolean.FALSE.equals(employee.getActive())) {
            throw new AttendanceRequestException(HttpStatus.BAD_REQUEST, "Attendance requests cannot be created for an inactive employee");
        }
        requireSubmissionAccess(actor, employee);
        validateTimesAndDate(input);

        if (attendanceRepository.existsByEmployeeIdAndDate(employee.getId(), input.date())) {
            throw new AttendanceRequestException(HttpStatus.CONFLICT, "Attendance already exists for this employee and date");
        }
        if (requestRepository.existsByEmployeeIdAndDateAndStatus(employee.getId(), input.date(), AttendanceRequestStatus.PENDING)) {
            throw new AttendanceRequestException(HttpStatus.CONFLICT, "A pending attendance request already exists for this employee and date");
        }

        AttendanceRequest request = new AttendanceRequest();
        request.setEmployee(employee);
        request.setDate(input.date());
        request.setCheckIn(input.checkIn());
        request.setCheckOut(input.checkOut());
        request.setShift(input.shift().trim());
        request.setReason(input.reason().trim());
        request.setEvidenceReference(blankToNull(input.evidenceReference()));
        request.setStatus(AttendanceRequestStatus.PENDING);
        request.setRequestedBy(actor);
        request.setRequestedAt(LocalDateTime.now());
        return toResponse(requestRepository.save(request));
    }

    @Transactional
    public List<AttendanceRequestResponseDTO> createBatch(List<AttendanceRequestCreateDTO> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new AttendanceRequestException(HttpStatus.BAD_REQUEST, "At least one employee must be selected");
        }
        Set<Long> employeeIds = new HashSet<>();
        for (AttendanceRequestCreateDTO input : inputs) {
            if (!employeeIds.add(input.employeeId())) {
                throw new AttendanceRequestException(HttpStatus.BAD_REQUEST,
                        "The same employee cannot appear more than once in a batch");
            }
        }
        List<AttendanceRequestResponseDTO> results = inputs.stream().map(this::createRequest).toList();
        scheduleReviewEmail(results, results.get(0).requestedBy());
        return results;
    }

    private void scheduleReviewEmail(List<AttendanceRequestResponseDTO> requests, String submittedBy) {
        Runnable notification = () -> emailService.sendAttendanceRequestReviewNotification(requests, submittedBy);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notification.run();
                }
            });
        } else {
            notification.run();
        }
    }

    @Transactional(readOnly = true)
    public Page<AttendanceRequestResponseDTO> find(
            AttendanceRequestStatus status,
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {
        User actor = currentUser();
        String role = role(actor);
        Long supervisorId = null;
        Long restrictedEmployeeId = employeeId;

        if ("ROLE_SUPERVISOR".equals(role)) {
            if (actor.getEmployee() == null) {
                throw new AttendanceRequestException(HttpStatus.FORBIDDEN, "Supervisor account is not linked to an employee");
            }
            supervisorId = actor.getEmployee().getId();
        } else if (!FULL_REVIEW_ROLES.contains(role)) {
            if (actor.getEmployee() == null) {
                throw new AttendanceRequestException(HttpStatus.FORBIDDEN, "User account is not linked to an employee");
            }
            restrictedEmployeeId = actor.getEmployee().getId();
        }

        return requestRepository.search(
                status,
                restrictedEmployeeId,
                startDate,
                endDate,
                blankToNull(search),
                supervisorId,
                pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceRequestResponseDTO findById(Long id) {
        AttendanceRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new AttendanceRequestException(HttpStatus.NOT_FOUND, "Attendance request not found"));
        requireViewAccess(currentUser(), request);
        return toResponse(request);
    }

    @Transactional
    public List<AttendanceRequestResponseDTO> verify(List<Long> requestIds) {
        User reviewer = currentUser();
        requireReviewerRole(reviewer);
        List<AttendanceRequest> requests = loadPendingForUpdate(requestIds);

        for (AttendanceRequest request : requests) {
            requireReviewAccess(reviewer, request.getEmployee());
        }

        LocalDateTime reviewedAt = LocalDateTime.now();
        for (AttendanceRequest request : requests) {
            request.setStatus(AttendanceRequestStatus.VERIFIED);
            request.setReviewedBy(reviewer);
            request.setReviewedAt(reviewedAt);
            request.setRejectionReason(null);
        }

        return requestRepository.saveAll(requests).stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<AttendanceRequestResponseDTO> addToAttendance(List<Long> requestIds) {
        User reviewer = currentUser();
        requireReviewerRole(reviewer);
        List<AttendanceRequest> requests = loadForUpdate(requestIds);

        for (AttendanceRequest request : requests) {
            requireReviewAccess(reviewer, request.getEmployee());
            if (request.getStatus() != AttendanceRequestStatus.VERIFIED) {
                throw new AttendanceRequestException(HttpStatus.CONFLICT,
                        "Attendance request " + request.getId() + " must be verified before it can be added to attendance");
            }
            if (request.getAttendance() != null) {
                throw new AttendanceRequestException(HttpStatus.CONFLICT,
                        "Attendance request " + request.getId() + " is already in attendance records");
            }
            if (attendanceRepository.existsByEmployeeIdAndDate(request.getEmployee().getId(), request.getDate())) {
                throw new AttendanceRequestException(HttpStatus.CONFLICT,
                        "Attendance already exists for " + employeeName(request.getEmployee()) + " on " + request.getDate());
            }
        }

        for (AttendanceRequest request : requests) {
            request.setAttendance(createAttendance(request));
        }
        return requestRepository.saveAll(requests).stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<AttendanceRequestResponseDTO> removeFromAttendance(List<Long> requestIds) {
        User reviewer = currentUser();
        requireReviewerRole(reviewer);
        List<AttendanceRequest> requests = loadForUpdate(requestIds);

        for (AttendanceRequest request : requests) {
            requireReviewAccess(reviewer, request.getEmployee());
            if (request.getStatus() != AttendanceRequestStatus.VERIFIED || request.getAttendance() == null) {
                throw new AttendanceRequestException(HttpStatus.CONFLICT,
                        "Attendance request " + request.getId() + " is not linked to an attendance record");
            }
        }

        for (AttendanceRequest request : requests) {
            Attendance attendance = request.getAttendance();
            request.setAttendance(null);
            requestRepository.save(request);
            attendanceRepository.delete(attendance);
        }
        return requests.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<AttendanceRequestResponseDTO> reject(List<Long> requestIds, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new AttendanceRequestException(HttpStatus.BAD_REQUEST, "Rejection reason is required");
        }
        User reviewer = currentUser();
        requireReviewerRole(reviewer);
        List<AttendanceRequest> requests = loadPendingForUpdate(requestIds);
        LocalDateTime reviewedAt = LocalDateTime.now();

        for (AttendanceRequest request : requests) {
            requireReviewAccess(reviewer, request.getEmployee());
            request.setStatus(AttendanceRequestStatus.REJECTED);
            request.setReviewedBy(reviewer);
            request.setReviewedAt(reviewedAt);
            request.setRejectionReason(rejectionReason.trim());
        }
        return requestRepository.saveAll(requests).stream().map(this::toResponse).toList();
    }

    private List<AttendanceRequest> loadPendingForUpdate(List<Long> requestIds) {
        List<AttendanceRequest> requests = loadForUpdate(requestIds);
        requests.stream()
                .filter(request -> request.getStatus() != AttendanceRequestStatus.PENDING)
                .findFirst()
                .ifPresent(request -> {
                    throw new AttendanceRequestException(HttpStatus.CONFLICT,
                            "Attendance request " + request.getId() + " has already been processed");
                });
        return requests;
    }

    private List<AttendanceRequest> loadForUpdate(List<Long> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            throw new AttendanceRequestException(HttpStatus.BAD_REQUEST, "At least one attendance request is required");
        }
        Set<Long> uniqueIds = new HashSet<>(requestIds);
        List<AttendanceRequest> requests = requestRepository.findAllByIdForUpdate(uniqueIds);
        if (requests.size() != uniqueIds.size()) {
            throw new AttendanceRequestException(HttpStatus.NOT_FOUND, "One or more attendance requests were not found");
        }
        return requests;
    }

    private Attendance createAttendance(AttendanceRequest request) {
        double hours = Duration.between(request.getCheckIn(), request.getCheckOut()).toMinutes() / 60.0;
        Attendance attendance = new Attendance();
        attendance.setEmployee(request.getEmployee());
        attendance.setDate(request.getDate());
        attendance.setCheckIn(request.getCheckIn());
        attendance.setCheckOut(request.getCheckOut());
        attendance.setShift(request.getShift());
        attendance.setWorkType(request.getEmployee().getWorkType());
        attendance.setStatus("Present");
        attendance.setMinimumHour(hours);
        attendance.setTotalMinimumHour(hours);
        attendance.setBiometric(false);
        attendance.setTimestamp(LocalDateTime.now());
        attendance.setMethod("ATTENDANCE_REQUEST");
        return attendanceRepository.save(attendance);
    }

    private void validateTimesAndDate(AttendanceRequestCreateDTO input) {
        if (input.date().isAfter(LocalDate.now())) {
            throw new AttendanceRequestException(HttpStatus.BAD_REQUEST, "Attendance date cannot be in the future");
        }
        if (!input.checkOut().isAfter(input.checkIn())) {
            throw new AttendanceRequestException(HttpStatus.BAD_REQUEST, "Check-out must be after check-in");
        }
    }

    private void requireSubmissionAccess(User actor, Employee employee) {
        if (DELEGATED_SUBMISSION_ROLES.contains(role(actor))) return;
        if (actor.getEmployee() == null || !actor.getEmployee().getId().equals(employee.getId())) {
            throw new AttendanceRequestException(HttpStatus.FORBIDDEN, "You can only submit an attendance request for yourself");
        }
    }

    private void requireReviewerRole(User reviewer) {
        String role = role(reviewer);
        if (!FULL_REVIEW_ROLES.contains(role) && !"ROLE_SUPERVISOR".equals(role)) {
            throw new AttendanceRequestException(HttpStatus.FORBIDDEN, "You are not authorized to verify attendance requests");
        }
    }

    private void requireReviewAccess(User reviewer, Employee employee) {
        if (FULL_REVIEW_ROLES.contains(role(reviewer))) return;
        if (reviewer.getEmployee() == null || employee.getSupervisor() == null
                || !reviewer.getEmployee().getId().equals(employee.getSupervisor().getId())) {
            throw new AttendanceRequestException(HttpStatus.FORBIDDEN, "Supervisors can only review requests from their direct reports");
        }
    }

    private void requireViewAccess(User actor, AttendanceRequest request) {
        if (FULL_REVIEW_ROLES.contains(role(actor))) return;
        if (actor.getEmployee() == null) {
            throw new AttendanceRequestException(HttpStatus.FORBIDDEN, "User account is not linked to an employee");
        }
        Long actorEmployeeId = actor.getEmployee().getId();
        boolean ownRequest = actorEmployeeId.equals(request.getEmployee().getId());
        boolean directReport = request.getEmployee().getSupervisor() != null
                && actorEmployeeId.equals(request.getEmployee().getSupervisor().getId());
        if (!ownRequest && !directReport) {
            throw new AttendanceRequestException(HttpStatus.FORBIDDEN, "You cannot view this attendance request");
        }
    }

    private User currentUser() {
        try {
            return userService.getCurrentUser();
        } catch (RuntimeException exception) {
            throw new AttendanceRequestException(HttpStatus.UNAUTHORIZED, exception.getMessage());
        }
    }

    private String role(User user) {
        return user.getRole() == null || user.getRole().getName() == null
                ? ""
                : user.getRole().getName().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String employeeName(Employee employee) {
        return ((employee.getFirstName() == null ? "" : employee.getFirstName()) + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private String userName(User user) {
        if (user == null) return null;
        if (user.getName() != null && !user.getName().isBlank()) return user.getName();
        if (user.getUserName() != null && !user.getUserName().isBlank()) return user.getUserName();
        return user.getEmail();
    }

    private String frontendStatus(AttendanceRequestStatus status) {
        if (status == null) return "Pending";
        String value = status.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private AttendanceRequestResponseDTO toResponse(AttendanceRequest request) {
        Employee employee = request.getEmployee();
        return new AttendanceRequestResponseDTO(
                request.getId(),
                employee.getId(),
                employeeName(employee),
                new AttendanceRequestResponseDTO.EmployeeSummary(
                        employee.getId(), employee.getEmployeeId(), employee.getFirstName(), employee.getLastName()),
                request.getDate(),
                request.getCheckIn(),
                request.getCheckOut(),
                request.getShift(),
                request.getReason(),
                request.getEvidenceReference(),
                frontendStatus(request.getStatus()),
                userName(request.getRequestedBy()),
                request.getRequestedAt(),
                userName(request.getReviewedBy()),
                request.getReviewedAt(),
                request.getRejectionReason(),
                request.getAttendance() == null ? null : request.getAttendance().getId()
        );
    }
}
