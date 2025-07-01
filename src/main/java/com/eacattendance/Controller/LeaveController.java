package com.eacattendance.Controller;

import com.eacattendance.Exceptions.ResourceNotFoundException;
import com.eacattendance.Service.LeaveService;
import com.eacattendance.entity.Leave;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("api/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping
    public ResponseEntity<Leave> createLeave(@RequestBody Leave leave) {
        Leave savedLeave = leaveService.createLeave(leave);
        return ResponseEntity.ok(savedLeave);
    }

    @GetMapping
    public List<Leave> getAllLeave() {
        return leaveService.getAllLeave();
    }


    @PostMapping("/validate/{leaveId}")
    public ResponseEntity<Leave> validateLeave(@PathVariable Long leaveId) {
        try {
            Leave validatedLeave = leaveService.validateLeave(leaveId);
            return ResponseEntity.ok(validatedLeave);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
