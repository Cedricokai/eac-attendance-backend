package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.controller.settings.DatabaseBackupController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BackupScheduleServiceTest {
    @TempDir Path temp;
    private BackupScheduleService.Settings settings(boolean enabled) { return new BackupScheduleService.Settings(enabled, "23:00", "Africa/Accra", Set.of(1,2,3,4,5,6,7), 30, ""); }
    @Test void nextDailyAndDisabled() {
        assertEquals("2026-09-07T23:00:00Z", BackupScheduleService.next(settings(true), Instant.parse("2026-09-07T10:00:00Z")));
        assertEquals("2026-09-08T23:00:00Z", BackupScheduleService.next(settings(true), Instant.parse("2026-09-07T23:00:00Z")));
        assertNull(BackupScheduleService.next(settings(false), Instant.now()));
    }
    @Test void selectedWeekdaysAndTimezone() {
        var s = new BackupScheduleService.Settings(true, "09:00", "America/New_York", Set.of(1), 30, "");
        assertEquals("2026-09-14T13:00:00Z", BackupScheduleService.next(s, Instant.parse("2026-09-07T14:00:00Z")));
    }
    @Test void rejectsTraversalAndEmptyDays() {
        assertThrows(RuntimeException.class, () -> BackupScheduleService.validate(new BackupScheduleService.Settings(true,"23:00","Africa/Accra",Set.of(1),30,"../outside")));
        assertThrows(RuntimeException.class, () -> BackupScheduleService.validate(new BackupScheduleService.Settings(true,"23:00","Africa/Accra",Set.of(),30,"")));
    }
    @Test void settingsSurviveRestart() throws Exception {
        var first = new BackupScheduleService(mock(DatabaseBackupController.class), new ObjectMapper(), temp.resolve("backups").toString(), temp.resolve("state").toString());
        first.init(); first.save(settings(false)); first.close();
        var second = new BackupScheduleService(mock(DatabaseBackupController.class), new ObjectMapper(), temp.resolve("backups").toString(), temp.resolve("state").toString());
        second.init(); assertFalse(second.status().state().settings().enabled()); assertNull(second.status().state().nextRun()); second.close();
    }
    @Test void retentionPreservesOtherFilesAndRecentBackups() throws Exception {
        Path old = Files.writeString(temp.resolve("eac-scheduled-1-12345678-1234-1234-1234-123456789012.sql"), "old");
        Path recent = Files.writeString(temp.resolve("eac-scheduled-2-12345678-1234-1234-1234-123456789012.sql"), "new");
        Path unrelated = Files.writeString(temp.resolve("manual.sql"), "keep");
        var ancient = FileTime.from(Instant.now().minus(Duration.ofDays(31)));
        Files.setLastModifiedTime(old, ancient); Files.setLastModifiedTime(unrelated, ancient);
        BackupScheduleService.prune(temp,30,Instant.now());
        assertFalse(Files.exists(old)); assertTrue(Files.exists(recent)); assertTrue(Files.exists(unrelated));
    }
}
