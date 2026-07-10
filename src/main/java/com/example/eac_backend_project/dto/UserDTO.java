package com.example.eac_backend_project.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String name;
    private String userName;
    private String email;
    private String mobile;
    private String role;
    private Boolean enabled;
    private Set<Long> accessiblePageIds;

    public UserDTO() {}

    // Full constructor with accessiblePageIds
    public UserDTO(Long id, String name, String userName, String email, String mobile, String role, Boolean enabled, Set<Long> accessiblePageIds) {
        this.id = id;
        this.name = name;
        this.userName = userName;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
        this.enabled = enabled;
        this.accessiblePageIds = accessiblePageIds;
    }

    // Constructor without accessiblePageIds - FIX THIS ONE!
    public UserDTO(Long id, String name, String userName, String email, String mobile, String role, Boolean enabled) {
        this.id = id;
        this.name = name;
        this.userName = userName;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
        this.enabled = enabled;
        // accessiblePageIds will be null by default
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Set<Long> getAccessiblePageIds() {
        return accessiblePageIds;
    }

    public void setAccessiblePageIds(Set<Long> accessiblePageIds) {
        this.accessiblePageIds = accessiblePageIds;
    }
}