package com.example.eac_backend_project.Service.transport;

import com.example.eac_backend_project.entity.transport.Maintenance;
import com.example.eac_backend_project.repository.transport.MaintenanaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@org.springframework.transaction.annotation.Transactional
public class MaintenanceService {
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportAccess access;
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportCoreService transport;


    private final MaintenanaceRepository maintenanceRepository;

    public MaintenanceService(MaintenanaceRepository maintenanceRepository) {
        this.maintenanceRepository = maintenanceRepository;
    }

    // ✔️ Create Maintenance Entry
    public Maintenance createMaintenance(Maintenance maintenance) {
        transport.maintenancePermission(maintenance);
        maintenance.setId(null);
        Maintenance saved=maintenanceRepository.save(maintenance);
        transport.maintenanceSaved(saved,true);
        return saved;
    }

    // ✔️ Get All Maintenance Entries
    public List<Maintenance> getAllMaintenanceEntries() {
        return maintenanceRepository.findAll().stream().filter(transport::maySeeMaintenance).toList();
    }

    // ✔️ Get Maintenance Entry by ID
    public Maintenance getById(Long id) {
        var record=maintenanceRepository.findById(id).orElse(null);
        if(record!=null&&!transport.maySeeMaintenance(record))throw com.example.eac_backend_project.transport.TransportAccess.fail(403,"Maintenance record is outside your access");
        return record;
    }

    // ✔️ Update Maintenance Entry
    public Maintenance updateMaintenance(Long id, Maintenance updatedMaintenance) {
        Maintenance existing = maintenanceRepository.findById(id).orElse(null);
        if (existing == null) return null;
        transport.beforeMaintenanceEdit(existing);
        transport.maintenancePermission(existing);
        transport.maintenancePermission(updatedMaintenance);

        existing.setVehicles(updatedMaintenance.getVehicles());
        existing.setMaintenanceType(updatedMaintenance.getMaintenanceType());
        existing.setDate(updatedMaintenance.getDate());
        existing.setStatus(updatedMaintenance.getStatus());
        existing.setCost(updatedMaintenance.getCost());
        existing.setNextDue(updatedMaintenance.getNextDue());
        existing.setVendor(updatedMaintenance.getVendor());

        Maintenance saved=maintenanceRepository.save(existing);
        transport.maintenanceSaved(saved,false);
        return saved;
    }

    // ✔️ Delete Maintenance Entry
    public boolean deleteMaintenance(Long id) {
        if (!maintenanceRepository.existsById(id)) return false;
        access.require("TRANSPORT_MANAGE_MAINTENANCE");
        var row=maintenanceRepository.findById(id).orElseThrow();transport.beforeMaintenanceEdit(row);row.setArchivedAt(java.time.Instant.now());maintenanceRepository.save(row);
        return true;
    }

    public List<Maintenance> getMaintenanceByVehicle(String vehicle) {
        return maintenanceRepository.findByVehicles(vehicle).stream().filter(transport::maySeeMaintenance).toList();
    }



    // ✔️ Get Total Maintenance Cost
    public Double getTotalMaintenanceCost() {
        return getAllMaintenanceEntries().stream().mapToDouble(m->m.getCost()==null?0:m.getCost()).sum();
    }



}
