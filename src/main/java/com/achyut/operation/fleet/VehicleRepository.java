package com.achyut.operation.fleet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatus(Vehicle.VehicleStatus status);
}
