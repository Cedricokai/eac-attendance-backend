package com.example.eac_backend_project.entity.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "job_grades")
public class JobGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grade_level", nullable = false)
    private String level;

    @Column(name = "hourly_rate", nullable = false)
    private Double rate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_position_id")
    @JsonIgnore // Add this to break circular reference
    private JobPosition jobPosition;

    // Constructors
    public JobGrade() {}

    public JobGrade(String level, Double rate) {
        this.level = level;
        this.rate = rate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public JobPosition getJobPosition() { return jobPosition; }
    public void setJobPosition(JobPosition jobPosition) { this.jobPosition = jobPosition; }
}