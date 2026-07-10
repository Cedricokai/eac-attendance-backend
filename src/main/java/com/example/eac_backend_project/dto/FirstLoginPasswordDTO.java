package com.example.eac_backend_project.dto;

import lombok.Data;

@Data
public class FirstLoginPasswordDTO {
    private String newPassword;
    private String confirmPassword;

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
