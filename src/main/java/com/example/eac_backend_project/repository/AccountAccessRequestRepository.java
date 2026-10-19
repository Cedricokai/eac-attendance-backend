package com.example.eac_backend_project.repository;
import com.example.eac_backend_project.entity.portal.AccountAccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AccountAccessRequestRepository extends JpaRepository<AccountAccessRequest, Long> {
    boolean existsByEmployeeIdIgnoreCaseAndStatus(String employeeId, String status);
    boolean existsByWorkEmailIgnoreCaseAndStatus(String workEmail, String status);
    List<AccountAccessRequest> findAllByOrderByCreatedAtDesc();
}
