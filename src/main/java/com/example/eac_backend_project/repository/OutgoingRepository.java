package com.example.eac_backend_project.repository;


import com.example.eac_backend_project.entity.inventory.Outgoing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutgoingRepository extends JpaRepository<Outgoing, Long> {
}