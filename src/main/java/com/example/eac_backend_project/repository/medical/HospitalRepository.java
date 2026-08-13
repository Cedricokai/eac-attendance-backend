package com.example.eac_backend_project.repository.medical;
import com.example.eac_backend_project.entity.medical.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<Hospital> findByAccessTokenAndStatus(String token, Hospital.HospitalStatus status);
}
