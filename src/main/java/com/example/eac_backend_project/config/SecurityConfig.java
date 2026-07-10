package com.example.eac_backend_project.config;

import com.example.eac_backend_project.Service.EndpointPermissionService;
import com.example.eac_backend_project.Service.UserService;
import com.example.eac_backend_project.entity.inventory.EndpointPermission;
import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final UserService userService;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtProvider jwtProvider;
    private final EndpointPermissionService endpointPermissionService;

    @Autowired
    public SecurityConfig(@Lazy UserService userService,
                          CustomAccessDeniedHandler accessDeniedHandler,
                          JwtProvider jwtProvider,
                          EndpointPermissionService endpointPermissionService) {
        this.userService = userService;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtProvider = jwtProvider;
        this.endpointPermissionService = endpointPermissionService;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.*.*",
                "https://eac-system.eaconcept.org",
                "https://eaconcept.org",
                "http://100.114.178.13",
                "https://*.tail*.ts.net",
                "https://cedricokai.github.io"
        ));
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition")); // Add Content-Disposition for file downloads
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()).cors();
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Public auth endpoints:
                    .requestMatchers(HttpMethod.POST,
                            "/ONEPASS/UPLOAD_RECORD_NOW",
                            "/ONEPASS/retry-attempt"
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET,
                            "/ONEPASS/recent-attempts"
                    ).permitAll()
                    .requestMatchers(HttpMethod.POST,
                            "/auth/signin",
                            "/auth/signup",
                            "/auth/forgot-password",
                            "/auth/reset-password",
                            "/api/leave/public-approve",
                            "/api/leave/public-approve/**",
                            "/api/leave/quick-approve/**",
                            "/api/leave/quick-reject/**"
                    ).permitAll()

                    // ========== ADD REPORTS ENDPOINTS HERE ==========
                    // Allow authenticated access to reports (they need JWT token)
                    .requestMatchers(HttpMethod.GET, "/api/reports/**").authenticated()

                    // Everything else under /auth requires auth:
                    .requestMatchers("/api/excel-comparator/rule-sets/**").authenticated()
                    .requestMatchers("/auth/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/activity-logs/**").authenticated()
                    .requestMatchers("/api/pages/check-access").authenticated()
                    .requestMatchers("/api/leave/admin/employee-balances").authenticated()
                    .requestMatchers("/api/loans/**").authenticated()
                    .requestMatchers("/api/settings/email/**").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/payroll/reports/**").authenticated()
                    .requestMatchers("/api/payroll/**").authenticated();

            loadDynamicPermissions(auth);
            auth.anyRequest().authenticated();
        });

        http.exceptionHandling(e -> e
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );

        http.formLogin().disable();
        http.logout().disable();

        return http.build();
    }

    private void loadDynamicPermissions(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        try {
            List<EndpointPermission> permissions = endpointPermissionService.getAllActivePermissions();

            for (EndpointPermission permission : permissions) {
                if (permission.getRoles().isEmpty() && permission.getUsers().isEmpty()) continue;

                List<String> authorities = new ArrayList<>();
                for (Role role : permission.getRoles()) authorities.add(role.getName());
                for (User user : permission.getUsers()) authorities.add("USER_" + user.getId() + "_" + permission.getId());

                if (!authorities.isEmpty()) {
                    auth.requestMatchers(
                            HttpMethod.valueOf(permission.getHttpMethod()),
                            permission.getEndpointPattern()
                    ).hasAnyAuthority(authorities.toArray(new String[0]));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading dynamic permissions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, userService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userService)
                .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }
}