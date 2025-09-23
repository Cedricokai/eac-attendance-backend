package com.example.eac_backend_project.Controller;


import com.example.eac_backend_project.Service.AttendanceService;
import com.example.eac_backend_project.entity.attendance.Attendance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/ONEPASS")
public class MobileAttendanceController {

    private final AttendanceService attendanceService;

    public MobileAttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/UPLOAD_RECORD_NOW")
    public ResponseEntity<Map<String, Object>> receiveBiometricAttendance(@RequestBody Map<String, Object> payload) {
        try {
            Attendance attendance = convertMobilePayload(payload);
            attendance.setBiometric(true); // Mark as biometric record
            Attendance saved = attendanceService.createAttendance(attendance);

            return ResponseEntity.ok(Map.of(
                    "result", "",
                    "code", 200,
                    "message", "ok"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "result", "error",
                    "code", 500,
                    "message", e.getMessage()
            ));
        }
    }

    private Attendance convertMobilePayload(Map<String, Object> payload) {
        Attendance attendance = new Attendance();
        // Map fields from payload to attendance
        return attendance;
    }
}