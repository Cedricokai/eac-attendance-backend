package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.PayrollPeriod;
import com.example.eac_backend_project.repository.PayrollPeriodRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollPeriodOverlapTest {
    private final PayrollPeriodRepository periods = mock(PayrollPeriodRepository.class);
    private final PayrollService service = new PayrollService(periods, null, null, null,
            null, null, null, null, null, null, null, null, null);

    private PayrollPeriod period(String category, int startDay) {
        PayrollPeriod period = new PayrollPeriod();
        period.setCategory(category);
        period.setStartDate(LocalDate.of(2026, 7, startDay));
        period.setEndDate(LocalDate.of(2026, 8, 26));
        return period;
    }

    @Test void rejectsOverlappingSameCategoryOnBothCreationRoutes() {
        PayrollPeriod next = period("Staff", 24);
        PayrollPeriod previous = new PayrollPeriod();
        previous.setCategory("Staff");
        previous.setStartDate(LocalDate.of(2026, 6, 26));
        previous.setEndDate(LocalDate.of(2026, 7, 25));
        when(periods.findOverlappingPeriodsForCategory(next.getStartDate(), next.getEndDate(), "Staff"))
                .thenReturn(List.of(previous));
        assertThrows(IllegalArgumentException.class, () -> service.createPeriod(next));
        assertThrows(IllegalArgumentException.class, () -> service.createPeriodWithCategory(
                "August", next.getStartDate(), next.getEndDate(), "Staff", true));
        verify(periods, never()).save(any());
    }

    @Test void acceptsConsecutivePeriodStartingJuly26() {
        PayrollPeriod next = period("Staff", 26);
        when(periods.save(next)).thenReturn(next);
        assertSame(next, service.createPeriod(next));
        verify(periods).findOverlappingPeriodsForCategory(next.getStartDate(), next.getEndDate(), "Staff");
    }

    @Test void checksOnlyRequestedCategorySoOtherCategoriesMayOverlap() {
        PayrollPeriod next = period("Field", 24);
        when(periods.save(next)).thenReturn(next);
        assertSame(next, service.createPeriod(next));
        verify(periods).findOverlappingPeriodsForCategory(next.getStartDate(), next.getEndDate(), "Field");
    }

    @Test void rejectsMissingCategoryOrDatesAndReversedDatesBeforeSaving() {
        PayrollPeriod next = period(null, 26);
        assertThrows(IllegalArgumentException.class, () -> service.createPeriod(next));
        next.setCategory("  ");
        assertThrows(IllegalArgumentException.class, () -> service.createPeriod(next));
        next.setCategory("Staff");
        next.setStartDate(null);
        assertThrows(IllegalArgumentException.class, () -> service.createPeriod(next));
        next.setStartDate(LocalDate.of(2026, 9, 1));
        assertThrows(IllegalArgumentException.class, () -> service.createPeriod(next));
        verify(periods, never()).save(any());
    }
    @Test void projectPeriodsUseProjectOverlapAndMayOverlapCategoryDates() {
        var jobs=mock(com.example.eac_backend_project.repository.JobRepository.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"projectPayrollJobs",jobs);
        var job=new com.example.eac_backend_project.entity.attendance.Job();job.setId(7L);job.setName("Project A");
        when(jobs.findById(7L)).thenReturn(java.util.Optional.of(job));
        PayrollPeriod p=period(null,26);p.setPeriodType("PROJECT");p.setProjectId(7L);
        when(periods.save(p)).thenReturn(p);assertSame(p,service.createPeriod(p));assertEquals("Project A",p.getProjectName());assertNull(p.getCategory());
        verify(periods,never()).findOverlappingPeriodsForCategory(any(),any(),any());
        when(periods.findOverlappingProjectPeriods(p.getStartDate(),p.getEndDate(),7L)).thenReturn(List.of(p));
        assertThrows(IllegalArgumentException.class,()->service.createPeriod(p));
    }
    @Test void rejectsProjectWithoutSelectionAndUnknownPeriodType() {
        PayrollPeriod p=period(null,26);p.setPeriodType("PROJECT");assertThrows(IllegalArgumentException.class,()->service.createPeriod(p));
        p.setPeriodType("INVALID");assertThrows(IllegalArgumentException.class,()->service.createPeriod(p));
    }
}