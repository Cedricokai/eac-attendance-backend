package com.example.eac_backend_project.Service;


import com.example.eac_backend_project.entity.inventory.Role;
import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.UserRepository;
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
    
    // Helper method to check if a string is an email
    private boolean isEmail(String str) {
        return str != null && str.contains("@"); // Simple check, improve if needed
    }
    


    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Ensure password is encoded
        return userRepository.save(user);
    }

    public Optional<User> getUserByUserName(String userName) {
        return userRepository.findByUserName(userName); // Make sure User has all necessary fields
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

        // Try to find user by username first, then by email
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
}
