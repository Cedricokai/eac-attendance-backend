package com.example.eac_backend_project.controller.settings;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/settings/database-backups")
public class DatabaseBackupController {
    private final AtomicBoolean running = new AtomicBoolean();
    private final String datasourceUrl;
    private final String username;
    private final String password;
    private final String executable;

    public DatabaseBackupController(
            @Value("${spring.datasource.url}") String datasourceUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${app.backup.executable:}") String executable) {
        this.datasourceUrl = datasourceUrl;
        this.username = username;
        this.password = password;
        Path windowsClient = Path.of(System.getenv().getOrDefault("ProgramFiles", "C:/Program Files"),
                "MySQL", "MySQL Server 8.0", "bin", "mysqldump.exe");
        this.executable = !executable.isBlank() ? executable
                : Files.isRegularFile(windowsClient) ? windowsClient.toString() : "mysqldump";
    }

    // Return failures directly so an anonymous /error dispatch cannot mask them as 401.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> backupError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).header("Cache-Control", "no-store")
                .body(Map.of("message", error.getReason() == null ? "Database backup failed" : error.getReason()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Administrator access is required"));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> storageError() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).header("Cache-Control", "no-store")
                .body(Map.of("message", "Unable to write or transfer the database backup"));
    }

    @PostMapping
    public void download(Authentication auth, HttpServletResponse response) throws IOException {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Administrator access is required");
        }
        export(dump -> {
            response.setContentType("application/octet-stream");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Content-Disposition", "attachment; filename=\"eac-backup-"
                    + Instant.now().toString().replace(':', '-') + ".sql\"");
            response.setContentLengthLong(Files.size(dump));
            Files.copy(dump, response.getOutputStream());
        });
    }

    @FunctionalInterface
    public interface BackupWriter { void write(Path dump) throws IOException; }

    // Shared export lock covers both browser downloads and scheduled server backups.
    public void export(BackupWriter writer) throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A backup is already running");
        }
        Path dump = null;
        Process process = null;
        try {
            URI uri;
            try {
                if (!datasourceUrl.startsWith("jdbc:mysql://")) throw new IllegalArgumentException();
                uri = URI.create(datasourceUrl.substring(5));
                if (uri.getHost() == null || uri.getPath() == null || !uri.getPath().matches("/[A-Za-z0-9_]+")) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException invalid) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Backup datasource configuration is unsupported");
            }
            dump = Files.createTempFile("eac-backup-", ".sql");
            String database = uri.getPath().substring(1);
            ProcessBuilder builder = new ProcessBuilder(List.of(executable,
                    "--host=" + uri.getHost(), "--port=" + (uri.getPort() < 0 ? 3306 : uri.getPort()),
                    "--user=" + username, "--single-transaction", "--quick", "--routines", "--events",
                    "--triggers", "--hex-blob", "--no-tablespaces", "--set-gtid-purged=OFF",
                    "--databases", database));
            builder.environment().put("MYSQL_PWD", password);
            builder.redirectOutput(dump.toFile());
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            try {
                process = builder.start();
            } catch (IOException unavailable) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MySQL backup utility is unavailable");
            }
            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Database backup timed out");
            }
            if (process.exitValue() != 0 || Files.size(dump) == 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database export failed");
            }
            writer.write(dump);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Backup interrupted");
        } finally {
            try {
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                    try { process.waitFor(10, TimeUnit.SECONDS); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                }
                if (dump != null) Files.deleteIfExists(dump);
            } finally {
                running.set(false);
            }
        }
    }
}
