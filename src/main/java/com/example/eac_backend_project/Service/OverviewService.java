package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Overview;
import com.example.eac_backend_project.repository.OverviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OverviewService {

    private OverviewRepository overviewRepository;

    @Autowired
    public OverviewService(OverviewRepository overviewRepository) {
        this.overviewRepository = overviewRepository;
    }

    public List<Overview> getAllOverviews() {
        return overviewRepository.findAll();
    }
}


