package com.example.eac_backend_project.controller;

import com.example.eac_backend_project.Service.UserService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.config.JwtProvider;
import com.example.eac_backend_project.dto.ForgotPasswordRequestDTO;
import com.example.eac_backend_project.dto.LoginRequest;
import com.example.eac_backend_project.dto.UserDTO;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.RoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    private EmailService emailService;

    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtProvider jwtProvider,
            RoleRepository roleRepository,
            EmployeeRepository employeeRepository
    ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.roleRepository = roleRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/signin")
    public ResponseEntity<?> getSigninPage() {
        return ResponseEntity.ok(Map.of(
                "message", "Please use POST method to authenticate",
                "endpoint", "/auth/signin",
                "method", "POST",
                "required_fields", Map.of(
                        "userName", "string",
                        "password", "string"
                )
        ));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserDTO>> getUsers() {
        return ResponseEntity.ok(userService.getAllUserDTOs());
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            Role defaultRole = roleRepository.findByName("CUSTOMER")
                    .or(() -> roleRepository.findByName("USER"))
                    .orElseThrow(() -> new RuntimeException("Default role not found"));

            user.setRole(defaultRole);
            userService.registerUser(user);

            return ResponseEntity.ok(Map.of("message", "User registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/create-user")
    public ResponseEntity<?> createUserByAdmin(@RequestBody Map<String, Object> userData) {
        try {
            if (userData.get("name") == null || ((String) userData.get("name")).trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
            }
            if (userData.get("userName") == null || ((String) userData.get("userName")).trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
            }
            if (userData.get("email") == null || ((String) userData.get("email")).trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
            }
            if (userData.get("password") == null || ((String) userData.get("password")).trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
            }
            if (userData.get("role") == null || ((String) userData.get("role")).trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Role is required"));
            }

            User user = new User();
            user.setName(((String) userData.get("name")).trim());
            user.setUserName(((String) userData.get("userName")).trim());
            user.setEmail(((String) userData.get("email")).trim());
            user.setPassword(((String) userData.get("password")).trim());
            user.setMustChangePassword(true);
            user.setMobile(userData.get("mobile") != null ? ((String) userData.get("mobile")).trim() : "");

            String roleName = ((String) userData.get("role")).trim();

            Role role = roleRepository.findByName(roleName)
                    .or(() -> roleRepository.findByName("ROLE_" + roleName))
                    .orElseThrow(() -> new RuntimeException("Role '" + roleName + "' not found. Available roles: " +
                            roleRepository.findAll().stream()
                                    .map(Role::getName)
                                    .collect(Collectors.joining(", "))));

            User createdUser = userService.createUserByAdmin(user, role);

            return ResponseEntity.ok(Map.of(
                    "message", "User created successfully",
                    "user", Map.of(
                            "id", createdUser.getId(),
                            "name", createdUser.getName(),
                            "userName", createdUser.getUserName(),
                            "email", createdUser.getEmail(),
                            "role", createdUser.getRole().getName()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Failed to create user: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            ));
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

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<String> authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            User currentUser = userService.getCurrentUser();

            response.setHeader("Authorization", "Bearer " + token);

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "token", token,
                    "role", currentUser.getRole().getName(),
                    "authorities", authorities,
                    "user", Map.of(
                            "id", currentUser.getId(),
                            "name", currentUser.getName(),
                            "username", currentUser.getUserName(),
                            "email", currentUser.getEmail(),
                            "mustChangePassword", Boolean.TRUE.equals(currentUser.getMustChangePassword()),
                            "enabled", currentUser.getEnabled()
                    )
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        try {
            User currentUser = userService.getCurrentUser();
            Employee employee = employeeRepository.findByEmail(currentUser.getEmail()).orElse(null);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("username", currentUser.getUserName());
            response.put("name", currentUser.getName());
            response.put("email", currentUser.getEmail());
            response.put("role", currentUser.getRole().getName());
            response.put("authorities", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
            response.put("mobile", currentUser.getMobile());
            response.put("enabled", currentUser.getEnabled());

            if (employee != null) {
                Map<String, Object> empMap = new LinkedHashMap<>();
                empMap.put("id", employee.getId());
                empMap.put("firstName", employee.getFirstName());
                empMap.put("lastName", employee.getLastName());
                empMap.put("employeeId", employee.getEmployeeId());
                empMap.put("department", employee.getDepartment());
                empMap.put("position", employee.getJobPosition());
                response.put("employee", empMap);
            } else {
                response.put("employee", null);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to get current user: " + e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> requestBody
    ) {
        try {
            String roleName = requestBody.get("role");

            Role role = roleRepository.findByName(roleName)
                    .or(() -> roleRepository.findByName("ROLE_" + roleName))
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

            User updatedUser = userService.updateUserRole(userId, role);

            return ResponseEntity.ok(Map.of(
                    "message", "User role updated successfully",
                    "role", updatedUser.getRole().getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllRoles() {
        try {
            List<Role> roles = roleRepository.findAll();
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to fetch roles: " + e.getMessage()));
        }
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createRole(@RequestBody Map<String, String> requestBody) {
        try {
            String roleName = requestBody.get("name");

            if (roleName == null || roleName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Role name is required"));
            }

            String formattedRoleName = roleName.trim().toUpperCase();
            if (!formattedRoleName.startsWith("ROLE_")) {
                formattedRoleName = "ROLE_" + formattedRoleName;
            }

            Optional<Role> existingRole = roleRepository.findByName(formattedRoleName);
            if (existingRole.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Role already exists"));
            }

            Role role = new Role();
            role.setName(formattedRoleName);

            Role savedRole = roleRepository.save(role);

            return ResponseEntity.ok(savedRole);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to create role: " + e.getMessage()));
        }
    }

    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteRole(@PathVariable Long roleId) {
        try {
            Optional<Role> roleOptional = roleRepository.findById(roleId);

            if (!roleOptional.isPresent()) {
                return ResponseEntity.status(404)
                        .body(Map.of("message", "Role not found"));
            }

            Role role = roleOptional.get();

            List<String> protectedRoles = Arrays.asList(
                    "ROLE_ADMIN", "ROLE_CUSTOMER", "ROLE_HR", "ROLE_INVENTORY",
                    "ROLE_SUPERVISOR", "ROLE_PLANNER", "ROLE_PROCUREMENT_OFFICER",
                    "ROLE_TRANSPORT"
            );

            if (protectedRoles.contains(role.getName())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Cannot delete system role"));
            }

            long usersWithRole = userService.countUsersByRole(role);
            if (usersWithRole > 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message",
                                String.format("Cannot delete role - %d user(s) assigned to it", usersWithRole)));
            }

            roleRepository.delete(role);

            return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to delete role: " + e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/password")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> setUserPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> requestBody) {
        try {
            String password = requestBody.get("password");

            if (password == null || password.length() < 4) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Password must be at least 4 characters"));
            }

            User updatedUser = userService.setUserPassword(userId, password);

            return ResponseEntity.ok(Map.of(
                    "message", "Password updated successfully",
                    "userId", updatedUser.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to update password: " + e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/toggle-status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId) {
        try {
            User updatedUser = userService.toggleUserStatus(userId);

            String status = updatedUser.getEnabled() ? "enabled" : "disabled";
            return ResponseEntity.ok(Map.of(
                    "message", String.format("User %s successfully", status),
                    "enabled", updatedUser.getEnabled()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to toggle user status: " + e.getMessage()));
        }
    }

    @PostMapping("/first-login/set-password")
    public ResponseEntity<?> setPasswordOnFirstLogin(@RequestBody com.example.eac_backend_project.dto.FirstLoginPasswordDTO dto) {
        try {
            if (dto.getNewPassword() == null || dto.getConfirmPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Password fields are required"));
            }
            if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match"));
            }

            userService.setPasswordOnFirstLogin(dto.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password set successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Value("${app.email.base-url:http://localhost:5173}")
    private String baseUrl;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO dto,
            HttpServletRequest request
    ) {
        try {
            userService.requestPasswordReset(dto.getEmailOrUsername());

            Optional<User> userOpt = userService.getUserByEmail(dto.getEmailOrUsername())
                    .or(() -> userService.getUserByUserName(dto.getEmailOrUsername()));

            if (userOpt.isPresent()) {
                User u = userOpt.get();
                if (u.getPasswordResetToken() != null) {
                    String link = userService.buildResetPasswordLink(request, u.getPasswordResetToken());

                    emailService.sendPasswordResetEmail(u.getEmail(), link, u.getName());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "If the account exists, a reset link has been sent"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "message", "If the account exists, a reset link has been sent"
            ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody com.example.eac_backend_project.dto.ResetPasswordDTO dto) {
        try {
            if (dto.getNewPassword() == null || dto.getConfirmPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Password fields are required"));
            }
            if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match"));
            }

            userService.resetPasswordWithToken(dto.getToken(), dto.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}