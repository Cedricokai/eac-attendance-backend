package com.eacattendance.Controller;

import com.eacattendance.Service.OverviewService;
import com.eacattendance.entity.Overview;
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
