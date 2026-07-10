package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "rule_set_items")
@Data
@NoArgsConstructor
public class RuleSetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "rule_set_id", nullable = false)
    private RuleSet ruleSet;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "expected_hours", nullable = false)
    private Double expectedHours;

    @Column(length = 255)
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}