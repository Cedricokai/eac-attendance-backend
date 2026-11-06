package com.example.eac_backend_project.communication;
import org.springframework.stereotype.Component;import java.time.Instant;import java.util.concurrent.ConcurrentHashMap;
@Component public class CommunicationPresence {
 private final ConcurrentHashMap<Long,Instant> seen=new ConcurrentHashMap<>();private final ConcurrentHashMap<String,Instant> typing=new ConcurrentHashMap<>();
 public void touch(Long user){seen.put(user,Instant.now());}public boolean online(Long user){return seen.getOrDefault(user,Instant.EPOCH).isAfter(Instant.now().minusSeconds(45));}public Instant lastSeen(Long user){return seen.get(user);}
 public void typing(Long conversation,Long user){typing.put(conversation+":"+user,Instant.now());touch(user);}public boolean typingNow(Long conversation,Long user){return typing.getOrDefault(conversation+":"+user,Instant.EPOCH).isAfter(Instant.now().minusSeconds(5));}
}
