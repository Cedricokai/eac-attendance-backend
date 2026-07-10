package com.example.eac_backend_project.entity.directpurchase;

public enum DirectPurchaseStatus {
    PENDING,
    APPROVED,
    ACCOUNTANT_APPROVED,   // after Accountant approval
    REVIEWER_APPROVED,
    REJECTED,
    PAID
}