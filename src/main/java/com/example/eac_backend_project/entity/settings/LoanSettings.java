package com.example.eac_backend_project.entity.settings;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "loan_settings")
public class LoanSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private double maximumLoanLimit;
}
