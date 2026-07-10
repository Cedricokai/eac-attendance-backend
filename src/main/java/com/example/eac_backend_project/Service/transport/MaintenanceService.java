package com.example.eac_backend_project.Service.transport;

import com.example.eac_backend_project.entity.transport.Maintenance;
import com.example.eac_backend_project.repository.transport.MaintenanaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaintenanceService {


    private final MaintenanaceRepository maintenanceRepository;

    public MaintenanceService(MaintenanaceRepository maintenanceRepository) {
        this.maintenanceRepository = maintenanceRepository;
    }

    // ✔️ Create Maintenance Entry
    public Maintenance createMaintenance(Maintenance maintenance) {
        return maintenanceRepository.save(maintenance);
    }

    // ✔️ Get All Maintenance Entries
    public List<Maintenance> getAllMaintenanceEntries() {
        return maintenanceRepository.findAll();
    }

    // ✔️ Get Maintenance Entry by ID
    public Maintenance getById(Long id) {
        return maintenanceRepository.findById(id)
                .orElse(null);
    }

    // ✔️ Update Maintenance Entry
    public Maintenance updateMaintenance(Long id, Maintenance updatedMaintenance) {
        Maintenance existing = maintenanceRepository.findById(id).orElse(null);
        if (existing == null) return null;

        existing.setVehicles(updatedMaintenance.getVehicles());
        existing.setMaintenanceType(updatedMaintenance.getMaintenanceType());

        return maintenanceRepository.save(existing);
    }

    // ✔️ Delete Maintenance Entry
    public boolean deleteMaintenance(Long id) {
        if (!maintenanceRepository.existsById(id)) return false;
        maintenanceRepository.deleteById(id);
        return true;
    }

    public List<Maintenance> getMaintenanceByVehicle(String vehicle) {
        return maintenanceRepository.findByVehicles(vehicle);
    }



    // ✔️ Get Total Maintenance Cost
    public Double getTotalMaintenanceCost() {
        return maintenanceRepository.sumTotalCost();
    }



}
