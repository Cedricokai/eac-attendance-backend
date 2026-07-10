package com.example.eac_backend_project.Service.transport;


import com.example.eac_backend_project.entity.transport.Drivers;
import com.example.eac_backend_project.repository.transport.DriverRepository;
import org.springframework.stereotype.Service;

import java.sql.Driver;
import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    // ✔️ Create Drivers Entry
    public Drivers createDriver(Drivers driver) {
        return driverRepository.save(driver);
    }

    // ✔️ Get All Drivers Entries
    public List<Drivers> getAllDriversEntries() {
        return driverRepository.findAll();
    }

    // ✔️ Get Drivers Entry by ID
    public Drivers getDriversById(Long id) {
        return driverRepository.findById(id)
                .orElse(null);
    }

    // ✔️ Update Drivers Entry
    public Drivers updateDrivers(Long id, Drivers updatedDrivers) {
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
        driverRepository.deleteById(id);
        return true;
    }

}
