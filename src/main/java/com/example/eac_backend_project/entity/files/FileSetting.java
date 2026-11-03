package com.example.eac_backend_project.entity.files;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    private Integer maxUploadSizeMb = 50;

    @Builder.Default
    private String allowedExtensions = "pdf,xlsx,xls,docx,doc,csv,xml,png,jpg,jpeg,txt,json,zip";

    private String updatedBy;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
