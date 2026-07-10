package com.example.eac_backend_project.controller.transport;


import com.example.eac_backend_project.Service.transport.VehicleService;
import com.example.eac_backend_project.dto.VehicleDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<?> createVehicle(@RequestBody VehicleDTO vehicleDTO) {
        try {
            VehicleDTO savedVehicle = vehicleService.createVehicle(vehicleDTO);
            return ResponseEntity.ok(savedVehicle);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<VehicleDTO>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDTO> getVehicleById(@PathVariable Long id) {
        VehicleDTO vehicle = vehicleService.getVehicleById(id);
        return (vehicle != null)
                ? ResponseEntity.ok(vehicle)
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVehicle(
            @PathVariable Long id,
            @RequestBody VehicleDTO updatedVehicleDTO
    ) {
        try {
            VehicleDTO vehicle = vehicleService.updateVehicle(id, updatedVehicleDTO);
            return (vehicle != null)
                    ? ResponseEntity.ok(vehicle)
                    : ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        boolean deleted = vehicleService.deleteVehicle(id);
        return (deleted)
                ? ResponseEntity.ok("Vehicle deleted successfully")
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<VehicleDTO>> searchVehicles(@RequestParam String q) {
        List<VehicleDTO> vehicles = vehicleService.searchVehicles(q);
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<VehicleDTO>> getVehiclesByStatus(@PathVariable String status) {
        List<VehicleDTO> vehicles = vehicleService.getVehiclesByStatus(status);
        return ResponseEntity.ok(vehicles);
    }
}