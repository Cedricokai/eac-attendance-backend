package com.example.eac_backend_project.entity.files;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_shares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fileId;

    private String sharedWithUsername;

    private String sharedWithEmail;

    @Column(nullable = false)
    private String sharedBy;

    @Builder.Default
    private String permission = "VIEW"; // "VIEW" or "EDIT"

    @CreationTimestamp
    private LocalDateTime sharedAt;
}
