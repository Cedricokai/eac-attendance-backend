package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.reports.IndividualReportScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IndividualReportScheduleControllerTest {
    @Test void onlyAdminsCanReadOrChangeSchedule() throws Exception {
        var service = mock(IndividualReportScheduleService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new IndividualReportScheduleController(service)).build();
        var user = new UsernamePasswordAuthenticationToken("user", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String path = "/api/reports/attendance/individual-schedule";
        mvc.perform(get(path)).andExpect(status().isForbidden());
        mvc.perform(get(path).principal(user)).andExpect(status().isForbidden());
        mvc.perform(put(path).principal(user).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
        var admin = new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        mvc.perform(get(path).principal(admin)).andExpect(status().isOk());
        verify(service).status();
    }
}
