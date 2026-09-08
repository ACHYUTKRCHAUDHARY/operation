package com.achyut.operation.tracking;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.tracking.redis-enabled", havingValue = "true")
public class RedisLatestLocationStore implements LatestLocationStore {
    private final StringRedisTemplate redis;

    public RedisLatestLocationStore(StringRedisTemplate redis) { this.redis = redis; }

    public void put(Long deliveryId, Snapshot s) {
        String value = s.latitude() + "," + s.longitude() + "," + nullable(s.accuracyMeters()) + "," + nullable(s.speedKph()) + "," + s.recordedAt();
        redis.opsForValue().set(key(deliveryId), value, Duration.ofHours(24));
    }

    public Optional<Snapshot> get(Long deliveryId) {
        String value = redis.opsForValue().get(key(deliveryId));
        if (value == null) return Optional.empty();
        String[] p = value.split(",", -1);
        return Optional.of(new Snapshot(Double.parseDouble(p[0]), Double.parseDouble(p[1]), parseNullable(p[2]), parseNullable(p[3]), Instant.parse(p[4])));
    }

    private static String key(Long id) { return "operation:delivery:" + id + ":latest-location"; }
    private static String nullable(Double value) { return value == null ? "" : value.toString(); }
    private static Double parseNullable(String value) { return value.isBlank() ? null : Double.parseDouble(value); }
}
