package com.achyut.operation.fleet;

import com.achyut.operation.delivery.Delivery;
import com.achyut.operation.delivery.DeliveryRepository;
import com.achyut.operation.service.OperationsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class FleetService {
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final DeliveryRepository deliveries;
    private final OperationsService operationsService;

    public Driver createDriver(DriverRequest r) {
        return drivers.save(Driver.builder().name(r.name()).phone(r.phone()).licenseNumber(r.licenseNumber())
            .licenseExpiry(r.licenseExpiry()).status(Driver.DriverStatus.AVAILABLE).build());
    }

    public Vehicle createVehicle(VehicleRequest r) {
        return vehicles.save(Vehicle.builder().registrationNumber(r.registrationNumber()).vehicleType(r.vehicleType())
            .capacityDescription(r.capacityDescription()).insuranceExpiry(r.insuranceExpiry()).permitExpiry(r.permitExpiry())
            .serviceDueAt(r.serviceDueAt()).status(Vehicle.VehicleStatus.AVAILABLE).build());
    }

    public List<Driver> drivers() { return drivers.findAll(); }
    public List<Vehicle> vehicles() { return vehicles.findAll(); }
    public List<Driver> availableDrivers() { return drivers.findByStatus(Driver.DriverStatus.AVAILABLE); }
    public List<Vehicle> availableVehicles() { return vehicles.findByStatus(Vehicle.VehicleStatus.AVAILABLE); }

    public AssignmentView assign(Long deliveryId, AssignmentRequest r) {
        Delivery delivery = deliveries.findById(deliveryId).orElseThrow(() -> new NoSuchElementException("Delivery not found"));
        Driver driver = drivers.findById(r.driverId()).orElseThrow(() -> new NoSuchElementException("Driver not found"));
        Vehicle vehicle = vehicles.findById(r.vehicleId()).orElseThrow(() -> new NoSuchElementException("Vehicle not found"));
        if (driver.getStatus() != Driver.DriverStatus.AVAILABLE) throw new IllegalStateException("Driver is not available");
        if (vehicle.getStatus() != Vehicle.VehicleStatus.AVAILABLE) throw new IllegalStateException("Vehicle is not available");
        LocalDate today = LocalDate.now();
        if (driver.getLicenseExpiry() != null && driver.getLicenseExpiry().isBefore(today)) throw new IllegalStateException("Driver license is expired");
        if (vehicle.getInsuranceExpiry() != null && vehicle.getInsuranceExpiry().isBefore(today)) throw new IllegalStateException("Vehicle insurance is expired");
        if (vehicle.getPermitExpiry() != null && vehicle.getPermitExpiry().isBefore(today)) throw new IllegalStateException("Vehicle permit is expired");

        driver.setStatus(Driver.DriverStatus.ON_DELIVERY);
        vehicle.setStatus(Vehicle.VehicleStatus.ASSIGNED);
        delivery.setDriverName(driver.getName());
        delivery.setDriverPhone(driver.getPhone());
        delivery.setVehicleNumber(vehicle.getRegistrationNumber());
        delivery.setVehicleType(vehicle.getVehicleType());
        delivery.setStatus(Delivery.DeliveryStatus.VEHICLE_ASSIGNED);
        operationsService.audit("DELIVERY", deliveryId, "FLEET_ASSIGNED", r.actor() == null ? "system" : r.actor(),
            driver.getName() + " / " + vehicle.getRegistrationNumber());
        return new AssignmentView(deliveryId, driver.getId(), driver.getName(), vehicle.getId(), vehicle.getRegistrationNumber(), delivery.getStatus());
    }

    public record DriverRequest(String name, String phone, String licenseNumber, LocalDate licenseExpiry) {}
    public record VehicleRequest(String registrationNumber, String vehicleType, String capacityDescription, LocalDate insuranceExpiry, LocalDate permitExpiry, LocalDate serviceDueAt) {}
    public record AssignmentRequest(Long driverId, Long vehicleId, String actor) {}
    public record AssignmentView(Long deliveryId, Long driverId, String driverName, Long vehicleId, String vehicleNumber, Delivery.DeliveryStatus status) {}
}
