package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.DocumentUploadRequest;
import com.example.eac_backend_project.dto.EmployeeDocumentDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.attendance.EmployeeDocument;
import com.example.eac_backend_project.repository.EmployeeDocumentRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmployeeDocumentService {

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDirectory;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    private EmployeeDocumentRepository documentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // Create upload directory if it doesn't exist
    private void ensureUploadDirectoryExists(Path uploadPath) throws IOException {
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    // Generate unique filename
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    @Transactional
    public EmployeeDocumentDTO uploadDocument(Long employeeId, DocumentUploadRequest request, String uploadedBy)
            throws IOException {

        // Validate employee exists
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        // Validate file size (10MB limit)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("File size must be less than 10MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !isValidContentType(contentType)) {
            throw new RuntimeException("Invalid file type. Allowed: PDF, images, Word, Excel");
        }

        // Create employee-specific directory
        Path employeeUploadDir = Paths.get(uploadDirectory, employeeId.toString());
        ensureUploadDirectoryExists(employeeUploadDir);

        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = generateUniqueFileName(originalFileName);
        Path filePath = employeeUploadDir.resolve(uniqueFileName);

        // Save file to disk
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create document entity
        EmployeeDocument document = new EmployeeDocument();
        document.setTitle(request.getTitle());
        document.setFileName(uniqueFileName);
        document.setOriginalFileName(originalFileName);
        document.setFileType(contentType);
        document.setFileSize(file.getSize());
        document.setFilePath(filePath.toString());
        document.setDocumentType(request.getDocumentType() != null ? request.getDocumentType() : "GENERAL");
        document.setDescription(request.getDescription());
        document.setTags(request.getTags());
        document.setIssueDate(request.getIssueDate());
        document.setExpiryDate(request.getExpiryDate());
        document.setUploadedBy(uploadedBy);
        document.setEmployee(employee);

        // Save document metadata
        EmployeeDocument savedDocument = documentRepository.save(document);

        // Return DTO
        return new EmployeeDocumentDTO(savedDocument, baseUrl, employeeId);
    }

    private boolean isValidContentType(String contentType) {
        return contentType.startsWith("image/") ||
                contentType.equals("application/pdf") ||
                contentType.equals("application/msword") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                contentType.equals("application/vnd.ms-excel") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    public List<EmployeeDocumentDTO> getEmployeeDocuments(Long employeeId) {
        // Verify employee exists
        if (!employeeRepository.existsById(employeeId)) {
            throw new RuntimeException("Employee not found with id: " + employeeId);
        }

        List<EmployeeDocument> documents = documentRepository
                .findByEmployeeIdOrderByUploadDateDesc(employeeId);

        return documents.stream()
                .map(doc -> new EmployeeDocumentDTO(doc, baseUrl, employeeId))
                .collect(Collectors.toList());
    }

    public EmployeeDocumentDTO getDocument(Long employeeId, Long documentId) {
        EmployeeDocument document = documentRepository.findByEmployeeIdAndId(employeeId, documentId);
        if (document == null) {
            throw new RuntimeException("Document not found");
        }

        return new EmployeeDocumentDTO(document, baseUrl, employeeId);
    }

    public byte[] downloadDocument(Long employeeId, Long documentId) throws IOException {
        EmployeeDocument document = documentRepository.findByEmployeeIdAndId(employeeId, documentId);
        if (document == null) {
            throw new RuntimeException("Document not found");
        }

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found on server");
        }

        return Files.readAllBytes(filePath);
    }

    @Transactional
    public void deleteDocument(Long employeeId, Long documentId) throws IOException {
        EmployeeDocument document = documentRepository.findByEmployeeIdAndId(employeeId, documentId);
        if (document == null) {
            throw new RuntimeException("Document not found");
        }

        // Delete file from disk
        Path filePath = Paths.get(document.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Delete from database
        documentRepository.delete(document);
    }

    @Transactional
    public void deleteAllEmployeeDocuments(Long employeeId) throws IOException {
        List<EmployeeDocument> documents = documentRepository.findByEmployeeIdOrderByUploadDateDesc(employeeId);

        // Delete all files from disk
        for (EmployeeDocument document : documents) {
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        }

        // Delete all from database
        documentRepository.deleteByEmployeeId(employeeId);
    }

    public long getDocumentCount(Long employeeId) {
        return documentRepository.countByEmployeeId(employeeId);
    }

    public List<EmployeeDocumentDTO> searchDocuments(Long employeeId, String searchTerm) {
        List<EmployeeDocument> documents = documentRepository
                .searchByEmployeeIdAndKeyword(employeeId, searchTerm);

        return documents.stream()
                .map(doc -> new EmployeeDocumentDTO(doc, baseUrl, employeeId))
                .collect(Collectors.toList());
    }

    public List<EmployeeDocumentDTO> getDocumentsByType(Long employeeId, String documentType) {
        List<EmployeeDocument> documents = documentRepository
                .findByEmployeeIdAndDocumentTypeOrderByUploadDateDesc(employeeId, documentType);

        return documents.stream()
                .map(doc -> new EmployeeDocumentDTO(doc, baseUrl, employeeId))
                .collect(Collectors.toList());
    }

    public List<EmployeeDocumentDTO> getExpiringDocuments(int daysBeforeExpiry) {
        LocalDate thresholdDate = LocalDate.now().plusDays(daysBeforeExpiry);
        // You would need to add this method to the repository
        // For now, we'll filter from all documents
        List<EmployeeDocument> allDocuments = documentRepository.findAll();

        return allDocuments.stream()
                .filter(doc -> doc.getExpiryDate() != null &&
                        !doc.getExpiryDate().isBefore(LocalDate.now()) &&
                        !doc.getExpiryDate().isAfter(thresholdDate))
                .map(doc -> new EmployeeDocumentDTO(doc, baseUrl, doc.getEmployee().getId()))
                .collect(Collectors.toList());
    }
}