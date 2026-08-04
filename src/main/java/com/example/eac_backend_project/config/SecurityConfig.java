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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
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
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtProvider jwtProvider;
    private final EndpointPermissionService endpointPermissionService;

    @Autowired
    public SecurityConfig(
            @Lazy UserService userService,
            CustomAccessDeniedHandler accessDeniedHandler,
            JwtProvider jwtProvider,
            EndpointPermissionService endpointPermissionService
    ) {
        this.userService = userService;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtProvider = jwtProvider;
        this.endpointPermissionService = endpointPermissionService;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOriginPatterns(
                Arrays.asList(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://192.168.*.*",
                        "https://eac-system.eaconcept.org",
                        "https://eaconcept.org",
                        "http://100.114.178.13",
                        "https://*.tail*.ts.net",
                        "https://cedricokai.github.io"
                )
        );

        configuration.setAllowedMethods(
                Arrays.asList(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS",
                        "PATCH"
                )
        );

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setExposedHeaders(
                List.of(
                        "Authorization",
                        "Content-Disposition"
                )
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                .addFilterBefore(
                        jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )

                .authorizeHttpRequests(auth -> {
                    /*
                     * CORS preflight requests.
                     */
                    auth.requestMatchers(
                            HttpMethod.OPTIONS,
                            "/**"
                    ).permitAll();

                    /*
                     * Public authentication endpoints.
                     */
                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/auth/signin",
                            "/auth/signup",
                            "/auth/forgot-password",
                            "/auth/reset-password",
                            "/ONEPASS/UPLOAD_RECORD_NOW",
                            "/ONEPASS/retry-attempt"
                    ).permitAll();

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/ONEPASS/recent-attempts"
                    ).permitAll();

                    /*
                     * Public leave approval links.
                     */
                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/leave/public-approve",
                            "/api/leave/public-approve/**",
                            "/api/leave/quick-approve/**",
                            "/api/leave/quick-reject/**"
                    ).permitAll();

                    // =================================================
                    // LUNCH SERVING ENDPOINTS
                    // =================================================

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/lunch/serve/stats/daily"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/lunch/serve/today"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/lunch/serve/**"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/lunch/serve"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/lunch/serve/**"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.PUT,
                            "/api/lunch/serve/**"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN"
                    );

                    auth.requestMatchers(
                            HttpMethod.PATCH,
                            "/api/lunch/serve/**"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN"
                    );

                    auth.requestMatchers(
                            HttpMethod.DELETE,
                            "/api/lunch/serve/**"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR"
                    );

                    // =================================================
                    // LUNCH ASSIGNMENT ENDPOINTS
                    // =================================================

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/lunch/assignments/active"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/lunch/assignments/date"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/lunch/assignments/employee/*"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/lunch/assignments/employee/*/active"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/lunch/assignments/weekly"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/lunch/assignments/weekly"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR"
                    );

                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/lunch/assignments"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR"
                    );

                    auth.requestMatchers(
                            HttpMethod.PUT,
                            "/api/lunch/assignments/*"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR"
                    );

                    auth.requestMatchers(
                            HttpMethod.DELETE,
                            "/api/lunch/assignments/*"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR"
                    );

                    auth.requestMatchers(
                            HttpMethod.PATCH,
                            "/api/lunch/assignments/*/suspend"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR"
                    );

                    auth.requestMatchers(
                            HttpMethod.PATCH,
                            "/api/lunch/assignments/*/activate"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR"
                    );

                    // =================================================
                    // LUNCH MEAL ENDPOINTS
                    // =================================================

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/lunch/meals/**"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR",
                            "KITCHEN",
                            "MANAGER"
                    );

                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/lunch/meals/**"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR"
                    );

                    auth.requestMatchers(
                            HttpMethod.PUT,
                            "/api/lunch/meals/**"
                    ).hasAnyRole(
                            "ADMIN",
                            "HR"
                    );

                    auth.requestMatchers(
                            HttpMethod.DELETE,
                            "/api/lunch/meals/**"
                    ).hasRole("ADMIN");

                    // =================================================
                    // OTHER AUTHENTICATED ENDPOINTS
                    // =================================================

                    auth.requestMatchers(
                            "/api/excel-comparator/rule-sets/**"
                    ).authenticated();

                    auth.requestMatchers(
                            "/auth/**"
                    ).authenticated();

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/activity-logs/**"
                    ).authenticated();

                    auth.requestMatchers(
                            "/api/pages/check-access"
                    ).authenticated();

                    auth.requestMatchers(
                            "/api/leave/admin/employee-balances"
                    ).authenticated();

                    auth.requestMatchers(
                            "/api/loans/**"
                    ).authenticated();

                    auth.requestMatchers(
                            "/api/settings/email/**"
                    ).authenticated();

                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/payroll/reports/**"
                    ).authenticated();

                    auth.requestMatchers(
                            "/api/payroll/**"
                    ).authenticated();

                    /*
                     * Load database-configured endpoint permissions.
                     */
                    loadDynamicPermissions(auth);

                    /*
                     * Every endpoint not listed above requires login.
                     */
                    auth.anyRequest().authenticated();
                })

                .exceptionHandling(exception ->
                        exception
                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                                .authenticationEntryPoint(
                                        new HttpStatusEntryPoint(
                                                HttpStatus.UNAUTHORIZED
                                        )
                                )
                )

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    private void loadDynamicPermissions(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry auth
    ) {
        try {
            List<EndpointPermission> permissions =
                    endpointPermissionService
                            .getAllActivePermissions();

            for (EndpointPermission permission : permissions) {
                String endpointPattern =
                        permission.getEndpointPattern();

                if (endpointPattern == null
                        || endpointPattern.isBlank()) {
                    continue;
                }

                /*
                 * Lunch endpoints are handled by the static rules above.
                 */
                if (endpointPattern.startsWith("/api/lunch/")) {
                    continue;
                }

                if (permission.getRoles().isEmpty()
                        && permission.getUsers().isEmpty()) {
                    continue;
                }

                List<String> authorities =
                        new ArrayList<>();

                for (Role role : permission.getRoles()) {
                    authorities.add(role.getName());
                }

                for (User user : permission.getUsers()) {
                    authorities.add(
                            "USER_"
                                    + user.getId()
                                    + "_"
                                    + permission.getId()
                    );
                }

                if (!authorities.isEmpty()) {
                    auth.requestMatchers(
                            HttpMethod.valueOf(
                                    permission
                                            .getHttpMethod()
                                            .toUpperCase()
                            ),
                            endpointPattern
                    ).hasAnyAuthority(
                            authorities.toArray(new String[0])
                    );
                }
            }

        } catch (Exception exception) {
            System.err.println(
                    "Error loading dynamic permissions: "
                            + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(
                jwtProvider,
                userService
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(
            HttpSecurity http
    ) throws Exception {

        AuthenticationManagerBuilder builder =
                http.getSharedObject(
                        AuthenticationManagerBuilder.class
                );

        builder
                .userDetailsService(userService)
                .passwordEncoder(passwordEncoder());

        return builder.build();
    }
}