package com.example.eac_backend_project.controller.transport;


import com.example.eac_backend_project.Service.transport.DriverService;
import com.example.eac_backend_project.entity.transport.Drivers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    // ✔️ CREATE DRIVER ENTRY
    @PostMapping
    public ResponseEntity<Drivers> createDriver(@RequestBody Drivers driver) {
        Drivers savedDriver = driverService.createDriver(driver);
        return ResponseEntity.ok(savedDriver);
    }

    // ✔️ GET ALL DRIVER ENTRIES
    @GetMapping
    public ResponseEntity<List<Drivers>> getAllDriverEntries() {
        return ResponseEntity.ok(driverService.getAllDriversEntries());
    }

    // ✔️ GET DRIVER ENTRY BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Drivers> getFuelById(@PathVariable Long id) {
        Drivers driver = driverService.getDriversById(id);
        return (driver != null)
                ? ResponseEntity.ok(driver)
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Drivers> updateDriver(
            @PathVariable Long id,
            @RequestBody Drivers updatedDriver
    ) {
        Drivers driver = driverService.updateDrivers(id, updatedDriver); // Use correct method name
        return (driver != null)
                ? ResponseEntity.ok(driver)
                : ResponseEntity.notFound().build();
    }

    // ✔️ DELETE DRIVER ENTRY
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDriver(@PathVariable Long id) {
        boolean deleted = driverService.deleteDrivers(id);
        return (deleted)
                ? ResponseEntity.ok("Fuel entry deleted successfully")
                : ResponseEntity.notFound().build();
    }






}

