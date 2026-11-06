package com.example.eac_backend_project.files;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="file_activity_logs",indexes=@Index(name="idx_file_event_item",columnList="itemId")) @Getter @Setter
public class FileEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long itemId;
    private Long ownerId;
    private String filename;
    private String username;
    private String action;
    private String status="SUCCESS";
    private String ipAddress;
    private Instant createdAt=Instant.now();
}
