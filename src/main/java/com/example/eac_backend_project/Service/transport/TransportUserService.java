package com.example.eac_backend_project.Service.transport;

import com.example.eac_backend_project.entity.transport.Users;
import com.example.eac_backend_project.repository.transport.TransportUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@org.springframework.transaction.annotation.Transactional
public class TransportUserService {
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.transport.TransportAccess access;

    private final TransportUserRepository transportUserRepository;

    // Constructor injection
    public TransportUserService(TransportUserRepository transportUserRepository) {
        this.transportUserRepository = transportUserRepository;
    }

    // ✔️ Create User
    public Users createUser(Users user) {
        access.require("TRANSPORT_SETTINGS");validate(user);user.setId(null);user.setArchivedAt(null);
        return transportUserRepository.save(user);
    }

    // ✔️ Get All Users
    public List<Users> getAllUsers() {
        access.requireEntry();return transportUserRepository.findAll().stream().filter(u->u.getArchivedAt()==null).toList();
    }

    // ✔️ Get User by ID
    public Users getUserById(Long id) {
        access.require("TRANSPORT_SETTINGS");
        return transportUserRepository.findById(id)
                .orElse(null);
    }

    // ✔️ Update User
    public Users updateUser(Long id, Users updatedUser) {
        access.require("TRANSPORT_SETTINGS");validate(updatedUser);
        Users existing = transportUserRepository.findById(id).orElse(null);
        if (existing == null) return null;

        existing.setFullName(updatedUser.getFullName());
        existing.setEmail(updatedUser.getEmail());
        existing.setRole(updatedUser.getRole());existing.setPhone(updatedUser.getPhone());
        return transportUserRepository.save(existing);
    }

    // ✔️ Delete User
    public boolean deleteUser(Long id) {
        access.require("TRANSPORT_SETTINGS");
        if (!transportUserRepository.existsById(id)) return false;
        var record=transportUserRepository.findById(id).orElseThrow();record.setArchivedAt(java.time.Instant.now());transportUserRepository.save(record);
        return true;
    }
    private void validate(Users user){if(user.getFullName()==null||user.getFullName().isBlank()||user.getFullName().length()>180)throw com.example.eac_backend_project.transport.TransportAccess.fail(400,"A team member name up to 180 characters is required");user.setFullName(user.getFullName().trim());if(user.getEmail()!=null&&!user.getEmail().isBlank()&&!user.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw com.example.eac_backend_project.transport.TransportAccess.fail(400,"Enter a valid email address");if(user.getRole()!=null&&user.getRole().length()>100||user.getPhone()!=null&&user.getPhone().length()>50)throw com.example.eac_backend_project.transport.TransportAccess.fail(400,"Team role or phone is too long");}
}
