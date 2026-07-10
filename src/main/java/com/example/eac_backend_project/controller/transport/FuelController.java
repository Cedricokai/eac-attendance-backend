package com.example.eac_backend_project.controller.transport;

import com.example.eac_backend_project.Service.transport.FuelService;
import com.example.eac_backend_project.entity.transport.Fuel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fuel")
public class FuelController {

    private final FuelService fuelService;

    public FuelController(FuelService fuelService) {
        this.fuelService = fuelService;
    }

    // ✔️ CREATE FUEL ENTRY
    @PostMapping
    public ResponseEntity<Fuel> createFuel(@RequestBody Fuel fuel) {
        Fuel savedFuel = fuelService.createFuel(fuel);
        return ResponseEntity.ok(savedFuel);
    }

    // ✔️ GET ALL FUEL ENTRIES
    @GetMapping
    public ResponseEntity<List<Fuel>> getAllFuelEntries() {
        return ResponseEntity.ok(fuelService.getAllFuelEntries());
    }

    // ✔️ GET FUEL ENTRY BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Fuel> getFuelById(@PathVariable Long id) {
        Fuel fuel = fuelService.getFuelById(id);
        return (fuel != null)
                ? ResponseEntity.ok(fuel)
                : ResponseEntity.notFound().build();
    }

    // ✔️ UPDATE FUEL ENTRY
    @PutMapping("/{id}")
    public ResponseEntity<Fuel> updateFuel(
            @PathVariable Long id,
            @RequestBody Fuel updatedFuel
    ) {
        Fuel fuel = fuelService.updateFuel(id, updatedFuel);
        return (fuel != null)
                ? ResponseEntity.ok(fuel)
                : ResponseEntity.notFound().build();
    }

    // ✔️ DELETE FUEL ENTRY
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFuel(@PathVariable Long id) {
        boolean deleted = fuelService.deleteFuel(id);
        return (deleted)
                ? ResponseEntity.ok("Fuel entry deleted successfully")
                : ResponseEntity.notFound().build();
    }

    // ✔️ GET FUEL ENTRIES BY VEHICLE
    @GetMapping("/vehicle/{vehicle}")
    public ResponseEntity<List<Fuel>> getFuelByVehicle(@PathVariable String vehicle) {
        List<Fuel> fuelEntries = fuelService.getFuelByVehicle(vehicle);
        return ResponseEntity.ok(fuelEntries);
    }

    // ✔️ GET FUEL ENTRIES BY DRIVER
    @GetMapping("/driver/{driver}")
    public ResponseEntity<List<Fuel>> getFuelByDriver(@PathVariable String driver) {
        List<Fuel> fuelEntries = fuelService.getFuelByDriver(driver);
        return ResponseEntity.ok(fuelEntries);
    }

    // ✔️ GET TOTAL FUEL COST
    @GetMapping("/total-cost")
    public ResponseEntity<Double> getTotalFuelCost() {
        Double totalCost = fuelService.getTotalFuelCost();
        return ResponseEntity.ok(totalCost);
    }

    // ✔️ GET TOTAL LITERS
    @GetMapping("/total-liters")
    public ResponseEntity<Double> getTotalLiters() {
        Double totalLiters = fuelService.getTotalLiters();
        return ResponseEntity.ok(totalLiters);
    }
}