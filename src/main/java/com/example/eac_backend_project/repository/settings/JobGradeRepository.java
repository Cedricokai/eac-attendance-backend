package com.example.eac_backend_project.repository.settings;

import com.example.eac_backend_project.entity.settings.JobGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobGradeRepository extends JpaRepository<JobGrade, Long> {
    void deleteByJobPositionId(Long jobPositionId);
}