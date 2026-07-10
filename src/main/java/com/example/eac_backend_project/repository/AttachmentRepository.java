package com.example.eac_backend_project.repository;

import com.example.eac_backend_project.entity.transport.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByVehicleId(Long vehicleId);

    void deleteByVehicleId(Long vehicleId);

    Long countByVehicleId(Long id);
}