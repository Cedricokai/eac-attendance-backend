package com.example.eac_backend_project.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "email.templates")
public class EmailTemplatesConfig {

    private LeaveTemplates leave = new LeaveTemplates();

    public static class LeaveTemplates {
        private String supervisorApprovalRequest;
        private String plannerApprovalRequest;
        private String hrApprovalRequest;
        private String approved;
        private String rejected;
        private String submitted;

        public String getSupervisorApprovalRequest() { return supervisorApprovalRequest; }
        public void setSupervisorApprovalRequest(String supervisorApprovalRequest) { this.supervisorApprovalRequest = supervisorApprovalRequest; }
        public String getPlannerApprovalRequest() { return plannerApprovalRequest; }
        public void setPlannerApprovalRequest(String plannerApprovalRequest) { this.plannerApprovalRequest = plannerApprovalRequest; }
        public String getHrApprovalRequest() { return hrApprovalRequest; }
        public void setHrApprovalRequest(String hrApprovalRequest) { this.hrApprovalRequest = hrApprovalRequest; }
        public String getApproved() { return approved; }
        public void setApproved(String approved) { this.approved = approved; }
        public String getRejected() { return rejected; }
        public void setRejected(String rejected) { this.rejected = rejected; }
        public String getSubmitted() { return submitted; }
        public void setSubmitted(String submitted) { this.submitted = submitted; }
    }

    public LeaveTemplates getLeave() { return leave; }
    public void setLeave(LeaveTemplates leave) { this.leave = leave; }
}