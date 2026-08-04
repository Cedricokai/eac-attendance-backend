package com.example.eac_backend_project.entity.lunch;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "lunch_meal")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LunchMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String category;

    @Column(name = "is_active")
    private boolean active = true;
}