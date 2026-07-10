package com.example.eac_backend_project.dto;


// In AssignJobRequest.java - add supervisor field
public class AssignJobRequest {
    private Long jobId;
    private Boolean isSupervisor;

    // Getters and setters
    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Boolean getIsSupervisor() {
        return isSupervisor != null ? isSupervisor : false;
    }

    public void setIsSupervisor(Boolean isSupervisor) {
        this.isSupervisor = isSupervisor;
    }
}