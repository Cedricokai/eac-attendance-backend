package com.example.eac_backend_project.Controller;

import com.example.eac_backend_project.Service.UserService;
import com.example.eac_backend_project.config.JwtProvider;
import com.example.eac_backend_project.dto.LoginRequest;
import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH}, allowedHeaders = "*", allowCredentials = "true")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public AuthController(UserService userService, AuthenticationManager authenticationManager, JwtProvider jwtProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    @GetMapping
    public List<User> getUsers(){
        return userService.getAllUsers();
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        user.setRole(Role.ROLE_CUSTOMER);
        User savedUser = userService.registerUser(user);
        return ResponseEntity.ok("User registered successfully. Redirect to login.");
    }

    @PostMapping("/admin/create-user")
    public ResponseEntity<?> createUserByAdmin(@RequestBody Map<String, Object> userData) {
        try {
            User user = new User();
            user.setName((String) userData.get("name"));
            user.setUserName((String) userData.get("userName"));
            user.setEmail((String) userData.get("email"));
            user.setPassword((String) userData.get("password"));
            user.setMobile((String) userData.get("mobile"));

            String roleStr = (String) userData.get("role");
            Role role = Role.valueOf("ROLE_" + roleStr.toUpperCase());

            User createdUser = userService.createUserByAdmin(user, role);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User created successfully");
            response.put("user", createdUser);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUserName(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtProvider.generateToken(authentication);

            // Get user role and details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_CUSTOMER");

            // Get full user details
            User currentUser = userService.getCurrentUser();

            response.setHeader("Authorization", "Bearer " + token);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Login successful");
            responseBody.put("token", token);
            responseBody.put("role", role);
            responseBody.put("user", Map.of(
                    "name", currentUser.getName(),
                    "username", currentUser.getUserName(),
                    "email", currentUser.getEmail()
            ));

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
            }

            // Get the full user details from your UserService
            User currentUser = userService.getCurrentUser();

            Map<String, Object> userData = new HashMap<>();
            userData.put("username", currentUser.getUserName());
            userData.put("name", currentUser.getName());
            userData.put("email", currentUser.getEmail());
            userData.put("role", currentUser.getRole().name());
            userData.put("mobile", currentUser.getMobile());

            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized: " + e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request
    ) {
        try {
            String rawRole = requestBody.get("role");
            Role role = null;

            if (rawRole != null && !rawRole.equalsIgnoreCase("None")) {
                try {
                    role = Role.valueOf("ROLE_" + rawRole.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Invalid role: " + rawRole);
                }
            }

            User updatedUser = userService.updateUserRole(userId, role);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        Map<String, String> responseMessage = new HashMap<>();
        responseMessage.put("message", "Logout successful");
        return ResponseEntity.ok(responseMessage);
    }
}