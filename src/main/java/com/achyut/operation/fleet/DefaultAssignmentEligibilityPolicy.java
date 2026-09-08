package com.achyut.operation.fleet;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DefaultAssignmentEligibilityPolicy implements AssignmentEligibilityPolicy {
    @Override
    public void validate(Driver driver, Vehicle vehicle) {
        if (driver.getStatus() != Driver.DriverStatus.AVAILABLE) throw new IllegalStateException("Driver is not available");
        if (vehicle.getStatus() != Vehicle.VehicleStatus.AVAILABLE) throw new IllegalStateException("Vehicle is not available");
        LocalDate today = LocalDate.now();
        if (driver.getLicenseExpiry() != null && driver.getLicenseExpiry().isBefore(today)) throw new IllegalStateException("Driver license is expired");
        if (vehicle.getInsuranceExpiry() != null && vehicle.getInsuranceExpiry().isBefore(today)) throw new IllegalStateException("Vehicle insurance is expired");
        if (vehicle.getPermitExpiry() != null && vehicle.getPermitExpiry().isBefore(today)) throw new IllegalStateException("Vehicle permit is expired");
        if (vehicle.getServiceDueAt() != null && vehicle.getServiceDueAt().isBefore(today)) throw new IllegalStateException("Vehicle service is overdue");
    }
}
