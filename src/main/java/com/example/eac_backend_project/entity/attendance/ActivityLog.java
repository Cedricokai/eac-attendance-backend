package com.example.eac_backend_project.entity.attendance;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private String type; // "INFO", "WARNING", "ERROR", "SUCCESS"

    @CreationTimestamp
    private LocalDateTime timestamp;

    private String source; // e.g., "EMPLOYEE", "PAYROLL", "LOAN", "CREDIT", "SYSTEM"

    private String username; // who performed the action

    private Long relatedEntityId; // optional: employeeId, periodId, loanId etc.

    // Getters and setters (or use Lombok @Data)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getRelatedEntityId() { return relatedEntityId; }
    public void setRelatedEntityId(Long relatedEntityId) { this.relatedEntityId = relatedEntityId; }
}