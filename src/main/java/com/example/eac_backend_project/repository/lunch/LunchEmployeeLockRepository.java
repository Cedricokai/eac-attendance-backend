package com.example.eac_backend_project.repository.lunch;

import com.example.eac_backend_project.entity.attendance.Employee;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface LunchEmployeeLockRepository extends Repository<Employee, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> lockEmployee(@Param("id") Long id);
}
