package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.SettingsDTO;
import com.example.eac_backend_project.entity.attendance.*;
import com.example.eac_backend_project.entity.settings.SystemSettings;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PayrollRateSelectionTest {
    Employee employee(double hourly, double salary) {
        Employee employee = new Employee(); employee.setId(1L); employee.setMinimumRate(hourly); employee.setBasicSalary(salary);
        return employee;
    }
    PayrollRecord calculate(Employee employee) {
        PayrollRecord record = new PayrollRecord();
        new GhanaPayrollCalculator().calculatePayrollWithMultipliers(employee,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                80, 10, 5, 0, 0, 0, 0, 0, new SettingsDTO(), true,
                record, new SystemSettings(), 0, 0, true);
        return record;
    }
    @Test void savedAndUpdatedRatesProduceDifferentPayWithoutChangingTheEmployee() {
        Employee original = employee(10, 0);
        PayrollRecord previous = new PayrollRecord();
        PayrollRateSelection.capture(previous, original, 10);
        Employee current = employee(20, 0);
        Employee saved = PayrollRateSelection.employeeForCalculation(current, previous, "SAVED");
        Employee updated = PayrollRateSelection.employeeForCalculation(current, previous, "CURRENT");
        assertEquals(800.0, calculate(saved).getBasicSalary());
        assertEquals(50.0, calculate(saved).getOvertimePay());
        assertEquals(1600.0, calculate(updated).getBasicSalary());
        assertEquals(100.0, calculate(updated).getOvertimePay());
        assertEquals(20.0, current.getMinimumRate());
        assertNotSame(current, saved);
    }
    @Test void monthlySalaryAndZeroRatesArePreservedExactly() {
        PayrollRecord previous = new PayrollRecord();
        PayrollRateSelection.capture(previous, employee(0, 1000), 0);
        Employee current = employee(25, 2000);
        Employee saved = PayrollRateSelection.employeeForCalculation(current, previous, "SAVED");
        assertEquals(0.0, saved.getMinimumRate());
        assertEquals(1000.0, calculate(saved).getBasicSalary());
        assertEquals(2000.0, calculate(PayrollRateSelection.employeeForCalculation(current, previous, "CURRENT")).getBasicSalary());
    }
    @Test void regenerationNeedsAnExplicitChoiceAndLegacyRecordsCannotInventSavedRates() {
        PayrollRecord legacy = new PayrollRecord();
        assertThrows(IllegalArgumentException.class, () -> PayrollRateSelection.validate(null, List.of(legacy)));
        assertThrows(IllegalArgumentException.class, () -> PayrollRateSelection.validate("SAVED", List.of(legacy)));
        assertDoesNotThrow(() -> PayrollRateSelection.validate("CURRENT", List.of(legacy)));
        assertDoesNotThrow(() -> PayrollRateSelection.validate(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> PayrollRateSelection.validate("INVALID", List.of()));
        legacy.setStatus("Processed");
        assertThrows(IllegalStateException.class, () -> PayrollRateSelection.validate("CURRENT", List.of(legacy)));
    }
    @Test void newEmployeesUseCurrentRatesEvenWhenSavedRatesAreChosen() {
        assertEquals(30.0, PayrollRateSelection.employeeForCalculation(employee(30, 0), null, "SAVED").getMinimumRate());
    }
}
