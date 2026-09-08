package com.achyut.operation.fleet;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fleet")
@RequiredArgsConstructor
public class FleetController {
    private final FleetService fleetService;

    @PostMapping("/drivers") @ResponseStatus(HttpStatus.CREATED)
    public Driver createDriver(@Valid @RequestBody FleetService.DriverRequest request) { return fleetService.createDriver(request); }

    @GetMapping("/drivers")
    public List<Driver> drivers(@RequestParam(required = false) Boolean available) {
        return Boolean.TRUE.equals(available) ? fleetService.availableDrivers() : fleetService.drivers();
    }

    @PostMapping("/vehicles") @ResponseStatus(HttpStatus.CREATED)
    public Vehicle createVehicle(@Valid @RequestBody FleetService.VehicleRequest request) { return fleetService.createVehicle(request); }

    @GetMapping("/vehicles")
    public List<Vehicle> vehicles(@RequestParam(required = false) Boolean available) {
        return Boolean.TRUE.equals(available) ? fleetService.availableVehicles() : fleetService.vehicles();
    }

    @PostMapping("/deliveries/{deliveryId}/assign")
    public FleetService.AssignmentView assign(@PathVariable Long deliveryId, @RequestBody FleetService.AssignmentRequest request) {
        return fleetService.assign(deliveryId, request);
    }
}
