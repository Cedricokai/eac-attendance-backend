package com.example.eac_backend_project.Service.transport;

import com.example.eac_backend_project.dto.VehicleDTO;
import com.example.eac_backend_project.entity.transport.Vehicles;
import com.example.eac_backend_project.repository.AttachmentRepository;
import com.example.eac_backend_project.repository.transport.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@org.springframework.transaction.annotation.Transactional
public class VehicleService {

    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportAccess access;
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportCoreService transport;
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.files.TransportManagedFiles managedFiles;
    private final VehicleRepository vehicleRepository;
    private final AttachmentRepository attachmentRepository;

    public VehicleService(VehicleRepository vehicleRepository, AttachmentRepository attachmentRepository) {
        this.vehicleRepository = vehicleRepository;
        this.attachmentRepository = attachmentRepository;
    }

    public VehicleDTO createVehicle(VehicleDTO vehicleDTO) {
        transport.beforeChange(null,vehicleDTO);
        validate(vehicleDTO);
        // Check if license plate already exists
        if (vehicleRepository.existsByDvla(vehicleDTO.getDvla())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Vehicle registration already exists");
        }

        Vehicles vehicle = convertToEntity(vehicleDTO);
        Vehicles savedVehicle = vehicleRepository.save(vehicle);
        transport.saved(savedVehicle,vehicleDTO,null,true);
        return convertToDTO(savedVehicle);
    }

    public List<VehicleDTO> getAllVehicles() {
        return getAllVehicles(false);
    }
    public List<VehicleDTO> getAllVehicles(boolean includeArchived) {
        if(includeArchived)access.require("TRANSPORT_MANAGE_VEHICLES");
        List<Vehicles> vehicles = access.visible(vehicleRepository.findAll()).stream().filter(v->includeArchived||v.getArchivedAt()==null).toList();
        return vehicles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public VehicleDTO getVehicleById(Long id) {
        Vehicles vehicle = vehicleRepository.findById(id).orElse(null);
        if(vehicle!=null)access.vehicle(vehicle);
        return vehicle != null ? convertToDTO(vehicle) : null;
    }

    public VehicleDTO updateVehicle(Long id, VehicleDTO updatedVehicleDTO) {
        validate(updatedVehicleDTO);
        Vehicles existing = vehicleRepository.lock(id).filter(v->v.getArchivedAt()==null).orElse(null);
        if (existing == null) return null;
        transport.beforeChange(existing,updatedVehicleDTO);
        Double previousMileage=existing.getCurrentMileage();

        // Check if license plate is being changed and if it already exists
        if (!existing.getDvla().equals(updatedVehicleDTO.getDvla()) &&
                vehicleRepository.existsByDvla(updatedVehicleDTO.getDvla())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Vehicle registration already exists");
        }

        existing.setName(updatedVehicleDTO.getName());
        existing.setVehicleLicenseNumber(updatedVehicleDTO.getVehicleLicenseNumber());
        existing.setVehicleLicenseExpiry(updatedVehicleDTO.getVehicleLicenseExpiry());
        existing.setInsuranceExpiry(updatedVehicleDTO.getInsuranceExpiry());
        existing.setVin(updatedVehicleDTO.getVin());
        existing.setType(updatedVehicleDTO.getType());
        existing.setDvla(updatedVehicleDTO.getDvla());

        // Convert status to uppercase
        existing.setStatus(normalizeStatus(updatedVehicleDTO.getStatus()));

        existing.setFuelType(updatedVehicleDTO.getFuelType());
        existing.setMaxFuelCapacity(updatedVehicleDTO.getMaxFuelCapacity());
        existing.setLastService(updatedVehicleDTO.getLastService());
        existing.setNextService(updatedVehicleDTO.getNextService());
        if(updatedVehicleDTO.getAssignedDriver()!=null)existing.setAssignedDriver(updatedVehicleDTO.getAssignedDriver());
        existing.setYear(updatedVehicleDTO.getYear());
        existing.setColor(updatedVehicleDTO.getColor());
        if(updatedVehicleDTO.getCurrentMileage()!=null)existing.setCurrentMileage(updatedVehicleDTO.getCurrentMileage());
        existing.setNextServiceMileage(updatedVehicleDTO.getNextServiceMileage());
        existing.setNotes(updatedVehicleDTO.getNotes());

        Vehicles updatedVehicle = vehicleRepository.save(existing);
        transport.saved(updatedVehicle,updatedVehicleDTO,previousMileage,false);
        return convertToDTO(updatedVehicle);
    }

    public boolean deleteVehicle(Long id) {
        if (!vehicleRepository.existsById(id)) return false;

        var vehicle=vehicleRepository.lock(id).orElseThrow();
        transport.archive(vehicle);
        vehicleRepository.save(vehicle);
        return true;
    }

    public List<VehicleDTO> searchVehicles(String searchTerm) {
        List<Vehicles> vehicles = access.visible(vehicleRepository.searchVehicles(searchTerm)).stream().filter(v->v.getArchivedAt()==null).toList();
        return vehicles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<VehicleDTO> getVehiclesByStatus(String status) {
        List<Vehicles> vehicles = access.visible(vehicleRepository.findByStatus(status)).stream().filter(v->v.getArchivedAt()==null).toList();
        return vehicles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Conversion methods
    private VehicleDTO convertToDTO(Vehicles vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setArchivedAt(vehicle.getArchivedAt());
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


        var attachments=attachmentRepository.findByVehicleId(vehicle.getId()).stream().filter(item->item.getArchivedAt()==null && (item.getManagedFile()!=null?managedFiles.readable(item.getManagedFile().getId()):!item.getFilePath().startsWith("managed:"))).toList();
        dto.setAttachmentIds(attachments.stream().map(item->item.getId()).toList());
        dto.setAttachmentFileNames(attachments.stream().map(item->item.getFileName()).toList());
        dto.setAttachmentCount(attachments.size());

        return transport.enrich(dto,vehicle);
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

    private void validate(VehicleDTO dto) {
        if(dto.getDvla()==null || dto.getDvla().isBlank())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Vehicle registration is required");
        if(dto.getName()==null||dto.getName().isBlank())dto.setName(dto.getDvla().trim());
        dto.setName(dto.getName().trim()); dto.setDvla(dto.getDvla().trim().toUpperCase(java.util.Locale.ROOT));
        for(String value:new String[]{dto.getName(),dto.getDvla(),dto.getType(),dto.getFuelType(),dto.getVin(),dto.getVehicleLicenseNumber(),dto.getColor(),dto.getAssignedDriver(),dto.getNotes()})if(value!=null&&value.length()>255)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Vehicle text fields must not exceed 255 characters");
        if(dto.getYear()!=null&&(dto.getYear()<1900||dto.getYear()>java.time.Year.now().getValue()+2))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Vehicle year is outside the supported range");
        for(Double mileage:new Double[]{dto.getCurrentMileage(),dto.getNextServiceMileage()})
            if(mileage!=null && (!Double.isFinite(mileage) || mileage<0)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Mileage must be a non-negative number or blank");
        if(dto.getMaxFuelCapacity()!=null && (!Double.isFinite(dto.getMaxFuelCapacity()) || dto.getMaxFuelCapacity()<=0)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Fuel capacity must be positive");
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
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Choose ACTIVE, INACTIVE or MAINTENANCE status");
        }

        return upper;
    }


}
