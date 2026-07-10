package com.example.eac_backend_project.entity.settings;

import com.example.eac_backend_project.entity.attendance.RequestWorkflowInstance;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "workflow_step_approvals")
public class WorkflowStepApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private RequestWorkflowInstance workflowInstance;
    private Integer stepIndex;
    private String stepName;
    private String approvedBy;
    private LocalDateTime approvalDate;
    private String notes;
    private String status;   // APPROVED, REJECTED
    // getters/setters
}