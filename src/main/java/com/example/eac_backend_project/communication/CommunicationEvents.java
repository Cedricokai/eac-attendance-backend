package com.example.eac_backend_project.communication;
import org.springframework.stereotype.Component;import org.springframework.context.ApplicationEventPublisher;import org.springframework.transaction.support.*;import java.util.*;
@Component public class CommunicationEvents {
 public record Event(Set<Long> users,Map<String,Object> body){}
 private final ApplicationEventPublisher publisher;
 public CommunicationEvents(ApplicationEventPublisher publisher){this.publisher=publisher;}
 public void send(Set<Long> users,Map<String,Object> body){Map<String,Object> payload=new HashMap<>(body);payload.put("eventId",UUID.randomUUID().toString());Event event=new Event(Set.copyOf(users),Map.copyOf(payload));if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){publisher.publishEvent(event);}});else publisher.publishEvent(event);}
}
