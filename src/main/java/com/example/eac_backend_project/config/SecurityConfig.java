package com.example.eac_backend_project.config;

import com.example.eac_backend_project.Service.UserService;
import com.example.eac_backend_project.filter.JwtAuthenticationFilter;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@EnableWebSecurity
@Configuration
@Data
public class SecurityConfig {

    private final UserService userService;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtProvider jwtProvider;  // Add this line to inject JwtProvider

    @Value("${cors.allowedOrigins}")
    private List<String> allowedOrigins;

    @Value("${jwt.secret}")
    private String secretKey;

    // Constructor injection with @Lazy for UserService
    public SecurityConfig(@Lazy UserService userService, CustomAccessDeniedHandler accessDeniedHandler, JwtProvider jwtProvider) {
        this.userService = userService;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtProvider = jwtProvider;  // Initialize jwtProvider in the constructor
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Disable CSRF (consider enabling it in production)
        http.csrf(csrf -> csrf.disable());

        // Configure CORS with custom CorsConfigurationSource bean
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // Add the JwtAuthenticationFilter
        http.addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userService), UsernamePasswordAuthenticationFilter.class);

        // Authorize HTTP requests
        http.authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/auth/signup", "/auth/signin").permitAll()

                // Admin-protected endpoints
                .requestMatchers("/api/products/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_INVENTORY")
                .requestMatchers("/api/assets/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_INVENTORY")
                .requestMatchers("/api/product-history").hasAnyAuthority("ROLE_ADMIN", "ROLE_INVENTORY")
                .requestMatchers("/api/outgoing").hasAnyAuthority("ROLE_ADMIN", "ROLE_INVENTORY")
                .requestMatchers("/api/employee").permitAll()
                .requestMatchers("/api/attendance").hasAnyAuthority("ROLE_ADMIN", "ROLE_HR")
                .requestMatchers("/api/holiday").hasAnyAuthority("ROLE_ADMIN", "ROLE_HR")
                .requestMatchers("/api/leave").permitAll()
                .requestMatchers("/api/overtime").hasAnyAuthority("ROLE_ADMIN", "ROLE_HR")
                .requestMatchers("/api/overview").hasAnyAuthority("ROLE_ADMIN", "ROLE_HR")
                .requestMatchers("/api/payroll").hasAnyAuthority("ROLE_ADMIN", "ROLE_HR")
                .requestMatchers("/api/timesheets/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_HR")
                .requestMatchers("/api/leave/supervisor/pending").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPERVISOR")
                .requestMatchers("/auth").authenticated()
                // Allow all users to access the /auth endpoint


                // Protected endpoints (require authentication)
                .requestMatchers("/api/user/**").hasAnyAuthority("ADMIN") // Example of authenticated-only endpoint
                .anyRequest().authenticated() // All other requests require authentication
        );

        // Exception handling
        http.exceptionHandling(e -> e
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );

        // Disable form login and customize logout handling
        http.formLogin(form -> form.disable());
        http.logout(logout ->
                logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpStatus.OK.value()))
                        .deleteCookies("JSESSIONID") // Optional: if using cookies for session tracking
                        .permitAll()
        );

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(userService) // Use UserService directly as UserDetailsService
                .passwordEncoder(passwordEncoder()); // Set password encoder
        return authenticationManagerBuilder.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:3000"
        ));
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}