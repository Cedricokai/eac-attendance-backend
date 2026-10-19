package com.example.eac_backend_project.controller;
import com.example.eac_backend_project.entity.attendance.RawBiometricEvent;
import com.example.eac_backend_project.repository.RawBiometricEventRepository;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/biometric/raw-events")
public class RawBiometricEventController {
    private final RawBiometricEventRepository repository;
    private final com.example.eac_backend_project.Service.RawBiometricConversionService conversion;
    public RawBiometricEventController(RawBiometricEventRepository repository, com.example.eac_backend_project.Service.RawBiometricConversionService conversion){this.repository=repository;this.conversion=conversion;}
    @PostMapping("/convert") public Map<String,Object> convert(@RequestBody List<Long> eventIds, org.springframework.security.core.Authentication authentication){
        if (authentication == null || !authentication.isAuthenticated())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        boolean allowed = authentication.getAuthorities().stream().anyMatch(authority ->
            Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_HUMAN_RESOURCES").contains(authority.getAuthority()));
        if (!allowed) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only HR and administrators can convert raw biometric events");
        return conversion.convert(eventIds);
    }
    @GetMapping public Page<Map<String,Object>> search(@RequestParam(required=false) Long employeeId,
                                                       @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                       @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                       @RequestParam(defaultValue="") String source,@RequestParam(defaultValue="") String status,
                                                       @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="100") int size){
        return repository.search(employeeId,startDate==null?null:startDate.atStartOfDay(),endDate==null?null:endDate.atTime(LocalTime.MAX),source.toUpperCase(),status.toUpperCase(),PageRequest.of(page,Math.min(Math.max(size,1),500))).map(this::view);
    }
    @GetMapping("/{id}") public Map<String,Object> get(@PathVariable Long id){return view(repository.findById(id).orElseThrow());}
    private Map<String,Object> view(RawBiometricEvent e){Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.getId());m.put("sourceType",e.getSourceType());m.put("deviceId",e.getDeviceId());m.put("deviceEventId",e.getDeviceEventId());m.put("deviceUserId",e.getDeviceUserId());m.put("rawEmployeeName",e.getRawEmployeeName());m.put("eventTimestamp",e.getEventTimestamp());m.put("punchType",e.getPunchType());m.put("verificationMethod",e.getVerificationMethod());m.put("receivedAt",e.getReceivedAt());m.put("processedAt",e.getProcessedAt());m.put("processingStatus",e.getProcessingStatus());m.put("batchId",e.getBatchId());m.put("errorMessage",e.getErrorMessage());m.put("employeeId",e.getEmployee()==null?null:e.getEmployee().getId());m.put("attendanceId",e.getAttendance()==null?null:e.getAttendance().getId());m.put("rawPayload",e.getRawPayload());m.putAll(conversion.preview(e));return m;}
}
