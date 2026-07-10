package com.example.eac_backend_project.entity.inventory;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true, nullable = false)
    private String name; // ROLE_ADMIN, ROLE_PLANNER, ROLE_TRANSPORT

    @Column(name = "is_system", nullable = false)
    private boolean system;// true = protected role

    public Role() {}

    public Role(String name, boolean system) {
        this.name = name;
        this.system = system;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isSystem() { return system; }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }
}