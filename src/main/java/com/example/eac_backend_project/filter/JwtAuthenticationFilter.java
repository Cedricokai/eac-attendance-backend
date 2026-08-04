package com.example.eac_backend_project.filter;

import com.example.eac_backend_project.Service.UserService;
import com.example.eac_backend_project.config.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserService userService;

    public JwtAuthenticationFilter(
            JwtProvider jwtProvider,
            UserService userService
    ) {
        this.jwtProvider = jwtProvider;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestURI = request.getRequestURI();
        final String authorizationHeader =
                request.getHeader("Authorization");

        logger.debug("JWT filter processing request: " + requestURI);

        /*
         * No token was supplied.
         *
         * Do not immediately return a 401 here because public endpoints
         * must still be allowed to continue through Spring Security.
         */
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            logger.debug(
                    "No Bearer token found for request: " + requestURI
            );

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isEmpty()) {
            logger.warn(
                    "Empty Bearer token received for request: "
                            + requestURI
            );

            filterChain.doFilter(request, response);
            return;
        }

        try {
            String tokenPreview =
                    token.substring(0, Math.min(token.length(), 20));

            logger.debug(
                    "JWT token received: " + tokenPreview + "..."
            );

            String username = jwtProvider.extractUsername(token);

            logger.debug(
                    "Username extracted from JWT: " + username
            );

            if (username != null
                    && SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userService.loadUserByUsername(username);

                logger.debug(
                        "UserDetails loaded for username: " + username
                );

                boolean validToken =
                        jwtProvider.validateToken(token, userDetails);

                boolean enabledUser = userDetails.isEnabled();

                if (validToken && enabledUser) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);

                    logger.debug(
                            "JWT authentication successful for: "
                                    + username
                    );

                    logger.debug(
                            "Authenticated authorities: "
                                    + userDetails.getAuthorities()
                    );
                } else {
                    logger.warn(
                            "JWT validation failed or user is disabled: "
                                    + username
                    );
                }
            }

        } catch (Exception exception) {
            /*
             * Authentication is not set when the token is invalid,
             * expired or malformed. Spring Security will return 401
             * for protected endpoints.
             */
            SecurityContextHolder.clearContext();

            logger.error(
                    "JWT authentication failed for request "
                            + requestURI
                            + ": "
                            + exception.getMessage(),
                    exception
            );
        }

        filterChain.doFilter(request, response);
    }
}