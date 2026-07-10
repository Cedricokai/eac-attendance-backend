package com.example.eac_backend_project.Service.transport;

import com.example.eac_backend_project.entity.transport.Fuel;
import com.example.eac_backend_project.repository.transport.FuelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FuelService {

    private final FuelRepository fuelRepository;

    // Constructor injection
    public FuelService(FuelRepository fuelRepository) {
        this.fuelRepository = fuelRepository;
    }

    // ✔️ Create Fuel Entry
    public Fuel createFuel(Fuel fuel) {
        return fuelRepository.save(fuel);
    }

    // ✔️ Get All Fuel Entries
    public List<Fuel> getAllFuelEntries() {
        return fuelRepository.findAll();
    }

    // ✔️ Get Fuel Entry by ID
    public Fuel getFuelById(Long id) {
        return fuelRepository.findById(id)
                .orElse(null);
    }

    // ✔️ Update Fuel Entry
    public Fuel updateFuel(Long id, Fuel updatedFuel) {
        Fuel existing = fuelRepository.findById(id).orElse(null);
        if (existing == null) return null;

        existing.setVehicle(updatedFuel.getVehicle());
        existing.setDriver(updatedFuel.getDriver());
        existing.setDate(updatedFuel.getDate());
        existing.setLitres(updatedFuel.getLitres());
        existing.setCost(updatedFuel.getCost());
        existing.setOdometer(updatedFuel.getOdometer());
        return fuelRepository.save(existing);
    }

    // ✔️ Delete Fuel Entry
    public boolean deleteFuel(Long id) {
        if (!fuelRepository.existsById(id)) return false;
        fuelRepository.deleteById(id);
        return true;
    }

    // ✔️ Get Fuel Entries by Vehicle
    public List<Fuel> getFuelByVehicle(String vehicle) {
        return fuelRepository.findByVehicle(vehicle);
    }

    // ✔️ Get Fuel Entries by Driver
    public List<Fuel> getFuelByDriver(String driver) {
        return fuelRepository.findByDriver(driver);
    }

    // ✔️ Get Total Fuel Cost
    public Double getTotalFuelCost() {
        return fuelRepository.sumTotalCost();
    }

    // ✔️ Get Total Liters
    public Double getTotalLiters() {
        return fuelRepository.sumTotalLiters();
    }
}