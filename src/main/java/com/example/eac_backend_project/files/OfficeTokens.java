package com.example.eac_backend_project.files;

import com.fasterxml.jackson.databind.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.springframework.http.HttpStatus.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OfficeTokens {
 private final OfficeSettings settings;private final ObjectMapper mapper;
 public OfficeTokens(OfficeSettings settings,ObjectMapper mapper){this.settings=settings;this.mapper=mapper;}
 private javax.crypto.SecretKey key(){if(settings.getJwtSecret().getBytes(StandardCharsets.UTF_8).length<32)throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Configure a separate ONLYOFFICE JWT secret of at least 32 bytes");return Keys.hmacShaKeyFor(settings.getJwtSecret().getBytes(StandardCharsets.UTF_8));}
 public String sign(Map<String,Object> data){return Jwts.builder().setClaims(new LinkedHashMap<>(data)).setIssuedAt(new Date()).setExpiration(Date.from(Instant.now().plusSeconds(Math.max(5,Math.min(60,settings.getTokenMinutes()))*60L))).signWith(key(),SignatureAlgorithm.HS256).compact();}
 public Claims verify(String token){try{if(token==null||token.length()>100000)throw new IllegalArgumentException();var jwt=Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);if(!"HS256".equals(jwt.getHeader().getAlgorithm()))throw new IllegalArgumentException();return jwt.getBody();}catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(UNAUTHORIZED,"Invalid ONLYOFFICE authentication");}}
 public JsonNode callback(String authorization,JsonNode body){String token=authorization!=null&&authorization.startsWith("Bearer ")?authorization.substring(7):body.path("token").asText(null);Claims claims=verify(token);JsonNode signed=mapper.valueToTree(claims.containsKey("payload")?claims.get("payload"):claims);if(!signed.isObject()||!signed.has("key")||!signed.has("status"))throw new ResponseStatusException(UNAUTHORIZED,"Unsigned ONLYOFFICE callback fields");return signed;}
 public void sourceHeader(String authorization,String expectedUrl){if(authorization==null||!authorization.startsWith("Bearer "))throw new ResponseStatusException(UNAUTHORIZED,"Document Server authentication required");Claims claims=verify(authorization.substring(7));JsonNode signed=mapper.valueToTree(claims.containsKey("payload")?claims.get("payload"):claims);if(!expectedUrl.equals(signed.path("url").asText()))throw new ResponseStatusException(UNAUTHORIZED,"Document Server retrieval signature mismatch");}
}
