package com.example.eac_backend_project.entity.lunch;

import com.example.eac_backend_project.entity.attendance.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_meal")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_of_week", nullable = false, length = 20)
    private String dayOfWeek; // MONDAY, TUESDAY, ...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal1_id")
    private LunchMeal meal1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal2_id")
    private LunchMeal meal2;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDate createdDate;

    @Column(name = "updated_date")
    private LocalDate updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDate.now();
        updatedDate = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDate.now();
    }
}