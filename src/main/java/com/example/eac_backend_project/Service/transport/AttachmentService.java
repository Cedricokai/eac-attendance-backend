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
public class AttachmentService {

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
        Vehicles vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));

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
        // Generate unique filename to avoid conflicts
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = System.currentTimeMillis() + "_" + vehicle.getDvla() + "_" + originalFileName;
        Path targetLocation = this.fileStorageLocation.resolve(fileName);

        // Copy file to target location with REPLACE_EXISTING option
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Save attachment info to database
        Attachment attachment = new Attachment();
        attachment.setFileName(originalFileName);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setFilePath(targetLocation.toString());
        attachment.setVehicle(vehicle);
        attachment.setUploadDate(java.time.LocalDateTime.now());

        Attachment savedAttachment = attachmentRepository.save(attachment);

        return convertToDTO(savedAttachment);
    }

    public List<AttachmentDTO> getAttachmentsByVehicleId(Long vehicleId) {
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

        Path filePath = Paths.get(attachment.getFilePath());

        // Check if file exists before reading
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found on disk: " + attachment.getFileName() + " at path: " + filePath);
        }

        return Files.readAllBytes(filePath);
    }

    public boolean deleteAttachment(Long attachmentId) {
        Optional<Attachment> attachmentOpt = attachmentRepository.findById(attachmentId);
        if (attachmentOpt.isPresent()) {
            Attachment attachment = attachmentOpt.get();

            try {
                Path filePath = Paths.get(attachment.getFilePath());

                // Check if file exists before attempting to delete
                if (Files.exists(filePath)) {
                    Files.deleteIfExists(filePath);
                }

                // Delete database record
                attachmentRepository.delete(attachment);
                return true;

            } catch (IOException e) {
                throw new RuntimeException("Failed to delete file: " + attachment.getFileName(), e);
            }
        }
        return false;
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