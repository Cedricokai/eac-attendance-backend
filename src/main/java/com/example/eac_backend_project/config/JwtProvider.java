package com.example.eac_backend_project.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private final long EXPIRATION_TIME = 86400000; // 1 day in milliseconds

    private final Key key;

    // Constructor: Initializes the key for signing and verifying the JWT
    public JwtProvider(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    // Generates a JWT token for an authenticated user
    public String generateToken(Authentication authentication) {
        Map<String, Object> claims = new HashMap<>();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Add username (user's login name, could be username or email)
        claims.put("username", userDetails.getUsername());

        // Check if email is available and include in claims
        if (userDetails instanceof org.springframework.security.core.userdetails.User) {
            org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) userDetails;
            String email = user.getUsername(); // This assumes the username is the email
            claims.put("email", email);
        }

        // Add roles to the token
        claims.put("roles", authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toList()));

        return createToken(claims, userDetails.getUsername());
    }

    // Creates the token with the given claims and subject (username)
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)  // Subject is typically the username
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // Validate the JWT token with the user details (username and expiration check)
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Extracts the username from the token
    public String extractUsername(String token) {
        return extractAllClaims(token).get("username").toString();
    }

    // Extracts all claims (including expiration time) from the token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    // Checks if the token is expired
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
