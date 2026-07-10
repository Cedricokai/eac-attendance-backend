package com.example.eac_backend_project.repository.transport;

import com.example.eac_backend_project.entity.transport.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanaceRepository extends JpaRepository<Maintenance, Long> {

    List<Maintenance> findByVehicles(String vehicles);

    @Query("SELECT SUM(m.cost) FROM Maintenance m")
    Double sumTotalCost();
}
