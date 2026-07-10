package com.example.eac_backend_project.repository.transport;

import com.example.eac_backend_project.entity.transport.Fuel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FuelRepository extends JpaRepository<Fuel, Long> {

    List<Fuel> findByVehicle(String vehicle);

    List<Fuel> findByDriver(String driver);

    // SUM of cost
    @Query("SELECT SUM(f.cost) FROM Fuel f")
    Double sumTotalCost();

    // SUM of litres
    @Query("SELECT SUM(f.litres) FROM Fuel f")
    Double sumTotalLiters();
}
