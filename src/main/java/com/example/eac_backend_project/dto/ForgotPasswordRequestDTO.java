package com.example.eac_backend_project.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequestDTO {
    private String emailOrUsername;
    public String getEmailOrUsername() { return emailOrUsername; }
    public void setEmailOrUsername(String emailOrUsername) { this.emailOrUsername = emailOrUsername; }
}
