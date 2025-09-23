package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.attendance.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

}
