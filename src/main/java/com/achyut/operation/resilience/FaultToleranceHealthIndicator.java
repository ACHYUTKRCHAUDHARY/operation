package com.achyut.operation.resilience;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("faultTolerance")
@RequiredArgsConstructor
public class FaultToleranceHealthIndicator implements HealthIndicator {
    private final FaultToleranceExecutor executor;

    @Override
    public Health health() {
        var snapshots = executor.snapshots();
        boolean degraded = snapshots.values().stream().anyMatch(s -> "OPEN".equals(s.state()));
        Health.Builder builder = degraded ? Health.status("DEGRADED") : Health.up();
        return builder.withDetail("dependencies", snapshots).build();
    }
}
