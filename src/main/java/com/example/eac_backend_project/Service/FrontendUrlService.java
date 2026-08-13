package com.example.eac_backend_project.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class FrontendUrlService {
    private static final ThreadLocal<String> ASYNC_BASE_URL = new ThreadLocal<>();
    private final String fallbackBaseUrl;
    private final Set<String> allowedOrigins;

    public FrontendUrlService(
            @Value("${app.frontend.base-url:https://eac-system.eaconcept.org}") String fallbackBaseUrl,
            @Value("${app.frontend.allowed-origins:http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173,http://192.168.1.97:5173,http://100.114.178.13:5173,https://eac-system.eaconcept.org}") String allowedOrigins
    ) {
        this.fallbackBaseUrl = normalize(fallbackBaseUrl);
        this.allowedOrigins = new LinkedHashSet<>();
        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(FrontendUrlService::normalize)
                .forEach(this.allowedOrigins::add);
    }

    public String getBaseUrl() {
        String capturedBaseUrl = ASYNC_BASE_URL.get();
        if (capturedBaseUrl != null && !capturedBaseUrl.isBlank()) return capturedBaseUrl;

        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String origin = firstValue(request.getHeader("Origin"));
            if (isAllowed(origin)) return normalize(origin);

            String forwardedOrigin = firstValue(request.getHeader("X-Forwarded-Origin"));
            if (isAllowed(forwardedOrigin)) return normalize(forwardedOrigin);
        }
        return fallbackBaseUrl;
    }

    public Runnable withBaseUrl(String baseUrl, Runnable task) {
        String normalizedBaseUrl = normalize(baseUrl);
        return () -> {
            try {
                ASYNC_BASE_URL.set(normalizedBaseUrl);
                task.run();
            } finally {
                ASYNC_BASE_URL.remove();
            }
        };
    }

    public String route(String route) {
        String normalizedRoute = route == null || route.isBlank() ? "/" : (route.startsWith("/") ? route : "/" + route);
        return getBaseUrl() + "/#" + normalizedRoute;
    }

    private boolean isAllowed(String origin) {
        return origin != null && allowedOrigins.contains(normalize(origin));
    }

    private static String firstValue(String value) {
        return value == null || value.isBlank() ? null : value.split(",")[0].trim();
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }
}
