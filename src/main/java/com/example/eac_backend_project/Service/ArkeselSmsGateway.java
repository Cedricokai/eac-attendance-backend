package com.example.eac_backend_project.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
@Service
public class ArkeselSmsGateway {
 private final String apiKey;
 private final ObjectMapper mapper;
 private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 public ArkeselSmsGateway(@Value("${SMS_ARKESEL_API_KEY:}") String apiKey, ObjectMapper mapper) { this.apiKey=apiKey; this.mapper=mapper; }
 public boolean configured() { return apiKey != null && !apiKey.isBlank(); }
 public void send(String recipient, String sender, String message) throws Exception {
  if (!configured()) throw new IllegalStateException("Arkesel API key is not configured on the server");
  var body=mapper.writeValueAsString(Map.of("sender",sender,"message",message,"recipients",List.of(recipient)));
  var request=HttpRequest.newBuilder(URI.create("https://sms.arkesel.com/api/v2/sms/send"))
   .timeout(Duration.ofSeconds(25)).header("api-key",apiKey).header("Content-Type","application/json")
   .POST(HttpRequest.BodyPublishers.ofString(body)).build();
  var response=client.send(request,HttpResponse.BodyHandlers.ofString());
  if (response.statusCode()==401 || response.statusCode()==403) throw new IllegalStateException("Arkesel rejected authentication (HTTP " + response.statusCode() + "). Verify the server API key and account access.");
  if (response.statusCode()==400 || response.statusCode()==422) throw new IllegalStateException("Arkesel rejected the request (HTTP " + response.statusCode() + "). Verify the approved sender ID and recipient number in Arkesel.");
  if (response.statusCode()==402) throw new IllegalStateException("Arkesel requires SMS credits (HTTP 402). Check the account balance.");
  if (response.statusCode()==429) throw new IllegalStateException("Arkesel rate limit reached (HTTP 429). Wait before retrying.");
  if (response.statusCode()<200 || response.statusCode()>=300 || !"success".equalsIgnoreCase(mapper.readTree(response.body()).path("status").asText()))
   throw new IllegalStateException("SMS provider did not accept the message. Check the Arkesel account and credits.");
 }
}


