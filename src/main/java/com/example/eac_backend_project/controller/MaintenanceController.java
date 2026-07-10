package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.transport.MaintenanceService;
import com.example.eac_backend_project.entity.transport.Maintenance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    // ✔️ CREATE MAINTENANCE ENTRY
    @PostMapping
    public ResponseEntity<Maintenance> createMaintenance(@RequestBody Maintenance maintenance) {
        Maintenance savedMaintenance = maintenanceService.createMaintenance(maintenance);
        return ResponseEntity.ok(savedMaintenance);
    }

    // ✔️ GET ALL MAINTENANCE ENTRIES
    @GetMapping
    public ResponseEntity<List<Maintenance>> getAllMaintenanceEntries() {
        return ResponseEntity.ok(maintenanceService.getAllMaintenanceEntries());
    }

    // ✔️ GET MAINTENANCE ENTRY BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Maintenance> getMaintenanceById(@PathVariable Long id) {
        Maintenance maintenance = maintenanceService.getById(id);
        return (maintenance != null)
                ? ResponseEntity.ok(maintenance)
                : ResponseEntity.notFound().build();
    }

    // ✔️ UPDATE MAINTENANCE ENTRY
    @PutMapping("/{id}")
    public ResponseEntity<Maintenance> updateMaintenance(
            @PathVariable Long id,
            @RequestBody Maintenance updatedMaintenance
    ) {
        Maintenance maintenance = maintenanceService.updateMaintenance(id, updatedMaintenance);
        return (maintenance != null)
                ? ResponseEntity.ok(maintenance)
                : ResponseEntity.notFound().build();
    }

    // ✔️ DELETE MAINTENANCE ENTRY
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMaintenance(@PathVariable Long id) {
        boolean deleted = maintenanceService.deleteMaintenance(id);
        return (deleted)
                ? ResponseEntity.ok("Maintenance entry deleted successfully")
                : ResponseEntity.notFound().build();
    }

    // ✔️ GET MAINTENANCE ENTRIES BY VEHICLE
    @GetMapping("/vehicle/{vehicle}")
    public ResponseEntity<List<Maintenance>> getMaintenanceByVehicle(@PathVariable String vehicle) {
        List<Maintenance> maintenanceEntries = maintenanceService.getMaintenanceByVehicle(vehicle);
        return ResponseEntity.ok(maintenanceEntries);
    }

    // ✔️ GET TOTAL MAINTENANCE COST
    @GetMapping("/total-cost")
    public ResponseEntity<Double> getTotalMaintenanceCost() {
        Double totalCost = maintenanceService.getTotalMaintenanceCost();
        return ResponseEntity.ok(totalCost != null ? totalCost : 0.0);
    }
}