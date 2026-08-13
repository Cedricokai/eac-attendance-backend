package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.BiometricRecordDTO;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class RawBiometricEventService {
    private final RawBiometricEventRepository repository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    public RawBiometricEventService(RawBiometricEventRepository repository, EmployeeRepository employeeRepository, ObjectMapper objectMapper) {
        this.repository = repository; this.employeeRepository = employeeRepository; this.objectMapper = objectMapper;
    }
    @Transactional
    public RawBiometricEvent captureOnePass(Map<String,Object> payload) {
        RawBiometricEvent event = new RawBiometricEvent();
        event.setSourceType("ONEPASS"); event.setRawPayload(json(payload)); event.setProcessingStatus("RECEIVED");
        event.setDeviceId(value(payload, "deviceId", "deviceSn", "sn"));
        event.setDeviceEventId(value(payload, "eventId", "recordId", "id"));
        event.setDeviceUserId(value(payload, "employeeId", "userId", "personId"));
        event.setRawEmployeeName(value(payload, "employeeName", "name", "personName"));
        event.setPunchType(value(payload, "action", "punchType", "eventType"));
        event.setVerificationMethod(value(payload, "method", "authenticationMethod", "verifyMode"));
        matchEmployee(event); return repository.save(event);
    }
    @Transactional
    public RawBiometricEvent captureExcel(BiometricRecordDTO dto, String batchId) {
        RawBiometricEvent event = new RawBiometricEvent();
        event.setSourceType("EXCEL_IMPORT"); event.setBatchId(batchId); event.setRawPayload(json(dto));
        event.setDeviceUserId(dto.getEmployeeId() == null ? null : dto.getEmployeeId().toString());
        event.setRawEmployeeName(dto.getEmployeeName()); event.setEventTimestamp(dto.getTimestamp());
        event.setPunchType(dto.getAction()); event.setVerificationMethod(dto.getAuthenticationMethod());
        event.setProcessingStatus("RECEIVED"); matchEmployee(event); return repository.save(event);
    }
    @Transactional public void processed(RawBiometricEvent event, Attendance attendance) { event.setAttendance(attendance); event.setEmployee(attendance.getEmployee()); event.setProcessingStatus("PROCESSED"); event.setProcessedAt(LocalDateTime.now()); repository.save(event); }
    @Transactional public void failed(RawBiometricEvent event, Exception error) { event.setProcessingStatus("FAILED"); event.setErrorMessage(error.getMessage()); event.setProcessedAt(LocalDateTime.now()); repository.save(event); }
    private void matchEmployee(RawBiometricEvent event) { try { if (event.getDeviceUserId() != null) employeeRepository.findById(Long.valueOf(event.getDeviceUserId())).ifPresent(event::setEmployee); } catch (NumberFormatException ignored) {} }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException e) { return String.valueOf(value); } }
    private String value(Map<String,Object> map, String... keys) { for (String key: keys) if (map.get(key) != null) return String.valueOf(map.get(key)); return null; }
}