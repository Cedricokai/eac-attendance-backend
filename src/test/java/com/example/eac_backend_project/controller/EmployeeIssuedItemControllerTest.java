package com.example.eac_backend_project.controller;
import com.example.eac_backend_project.Service.EmployeeIssuedItemService;
import com.example.eac_backend_project.Service.PageService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeIssuedItemControllerTest {
    @Test void blocksUnassignedUsersAndReservesPolicyChangesForAdmins() throws Exception {
        var service = mock(EmployeeIssuedItemService.class); var pages = mock(PageService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new EmployeeIssuedItemController(service, pages)).build();
        var user = new UsernamePasswordAuthenticationToken("user", "", List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        String path = "/api/employee-issued-items";
        mvc.perform(get(path)).andExpect(status().isForbidden());
        mvc.perform(get(path).principal(user)).andExpect(status().isForbidden());
        mvc.perform(post(path).principal(user).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post(path + "/policies").principal(user).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(put(path + "/policies/1").principal(user).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
        var admin = new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        mvc.perform(get(path).principal(admin)).andExpect(status().isOk()); verify(service).list();
    }
}
