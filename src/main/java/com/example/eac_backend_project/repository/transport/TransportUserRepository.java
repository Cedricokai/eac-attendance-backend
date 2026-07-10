package com.example.eac_backend_project.repository.transport;

import com.example.eac_backend_project.entity.transport.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransportUserRepository extends JpaRepository<Users,Long> {
    
}
