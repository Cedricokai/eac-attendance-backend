package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.dto.files.*;
import com.example.eac_backend_project.entity.files.*;
import com.example.eac_backend_project.repository.files.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyFileService {

    private final CompanyFileRepository fileRepository;
    private final FileShareRepository shareRepository;
    private final FileAuditTrailRepository auditRepository;
    private final FileSettingRepository settingRepository;
    private final Optional<EmailService> emailService;

    private static final String UPLOAD_DIR = "uploads/company-files";

    private Path getStoragePath() {
        Path path = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            log.error("Could not initialize upload folder", e);
        }
        return path;
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
    }

    /**
     * Get or create default file settings
     */
    public FileSetting getSettings() {
        return settingRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            FileSetting setting = FileSetting.builder()
                    .maxUploadSizeMb(50)
                    .allowedExtensions("pdf,xlsx,xls,docx,doc,csv,xml,png,jpg,jpeg,txt,json,zip,pptx,ppt")
                    .build();
            return settingRepository.save(setting);
        });
    }

    public FileSettingDTO updateSettings(FileSettingDTO dto, Authentication auth) {
        if (!isAdmin(auth)) {
            throw new RuntimeException("Unauthorized: Only Admins can modify file settings");
        }
        FileSetting setting = getSettings();
        if (dto.getMaxUploadSizeMb() != null && dto.getMaxUploadSizeMb() > 0) {
            setting.setMaxUploadSizeMb(dto.getMaxUploadSizeMb());
        }
        if (StringUtils.hasText(dto.getAllowedExtensions())) {
            setting.setAllowedExtensions(dto.getAllowedExtensions().trim().toLowerCase());
        }
        setting.setUpdatedBy(auth.getName());
        setting.setUpdatedAt(LocalDateTime.now());
        FileSetting saved = settingRepository.save(setting);
        return FileSettingDTO.builder()
                .maxUploadSizeMb(saved.getMaxUploadSizeMb())
                .allowedExtensions(saved.getAllowedExtensions())
                .updatedBy(saved.getUpdatedBy())
                .build();
    }

    /**
     * Upload a new company file (v1)
     */
    @Transactional
    public CompanyFileDTO uploadFile(MultipartFile file, String description, Authentication auth) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        FileSetting settings = getSettings();
        long maxBytes = (long) settings.getMaxUploadSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size exceeds configured limit of " + settings.getMaxUploadSizeMb() + "MB");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = getFileExtension(originalFilename).toLowerCase();

        // Check allowed extensions
        if (StringUtils.hasText(settings.getAllowedExtensions())) {
            List<String> allowed = Arrays.stream(settings.getAllowedExtensions().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();
            if (!allowed.isEmpty() && !allowed.contains(extension)) {
                throw new IllegalArgumentException("File extension ." + extension + " is not permitted by company policy");
            }
        }

        String storedFileName = UUID.randomUUID() + "_" + originalFilename;
        Path targetPath = getStoragePath().resolve(storedFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String username = auth != null ? auth.getName() : "Anonymous";

        CompanyFile companyFile = CompanyFile.builder()
                .fileName(originalFilename)
                .storedFileName(storedFileName)
                .filePath(targetPath.toString())
                .fileType(file.getContentType())
                .extension(extension)
                .fileSize(file.getSize())
                .uploadedBy(username)
                .uploadedByFullName(username)
                .version(1)
                .isEdited(false)
                .originalFileId(null)
                .description(description)
                .build();

        CompanyFile saved = fileRepository.save(companyFile);

        // Record audit
        logAudit(saved.getId(), "UPLOAD", username, "Uploaded initial version v1 (" + formatFileSize(file.getSize()) + ")");

        return convertToDTO(saved, auth);
    }

    /**
     * List accessible files for current user
     */
    public List<CompanyFileDTO> getFiles(Authentication auth, String search, String extensionGroup) {
        String username = auth != null ? auth.getName() : "";
        boolean admin = isAdmin(auth);

        List<CompanyFile> rootFiles;
        if (admin) {
            rootFiles = fileRepository.findAllRootFiles();
        } else {
            // Find authored files
            Set<Long> accessibleIds = new HashSet<>();
            List<CompanyFile> authored = fileRepository.findByUploadedByAndOriginalFileIdIsNullOrderByCreatedAtDesc(username);
            authored.forEach(f -> accessibleIds.add(f.getId()));

            // Find shared files
            List<FileShare> shares = shareRepository.findBySharedWithUsername(username);
            shares.forEach(s -> {
                CompanyFile sharedFile = fileRepository.findById(s.getFileId()).orElse(null);
                if (sharedFile != null) {
                    Long rootId = sharedFile.getOriginalFileId() != null ? sharedFile.getOriginalFileId() : sharedFile.getId();
                    accessibleIds.add(rootId);
                }
            });

            if (accessibleIds.isEmpty()) {
                return Collections.emptyList();
            }
            rootFiles = fileRepository.findByIdIn(new ArrayList<>(accessibleIds));
        }

        return rootFiles.stream()
                .map(f -> {
                    // Find latest revision if edited
                    List<CompanyFile> revisions = fileRepository.findRevisionsDescending(f.getId());
                    CompanyFile latest = revisions.isEmpty() ? f : revisions.get(0);
                    CompanyFileDTO dto = convertToDTO(latest, auth);
                    dto.setTotalRevisions(revisions.size());
                    dto.setOriginalFileId(f.getId());
                    if (!admin && !f.getUploadedBy().equalsIgnoreCase(username)) {
                        dto.setIsSharedWithMe(true);
                    }
                    return dto;
                })
                .filter(dto -> {
                    if (StringUtils.hasText(search)) {
                        String q = search.toLowerCase();
                        boolean matchName = dto.getFileName() != null && dto.getFileName().toLowerCase().contains(q);
                        boolean matchAuthor = dto.getUploadedBy() != null && dto.getUploadedBy().toLowerCase().contains(q);
                        boolean matchDesc = dto.getDescription() != null && dto.getDescription().toLowerCase().contains(q);
                        if (!matchName && !matchAuthor && !matchDesc) return false;
                    }

                    if (StringUtils.hasText(extensionGroup) && !extensionGroup.equalsIgnoreCase("all")) {
                        String ext = dto.getExtension() != null ? dto.getExtension().toLowerCase() : "";
                        switch (extensionGroup.toLowerCase()) {
                            case "documents":
                                return ext.equals("doc") || ext.equals("docx") || ext.equals("odt") || ext.equals("txt") || ext.equals("rtf");
                            case "spreadsheets":
                                return ext.equals("xls") || ext.equals("xlsx") || ext.equals("csv");
                            case "data":
                                return ext.equals("csv") || ext.equals("xml") || ext.equals("json");
                            case "pdf":
                                return ext.equals("pdf");
                            case "images":
                                return ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("gif") || ext.equals("webp");
                        }
                    }
                    return true;
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Check access to a file
     */
    public CompanyFile getAuthorizedFile(Long fileId, Authentication auth) {
        CompanyFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));

        if (isAdmin(auth)) return file;

        String username = auth != null ? auth.getName() : "";
        Long rootId = file.getOriginalFileId() != null ? file.getOriginalFileId() : file.getId();

        // Check if author
        if (file.getUploadedBy().equalsIgnoreCase(username)) return file;
        CompanyFile root = fileRepository.findById(rootId).orElse(null);
        if (root != null && root.getUploadedBy().equalsIgnoreCase(username)) return file;

        // Check if shared
        boolean isShared = shareRepository.existsByFileIdAndSharedWithUsername(file.getId(), username)
                || (root != null && shareRepository.existsByFileIdAndSharedWithUsername(root.getId(), username));

        if (!isShared) {
            throw new RuntimeException("Access Denied: You do not have permission to view or access this file.");
        }

        if (Boolean.TRUE.equals(file.getIsDeleted())) {
            throw new RuntimeException("Access Denied: File has been moved to the Recycle Bin.");
        }

        return file;
    }

    /**
     * Download file resource
     */
    public Resource downloadFile(Long fileId, Authentication auth) {
        CompanyFile file = getAuthorizedFile(fileId, auth);
        Path path = Paths.get(file.getFilePath());
        try {
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                String username = auth != null ? auth.getName() : "Anonymous";
                logAudit(file.getId(), "DOWNLOAD", username, "Downloaded version v" + file.getVersion());
                return resource;
            } else {
                throw new RuntimeException("File not found on storage: " + file.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error accessing file storage", e);
        }
    }

    /**
     * Read text content for in-browser editing (XML, CSV, TXT, JSON, HTML, etc.)
     */
    public String getFileTextContent(Long fileId, Authentication auth) throws IOException {
        CompanyFile file = getAuthorizedFile(fileId, auth);
        Path path = Paths.get(file.getFilePath());
        if (!Files.exists(path)) {
            throw new RuntimeException("File not found on storage: " + file.getFileName());
        }
        String ext = file.getExtension() != null ? file.getExtension().toLowerCase() : "";
        if (ext.equals("pdf") || ext.equals("xlsx") || ext.equals("xls") || ext.equals("docx") || ext.equals("doc")
                || ext.equals("zip") || ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("gif")) {
            throw new IllegalArgumentException("File '" + file.getFileName() + "' is a binary format and cannot be loaded as plain text. Please use the spreadsheet editor or document revision upload.");
        }
        try {
            String username = auth != null ? auth.getName() : "Anonymous";
            logAudit(file.getId(), "PREVIEW", username, "Opened text/data preview in editor");
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException e) {
            throw new IllegalArgumentException("File contains binary characters that cannot be edited as plain text.");
        }
    }

    /**
     * Edit in-browser: creates a new version while preserving original (for text content)
     */
    @Transactional
    public CompanyFileDTO saveInBrowserEdit(Long fileId, FileEditRequest editRequest, Authentication auth) throws IOException {
        CompanyFile currentFile = getAuthorizedFile(fileId, auth);
        String username = auth != null ? auth.getName() : "Anonymous";

        Long rootId = currentFile.getOriginalFileId() != null ? currentFile.getOriginalFileId() : currentFile.getId();
        CompanyFile rootFile = fileRepository.findById(rootId).orElse(currentFile);

        // Calculate next version
        List<CompanyFile> history = fileRepository.findRevisionsDescending(rootId);
        int nextVersion = history.stream().mapToInt(CompanyFile::getVersion).max().orElse(1) + 1;

        // Save new content to disk
        String newStoredName = UUID.randomUUID() + "_v" + nextVersion + "_" + rootFile.getFileName();
        Path newPath = getStoragePath().resolve(newStoredName);
        Files.writeString(newPath, editRequest.getNewContent() != null ? editRequest.getNewContent() : "", StandardCharsets.UTF_8);

        long newSize = Files.size(newPath);

        CompanyFile newRevision = CompanyFile.builder()
                .fileName(rootFile.getFileName())
                .storedFileName(newStoredName)
                .filePath(newPath.toString())
                .fileType(rootFile.getFileType())
                .extension(rootFile.getExtension())
                .fileSize(newSize)
                .uploadedBy(username)
                .uploadedByFullName(username)
                .version(nextVersion)
                .isEdited(true)
                .originalFileId(rootId)
                .description(StringUtils.hasText(editRequest.getEditNote()) ? editRequest.getEditNote() : "Edited in browser (v" + nextVersion + ")")
                .lastModifiedBy(username)
                .isDeleted(false)
                .build();

        CompanyFile savedRevision = fileRepository.save(newRevision);

        // Update root file timestamp and last modified by
        rootFile.setLastModifiedBy(username);
        rootFile.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(rootFile);

        // Audit log
        logAudit(rootId, "EDIT", username, "Edited in browser and created version v" + nextVersion + ". Original v1 intact.");
        logAudit(savedRevision.getId(), "VERSION_CREATE", username, "Created version v" + nextVersion);

        return convertToDTO(savedRevision, auth);
    }

    /**
     * Upload an updated file revision (e.g. edited spreadsheet binary or updated PDF/DOCX)
     */
    @Transactional
    public CompanyFileDTO uploadRevision(Long fileId, MultipartFile file, String editNote, Authentication auth) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File revision cannot be empty");
        }

        CompanyFile currentFile = getAuthorizedFile(fileId, auth);
        String username = auth != null ? auth.getName() : "Anonymous";

        Long rootId = currentFile.getOriginalFileId() != null ? currentFile.getOriginalFileId() : currentFile.getId();
        CompanyFile rootFile = fileRepository.findById(rootId).orElse(currentFile);

        List<CompanyFile> history = fileRepository.findRevisionsDescending(rootId);
        int nextVersion = history.stream().mapToInt(CompanyFile::getVersion).max().orElse(1) + 1;

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = getFileExtension(originalFilename).toLowerCase();

        String storedFileName = UUID.randomUUID() + "_v" + nextVersion + "_" + originalFilename;
        Path targetPath = getStoragePath().resolve(storedFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        CompanyFile newRevision = CompanyFile.builder()
                .fileName(rootFile.getFileName())
                .storedFileName(storedFileName)
                .filePath(targetPath.toString())
                .fileType(file.getContentType() != null ? file.getContentType() : rootFile.getFileType())
                .extension(extension)
                .fileSize(file.getSize())
                .uploadedBy(username)
                .uploadedByFullName(username)
                .version(nextVersion)
                .isEdited(true)
                .originalFileId(rootId)
                .description(StringUtils.hasText(editNote) ? editNote : "Updated file revision v" + nextVersion)
                .lastModifiedBy(username)
                .isDeleted(false)
                .build();

        CompanyFile savedRevision = fileRepository.save(newRevision);

        rootFile.setLastModifiedBy(username);
        rootFile.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(rootFile);

        logAudit(rootId, "EDIT", username, "Uploaded new revision v" + nextVersion + " (" + formatFileSize(file.getSize()) + "). Original v1 intact.");
        logAudit(savedRevision.getId(), "VERSION_CREATE", username, "Created version v" + nextVersion);

        return convertToDTO(savedRevision, auth);
    }

    /**
     * Share file with usernames and/or email addresses
     */
    @Transactional
    public void shareFile(Long fileId, FileShareRequest request, Authentication auth) {
        CompanyFile file = getAuthorizedFile(fileId, auth);
        String username = auth != null ? auth.getName() : "Anonymous";

        // Save username shares
        if (request.getUsernames() != null) {
            for (String user : request.getUsernames()) {
                if (StringUtils.hasText(user) && !shareRepository.existsByFileIdAndSharedWithUsername(file.getId(), user.trim())) {
                    FileShare share = FileShare.builder()
                            .fileId(file.getId())
                            .sharedWithUsername(user.trim())
                            .sharedBy(username)
                            .permission(StringUtils.hasText(request.getPermission()) ? request.getPermission() : "VIEW")
                            .build();
                    shareRepository.save(share);
                    logAudit(file.getId(), "SHARE", username, "Shared with employee: " + user.trim());
                }
            }
        }

        // Save and dispatch email shares
        if (request.getEmails() != null) {
            for (String email : request.getEmails()) {
                if (StringUtils.hasText(email) && !shareRepository.existsByFileIdAndSharedWithEmail(file.getId(), email.trim())) {
                    FileShare share = FileShare.builder()
                            .fileId(file.getId())
                            .sharedWithEmail(email.trim())
                            .sharedBy(username)
                            .permission(StringUtils.hasText(request.getPermission()) ? request.getPermission() : "VIEW")
                            .build();
                    shareRepository.save(share);
                    logAudit(file.getId(), "SHARE", username, "Shared via email with: " + email.trim());

                    // Send email notification
                    sendShareEmail(email.trim(), file.getFileName(), username, request.getMessage());
                }
            }
        }
    }

    private void sendShareEmail(String toEmail, String fileName, String sharedBy, String message) {
        try {
            if (emailService.isPresent()) {
                String subject = "Document Shared With You: " + fileName;
                String content = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                        + "<h2 style='color: #4F46E5;'>EAC Document Management</h2>"
                        + "<p><strong>" + sharedBy + "</strong> has shared a document with you:</p>"
                        + "<div style='background: #F3F4F6; padding: 15px; border-radius: 8px; margin: 15px 0;'>"
                        + "<p style='margin: 0; font-size: 16px;'>📄 <strong>" + fileName + "</strong></p>"
                        + (StringUtils.hasText(message) ? "<p style='margin-top: 8px; color: #555;'><em>\"" + message + "\"</em></p>" : "")
                        + "</div>"
                        + "<p>You can access and review this file through the EAC Attendance & File Management Suite.</p>"
                        + "<p style='color: #888; font-size: 12px; margin-top: 30px;'>EAC Automated System Notification</p>"
                        + "</div>";
                emailService.get().sendCustomEmail(toEmail, subject, content);
            }
        } catch (Exception e) {
            log.warn("Could not dispatch file share email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Get version history (Original v1 and all edited revisions)
     */
    public List<CompanyFileDTO> getVersionHistory(Long fileId, Authentication auth) {
        CompanyFile file = getAuthorizedFile(fileId, auth);
        Long rootId = file.getOriginalFileId() != null ? file.getOriginalFileId() : file.getId();
        List<CompanyFile> history = fileRepository.findVersionHistory(rootId);
        return history.stream()
                .map(f -> convertToDTO(f, auth))
                .collect(Collectors.toList());
    }

    /**
     * Get audit trail for a file
     */
    public List<FileAuditTrail> getAuditTrail(Long fileId, Authentication auth) {
        CompanyFile file = getAuthorizedFile(fileId, auth);
        Long rootId = file.getOriginalFileId() != null ? file.getOriginalFileId() : file.getId();
        return auditRepository.findByFileIdOrderByPerformedAtDesc(rootId);
    }

    private void logAudit(Long fileId, String action, String performedBy, String details) {
        try {
            FileAuditTrail audit = FileAuditTrail.builder()
                    .fileId(fileId)
                    .action(action)
                    .performedBy(performedBy)
                    .performedByFullName(performedBy)
                    .details(details)
                    .build();
            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to write audit trail for file {}", fileId, e);
        }
    }

    private CompanyFileDTO convertToDTO(CompanyFile file, Authentication auth) {
        List<FileShare> shares = shareRepository.findByFileId(file.getId());
        List<String> sharedList = shares.stream()
                .map(s -> s.getSharedWithUsername() != null ? s.getSharedWithUsername() : s.getSharedWithEmail())
                .filter(Objects::nonNull)
                .toList();

        return CompanyFileDTO.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .fileType(file.getFileType())
                .extension(file.getExtension())
                .fileSize(file.getFileSize())
                .formattedSize(formatFileSize(file.getFileSize()))
                .uploadedBy(file.getUploadedBy())
                .uploadedByFullName(file.getUploadedByFullName())
                .version(file.getVersion())
                .isEdited(file.getIsEdited())
                .originalFileId(file.getOriginalFileId())
                .description(file.getDescription())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .lastModifiedBy(file.getLastModifiedBy())
                .isDeleted(file.getIsDeleted() != null ? file.getIsDeleted() : false)
                .deletedBy(file.getDeletedBy())
                .deletedAt(file.getDeletedAt())
                .sharedWithList(sharedList)
                .build();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Soft Delete file: moves file and all revisions to Recycle Bin.
     * Records who deleted it and timestamp.
     * Allowed for Admins or the file author.
     */
    @Transactional
    public void deleteFile(Long fileId, Authentication auth) {
        CompanyFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        String username = auth != null ? auth.getName() : "Anonymous";
        boolean isAdmin = isAdmin(auth);
        boolean isOwner = file.getUploadedBy() != null && file.getUploadedBy().equalsIgnoreCase(username);

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("Access denied: You do not have permission to delete this file.");
        }

        LocalDateTime now = LocalDateTime.now();
        Long rootId = file.getOriginalFileId() != null ? file.getOriginalFileId() : file.getId();
        CompanyFile root = fileRepository.findById(rootId).orElse(file);

        root.setIsDeleted(true);
        root.setDeletedBy(username);
        root.setDeletedAt(now);
        fileRepository.save(root);

        List<CompanyFile> revisions = fileRepository.findByOriginalFileId(rootId);
        for (CompanyFile rev : revisions) {
            rev.setIsDeleted(true);
            rev.setDeletedBy(username);
            rev.setDeletedAt(now);
            fileRepository.save(rev);
        }

        logAudit(rootId, "DELETE", username, "Moved file to Recycle Bin");
    }

    /**
     * Get all deleted files in the Recycle Bin (Admin only).
     */
    public List<CompanyFileDTO> getDeletedFiles(Authentication auth) {
        if (!isAdmin(auth)) {
            throw new RuntimeException("Access denied: Only administrators can access the Recycle Bin.");
        }
        List<CompanyFile> deleted = fileRepository.findDeletedRootFiles();
        return deleted.stream().map(f -> convertToDTO(f, auth)).collect(Collectors.toList());
    }

    /**
     * Restore a deleted file from the Recycle Bin (Admin only).
     */
    @Transactional
    public CompanyFileDTO restoreFile(Long fileId, Authentication auth) {
        if (!isAdmin(auth)) {
            throw new RuntimeException("Access denied: Only administrators can restore files from the Recycle Bin.");
        }
        CompanyFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        Long rootId = file.getOriginalFileId() != null ? file.getOriginalFileId() : file.getId();
        CompanyFile root = fileRepository.findById(rootId).orElse(file);

        root.setIsDeleted(false);
        root.setDeletedBy(null);
        root.setDeletedAt(null);
        CompanyFile savedRoot = fileRepository.save(root);

        List<CompanyFile> revisions = fileRepository.findByOriginalFileId(rootId);
        for (CompanyFile rev : revisions) {
            rev.setIsDeleted(false);
            rev.setDeletedBy(null);
            rev.setDeletedAt(null);
            fileRepository.save(rev);
        }

        String username = auth != null ? auth.getName() : "Administrator";
        logAudit(rootId, "RESTORE", username, "Restored file from Recycle Bin");

        return convertToDTO(savedRoot, auth);
    }

    /**
     * Permanently delete a file from database and disk (Admin only).
     */
    @Transactional
    public void purgeFile(Long fileId, Authentication auth) {
        if (!isAdmin(auth)) {
            throw new RuntimeException("Access denied: Only administrators can permanently delete files.");
        }
        CompanyFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        Long rootId = file.getOriginalFileId() != null ? file.getOriginalFileId() : file.getId();
        CompanyFile root = fileRepository.findById(rootId).orElse(file);

        // Delete child revisions first
        List<CompanyFile> revisions = fileRepository.findByOriginalFileId(rootId);
        for (CompanyFile rev : revisions) {
            shareRepository.deleteByFileId(rev.getId());
            auditRepository.deleteByFileId(rev.getId());
            try {
                Files.deleteIfExists(Paths.get(rev.getFilePath()));
            } catch (Exception ignored) {}
            fileRepository.delete(rev);
        }

        // Delete root file
        shareRepository.deleteByFileId(root.getId());
        auditRepository.deleteByFileId(root.getId());
        try {
            Files.deleteIfExists(Paths.get(root.getFilePath()));
        } catch (Exception ignored) {}
        fileRepository.delete(root);
    }

    /**
     * Empty entire Recycle Bin (Admin only).
     */
    @Transactional
    public void emptyRecycleBin(Authentication auth) {
        if (!isAdmin(auth)) {
            throw new RuntimeException("Access denied: Only administrators can empty the Recycle Bin.");
        }
        List<CompanyFile> deletedFiles = fileRepository.findDeletedRootFiles();
        for (CompanyFile f : deletedFiles) {
            purgeFile(f.getId(), auth);
        }
    }
}
