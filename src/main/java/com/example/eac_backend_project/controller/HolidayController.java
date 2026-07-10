package com.example.eac_backend_project.controller;


import com.example.eac_backend_project.Service.HolidayService;
import com.example.eac_backend_project.entity.settings.Holiday;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@CrossOrigin(origins = "http://localhost:5173")
public class HolidayController {

    @Autowired
    private HolidayService holidayService;

    @PostMapping
    public ResponseEntity<Holiday> createHoliday(@RequestBody Holiday holiday) {
        Holiday savedHoliday = holidayService.createHoliday(holiday);
        return ResponseEntity.ok(savedHoliday);
    }

    @GetMapping
    public List<Holiday> getAllHoliday() {
        return holidayService.getAllHoliday();
    }
}
