// AttendanceCodeSettings.java
package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "attendance_code_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceCodeSettings {

    @Id
    private String code;  // P, A, L, H, S, WP, HP, ML, PL, OFF

    @Column(nullable = false)
    private Boolean capNH = false;  // Cap normal hours at standard hours

    @Column(nullable = false)
    private Double defaultNH = 8.0;  // Default normal hours

    @Column(nullable = false)
    private Boolean allowOT = true;  // Allow overtime

    @Column(length = 100)
    private String description;

    @Column(name = "is_system_default")
    private Boolean isSystemDefault = false;
}