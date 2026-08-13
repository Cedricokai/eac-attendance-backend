package com.example.eac_backend_project.entity.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "job_grades")
public class JobGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grade_level", nullable = false)
    private Integer level;

    // NEW: label field
    @Column(name = "label")
    private String label;

    @Column(name = "hourly_rate", nullable = false)
    private Double rate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_position_id")
    @JsonIgnore // prevents circular reference
    private JobPosition jobPosition;


}