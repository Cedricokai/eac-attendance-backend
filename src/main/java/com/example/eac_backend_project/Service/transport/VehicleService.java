package com.example.eac_backend_project.Service.transport;

import com.example.eac_backend_project.dto.VehicleDTO;
import com.example.eac_backend_project.entity.transport.Vehicles;
import com.example.eac_backend_project.repository.AttachmentRepository;
import com.example.eac_backend_project.repository.transport.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final AttachmentRepository attachmentRepository;

    public VehicleService(VehicleRepository vehicleRepository, AttachmentRepository attachmentRepository) {
        this.vehicleRepository = vehicleRepository;
        this.attachmentRepository = attachmentRepository;
    }

    public VehicleDTO createVehicle(VehicleDTO vehicleDTO) {
        // Check if license plate already exists
        if (vehicleRepository.existsByDvla(vehicleDTO.getDvla())) {
            throw new RuntimeException("Vehicle with license plate " + vehicleDTO.getDvla() + " already exists");
        }

        Vehicles vehicle = convertToEntity(vehicleDTO);
        Vehicles savedVehicle = vehicleRepository.save(vehicle);
        return convertToDTO(savedVehicle);
    }

    public List<VehicleDTO> getAllVehicles() {
        List<Vehicles> vehicles = vehicleRepository.findAll();
        return vehicles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public VehicleDTO getVehicleById(Long id) {
        Vehicles vehicle = vehicleRepository.findById(id).orElse(null);
        return vehicle != null ? convertToDTO(vehicle) : null;
    }

    public VehicleDTO updateVehicle(Long id, VehicleDTO updatedVehicleDTO) {
        Vehicles existing = vehicleRepository.findById(id).orElse(null);
        if (existing == null) return null;

        // Check if license plate is being changed and if it already exists
        if (!existing.getDvla().equals(updatedVehicleDTO.getDvla()) &&
                vehicleRepository.existsByDvla(updatedVehicleDTO.getDvla())) {
            throw new RuntimeException("Vehicle with license plate " + updatedVehicleDTO.getDvla() + " already exists");
        }

        existing.setType(updatedVehicleDTO.getType());
        existing.setDvla(updatedVehicleDTO.getDvla());

        // Convert status to uppercase
        existing.setStatus(normalizeStatus(updatedVehicleDTO.getStatus()));

        existing.setFuelType(updatedVehicleDTO.getFuelType());
        existing.setMaxFuelCapacity(updatedVehicleDTO.getMaxFuelCapacity());
        existing.setLastService(updatedVehicleDTO.getLastService());
        existing.setNextService(updatedVehicleDTO.getNextService());
        existing.setAssignedDriver(updatedVehicleDTO.getAssignedDriver());
        existing.setYear(updatedVehicleDTO.getYear());
        existing.setColor(updatedVehicleDTO.getColor());
        existing.setCurrentMileage(updatedVehicleDTO.getCurrentMileage());
        existing.setNextServiceMileage(updatedVehicleDTO.getNextServiceMileage());
        existing.setNotes(updatedVehicleDTO.getNotes());

        Vehicles updatedVehicle = vehicleRepository.save(existing);
        return convertToDTO(updatedVehicle);
    }

    public boolean deleteVehicle(Long id) {
        if (!vehicleRepository.existsById(id)) return false;

        // Delete attachments first to maintain referential integrity
        attachmentRepository.deleteByVehicleId(id);
        vehicleRepository.deleteById(id);
        return true;
    }

    public List<VehicleDTO> searchVehicles(String searchTerm) {
        List<Vehicles> vehicles = vehicleRepository.searchVehicles(searchTerm);
        return vehicles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<VehicleDTO> getVehiclesByStatus(String status) {
        List<Vehicles> vehicles = vehicleRepository.findByStatus(status);
        return vehicles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Conversion methods
    private VehicleDTO convertToDTO(Vehicles vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setName(vehicle.getName());
        dto.setType(vehicle.getType());
        dto.setDvla(vehicle.getDvla());
        dto.setVehicleLicenseNumber(vehicle.getVehicleLicenseNumber());  // Add this
        dto.setVehicleLicenseExpiry(vehicle.getVehicleLicenseExpiry());  // Add this
        dto.setInsuranceExpiry(vehicle.getInsuranceExpiry());  // Add this
        dto.setVin(vehicle.getVin());  // Add this
        dto.setStatus(vehicle.getStatus());
        dto.setFuelType(vehicle.getFuelType());
        dto.setMaxFuelCapacity(vehicle.getMaxFuelCapacity());
        dto.setLastService(vehicle.getLastService());
        dto.setNextService(vehicle.getNextService());
        dto.setAssignedDriver(vehicle.getAssignedDriver());
        dto.setYear(vehicle.getYear());
        dto.setColor(vehicle.getColor());
        dto.setCurrentMileage(vehicle.getCurrentMileage());
        dto.setNextServiceMileage(vehicle.getNextServiceMileage());
        dto.setNotes(vehicle.getNotes());


        // Add attachment information
        List<Long> attachmentIds = attachmentRepository.findByVehicleId(vehicle.getId())
                .stream()
                .map(attachment -> attachment.getId())
                .collect(Collectors.toList());
        dto.setAttachmentIds(attachmentIds);

        List<String> fileNames = attachmentRepository.findByVehicleId(vehicle.getId())
                .stream()
                .map(attachment -> attachment.getFileName())
                .collect(Collectors.toList());
        dto.setAttachmentFileNames(fileNames);

        Long attachmentCount = attachmentRepository.countByVehicleId(vehicle.getId());
        dto.setAttachmentCount(attachmentCount.intValue());

        return dto;
    }

    private Vehicles convertToEntity(VehicleDTO dto) {
        Vehicles vehicle = new Vehicles();
        vehicle.setName(dto.getName());
        vehicle.setType(dto.getType());
        vehicle.setDvla(dto.getDvla());
        vehicle.setVehicleLicenseNumber(dto.getVehicleLicenseNumber());  // Add this
        vehicle.setVehicleLicenseExpiry(dto.getVehicleLicenseExpiry());  // Add this
        vehicle.setInsuranceExpiry(dto.getInsuranceExpiry());  // Add this
        vehicle.setVin(dto.getVin());  // Add this
        vehicle.setStatus(normalizeStatus(dto.getStatus()));
        vehicle.setFuelType(dto.getFuelType());
        vehicle.setMaxFuelCapacity(dto.getMaxFuelCapacity());
        vehicle.setLastService(dto.getLastService());
        vehicle.setNextService(dto.getNextService());
        vehicle.setAssignedDriver(dto.getAssignedDriver());
        vehicle.setYear(dto.getYear());
        vehicle.setColor(dto.getColor());
        vehicle.setCurrentMileage(dto.getCurrentMileage());
        vehicle.setNextServiceMileage(dto.getNextServiceMileage());
        vehicle.setNotes(dto.getNotes());
        return vehicle;
    }

    private static final List<String> ALLOWED_STATUSES = List.of(
            "ACTIVE",
            "INACTIVE",
            "MAINTENANCE"
    );

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";  // Default
        }

        String upper = status.trim().toUpperCase();

        if (!ALLOWED_STATUSES.contains(upper)) {
            return "ACTIVE";  // fallback to valid value
        }

        return upper;
    }


}