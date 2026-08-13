package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.dto.UserDTO;
import com.example.eac_backend_project.entity.inventory.Page;
import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.entity.attendance.Employee;
import com.example.eac_backend_project.repository.UserRepository;
import com.example.eac_backend_project.repository.EmployeeRepository;
import com.example.eac_backend_project.repository.RoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final FrontendUrlService frontendUrlService;

    public UserService(UserRepository userRepository, EmployeeRepository employeeRepository,
                       PasswordEncoder passwordEncoder, RoleRepository roleRepository,
                       FrontendUrlService frontendUrlService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.frontendUrlService = frontendUrlService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createDefaultAdminUser() {
        try {
            System.out.println("=== Starting default user creation ===");

            // 1. Create default roles with ROLE_ prefix
            createDefaultRoles();

            // 2. Check if admin user exists
            if (!userRepository.findByUserName("admin").isPresent()) {
                System.out.println("Creating default admin user...");

                // 3. Get or create ROLE_ADMIN
                Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                        .orElseGet(() -> {
                            Role newRole = new Role();
                            newRole.setName("ROLE_ADMIN");
                            newRole.setSystem(true);
                            return roleRepository.save(newRole);
                        });

                // 4. Create admin user
                User admin = new User();
                admin.setUserName("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setName("System Administrator");
                admin.setEmail("admin@eac.com");
                admin.setMobile("0000000000");
                admin.setEnabled(true);
                admin.setRole(adminRole);

                User savedAdmin = userRepository.save(admin);
                System.out.println("Admin user created successfully with ID: " + savedAdmin.getId());

                // 5. Create corresponding employee record
                createEmployeeForUser(savedAdmin, savedAdmin.getName(), "System Administrator");

            } else {
                System.out.println("Admin user already exists, skipping creation.");
            }

            System.out.println("=== Default user creation completed ===");

        } catch (Exception e) {
            System.err.println("Error creating default admin user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    protected void createDefaultRoles() {
        try {
            System.out.println("Checking and creating default roles...");

            String[] defaultRoles = {"ADMIN", "USER", "MANAGER", "SUPERVISOR", "PLANNER",
                    "HR", "CUSTOMER", "PROCUREMENT_OFFICER", "INVENTORY", "TRANSPORT"};

            for (String roleName : defaultRoles) {
                String prefixedRoleName = "ROLE_" + roleName;
                if (!roleRepository.findByName(prefixedRoleName).isPresent()) {
                    Role role = new Role();
                    role.setName(prefixedRoleName);
                    // Mark ROLE_ADMIN and ROLE_USER as system roles
                    role.setSystem(roleName.equals("ADMIN") || roleName.equals("USER"));
                    roleRepository.save(role);
                    System.out.println("Created role: " + prefixedRoleName);
                }
            }

            System.out.println("Role creation check completed.");
        } catch (Exception e) {
            System.err.println("Error creating default roles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUserName(identifier))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + identifier)
                );

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new UsernameNotFoundException("User account is disabled: " + identifier);
        }

        // Ensure role name is used as authority
        String authority = user.getRole().getName();
        if (!authority.startsWith("ROLE_")) {
            authority = "ROLE_" + authority;
        }

        SimpleGrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority);

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassword(),
                Collections.singletonList(grantedAuthority)
        );
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        createEmployeeForUser(savedUser, user.getName(), "Employee");

        return savedUser;
    }

    @Transactional
    public User createUserByAdmin(User user, Role role) {
        User currentUser = getCurrentUser();

        // Fix: Check for ROLE_ADMIN instead of ADMIN
        if (!currentUser.getRole().getName().equals("ROLE_ADMIN")) {
            throw new RuntimeException("Only administrators can create users");
        }

        if (userRepository.findByUserName(user.getUserName()).isPresent()) {
            throw new RuntimeException("Username already exists: " + user.getUserName());
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(role);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        String jobPosition = getJobPositionFromRole(role);
        createEmployeeForUser(savedUser, user.getName(), jobPosition);

        return savedUser;
    }

    @Transactional
    public User updateUserPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(userId)) {
            throw new RuntimeException("You cannot disable your own account");
        }

        user.setEnabled(!user.getEnabled());
        return userRepository.save(user);
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> getUserByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        String identifier;
        if (authentication.getPrincipal() instanceof UserDetails) {
            identifier = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            identifier = authentication.getPrincipal().toString();
        }

        Optional<User> userOptional = userRepository.findByUserName(identifier);
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(identifier);
        }

        return userOptional.orElseThrow(() -> new RuntimeException("User not found with identifier: " + identifier));
    }

    @Transactional
    public User updateUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(role);
        User updatedUser = userRepository.save(user);

        updateEmployeeForUser(updatedUser, getJobPositionFromRole(role));

        return updatedUser;
    }

    private void createEmployeeForUser(User user, String fullName, String jobPosition) {
        try {
            Optional<Employee> existingEmployee = employeeRepository.findByEmail(user.getEmail());
            if (existingEmployee.isPresent()) {
                System.out.println("Employee already exists for user: " + user.getEmail());
                return;
            }

            String[] nameParts = fullName.split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";

            Employee employee = new Employee();
            employee.setFirstName(firstName);
            employee.setLastName(lastName);
            employee.setEmail(user.getEmail());
            employee.setJobPosition(jobPosition);
            employee.setStartDate(LocalDate.now());
            employee.setWorkType("Full-time");

            double basicSalary = getBasicSalaryFromRole(user.getRole());
            employee.setBasicSalary(basicSalary);

            employee.setEmployeeId(generateEmployeeIdForUser(user));
            employee.setMinimumRate(basicSalary / 160); // Assuming 160 hours per month

            Employee savedEmployee = employeeRepository.save(employee);
            System.out.println("Created employee record for user: " + user.getEmail() +
                    " with ID: " + savedEmployee.getId());
        } catch (Exception e) {
            System.err.println("Error creating employee for user " + user.getEmail() + ": " + e.getMessage());
        }
    }

    private void updateEmployeeForUser(User user, String jobPosition) {
        Optional<Employee> existingEmployee = employeeRepository.findByEmail(user.getEmail());
        if (existingEmployee.isPresent()) {
            Employee employee = existingEmployee.get();
            employee.setJobPosition(jobPosition);
            double basicSalary = getBasicSalaryFromRole(user.getRole());
            employee.setBasicSalary(basicSalary);
            employee.setMinimumRate(basicSalary / 160);
            employeeRepository.save(employee);
            System.out.println("Updated employee record for user: " + user.getEmail());
        }
    }

    private String generateEmployeeIdForUser(User user) {
        String prefix = getEmployeeIdPrefixFromRole(user.getRole());
        String datePart = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
        long count = employeeRepository.countByStartDate(LocalDate.now()) + 1;
        String seqPart = String.format("%03d", count);

        return prefix + datePart + "-" + seqPart;
    }

    private String getJobPositionFromRole(Role role) {
        String roleName = role.getName();

        switch (roleName) {
            case "ROLE_ADMIN":
                return "System Administrator";
            case "ROLE_SUPERVISOR":
                return "Supervisor";
            case "ROLE_PLANNER":
                return "Planner";
            case "ROLE_HR":
                return "HR Manager";
            case "ROLE_CUSTOMER":
                return "Employee";
            case "ROLE_PROCUREMENT_OFFICER":
                return "Procurement Officer";
            case "ROLE_INVENTORY":
                return "Inventory Manager";
            case "ROLE_TRANSPORT":
                return "Transport Officer";
            case "ROLE_MANAGER":
                return "Manager";
            default:
                return "Staff";
        }
    }

    private String getEmployeeIdPrefixFromRole(Role role) {
        String roleName = role.getName();

        switch (roleName) {
            case "ROLE_ADMIN":
                return "ADM-";
            case "ROLE_SUPERVISOR":
                return "SUP-";
            case "ROLE_PLANNER":
                return "PLN-";
            case "ROLE_HR":
                return "HR-";
            case "ROLE_CUSTOMER":
                return "EMP-";
            case "ROLE_PROCUREMENT_OFFICER":
                return "PRO-";
            case "ROLE_INVENTORY":
                return "INV-";
            case "ROLE_TRANSPORT":
                return "TRN-";
            case "ROLE_MANAGER":
                return "MGR-";
            default:
                return "STF-";
        }
    }

    private double getBasicSalaryFromRole(Role role) {
        String roleName = role.getName();

        switch (roleName) {
            case "ROLE_ADMIN":
                return 8000.0;
            case "ROLE_MANAGER":
                return 7000.0;
            case "ROLE_SUPERVISOR":
                return 6000.0;
            case "ROLE_PLANNER":
                return 5500.0;
            case "ROLE_HR":
                return 5000.0;
            case "ROLE_PROCUREMENT_OFFICER":
                return 4500.0;
            case "ROLE_INVENTORY":
                return 4500.0;
            case "ROLE_TRANSPORT":
                return 4000.0;
            case "ROLE_CUSTOMER":
                return 3000.0;
            default:
                return 2500.0;
        }
    }

    public Optional<Employee> getEmployeeByUserEmail(String email) {
        return employeeRepository.findByEmail(email);
    }

    public long countUsersByRole(Role role) {
        return userRepository.countByRole(role);
    }

    public User setUserPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    public List<UserDTO> getAllUserDTOs() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getName(),
                        user.getUserName(),
                        user.getEmail(),
                        user.getMobile(),
                        user.getRole().getName(),
                        user.getEnabled(),
                        user.getAccessiblePages()
                                .stream()
                                .map(Page::getId)
                                .collect(java.util.stream.Collectors.toSet())
                ))
                .toList();
    }

    @Transactional
    public void setPasswordOnFirstLogin(String newPassword) {
        User user = getCurrentUser();

        if (!Boolean.TRUE.equals(user.getMustChangePassword())) {
            throw new RuntimeException("Password change is not required for this account.");
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters.");
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setMustChangePassword(false);

        // Clear any reset tokens
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);

        userRepository.save(user);
    }

    @Transactional
    public void requestPasswordReset(String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            return; // do not reveal anything
        }

        Optional<User> userOpt = userRepository.findByEmail(emailOrUsername.trim())
                .or(() -> userRepository.findByUserName(emailOrUsername.trim()));

        if (userOpt.isEmpty()) return;

        User user = userOpt.get();
        if (!Boolean.TRUE.equals(user.getEnabled())) return;

        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiresAt(java.time.LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        // Build reset link (frontend route)
        // baseUrl already exists in your EmailServiceImpl via @Value("${app.email.base-url:...}")
        // Put baseUrl in UserService via @Value as well, or pass it in from controller.
    }

    @Transactional
    public void resetPasswordWithToken(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("Reset token is required");
        }

        User user = userRepository.findByPasswordResetToken(token.trim())
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (user.getPasswordResetExpiresAt() == null ||
                user.getPasswordResetExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Invalid or expired token");
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setMustChangePassword(false);

        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);

        userRepository.save(user);
    }

    private boolean isLocalRequest(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (ip == null) return false;

        if (ip.startsWith("192.168.") || ip.startsWith("10.")) return true;

        if (ip.startsWith("172.")) {
            try {
                int secondOctet = Integer.parseInt(ip.split("\\.")[1]);
                return secondOctet >= 16 && secondOctet <= 31;
            } catch (Exception ignored) {}
        }

        return false;
    }

    private String getRequestHost(HttpServletRequest request) {
        // If behind a proxy, this is the most reliable
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            // X-Forwarded-Host can be "host, host2"
            String host = forwardedHost.split(",")[0].trim();
            // strip any port if present
            return host.contains(":") ? host.substring(0, host.indexOf(':')) : host;
        }

        // Standard direct access
        String hostHeader = request.getHeader("Host");
        if (hostHeader != null && !hostHeader.isBlank()) {
            String host = hostHeader.trim();
            return host.contains(":") ? host.substring(0, host.indexOf(':')) : host;
        }

        // Fallback
        return request.getServerName(); // already host-only
    }

    public String buildResetPasswordLink(HttpServletRequest request, String token) {
        return frontendUrlService.route("/reset-password?token=" + token);
    }

    private boolean requestCameViaLocalhost(HttpServletRequest request) {
        String host = getClientHost(request);
        if (host == null) return false;

        // host might include port if present: "localhost:5173"
        host = host.split(":")[0].trim().toLowerCase();

        return host.equals("localhos") || host.equals("127.0.0.1");
    }

    private boolean requestCameViaLanIp(HttpServletRequest request) {
        String host = getClientHost(request);
        if (host == null) return false;

        host = host.split(":")[0].trim();

        // LAN ranges
        if (host.startsWith("192.168.") || host.startsWith("10.")) return true;

        if (host.startsWith("172.")) {
            try {
                String[] parts = host.split("\\.");
                int second = Integer.parseInt(parts[1]);
                return second >= 16 && second <= 31;
            } catch (Exception ignored) {}
        }

        return false;
    }

    /**
     * Gets the host the user used to access your backend (works better behind proxies).
     * Examples: "localhost:8080", "192.168.1.97", "100.114.178.13"
     */
    private String getClientHost(HttpServletRequest request) {
        // If you ever sit behind a proxy, this may be the only correct one:
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            // can be a list: "host1, host2"
            return forwardedHost.split(",")[0].trim();
        }

        String hostHeader = request.getHeader("Host");
        if (hostHeader != null && !hostHeader.isBlank()) {
            return hostHeader.trim();
        }

        // fallback
        return request.getServerName();
    }



    @Value("${app.email.base-url.local}")
    private String localBaseUrl;

    @Value("${app.email.base-url.public}")
    private String publicBaseUrl;




}
