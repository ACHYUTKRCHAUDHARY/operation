package com.achyut.operation.tracking;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.tracking.redis-enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryLatestLocationStore implements LatestLocationStore {
    private final ConcurrentHashMap<Long, Snapshot> locations = new ConcurrentHashMap<>();
    public void put(Long deliveryId, Snapshot snapshot) { locations.put(deliveryId, snapshot); }
    public Optional<Snapshot> get(Long deliveryId) { return Optional.ofNullable(locations.get(deliveryId)); }
}
