package com.example.eac_backend_project.controller.settings;
import com.example.eac_backend_project.Service.NightDutySmsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/api/night-duty-sms")
public class NightDutySmsController {
 private final NightDutySmsService service;
 public NightDutySmsController(NightDutySmsService service) { this.service=service; }
 private void authorize(Authentication auth) {
  if (auth==null || auth.getAuthorities().stream().noneMatch(a -> Set.of("ADMIN","ROLE_ADMIN","HR","ROLE_HR","HR_MANAGER","ROLE_HR_MANAGER").contains(a.getAuthority().toUpperCase())))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Admin or HR permission is required");
 }
 @GetMapping("/settings") public Map<String,Object> config(Authentication auth) { authorize(auth); return Map.of("settings",service.config(),"providerConfigured",service.configured()); }
 @PutMapping("/settings") public ResponseEntity<?> save(Authentication auth,@RequestBody Map<String,Object> body) {
  authorize(auth); try { service.save(Objects.toString(body.get("recipient"),""),Boolean.TRUE.equals(body.get("nightlyEnabled")),Objects.toString(body.get("sendTime"),"22:00"),Objects.toString(body.get("sender"),"EAC")); return ResponseEntity.ok(config(auth)); }
  catch(IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("error",error.getMessage())); }
 }
 @GetMapping("/preview") public Map<String,String> preview(Authentication auth) { authorize(auth); return Map.of("message",service.preview()); }
 @PostMapping("/send") public ResponseEntity<?> send(Authentication auth) {
  authorize(auth); try { return ResponseEntity.ok(Map.of("message",service.send())); }
  catch(IllegalStateException error) { return ResponseEntity.status(503).body(Map.of("error",error.getMessage())); }
 }
}
