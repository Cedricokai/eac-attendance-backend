package com.example.eac_backend_project.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    /**
     * Send an SMS message to a given phone number.
     * Currently logs the message and returns success.
     * Replace this with actual SMS gateway integration (e.g. Twilio, Africa's Talking).
     *
     * @param phoneNumber recipient's phone number
     * @param message     SMS text
     * @return true if sent (simulated)
     */
    public boolean sendSms(String phoneNumber, String message) {
        // TODO: Integrate with your SMS provider (Twilio, Africa's Talking, etc.)
        logger.info("📱 Sending SMS to {}: {}", phoneNumber, message);
        // Simulate sending (replace with actual API call)
        return true;
    }
}