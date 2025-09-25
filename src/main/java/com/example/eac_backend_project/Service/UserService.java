package com.example.eac_backend_project.Service;

import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.UserRepository;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createDefaultAdminUser() {
        if (userRepository.findByUserName("admin").isEmpty() &&
                userRepository.findByEmail("admin@example.com").isEmpty()) {

            User adminUser = new User();
            adminUser.setName("System Administrator");
            adminUser.setUserName("admin");
            adminUser.setEmail("admin@example.com");
            adminUser.setPassword(passwordEncoder.encode("1234"));
            adminUser.setRole(Role.ROLE_ADMIN);
            adminUser.setMobile("0000000000");

            userRepository.save(adminUser);
            System.out.println("Default admin user created: admin/1234");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Try to find by email first, then by username
        Optional<User> userOptional = userRepository.findByEmail(identifier);
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByUserName(identifier);
        }

        User user = userOptional
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // Use email as the principal
                user.getPassword(),
                new ArrayList<>(Collections.singletonList(authority))
        );
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // New method for admin to create users
    public User createUserByAdmin(User user, Role role) {
        // Validate that the current user is admin
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new RuntimeException("Only administrators can create users");
        }

        // Check if username or email already exists
        if (userRepository.findByUserName(user.getUserName()).isPresent()) {
            throw new RuntimeException("Username already exists: " + user.getUserName());
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(role);
        return userRepository.save(user);
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

    public User updateUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(role);
        return userRepository.save(user);
    }

    // Helper method to check if a string is an email
    private boolean isEmail(String str) {
        return str != null && str.contains("@");
    }
}