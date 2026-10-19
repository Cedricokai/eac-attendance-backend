package com.example.eac_backend_project.entity.settings;
import jakarta.persistence.*;
import lombok.Data;
import java.time.*;
@Entity @Data @Table(name="night_duty_sms_settings")
public class NightDutySmsSettings {
 @Id private Long id = 1L;
 private String recipient = "";
 private Boolean nightlyEnabled = false;
 private LocalTime sendTime = LocalTime.of(22,0);
 private String sender = "EAC";
 private LocalDate lastScheduledAttempt;
 private LocalDateTime lastAttemptAt;
 private String lastResult;
}
