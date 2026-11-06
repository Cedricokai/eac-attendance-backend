package com.example.eac_backend_project.Service.transport;

import com.example.eac_backend_project.entity.transport.Fuel;
import com.example.eac_backend_project.repository.transport.FuelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@org.springframework.transaction.annotation.Transactional
public class FuelService {
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportAccess access;
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportCoreService transport;

    private final FuelRepository fuelRepository;

    private void validate(Fuel fuel) {
        if(fuel.getVehicle()==null || fuel.getVehicle().isBlank() || fuel.getDate()==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Vehicle and fuel date are required");
        if(fuel.getLitres()==null || !Double.isFinite(fuel.getLitres()) || fuel.getLitres()<=0 || fuel.getCost()==null || !Double.isFinite(fuel.getCost()) || fuel.getCost()<=0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Litres and total cost must be positive");
        String reading=fuel.getOdometer();
        if(reading==null || reading.isBlank()) fuel.setOdometer(null);
        else try { double value=Double.parseDouble(reading); if(!Double.isFinite(value)||value<0) throw new NumberFormatException(); }
        catch(NumberFormatException ex){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Odometer must be a non-negative number or blank");}
    }
    // Constructor injection
    public FuelService(FuelRepository fuelRepository) {
        this.fuelRepository = fuelRepository;
    }

    // ✔️ Create Fuel Entry
    public Fuel createFuel(Fuel fuel) {
        validate(fuel);
        transport.fuelPermission(fuel,false);
        fuel.setId(null);
        Fuel saved=fuelRepository.save(fuel);
        transport.fuelSaved(saved,true);
        return saved;
    }

    // ✔️ Get All Fuel Entries
    public List<Fuel> getAllFuelEntries() {
        return fuelRepository.findAll().stream().filter(transport::maySeeFuel).toList();
    }

    // ✔️ Get Fuel Entry by ID
    public Fuel getFuelById(Long id) {
        return fuelRepository.findById(id)
                .orElse(null);
    }

    // ✔️ Update Fuel Entry
    public Fuel updateFuel(Long id, Fuel updatedFuel) {
        validate(updatedFuel);
        Fuel existing = fuelRepository.findById(id).orElse(null);
        if (existing == null) return null;
        transport.fuelPermission(existing,true);
        updatedFuel.setId(id);
        transport.fuelPermission(updatedFuel,true);

        transport.beforeFuelEdit(existing,updatedFuel);
        existing.setOdometerCorrectionReason(updatedFuel.getOdometerCorrectionReason());
        existing.setVehicle(updatedFuel.getVehicle());
        existing.setDriver(updatedFuel.getDriver());
        existing.setDate(updatedFuel.getDate());
        existing.setLitres(updatedFuel.getLitres());
        existing.setCost(updatedFuel.getCost());
        existing.setOdometer(updatedFuel.getOdometer());
        Fuel saved=fuelRepository.save(existing);
        transport.fuelSaved(saved,false);
        return saved;
    }

    // ✔️ Delete Fuel Entry
    public boolean deleteFuel(Long id) {
        if (!fuelRepository.existsById(id)) return false;
        access.require("TRANSPORT_MANAGE_FUEL");
        var row=fuelRepository.findById(id).orElseThrow();row.setArchivedAt(java.time.Instant.now());fuelRepository.save(row);
        return true;
    }

    // ✔️ Get Fuel Entries by Vehicle
    public List<Fuel> getFuelByVehicle(String vehicle) {
        return fuelRepository.findByVehicle(vehicle).stream().filter(transport::maySeeFuel).toList();
    }

    // ✔️ Get Fuel Entries by Driver
    public List<Fuel> getFuelByDriver(String driver) {
        return fuelRepository.findByDriver(driver).stream().filter(transport::maySeeFuel).toList();
    }

    // ✔️ Get Total Fuel Cost
    public Double getTotalFuelCost() {
        return getAllFuelEntries().stream().mapToDouble(f->f.getCost()==null?0:f.getCost()).sum();
    }

    // ✔️ Get Total Liters
    public Double getTotalLiters() {
        return getAllFuelEntries().stream().mapToDouble(f->f.getLitres()==null?0:f.getLitres()).sum();
    }
}
