package com.example.eac_backend_project.Controller;



import com.example.eac_backend_project.entity.attendance.ActivityLog;
import com.example.eac_backend_project.repository.ActivityLogRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "http://localhost:5173")
public class ActivityController {

    private final ActivityLogRepository activityLogRepository;

    public ActivityController(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping
    public List<ActivityLog> getRecentActivities() {
        return activityLogRepository.findTop10ByOrderByTimestampDesc();
    }
}