package com.example.eac_backend_project.controller.settings;
import com.example.eac_backend_project.Service.BackupScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
class BackupScheduleControllerTest {
    @Test void deniesNonAdminOnEveryOperation() throws Exception {
        var service = mock(BackupScheduleService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new BackupScheduleController(service)).build();
        var user = new UsernamePasswordAuthenticationToken("user", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        mvc.perform(get("/api/settings/database-backups/schedule").principal(user)).andExpect(status().isForbidden());
        mvc.perform(post("/api/settings/database-backups/server").principal(user)).andExpect(status().isForbidden());
        mvc.perform(put("/api/settings/database-backups/schedule").principal(user).contentType("application/json").content("{\"enabled\":false,\"time\":\"23:00\",\"timezone\":\"Africa/Accra\",\"days\":[1],\"retentionDays\":30,\"folder\":\"\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
