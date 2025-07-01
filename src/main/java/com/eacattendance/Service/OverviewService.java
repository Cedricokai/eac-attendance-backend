package com.eacattendance.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.eacattendance.entity.Overview;
import com.eacattendance.repository.OverviewRepository;

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


