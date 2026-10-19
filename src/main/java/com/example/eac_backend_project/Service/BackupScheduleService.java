package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.controller.settings.DatabaseBackupController;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@Service
public class BackupScheduleService {
    private static final Logger log = LoggerFactory.getLogger(BackupScheduleService.class);
    public record Settings(boolean enabled, String time, String timezone, Set<Integer> days, int retentionDays, String folder) {}
    public record State(Settings settings, String nextRun, String lastRun, String lastSuccess, String lastStatus, String lastFile) {}
    public record Status(State state, boolean running, String root) {}
    private final DatabaseBackupController exporter;
    private final ObjectMapper mapper;
    private final Path root;
    private final Path stateFile;
    private State state;
    private boolean running;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "database-backups"); t.setDaemon(true); return t;
    });

    public BackupScheduleService(DatabaseBackupController exporter, ObjectMapper mapper,
            @Value("${app.backup.root:./backups}") String root,
            @Value("${app.backup.state-directory:./backup-settings}") String stateDirectory) {
        this.exporter = exporter; this.mapper = mapper;
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.stateFile = Path.of(stateDirectory).toAbsolutePath().resolve("schedule.json");
    }

    @PostConstruct public synchronized void init() throws IOException {
        Files.createDirectories(stateFile.getParent());
        if (Files.exists(stateFile)) {
            state = mapper.readValue(stateFile.toFile(), State.class);
            validate(state.settings());
        } else {
            Settings defaults = new Settings(true, "23:00", "Africa/Accra", Set.of(1,2,3,4,5,6,7), 30, "");
            state = new State(defaults, next(defaults, Instant.now()), null, null, "No server backup yet", null);
            persist(state);
        }
    }
    public synchronized Status status() { return new Status(state, running, root.toString()); }

    public synchronized Status save(Settings input) throws IOException {
        validate(input);
        if (running) throw new ResponseStatusException(HttpStatus.CONFLICT, "Wait for the current backup to finish before changing settings");
        Settings settings = new Settings(input.enabled(), input.time(), input.timezone(), Set.copyOf(input.days()), input.retentionDays(), input.folder());
        destination(settings);
        State changed = new State(settings, next(settings, Instant.now()), state.lastRun(), state.lastSuccess(), state.lastStatus(), state.lastFile());
        persist(changed); state = changed;
        return status();
    }

    public static void validate(Settings s) {
        try {
            if (s == null || s.time() == null || !s.time().matches("\\d{2}:\\d{2}")) throw new IllegalArgumentException();
            LocalTime.parse(s.time()); ZoneId.of(s.timezone());
            if (s.days() == null || s.days().isEmpty() || s.days().stream().anyMatch(d -> d == null || d < 1 || d > 7)
                    || s.retentionDays() < 1 || s.retentionDays() > 3650 || s.folder() == null
                    || !s.folder().matches("(?:[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*)?")) throw new IllegalArgumentException();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a valid time, timezone, weekdays, retention of 1–3650 days, and a relative folder using letters, numbers, hyphens or underscores");
        }
    }

    public static String next(Settings s, Instant after) {
        if (!s.enabled()) return null;
        ZoneId zone = ZoneId.of(s.timezone());
        LocalDate today = after.atZone(zone).toLocalDate();
        for (int i = 0; i <= 7; i++) {
            LocalDate day = today.plusDays(i);
            Instant candidate = day.atTime(LocalTime.parse(s.time())).atZone(zone).toInstant();
            if (s.days().contains(day.getDayOfWeek().getValue()) && candidate.isAfter(after)) return candidate.toString();
        }
        throw new IllegalArgumentException("No scheduled weekday");
    }

    private Path destination(Settings s) throws IOException {
        Files.createDirectories(root);
        Path realRoot = root.toRealPath();
        Path target = realRoot;
        if (!s.folder().isEmpty()) for (String part : s.folder().split("/")) {
            target = target.resolve(part);
            if (Files.isSymbolicLink(target)) throw new IOException("Backup subfolders cannot be symbolic links");
            if (!Files.exists(target)) Files.createDirectory(target);
            if (!target.toRealPath().startsWith(realRoot)) throw new IOException("Backup folder is outside the configured root");
        }
        if (!Files.isWritable(target)) throw new IOException("Backup folder is not writable");
        return target;
    }

    @Scheduled(fixedDelay = 30000) public synchronized void tick() {
        if (state == null || running || state.nextRun() == null || Instant.parse(state.nextRun()).isAfter(Instant.now())) return;
        try { start(); } catch (IOException e) { log.error("Could not persist backup schedule; backup was not started", e); }
    }
    public synchronized Status start() throws IOException {
        if (running) throw new ResponseStatusException(HttpStatus.CONFLICT, "A server backup is already running");
        Instant now = Instant.now();
        State started = new State(state.settings(), next(state.settings(), now), now.toString(), state.lastSuccess(), "Running", state.lastFile());
        persist(started); state = started; running = true;
        worker.submit(() -> runBackup(started.settings()));
        return status();
    }
    private void runBackup(Settings settings) {
        String result = "Backup failed"; String filename = null; boolean success = false;
        try {
            Path directory = destination(settings);
            String name = "eac-scheduled-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".sql";
            Path completed = directory.resolve(name);
            exporter.export(dump -> {
                Path partial = Files.createTempFile(directory, ".eac-partial-", ".tmp");
                try {
                    Files.copy(dump, partial, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(partial, completed, StandardCopyOption.ATOMIC_MOVE);
                } finally { Files.deleteIfExists(partial); }
            });
            success = true; filename = completed.toString(); result = "Success";
            try { prune(directory, settings.retentionDays(), Instant.now()); }
            catch (IOException e) { result = "Backup saved; retention cleanup failed"; log.warn(result, e); }
        } catch (Exception e) {
            result = e instanceof ResponseStatusException r ? r.getReason() : "Backup failed: check server folder permissions and available disk space";
            log.error("Scheduled database backup failed", e);
        } finally {
            synchronized (this) {
                state = new State(state.settings(), state.nextRun(), state.lastRun(), success ? Instant.now().toString() : state.lastSuccess(), result, filename == null ? state.lastFile() : filename);
                try { persist(state); } catch (IOException e) { log.error("Unable to persist backup result", e); }
                running = false;
            }
        }
    }
    static void prune(Path directory, int days, Instant now) throws IOException {
        try (var files = Files.list(directory)) {
            for (Path file : files.toList()) {
                if (file.getFileName().toString().matches("eac-scheduled-\\d+-[0-9a-f-]{36}\\.sql")
                        && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                        && Files.getLastModifiedTime(file).toInstant().isBefore(now.minus(Duration.ofDays(days)))) Files.delete(file);
            }
        }
    }
    private void persist(State value) throws IOException {
        Path temp = Files.createTempFile(stateFile.getParent(), ".schedule-", ".tmp");
        try {
            mapper.writeValue(temp.toFile(), value);
            Files.move(temp, stateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temp); }
    }
    @PreDestroy public void close() { worker.shutdownNow(); }
}
