package com.example.eac_backend_project.controller.transport;

import com.example.eac_backend_project.Service.transport.TransportUserService;
import com.example.eac_backend_project.entity.transport.Users;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final TransportUserService transportUserService;

    public UserController(TransportUserService transportUserService) {
        this.transportUserService = transportUserService;
    }

    // ✔️ CREATE USER
    @PostMapping
    public ResponseEntity<Users> createUser(@RequestBody Users user) {
        Users savedUser = transportUserService.createUser(user);
        return ResponseEntity.ok(savedUser);
    }

    // ✔️ GET ALL USERS
    @GetMapping
    public ResponseEntity<List<Users>> getAllUsers() {
        return ResponseEntity.ok(transportUserService.getAllUsers());
    }

    // ✔️ GET USER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Users> getUserById(@PathVariable Long id) {
        Users user = transportUserService.getUserById(id);
        return (user != null)
                ? ResponseEntity.ok(user)
                : ResponseEntity.notFound().build();
    }

    // ✔️ UPDATE USER
    @PutMapping("/{id}")
    public ResponseEntity<Users> updateUser(
            @PathVariable Long id,
            @RequestBody Users updatedUser
    ) {
        Users user = transportUserService.updateUser(id, updatedUser);
        return (user != null)
                ? ResponseEntity.ok(user)
                : ResponseEntity.notFound().build();
    }

    // ✔️ DELETE USER
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        boolean deleted = transportUserService.deleteUser(id);
        return (deleted)
                ? ResponseEntity.ok("User deleted")
                : ResponseEntity.notFound().build();
    }
}
