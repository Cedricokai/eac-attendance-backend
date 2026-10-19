package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.settings.Holiday;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.settings.HolidayRepository;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;

class HolidayQueryValidationTest {
    @Test void holidayQueriesResolveAgainstActualEntityMappingsWithoutDatabaseAccess() throws Exception {
        var registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
            .applySetting("hibernate.temp.use_jdbc_metadata_defaults", false)
            .applySetting("hibernate.hbm2ddl.auto", "none")
            .build();
        try {
            var sources = new MetadataSources(registry);
            var types = new PersistenceManagedTypesScanner(new DefaultResourceLoader())
                .scan("com.example.eac_backend_project.entity");
            for (String name : types.getManagedClassNames()) sources.addAnnotatedClass(Class.forName(name));
            try (var factory = sources.buildMetadata().buildSessionFactory(); var session = factory.openSession()) {
                String eligible = EmployeeRepository.class.getMethod("findEligibleForHoliday", LocalDate.class)
                    .getAnnotation(Query.class).value();
                session.createQuery(eligible, Employee.class).setParameter("date", LocalDate.of(2026, 9, 21));
                String holiday = HolidayRepository.class.getMethod("lockHolidaysOn", LocalDate.class)
                    .getAnnotation(Query.class).value();
                session.createQuery(holiday, Holiday.class).setParameter("date", LocalDate.of(2026, 9, 21));
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
