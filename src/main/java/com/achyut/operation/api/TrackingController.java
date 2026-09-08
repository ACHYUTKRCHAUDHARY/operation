package com.achyut.operation.api;

import com.achyut.operation.api.ApiModels.LocationRequest;
import com.achyut.operation.api.ApiModels.LocationView;
import com.achyut.operation.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries/{deliveryId}/locations")
@RequiredArgsConstructor
public class TrackingController {
    private final TrackingService trackingService;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    LocationView record(@PathVariable Long deliveryId, @Valid @RequestBody LocationRequest request) {
        return trackingService.record(deliveryId, request);
    }

    @GetMapping("/latest")
    LocationView latest(@PathVariable Long deliveryId) { return trackingService.latest(deliveryId); }

    @GetMapping("/health")
    TrackingService.TrackingHealth health(@PathVariable Long deliveryId) { return trackingService.health(deliveryId); }

    @GetMapping
    List<LocationView> history(@PathVariable Long deliveryId) { return trackingService.history(deliveryId); }
}
