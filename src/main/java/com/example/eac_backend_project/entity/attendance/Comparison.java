package com.example.eac_backend_project.entity.attendance;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comparisons")
@Data
@NoArgsConstructor
public class Comparison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file1_name")
    private String file1Name;

    @Column(name = "file2_name")
    private String file2Name;

    @Column(name = "file1_sheet")
    private String file1Sheet;

    @Column(name = "file2_sheet")
    private String file2Sheet;

    @ManyToOne
    @JoinColumn(name = "rule_set_id")
    private RuleSet ruleSet;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "summary_stats", columnDefinition = "json")
    private String summaryStats; // MySQL uses 'json' type, not 'jsonb'

    @OneToMany(mappedBy = "comparison", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComparisonDisparity> disparities = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}