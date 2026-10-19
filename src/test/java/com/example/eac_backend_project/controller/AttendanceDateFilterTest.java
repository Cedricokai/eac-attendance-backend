package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.AttendanceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AttendanceDateFilterTest {
    @Test void dateRangeDoesNotLoadEntireAttendanceHistory() throws Exception {
        var service = mock(AttendanceService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AttendanceController(service, null, null, null, null)).build();
        mvc.perform(get("/api/attendance").param("startDate", "2026-09-01").param("endDate", "2026-09-07"))
            .andExpect(status().isOk());
        verify(service).getRecentAttendances(LocalDate.of(2026,9,1), LocalDate.of(2026,9,7));
        verify(service, never()).getAllAttendances();
    }
    @Test void missingOrReversedDateRangeIsRejected() throws Exception {
        var service = mock(AttendanceService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AttendanceController(service, null, null, null, null)).build();
        mvc.perform(get("/api/attendance").param("startDate", "2026-09-01")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/attendance").param("startDate", "2026-09-07").param("endDate", "2026-09-01"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void callersWithoutDateFiltersKeepLegacyBehavior() throws Exception {
        var service = mock(AttendanceService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AttendanceController(service, null, null, null, null)).build();
        mvc.perform(get("/api/attendance")).andExpect(status().isOk());
        verify(service).getAllAttendances();
    }
}
