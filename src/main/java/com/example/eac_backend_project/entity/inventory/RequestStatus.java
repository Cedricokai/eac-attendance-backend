package com.example.eac_backend_project.entity.inventory;

public enum RequestStatus {
    // Created by employee (entry point)
    PENDING,            // Non-PPE created and waiting planner/store review (legacy support)
    PENDING_PLANNER,    // If you still want planner step for some requests (optional)
    // For PPE (procurement first)
    PROCUREMENT_PENDING,
    PROCURED,           // Procurement approved / items procured and sent/ready for store
    STORE_REVIEW,       // Store has items to review (after procurement)
    RECEIVED_IN_STORE,  // Store has received the procured items (stock increased)
    // Non-PPE flow / store approval
    PENDING_STORE,      // waiting store approval (non-PPE)
    APPROVED_BY_STORE,
    APPROVED_BY_PLANNER,
    APPROVED_BY_PROCUREMENT,
    // Final lifecycle
    ISSUED,
    RECEIVED,           // if you also have a confirmation of receipt step
    REJECTED_BY_PLANNER,
    REJECTED_BY_PROCUREMENT,
    REJECTED_BY_STORE,

    CANCELLED
}
