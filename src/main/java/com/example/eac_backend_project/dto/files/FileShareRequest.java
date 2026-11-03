package com.example.eac_backend_project.dto.files;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileShareRequest {
    private List<String> usernames;
    private List<String> emails;
    private String permission; // "VIEW" or "EDIT"
    private String message;
}
