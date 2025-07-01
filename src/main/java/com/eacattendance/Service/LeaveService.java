package com.eacattendance.Service;

import com.eacattendance.Exceptions.ResourceNotFoundException;
import com.eacattendance.entity.Employee;
import com.eacattendance.entity.Leave;
import com.eacattendance.entity.Overview;
import com.eacattendance.repository.EmployeeRepository;
import com.eacattendance.repository.LeaveRepository;
import com.eacattendance.repository.OverviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OverviewRepository overviewRepository;

    public Leave createLeave(Leave leave) {
        Employee employee = employeeRepository.findById(leave.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        leave.setEmployee(employee);
        return leaveRepository.save(leave);
    }

    public List<Leave> getAllLeave() {
        return leaveRepository.findAll();
    }

    @Transactional
    public Leave validateLeave(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found with id: " + leaveId));

        // Check if leave is already validated
        if ("Approved".equals(leave.getStatus())) {
            throw new IllegalStateException("Leave is already approved");
        }

        // Update leave status
        leave.setStatus("Approved");
        Leave validatedLeave = leaveRepository.save(leave);

        // Update all overview records that fall within the leave period
        updateOverviewWithLeave(validatedLeave);

        return validatedLeave;
    }

    private void updateOverviewWithLeave(Leave leave) {
        LocalDate startDate = leave.getStartDate();
        LocalDate endDate = leave.getEndDate();
        Long employeeId = leave.getEmployee().getId();

        // Find all overview records for this employee during the leave period
        List<Overview> overviews = overviewRepository
                .findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);

        // Update each overview with leave information
        overviews.forEach(overview -> {
            overview.setLeave(leave);
            overview.setStatus("On Leave");
            overviewRepository.save(overview);
        });
    }


}
