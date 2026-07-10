package com.example.eac_backend_project.repository.billing;

import com.example.eac_backend_project.entity.billing.BillingConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingConfigurationRepository extends JpaRepository<BillingConfiguration, Long> {
}