package com.example.eac_backend_project.Service;
import com.example.eac_backend_project.entity.settings.NightDutySmsSettings;
import com.example.eac_backend_project.repository.settings.NightDutySmsSettingsRepository;
import com.example.eac_backend_project.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.*;
import java.util.*;
@Service
public class NightDutySmsService {
 private final NightDutySmsSettingsRepository settings;
 private final AttendanceRepository attendance;
 private final ArkeselSmsGateway gateway;
 public NightDutySmsService(NightDutySmsSettingsRepository settings, AttendanceRepository attendance, ArkeselSmsGateway gateway) { this.settings=settings; this.attendance=attendance; this.gateway=gateway; }
 private LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Africa/Accra")); }
 public NightDutySmsSettings config() { return settings.findById(1L).orElseGet(NightDutySmsSettings::new); }
 public boolean configured() { return gateway.configured(); }
 public synchronized NightDutySmsSettings save(String recipient, boolean enabled, String time, String sender) {
  recipient=recipient==null?"":recipient.replaceAll("[\\s()-]","");
  if (recipient.matches("0\\d{9}")) recipient="+233"+recipient.substring(1);
  if (!recipient.isEmpty() && !recipient.matches("\\+[1-9]\\d{7,14}")) throw new IllegalArgumentException("Enter a valid international phone number, e.g. +233244000000");
  if (enabled && recipient.isEmpty()) throw new IllegalArgumentException("A recipient number is required for nightly sending");
  if (sender==null || !sender.matches("[A-Za-z0-9]{1,11}")) throw new IllegalArgumentException("Sender ID must contain 1–11 letters or digits");
  var value=config(); value.setRecipient(recipient); value.setNightlyEnabled(enabled); value.setSendTime(LocalTime.parse(time)); value.setSender(sender);
  return settings.save(value);
 }
 public String preview() {
  var time=now(); var today=time.toLocalDate();
  var people=new LinkedHashMap<Long,String>(); int stale=0;
  for (var row:attendance.findByDateBetweenOrderByDateDesc(today.minusDays(1),today)) {
   var employee=row.getEmployee();
   if (employee==null || Boolean.FALSE.equals(employee.getActive()) || row.getCheckIn()==null || row.getCheckOut()!=null ||
      Set.of("Absent","On Leave","Sick","Holiday").contains(Objects.toString(row.getStatus(),""))) continue;
   boolean night="Night".equalsIgnoreCase(row.getShift()) || (employee.getCategory()!=null && Boolean.TRUE.equals(employee.getCategory().getOvernightShift()));
   var arrival=row.getDate().atTime(row.getCheckIn());
   if (!night || arrival.isAfter(time)) continue;
   if (Duration.between(arrival,time).toHours()>=18) { stale++; continue; }
   people.putIfAbsent(employee.getId(), employee.getFirstName()+" "+Objects.toString(employee.getLastName(),"")+": "+(employee.getPhone()==null || employee.getPhone().isBlank()?"phone missing":employee.getPhone()));
  }
  return "EAC night duty " + time.toString().substring(0,16).replace('T',' ') + " Ghana time.\n" +
   (people.isEmpty()?"No current open night attendance.":String.join("\n",people.values())) +
   "\nBased on check-ins; verify missed checkouts." + (stale>0?" "+stale+" old open record(s) excluded.":"");
 }
 public synchronized String send() {
  var value=config();
  if (value.getRecipient()==null || value.getRecipient().isBlank()) throw new IllegalStateException("Configure the duty-list recipient in Settings > SMS");
  if (!gateway.configured()) throw new IllegalStateException("Configure SMS_ARKESEL_API_KEY on the backend server first");
  if (value.getLastAttemptAt()!=null && Duration.between(value.getLastAttemptAt(),now()).getSeconds()<60) throw new IllegalStateException("Wait one minute before sending another duty list");
  var message=preview(); value.setLastAttemptAt(now());
  try { gateway.send(value.getRecipient(),value.getSender(),message); value.setLastResult("Accepted by provider; delivery is not yet confirmed"); settings.save(value); return value.getLastResult(); }
  catch(Exception error) { value.setLastResult(sendingFailure(error)); settings.save(value); throw new IllegalStateException(value.getLastResult()); }
 }
 private String sendingFailure(Exception error) {
  if (error instanceof IllegalStateException) return error.getMessage();
  if (error instanceof java.net.http.HttpTimeoutException) return "Arkesel request timed out; delivery is unknown. Check Arkesel history before retrying.";
  if (error instanceof javax.net.ssl.SSLException) return "Secure connection to Arkesel failed. Check the backend Java trust store and network; certificate verification remains enabled.";
  if (error instanceof java.net.ConnectException || error instanceof java.net.UnknownHostException) return "The backend could not connect to Arkesel. Check internet access, DNS, firewall and proxy settings.";
  if (error instanceof com.fasterxml.jackson.core.JsonProcessingException) return "Arkesel returned an unreadable response. Check delivery history before retrying.";
  if (error instanceof IllegalArgumentException) return "SMS request configuration is invalid. Check the server API key for surrounding spaces or line breaks.";
  return "Sending failed or delivery status unknown (" + error.getClass().getSimpleName() + "). Check Arkesel before retrying.";
 }
 @Scheduled(cron="0 * * * * *",zone="Africa/Accra")
 public synchronized void scheduled() {
  var value=config(); var time=now();
  if (!Boolean.TRUE.equals(value.getNightlyEnabled()) || value.getSendTime()==null || !gateway.configured() || value.getRecipient()==null || value.getRecipient().isBlank() ||
      time.toLocalTime().isBefore(value.getSendTime()) || Duration.between(value.getSendTime(),time.toLocalTime()).toMinutes()>5 || time.toLocalDate().equals(value.getLastScheduledAttempt())) return;
  value.setLastScheduledAttempt(time.toLocalDate()); settings.save(value);
  try { send(); } catch(IllegalStateException ignored) { /* Saved in lastResult; avoid duplicate automatic retries. */ }
 }
}



