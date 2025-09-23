package com.example.eac_backend_project.Controller;

import com.example.eac_backend_project.Exceptions.ResourceNotFoundException;
import com.example.eac_backend_project.Service.LeaveService;
import com.example.eac_backend_project.entity.attendance.Leave;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("api/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping
    public ResponseEntity<Leave> createLeave(@RequestBody Leave leave) {
        return ResponseEntity.ok(leaveService.createLeave(leave));
    }

    @GetMapping
    public List<Leave> getAllLeave() {
        return leaveService.getAllLeave();
    }

    @GetMapping("/employee/{employeeId}")
    public List<Leave> getLeavesByEmployee(@PathVariable Long employeeId) {
        return leaveService.getLeavesByEmployeeId(employeeId);
    }

    @PostMapping("/supervisor/approve/{leaveId}")
    public ResponseEntity<Leave> supervisorApprove(
            @PathVariable Long leaveId,
            @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(leaveService.supervisorApprove(leaveId, request.get("feedback")));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/supervisor")
    public List<Leave> getSupervisorPending() {
        return leaveService.getSupervisorPending();
    }

    @GetMapping("/planner")
    public List<Leave> getPlannerPending() {
        return leaveService.getPlannerPending();
    }

    @GetMapping("/hr")
    public List<Leave> getHrPending() {
        return leaveService.getHrPending();
    }

    @PostMapping("/planner/approve/{leaveId}")
    public ResponseEntity<Leave> plannerApprove(
            @PathVariable Long leaveId,
            @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(leaveService.plannerApprove(leaveId, request.get("feedback")));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/hr/approve/{leaveId}")
    public ResponseEntity<Leave> hrApprove(
            @PathVariable Long leaveId,
            @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(leaveService.hrApprove(leaveId, request.get("feedback")));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/reject/{leaveId}")
    public ResponseEntity<Leave> rejectLeave(
            @PathVariable Long leaveId,
            @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(
                    leaveService.rejectLeave(leaveId, request.get("role"), request.get("feedback"))
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


}

