package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.entity.lunch.LunchSettings;
import com.example.eac_backend_project.repository.lunch.LunchSettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class LunchSettingsServiceTest {
    @Test void updatesExistingKeysAndReturnsPersistedPolicy() {
        var repository = mock(LunchSettingsRepository.class);
        var store = new HashMap<String, LunchSettings>();
        when(repository.findAll()).thenAnswer(call -> new ArrayList<>(store.values()));
        when(repository.findBySettingKey(anyString())).thenAnswer(call -> Optional.ofNullable(store.get(call.getArgument(0))));
        when(repository.save(any())).thenAnswer(call -> { LunchSettings s = call.getArgument(0); store.put(s.getSettingKey(), s); return s; });
        var service = new LunchSettingsService(repository);
        assertEquals("17:00", service.getMealChangePolicy().cutoffTime());
        service.updateSettings(Map.of("mealChangeCutoffTime", "16:30", "mealChangeDaysBefore", "1"));
        service.updateSettings(Map.of("mealChangeCutoffTime", "15:00"));
        assertEquals(2, store.size());
        assertEquals("15:00", service.getMealChangePolicy().cutoffTime());
        assertEquals(1, service.getMealChangePolicy().daysBefore());
        assertEquals("Africa/Accra", service.getMealChangePolicy().timezone());
    }

    @Test void validatesNotificationRecipient() {
        var repository = mock(LunchSettingsRepository.class);
        var service = new LunchSettingsService(repository);
        assertEquals("", service.getSettings().get("mealChangeNotificationEmail"));
        service.updateSettings(Map.of("mealChangeNotificationEmail", "kitchen@example.com"));
        service.updateSettings(Map.of("mealChangeNotificationEmail", ""));
        clearInvocations(repository);
        for (String invalid : List.of("invalid", "a@example.com,b@example.com", "a@example.com\r\nBcc: b@example.com")) {
            assertThrows(ResponseStatusException.class, () -> service.updateSettings(Map.of("mealChangeNotificationEmail", invalid)));
        }
        verify(repository, never()).save(any());
    }

    @Test void rejectsInvalidSettingsBeforeAnyWrite() {
        var repository = mock(LunchSettingsRepository.class);
        var service = new LunchSettingsService(repository);
        for (var values : List.of(Map.<String, Object>of("mealChangeCutoffTime", "25:00"),
                Map.<String, Object>of("mealChangeDaysBefore", "-1"), Map.<String, Object>of("costPerLunch", "-2"))) {
            assertThrows(ResponseStatusException.class, () -> service.updateSettings(values));
        }
        verify(repository, never()).save(any());
    }
}
