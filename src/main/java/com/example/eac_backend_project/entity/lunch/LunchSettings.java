package com.example.eac_backend_project.entity.lunch;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lunch_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LunchSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value")
    private String settingValue;

    private String description;
}