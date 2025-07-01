package com.eacattendance.Controller;

import com.eacattendance.Service.AttendanceService;
import com.eacattendance.entity.Attendance;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class AttendanceSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final AttendanceService attendanceService;

    public AttendanceSocketController(SimpMessagingTemplate messagingTemplate,
                                      AttendanceService attendanceService) {
        this.messagingTemplate = messagingTemplate;
        this.attendanceService = attendanceService;
    }

    @MessageMapping("/attendance/new")
    public void handleNewAttendance(Attendance attendance) {
        Attendance saved = attendanceService.createAttendance(attendance);
        messagingTemplate.convertAndSend("/topic/attendance", saved);
    }
}