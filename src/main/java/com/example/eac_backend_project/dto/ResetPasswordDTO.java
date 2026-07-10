package com.example.eac_backend_project.dto;

import lombok.Data;

@Data
public class ResetPasswordDTO {
    private String token;
    private String newPassword;
    private String confirmPassword;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
