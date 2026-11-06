package com.example.eac_backend_project.Service.transport;

import com.example.eac_backend_project.dto.AttachmentDTO;
import com.example.eac_backend_project.entity.transport.Attachment;
import com.example.eac_backend_project.entity.transport.Vehicles;
import com.example.eac_backend_project.repository.AttachmentRepository;
import com.example.eac_backend_project.repository.transport.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@org.springframework.transaction.annotation.Transactional(rollbackFor=java.io.IOException.class)
public class AttachmentService {

    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportCoreService transport;
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportAccess access;
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.files.TransportManagedFiles managed;
    private final AttachmentRepository attachmentRepository;
    private final VehicleRepository vehicleRepository;
    private final Path fileStorageLocation;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             VehicleRepository vehicleRepository) {
        this.attachmentRepository = attachmentRepository;
        this.vehicleRepository = vehicleRepository;
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public List<AttachmentDTO> uploadFiles(MultipartFile[] files, Long vehicleId) throws IOException {
        access.require("TRANSPORT_MANAGE_VEHICLES");
        if(files.length>10)throw com.example.eac_backend_project.transport.TransportAccess.fail(400,"Upload at most ten attachments at once");
        Vehicles vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

        transport.vehicle(vehicleId);
        return Arrays.stream(files)
                .map(file -> {
                    try {
                        return saveFile(file, vehicle);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to save file: " + file.getOriginalFilename(), e);
                    }
                })
                .collect(Collectors.toList());
    }

    private AttachmentDTO saveFile(MultipartFile file, Vehicles vehicle) throws IOException {
        var checked=com.example.eac_backend_project.transport.TransportFileValidation.validate(file,false,true);
        var managedFile=managed.upload(com.example.eac_backend_project.transport.TransportFileValidation.part(checked));
        String originalFileName=checked.name();
        // Save attachment info to database
        Attachment attachment = new Attachment();
        attachment.setFileName(originalFileName);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(checked.mediaType());
        attachment.setManagedFile(managedFile);
        attachment.setFilePath("managed:"+managedFile.getId());
        attachment.setVehicle(vehicle);
        attachment.setUploadDate(java.time.LocalDateTime.now());

        Attachment savedAttachment = attachmentRepository.save(attachment);

        return convertToDTO(savedAttachment);
    }

    public List<AttachmentDTO> getAttachmentsByVehicleId(Long vehicleId) {
        transport.vehicle(vehicleId);
        return attachmentRepository.findByVehicleId(vehicleId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<AttachmentDTO> getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .map(this::convertToDTO);
    }

    public byte[] getFileData(Long attachmentId) throws IOException {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));

        transport.vehicle(attachment.getVehicle().getId());
        if(attachment.getArchivedAt()!=null)throw com.example.eac_backend_project.transport.TransportAccess.fail(404,"Attachment removed");
        if(attachment.getManagedFile()!=null)return Files.readAllBytes(managed.download(attachment.getManagedFile().getId()).path());
        if(attachment.getFilePath().startsWith("managed:"))throw com.example.eac_backend_project.transport.TransportAccess.fail(404,"Managed attachment was removed");
        Path filePath = Paths.get(attachment.getFilePath()).toAbsolutePath().normalize();
        if(!filePath.startsWith(fileStorageLocation))throw com.example.eac_backend_project.transport.TransportAccess.fail(403,"Invalid attachment storage path");

        // Check if file exists before reading
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found on disk: " + attachment.getFileName() + " at path: " + filePath);
        }

        return Files.readAllBytes(filePath);
    }

    public boolean deleteAttachment(Long attachmentId) {
        access.require("TRANSPORT_MANAGE_VEHICLES");
        var found=attachmentRepository.findById(attachmentId);if(found.isEmpty())return false;
        var attachment=found.get();transport.vehicle(attachment.getVehicle().getId());attachment.setArchivedAt(java.time.Instant.now());attachmentRepository.save(attachment);return true;
    }

    private AttachmentDTO convertToDTO(Attachment attachment) {
        AttachmentDTO dto = new AttachmentDTO();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setFileSize(attachment.getFileSize());
        dto.setFileType(attachment.getFileType());
        dto.setUploadDate(attachment.getUploadDate());
        return dto;
    }
}
