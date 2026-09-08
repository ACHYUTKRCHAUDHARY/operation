package com.achyut.operation.fleet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByStatus(Driver.DriverStatus status);
}
