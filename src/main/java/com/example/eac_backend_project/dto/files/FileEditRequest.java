package com.example.eac_backend_project.dto.files;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileEditRequest {
    private String newContent; // For text, xml, csv, json, html, code, etc.
    private String editNote;
}
