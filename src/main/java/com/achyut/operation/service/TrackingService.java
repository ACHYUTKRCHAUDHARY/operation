package com.achyut.operation.service;

import com.achyut.operation.api.ApiModels.LocationRequest;
import com.achyut.operation.api.ApiModels.LocationView;
import com.achyut.operation.delivery.*;
import com.achyut.operation.tracking.LatestLocationStore;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class TrackingService {
    private final DeliveryRepository deliveryRepository;
    private final LocationUpdateRepository locationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OperationsService operationsService;
    private final LatestLocationStore latestLocationStore;

    @Value("${app.tracking.near-destination-meters:500}")
    private double nearDestinationMeters;
    @Value("${app.tracking.stale-minutes:25}")
    private long staleMinutes;

    public LocationView record(Long deliveryId, LocationRequest request) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(() -> new NoSuchElementException("Delivery not found: " + deliveryId));
        LocationUpdate update = LocationUpdate.builder()
            .delivery(delivery).latitude(request.latitude()).longitude(request.longitude())
            .accuracyMeters(request.accuracyMeters()).speedKph(request.speedKph()).build();
        update = locationRepository.save(update);
        latestLocationStore.put(deliveryId, new LatestLocationStore.Snapshot(update.getLatitude(), update.getLongitude(), update.getAccuracyMeters(), update.getSpeedKph(), update.getRecordedAt()));
        double distance = distanceMeters(request.latitude(), request.longitude(), delivery.getDestinationLatitude(), delivery.getDestinationLongitude());

        if (distance <= nearDestinationMeters && delivery.getStatus() == Delivery.DeliveryStatus.IN_TRANSIT) {
            delivery.setStatus(Delivery.DeliveryStatus.NEAR_DESTINATION);
            operationsService.audit("DELIVERY", deliveryId, "GEOFENCE_ENTERED", "system", "Vehicle entered " + Math.round(nearDestinationMeters) + "m destination geofence");
        }

        LocationView view = view(update, distance);
        messagingTemplate.convertAndSend("/topic/deliveries/" + deliveryId + "/location", view);
        return view;
    }

    public LocationView latest(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(() -> new NoSuchElementException("Delivery not found: " + deliveryId));
        var cached = latestLocationStore.get(deliveryId);
        if (cached.isPresent()) {
            var s = cached.get();
            double distance = distanceMeters(s.latitude(), s.longitude(), delivery.getDestinationLatitude(), delivery.getDestinationLongitude());
            return new LocationView(null, deliveryId, s.latitude(), s.longitude(), s.accuracyMeters(), s.speedKph(), distance, s.recordedAt());
        }
        LocationUpdate update = locationRepository.findTopByDeliveryIdOrderByRecordedAtDesc(deliveryId).orElseThrow(() -> new NoSuchElementException("No location received yet"));
        return view(update, distanceMeters(update.getLatitude(), update.getLongitude(), delivery.getDestinationLatitude(), delivery.getDestinationLongitude()));
    }

    public TrackingHealth health(Long deliveryId) {
        LocationView latest = latest(deliveryId);
        long ageMinutes = Math.max(0, Duration.between(latest.recordedAt(), Instant.now()).toMinutes());
        return new TrackingHealth(deliveryId, ageMinutes > staleMinutes, ageMinutes, staleMinutes, latest.recordedAt());
    }

    public List<LocationView> history(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(() -> new NoSuchElementException("Delivery not found: " + deliveryId));
        return locationRepository.findTop100ByDeliveryIdOrderByRecordedAtDesc(deliveryId).stream()
            .map(u -> view(u, distanceMeters(u.getLatitude(), u.getLongitude(), delivery.getDestinationLatitude(), delivery.getDestinationLongitude())))
            .toList();
    }

    private LocationView view(LocationUpdate u, double distance) {
        return new LocationView(u.getId(), u.getDelivery().getId(), u.getLatitude(), u.getLongitude(), u.getAccuracyMeters(), u.getSpeedKph(), distance, u.getRecordedAt());
    }

    public record TrackingHealth(Long deliveryId, boolean stale, long ageMinutes, long staleAfterMinutes, Instant lastSeenAt) {}

    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000;
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1), dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
