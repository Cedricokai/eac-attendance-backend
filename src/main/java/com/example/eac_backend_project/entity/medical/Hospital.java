package com.example.eac_backend_project.entity.medical;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "hospitals", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Hospital {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false, length = 50) private String code;
    private String address;
    @Column(nullable = false) private String contactName;
    @Column(nullable = false, length = 50) private String phone;
    private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private HospitalStatus status = HospitalStatus.ACTIVE;
    @JsonIgnore @Column(name = "access_token", unique = true, length = 64) private String accessToken;
    @JsonIgnore private LocalDateTime tokenCreatedAt;
    private LocalDateTime createdAt;
    @PrePersist void created() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public enum HospitalStatus { ACTIVE, INACTIVE }
}
