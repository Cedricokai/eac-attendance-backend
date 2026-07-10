package com.example.eac_backend_project.repository.transport;

import com.example.eac_backend_project.entity.transport.Drivers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Drivers, Long> {
    List<Drivers> findByDriverName(String driverName);
    List<Drivers> findByLicenseNumber(String licenseNumber);
    List<Drivers> findByStatus(String status);
}