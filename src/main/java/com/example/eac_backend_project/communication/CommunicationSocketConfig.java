package com.example.eac_backend_project.communication;
import org.springframework.context.annotation.Configuration;import org.springframework.beans.factory.annotation.Value;import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket public class CommunicationSocketConfig implements WebSocketConfigurer {
 private final CommunicationSocket socket;private final String origin;
 public CommunicationSocketConfig(CommunicationSocket socket,@Value("${app.frontend.base-url:http://localhost:5173}")String origin){this.socket=socket;this.origin=origin.replaceAll("/$","");}
 @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(socket,"/communication-ws").setAllowedOrigins(origin,"http://localhost:5173","http://127.0.0.1:5173","https://eaconcept.org","https://eac-system.eaconcept.org");}
}
