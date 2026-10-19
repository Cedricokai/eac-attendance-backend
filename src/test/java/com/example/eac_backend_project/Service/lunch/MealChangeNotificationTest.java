package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.Service.email.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.*;
import org.springframework.transaction.TransactionDefinition;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MealChangeNotificationTest {
    @Configuration @EnableTransactionManagement static class Config {}

    @Test void sendsAfterCommitOnlyAndEscapesEmailContent() {
        var email = mock(EmailService.class);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(Config.class);
            context.registerBean(EmailService.class, () -> email);
            context.registerBean(MealChangeNotificationListener.class);
            context.refresh();
            var transaction = new TransactionTemplate(new AbstractPlatformTransactionManager() {
                protected Object doGetTransaction() { return new Object(); }
                protected void doBegin(Object tx, TransactionDefinition definition) {}
                protected void doCommit(DefaultTransactionStatus status) {}
                protected void doRollback(DefaultTransactionStatus status) {}
            });
            var event = new MealChangeNotification("kitchen@example.com", "Ama <Test>", "E123",
                    List.of(new MealChangeNotification.Change(LocalDate.of(2026, 9, 16), "Rice & stew", "Banku")));
            transaction.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(email);
                status.setRollbackOnly();
            });
            verifyNoInteractions(email);
            transaction.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(email);
            });
            verify(email).sendCustomEmail(eq("kitchen@example.com"), eq("Employee meal selection changed"),
                    argThat(body -> body.contains("Ama &lt;Test&gt;") && body.contains("Rice &amp; stew")
                            && body.contains("<th>Day</th>") && body.contains("Wednesday") && !body.contains("2026-09-16") && body.contains("Banku")));
        }
    }
}
