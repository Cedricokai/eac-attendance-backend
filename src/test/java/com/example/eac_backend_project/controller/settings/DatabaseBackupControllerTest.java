package com.example.eac_backend_project.controller.settings;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DatabaseBackupControllerTest {
    private MockMvc mvc(String url) {
        return MockMvcBuilders.standaloneSetup(new DatabaseBackupController(url, "test", "test",
                "nonexistent-eac-backup-test-executable")).build();
    }
    private UsernamePasswordAuthenticationToken user(String authority) {
        return new UsernamePasswordAuthenticationToken("test", "unused", List.of(new SimpleGrantedAuthority(authority)));
    }
    @Test void anonymousCannotExport() throws Exception {
        mvc("jdbc:mysql://localhost/eac").perform(post("/api/settings/database-backups"))
                .andExpect(status().isForbidden());
    }
    @Test void ordinaryUserCannotExport() throws Exception {
        mvc("jdbc:mysql://localhost/eac").perform(post("/api/settings/database-backups").principal(user("ROLE_USER")))
                .andExpect(status().isForbidden());
    }
    @Test void invalidDatasourceReturnsActualError() throws Exception {
        mvc("jdbc:postgresql://localhost/eac").perform(post("/api/settings/database-backups").principal(user("ROLE_ADMIN")))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("Backup datasource configuration is unsupported"));
    }
    @Test void missingUtilityReturns503AndReleasesLock() throws Exception {
        MockMvc mock = mvc("jdbc:mysql://localhost:3307/eac");
        for (int attempt = 0; attempt < 2; attempt++) {
            mock.perform(post("/api/settings/database-backups").principal(user("ROLE_ADMIN")))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("MySQL backup utility is unavailable"));
        }
    }
}
