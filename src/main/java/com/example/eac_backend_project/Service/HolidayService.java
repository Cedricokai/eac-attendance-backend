package com.example.eac_backend_project.Service;


import com.example.eac_backend_project.entity.attendance.Holiday;
import com.example.eac_backend_project.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    public Holiday createHoliday(Holiday holiday) {
        return holidayRepository.save(holiday);
    }

    public List<Holiday> getAllHoliday() {
        return holidayRepository.findAll();
    }
}
