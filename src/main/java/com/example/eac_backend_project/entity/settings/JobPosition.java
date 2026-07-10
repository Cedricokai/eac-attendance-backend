package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_positions")
public class JobPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    // Change from String to Category entity relationship
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "base_rate")
    private Double baseRate = 0.0;

    @Column(name = "standard_work_hours")
    private Integer standardWorkHours = 8;

    @OneToMany(mappedBy = "jobPosition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobGrade> grades = new ArrayList<>();

    // Helper method to manage bidirectional relationship
    public void addGrade(JobGrade grade) {
        grades.add(grade);
        grade.setJobPosition(this);
    }

    public void removeGrade(JobGrade grade) {
        grades.remove(grade);
        grade.setJobPosition(null);
    }

    // Constructors
    public JobPosition() {}

    public JobPosition(String name, String description, Category category, Double baseRate, Integer standardWorkHours) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.baseRate = baseRate;
        this.standardWorkHours = standardWorkHours;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Double getBaseRate() { return baseRate; }
    public void setBaseRate(Double baseRate) { this.baseRate = baseRate; }

    public Integer getStandardWorkHours() { return standardWorkHours; }
    public void setStandardWorkHours(Integer standardWorkHours) { this.standardWorkHours = standardWorkHours; }

    public List<JobGrade> getGrades() { return grades; }
    public void setGrades(List<JobGrade> grades) {
        this.grades.clear();
        if (grades != null) {
            for (JobGrade grade : grades) {
                addGrade(grade);
            }
        }
    }
}