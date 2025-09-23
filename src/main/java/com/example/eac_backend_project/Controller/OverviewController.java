package com.example.eac_backend_project.Controller;


import com.example.eac_backend_project.Service.OverviewService;
import com.example.eac_backend_project.entity.attendance.Overview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/overview")
@CrossOrigin(origins = "http://localhost:5173")
public class OverviewController {

    @Autowired
    private OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping
    public List<Overview> getAllOverviews() {
        return overviewService.getAllOverviews();
    }
}
