package com.example.eac_backend_project.entity.attendance;

import com.example.eac_backend_project.entity.inventory.InventoryRequest;
import com.example.eac_backend_project.entity.settings.WorkflowDefinition;
import com.example.eac_backend_project.entity.settings.WorkflowStepApproval;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "request_workflow_instances")
public class RequestWorkflowInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JoinColumn(name = "request_id")
    private InventoryRequest inventoryRequest;
    @ManyToOne
    private WorkflowDefinition workflowDefinition;
    private Integer currentStepIndex;        // 0-based
    private String currentStatus;             // denormalized for quick filter
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL)
    private List<WorkflowStepApproval> approvals = new ArrayList<>();
    // getters/setters
}
