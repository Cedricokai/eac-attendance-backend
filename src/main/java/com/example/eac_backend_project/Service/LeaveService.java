package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.Leave;
import com.example.eac_backend_project.entity.attendance.Overview;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.LeaveRepository;
import com.example.eac_backend_project.repository.OverviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OverviewRepository overviewRepository;

    // ------------------ CREATE LEAVE ------------------
    public Leave createLeave(Leave leave) {
        Employee employee = employeeRepository.findById(leave.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        leave.setEmployee(employee);

        // Initialize all workflow statuses
        leave.setStatus("Pending");
        leave.setSupervisorStatus("Pending");
        leave.setPlannerStatus("Pending");
        leave.setHrStatus("Pending");

        return leaveRepository.save(leave);
    }

    public List<Leave> getAllLeave() {
        return leaveRepository.findAll();
    }

    public List<Leave> getLeavesByEmployeeId(Long employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }

    // ------------------ SUPERVISOR ------------------
    @Transactional
    public Leave supervisorApprove(Long leaveId, String feedback) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if (!"Pending".equalsIgnoreCase(leave.getSupervisorStatus())) {
            throw new IllegalStateException("Supervisor already acted on this request");
        }

        leave.setSupervisorStatus("Approved");
        leave.setSupervisorFeedback(feedback);
        leave.setSupervisorActionDate(LocalDateTime.now());
        leave.setPlannerStatus("Pending"); // ready for planner
        leave.setStatus("Supervisor_Approved");

        return leaveRepository.save(leave);
    }

    public List<Leave> getSupervisorPending() {
        return leaveRepository.findBySupervisorStatus("Pending");
    }

    // ------------------ PLANNER ------------------
    @Transactional
    public Leave plannerApprove(Long leaveId, String feedback) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if (!"Approved".equalsIgnoreCase(leave.getSupervisorStatus())) {
            throw new IllegalStateException("Planner cannot act until Supervisor approves");
        }

        if (!"Pending".equalsIgnoreCase(leave.getPlannerStatus())) {
            throw new IllegalStateException("Planner already acted on this request");
        }

        leave.setPlannerStatus("Approved");
        leave.setPlannerFeedback(feedback);
        leave.setPlannerActionDate(LocalDateTime.now());
        leave.setHrStatus("Pending"); // ready for HR
        leave.setStatus("Planner_Approved");

        return leaveRepository.save(leave);
    }

    public List<Leave> getPlannerPending() {
        // Only supervisor-approved leaves with plannerStatus = Pending
        return leaveRepository.findByPlannerStatus("Pending");
    }

    // ------------------ HR ------------------
    @Transactional
    public Leave hrApprove(Long leaveId, String feedback) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if (!"Approved".equalsIgnoreCase(leave.getPlannerStatus())) {
            throw new IllegalStateException("HR cannot act until Planner approves");
        }

        if (!"Pending".equalsIgnoreCase(leave.getHrStatus())) {
            throw new IllegalStateException("HR already acted on this request");
        }

        leave.setHrStatus("Approved");
        leave.setHrFeedback(feedback);
        leave.setHrActionDate(LocalDateTime.now());
        leave.setStatus("Approved");

        updateOverviewWithLeave(leave);

        return leaveRepository.save(leave);
    }

    public List<Leave> getHrPending() {
        return leaveRepository.findByHrStatus("Pending");
    }

    // ------------------ REJECT ------------------
    @Transactional
    public Leave rejectLeave(Long leaveId, String role, String feedback) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        if ("supervisor".equalsIgnoreCase(role)) {
            leave.setSupervisorStatus("Rejected");
            leave.setSupervisorFeedback(feedback);
            leave.setSupervisorActionDate(LocalDateTime.now());
            leave.setPlannerStatus("Pending"); // optional: keep planner pending?
        } else if ("planner".equalsIgnoreCase(role)) {
            if (!"Approved".equalsIgnoreCase(leave.getSupervisorStatus())) {
                throw new IllegalStateException("Planner cannot reject before Supervisor acts");
            }
            leave.setPlannerStatus("Rejected");
            leave.setPlannerFeedback(feedback);
            leave.setPlannerActionDate(LocalDateTime.now());
        } else if ("hr".equalsIgnoreCase(role)) {
            if (!"Approved".equalsIgnoreCase(leave.getPlannerStatus())) {
                throw new IllegalStateException("HR cannot reject before Planner acts");
            }
            leave.setHrStatus("Rejected");
            leave.setHrFeedback(feedback);
            leave.setHrActionDate(LocalDateTime.now());
        }

        leave.setStatus("Rejected");
        return leaveRepository.save(leave);
    }

    // ------------------ UPDATE OVERVIEW ------------------
    private void updateOverviewWithLeave(Leave leave) {
        LocalDate startDate = leave.getStartDate();
        LocalDate endDate = leave.getEndDate();
        Long employeeId = leave.getEmployee().getId();

        List<Overview> overviews = overviewRepository
                .findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);

        overviews.forEach(overview -> {
            overview.setLeave(leave);
            overview.setStatus("On Leave");
            overviewRepository.save(overview);
        });
    }
}
