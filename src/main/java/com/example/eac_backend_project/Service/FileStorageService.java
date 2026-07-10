package com.example.eac_backend_project.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Store a file in the upload directory
     * @param file The file to store
     * @param subDirectory Optional subdirectory within the upload directory
     * @return The stored file path
     * @throws IOException If file cannot be stored
     */
    public String storeFile(MultipartFile file, String subDirectory) throws IOException {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Get original filename
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        // Validate filename
        if (originalFileName.contains("..")) {
            throw new RuntimeException("Invalid file path: " + originalFileName);
        }

        // Create safe filename with UUID to prevent collisions
        String fileName = generateSafeFileName(originalFileName);

        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (subDirectory != null && !subDirectory.trim().isEmpty()) {
            uploadPath = uploadPath.resolve(subDirectory);
        }

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Copy file to target location
        Path targetLocation = uploadPath.resolve(fileName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }

        // Return the relative path
        return targetLocation.toString();
    }

    /**
     * Load a file as a Resource
     * @param filePath The path of the file to load
     * @return Resource object
     */
    public Resource loadFileAsResource(String filePath) {
        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + filePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("File not found: " + filePath, e);
        }
    }

    /**
     * Delete a file
     * @param filePath The path of the file to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + filePath + ", error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if file exists
     * @param filePath The path to check
     * @return true if file exists
     */
    public boolean fileExists(String filePath) {
        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            return Files.exists(path) && Files.isReadable(path);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get file size
     * @param filePath The path of the file
     * @return File size in bytes
     */
    public long getFileSize(String filePath) {
        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            return Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException("Could not get file size: " + filePath, e);
        }
    }

    public String getFileMimeType(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.probeContentType(path);
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }


    /**
     * Generate a safe filename with UUID
     * @param originalFileName Original filename
     * @return Safe filename with UUID
     */
    private String generateSafeFileName(String originalFileName) {
        // Extract file extension
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFileName.substring(lastDotIndex);
        }

        // Generate UUID filename
        String uuid = UUID.randomUUID().toString();

        // Return filename with extension if it had one
        if (!extension.isEmpty()) {
            return uuid + extension;
        }
        return uuid;
    }

    /**
     * Get the base upload directory
     * @return The upload directory path
     */
    public String getUploadDir() {
        return uploadDir;
    }

    /**
     * Get full path including subdirectory
     * @param subDirectory Optional subdirectory
     * @return Full path
     */
    public Path getFullPath(String subDirectory) {
        Path path = Paths.get(uploadDir);
        if (subDirectory != null && !subDirectory.trim().isEmpty()) {
            path = path.resolve(subDirectory);
        }
        return path.toAbsolutePath().normalize();
    }


    /**
     * Clean up old files (optional utility method)
     * @param daysOld Files older than this many days will be deleted
     */
    public void cleanupOldFiles(int daysOld) {
        // Implementation for cleaning up old files
        // This is optional and can be implemented based on your needs
    }
}