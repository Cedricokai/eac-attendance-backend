package com.example.eac_backend_project.entity.attendance;

public enum ApprovalStage {
    PENDING("Pending"),
    SUPERVISOR_APPROVED("Supervisor_Approved"),
    PLANNER_APPROVED("Planner_Approved"),
    HR_APPROVED("HR_Approved"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    ApprovalStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isApproved() {
        return this == APPROVED || this == SUPERVISOR_APPROVED
                || this == PLANNER_APPROVED || this == HR_APPROVED;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }

    public ApprovalStage getNextStage() {
        switch (this) {
            case PENDING:
                return SUPERVISOR_APPROVED;
            case SUPERVISOR_APPROVED:
                return PLANNER_APPROVED;
            case PLANNER_APPROVED:
                return HR_APPROVED;
            case HR_APPROVED:
                return APPROVED;
            default:
                return this;
        }
    }

    public static ApprovalStage getStageForRole(String role) {
        switch (role.toLowerCase()) {
            case "supervisor":
                return SUPERVISOR_APPROVED;
            case "planner":
                return PLANNER_APPROVED;
            case "hr":
                return HR_APPROVED;
            default:
                return PENDING;
        }
    }
}