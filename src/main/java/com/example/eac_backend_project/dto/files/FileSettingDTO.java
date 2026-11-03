package com.example.eac_backend_project.dto.files;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileSettingDTO {
    private Integer maxUploadSizeMb;
    private String allowedExtensions;
    private String updatedBy;
}
