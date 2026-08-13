package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;

@Entity
public class WorkflowNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // INVENTORY, DIRECT_PURCHASE, LEAVE, LOAN...
    private String workflowType;

    // PENDING_STORE
    // PROCUREMENT_PENDING
    // ISSUED
    private String status;

    // comma-separated or JSON
    @Column(columnDefinition = "TEXT")
    private String recipientEmails;

    private Boolean notifyRequester = true;

    private Boolean notifyApprover = true;

    private Boolean enabled = true;
}