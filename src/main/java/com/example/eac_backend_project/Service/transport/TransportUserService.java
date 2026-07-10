package com.example.eac_backend_project.Service.transport;

import com.example.eac_backend_project.entity.transport.Users;
import com.example.eac_backend_project.repository.transport.TransportUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransportUserService {

    private final TransportUserRepository transportUserRepository;

    // Constructor injection
    public TransportUserService(TransportUserRepository transportUserRepository) {
        this.transportUserRepository = transportUserRepository;
    }

    // ✔️ Create User
    public Users createUser(Users user) {
        return transportUserRepository.save(user);
    }

    // ✔️ Get All Users
    public List<Users> getAllUsers() {
        return transportUserRepository.findAll();
    }

    // ✔️ Get User by ID
    public Users getUserById(Long id) {
        return transportUserRepository.findById(id)
                .orElse(null);
    }

    // ✔️ Update User
    public Users updateUser(Long id, Users updatedUser) {
        Users existing = transportUserRepository.findById(id).orElse(null);
        if (existing == null) return null;

        existing.setFullName(updatedUser.getFullName());
        existing.setEmail(updatedUser.getEmail());
        return transportUserRepository.save(existing);
    }

    // ✔️ Delete User
    public boolean deleteUser(Long id) {
        if (!transportUserRepository.existsById(id)) return false;
        transportUserRepository.deleteById(id);
        return true;
    }
}
