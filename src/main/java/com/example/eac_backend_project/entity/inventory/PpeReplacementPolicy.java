package com.example.eac_backend_project.entity.inventory;
import jakarta.persistence.*;
import lombok.Data;

@Entity @Data @Table(name = "ppe_replacement_policy")
public class PpeReplacementPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(nullable = false, unique = true) private String ppeType;
    @Column(nullable = false) private Integer intervalCount;
    @Column(nullable = false) private String intervalUnit;
    private boolean active = true;
}
