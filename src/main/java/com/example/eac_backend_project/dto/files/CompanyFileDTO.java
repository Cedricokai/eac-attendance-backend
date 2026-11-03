package com.example.eac_backend_project.dto.files;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyFileDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private String extension;
    private Long fileSize;
    private String formattedSize;
    private String uploadedBy;
    private String uploadedByFullName;
    private Integer version;
    private Boolean isEdited;
    private Long originalFileId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastModifiedBy;
    private Boolean isDeleted;
    private String deletedBy;
    private LocalDateTime deletedAt;
    private Integer totalRevisions;
    private Boolean isSharedWithMe;
    private List<String> sharedWithList;
}
