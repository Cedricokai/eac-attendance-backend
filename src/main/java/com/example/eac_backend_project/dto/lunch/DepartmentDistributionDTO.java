package com.example.eac_backend_project.dto.lunch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDistributionDTO {
    private String name;   // department name
    private long value;    // number of employees / lunches
}