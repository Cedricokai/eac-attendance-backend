package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "individual_attendance_report_schedule")
@Data
public class IndividualReportSchedule {
    @Id private Long id = 1L;
    @Version private Long version;
    private boolean enabled;
    @Column(columnDefinition = "TEXT") private String configuration;
    private Instant nextRun;
    private Instant lastAttempt;
    private String lastResult;
}
