package com.example.eac_backend_project.dto;

import lombok.Data;

@Data
public class ApprovalDTO {
    private String notes;
    private String approvedBy;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
}
