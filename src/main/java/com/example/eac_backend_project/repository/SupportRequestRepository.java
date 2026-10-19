package com.example.eac_backend_project.repository;
import com.example.eac_backend_project.entity.portal.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> { List<SupportRequest> findAllByOrderByCreatedAtDesc(); }
