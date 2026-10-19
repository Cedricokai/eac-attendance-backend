package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.repository.OvertimeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class PayrollEmployeeOvertimePayTest extends PayrollEmployeeScopeTest {
    @Test void freshPayrollRepricesStoredOvertimeUsingEmployeeRateRatherThanBillingPay() {
        when(records.findByPeriodId(7L)).thenReturn(List.of());
        OvertimeRepository overtime=(OvertimeRepository)ReflectionTestUtils.getField(service,"overtimeRepository");
        when(overtime.summarizeApprovedOvertimeBetweenDatesForCategory(any(),any(),eq("Staff")))
            .thenReturn(Collections.singletonList(new Object[]{1L,4.0,400.0,true}));
        service.generatePayrollWithSelectedCredits(request);
        ArgumentCaptor<Iterable<PayrollRecord>> saved=ArgumentCaptor.forClass(Iterable.class);
        verify(records).saveAll(saved.capture());
        PayrollRecord result=saved.getValue().iterator().next();
        assertEquals(4.0,result.getOvertimeHours());
        assertEquals(40.0,result.getOvertimePay());
        assertEquals(10.0,result.getAppliedOvertimeRate());
    }
}