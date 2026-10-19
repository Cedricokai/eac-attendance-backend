package com.example.eac_backend_project.controller;
import com.example.eac_backend_project.Service.TimesheetPayrollApprovalService;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.*;
@RestController
@RequestMapping("/api/timesheets/payroll-approvals")
public class TimesheetPayrollApprovalController {
    private final TimesheetPayrollApprovalService service;
    public TimesheetPayrollApprovalController(TimesheetPayrollApprovalService service) { this.service=service; }
    @GetMapping
    public Map<String,Object> list(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(required=false) Long jobId) {
        return service.list(startDate,endDate,jobId);
    }
    public record Decision(List<Long> ids, String action, String reason) {}
    @PostMapping
    public List<Map<String,Object>> decide(@RequestBody Decision request) { return service.decide(request.ids(),request.action(),request.reason()); }
}
