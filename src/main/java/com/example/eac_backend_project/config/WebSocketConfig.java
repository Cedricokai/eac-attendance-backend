package com.example.eac_backend_project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Use the same origins as in application.properties
        String[] allowedOrigins = {
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:8080",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174",
                "https://eaconcept.org",
                "http://192.168.1.97:5173",  // Add port for local network
                "http://192.168.1.97:5174",  // Add port for local network
                "http://192.168.1.97:8080",  // Add backend port too
                "http://100.114.178.13:5173", // Add port for public IP
                "http://100.114.178.13:5174",
                "http://100.114.178.13:80",   // Add HTTP port
                "http://100.114.178.13:8080"
        };

        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);
    }
}