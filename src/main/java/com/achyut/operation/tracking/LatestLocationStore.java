package com.achyut.operation.tracking;

import java.time.Instant;
import java.util.Optional;

public interface LatestLocationStore {
    void put(Long deliveryId, Snapshot snapshot);
    Optional<Snapshot> get(Long deliveryId);

    record Snapshot(double latitude, double longitude, Double accuracyMeters, Double speedKph, Instant recordedAt) {}
}
