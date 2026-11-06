package com.example.eac_backend_project.Service.transport;


import com.example.eac_backend_project.entity.transport.Drivers;
import com.example.eac_backend_project.repository.transport.DriverRepository;
import org.springframework.stereotype.Service;

import java.sql.Driver;
import java.util.List;

@Service
@org.springframework.transaction.annotation.Transactional
public class DriverService {
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportAccess access;
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportCoreService transport;

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    // ✔️ Create Drivers Entry
    public Drivers createDriver(Drivers driver) {
        access.require("TRANSPORT_MANAGE_DRIVERS");
        if(driver.getDriverName()==null||driver.getDriverName().isBlank())throw com.example.eac_backend_project.transport.TransportAccess.fail(400,"Driver name is required");
        driver.setId(null);
        return driverRepository.save(driver);
    }

    // ✔️ Get All Drivers Entries
    public List<Drivers> getAllDriversEntries() {
        return driverRepository.findAll().stream().filter(access::maySeeDriver).toList();
    }

    // ✔️ Get Drivers Entry by ID
    public Drivers getDriversById(Long id) {
        return driverRepository.findById(id)
                .orElse(null);
    }

    // ✔️ Update Drivers Entry
    public Drivers updateDrivers(Long id, Drivers updatedDrivers) {
        access.require("TRANSPORT_MANAGE_DRIVERS");
        Drivers existing = driverRepository.findById(id).orElse(null);
        if (existing == null) return null;

        existing.setDriverName(updatedDrivers.getDriverName());
        existing.setLicenseNumber(updatedDrivers.getLicenseNumber());
        existing.setLicenseExpiry(updatedDrivers.getLicenseExpiry());
        existing.setContact(updatedDrivers.getContact());
        existing.setStatus(updatedDrivers.getStatus());

        return driverRepository.save(existing);
    }

    // ✔️ Delete Drivers Entry
    public boolean deleteDrivers(Long id) {
        if (!driverRepository.existsById(id)) return false;
        access.require("TRANSPORT_MANAGE_DRIVERS");
        var row=driverRepository.findById(id).orElseThrow();transport.archiveDriver(row);driverRepository.save(row);
        return true;
    }

}
