package com.example.eac_backend_project.Service.lunch;

import com.example.eac_backend_project.Service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

@Component
@RequiredArgsConstructor
public class MealChangeNotificationListener {
    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMealChange(MealChangeNotification event) {
        StringBuilder body = new StringBuilder("<h2>Employee meal selection changed</h2><p>")
                .append(escape(event.employeeName()));
        if (event.employeeNumber() != null) body.append(" (").append(escape(event.employeeNumber())).append(")");
        body.append(" saved the following meal changes in their portal.</p>")
                .append("<table border=\"1\" cellpadding=\"8\" cellspacing=\"0\"><thead><tr>")
                .append("<th>Day</th><th>Previous meal</th><th>New meal</th></tr></thead><tbody>");
        for (var change : event.changes()) {
            body.append("<tr><td>").append(change.date().getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)).append("</td><td>")
                    .append(escape(change.previousMeal())).append("</td><td>")
                    .append(escape(change.newMeal())).append("</td></tr>");
        }
        body.append("</tbody></table>");
        emailService.sendCustomEmail(event.recipient(), "Employee meal selection changed", body.toString());
    }

    private String escape(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }
}
