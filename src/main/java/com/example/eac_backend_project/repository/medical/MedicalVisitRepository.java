package com.example.eac_backend_project.repository.medical;
import com.example.eac_backend_project.entity.medical.MedicalVisit;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface MedicalVisitRepository extends JpaRepository<MedicalVisit, Long> {
    List<MedicalVisit> findByEmployeeIdOrderByVisitDateDescCreatedAtDesc(Long employeeId);
    @Query("select v from MedicalVisit v join fetch v.employee e join fetch v.hospital h where " +
      "(:status is null or v.status=:status) and (:search is null or lower(concat(e.firstName,' ',e.lastName)) like lower(concat('%',:search,'%')) or lower(e.tagNumber) like lower(concat('%',:search,'%')) or lower(h.name) like lower(concat('%',:search,'%'))) order by v.createdAt desc")
    List<MedicalVisit> search(@Param("status") MedicalVisit.VisitStatus status, @Param("search") String search);
}
