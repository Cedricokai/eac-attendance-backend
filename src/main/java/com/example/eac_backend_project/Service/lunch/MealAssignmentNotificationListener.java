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
public class MealAssignmentNotificationListener {
    private final EmailService emailService;
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void assigned(MealAssignmentNotification event) {
        StringBuilder body = new StringBuilder("<h2>Your lunch assignments</h2><p>Hello ")
                .append(escape(event.employeeName())).append(", your saved meals have been updated.</p>")
                .append("<table border=\"1\" cellpadding=\"8\"><tr><th>Date</th><th>Day</th><th>Meal</th></tr>");
        for (var change : event.changes()) body.append("<tr><td>").append(change.date()).append("</td><td>")
                .append(change.date().getDayOfWeek()).append("</td><td>").append(escape(change.newMeal())).append("</td></tr>");
        body.append("</table><p>Open your lunch portal to view your saved weekly meals.</p>");
        emailService.sendCustomEmail(event.recipient(), "Your lunch assignments have been updated", body.toString());
    }
    private String escape(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }
}
