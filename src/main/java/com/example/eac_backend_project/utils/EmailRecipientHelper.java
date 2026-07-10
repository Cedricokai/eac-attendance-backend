package com.example.eac_backend_project.utils;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EmailRecipientHelper {

    /**
     * Parse comma-separated email string into a list of valid email addresses
     */
    public List<String> parseEmailRecipients(String emailString) {
        if (emailString == null || emailString.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(emailString.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .filter(this::isValidEmail)
                .collect(Collectors.toList());
    }

    /**
     * Validate email format (basic validation)
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Convert list of emails back to comma-separated string for storage
     */
    public String formatEmailList(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return "";
        }
        return String.join(", ", emails);
    }
}