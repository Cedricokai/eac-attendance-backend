package com.example.eac_backend_project.repository.transport;

import com.example.eac_backend_project.entity.transport.Vehicles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicles, Long> {
    Optional<Vehicles> findByDvla(String dvla);
    List<Vehicles> findByStatus(String status);
    List<Vehicles> findByAssignedDriver(String assignedDriver);

    @Query("SELECT v FROM Vehicles v WHERE v.name LIKE %:searchTerm% OR v.dvla LIKE %:searchTerm% OR v.type LIKE %:searchTerm%")
    List<Vehicles> searchVehicles(@Param("searchTerm") String searchTerm);

    boolean existsByDvla(String dvla);
}