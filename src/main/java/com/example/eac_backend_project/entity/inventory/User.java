package com.example.eac_backend_project.entity.inventory;

import com.example.eac_backend_project.entity.attendance.Employee;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String userName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String mobile;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private Boolean enabled = true;

    // First login
    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    // Forgot password
    @Column(name = "password_reset_token", length = 120)
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private java.time.LocalDateTime passwordResetExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee employee;




    @ManyToMany(mappedBy = "users")
    private Set<Page> accessiblePages = new HashSet<>();

    public User() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUserName() { return userName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getMobile() { return mobile; }
    public Role getRole() { return role; }
    public Boolean getEnabled() { return enabled; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public void setRole(Role role) { this.role = role; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Set<Page> getAccessiblePages() {
        return accessiblePages;
    }

    public void setAccessiblePages(Set<Page> accessiblePages) {
        this.accessiblePages = accessiblePages;
    }
}