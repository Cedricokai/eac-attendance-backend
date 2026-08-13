package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    private String name;

    @Size(max = 255)
    private String description;

    private String manager; // optional

    // Constructors, getters, setters
    public Department() {}

    public Department(String name, String description, String manager) {
        this.name = name;
        this.description = description;
        this.manager = manager;
    }

}