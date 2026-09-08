package com.achyut.operation.resilience;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class FaultToleranceExecutor {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentMap<String, DependencyState> states = new ConcurrentHashMap<>();

    @Value("${app.fault-tolerance.timeout-ms:800}")
    private long timeoutMs;
    @Value("${app.fault-tolerance.failure-threshold:5}")
    private int failureThreshold;
    @Value("${app.fault-tolerance.open-seconds:30}")
    private long openSeconds;
    @Value("${app.fault-tolerance.max-concurrent:20}")
    private int maxConcurrent;

    public <T> T execute(String dependency, Callable<T> operation, Supplier<T> fallback) {
        DependencyState state = states.computeIfAbsent(dependency, ignored -> new DependencyState(maxConcurrent));
        if (state.isOpen()) return fallback.get();
        boolean acquired = state.bulkhead.tryAcquire();
        if (!acquired) return fallback.get();
        try {
            Future<T> future = executor.submit(operation);
            try {
                T result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                state.success();
                return result;
            } catch (TimeoutException e) {
                future.cancel(true);
                state.failure(failureThreshold, openSeconds);
                return fallback.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                state.failure(failureThreshold, openSeconds);
                return fallback.get();
            } catch (ExecutionException e) {
                state.failure(failureThreshold, openSeconds);
                return fallback.get();
            }
        } finally {
            state.bulkhead.release();
        }
    }

    public void run(String dependency, Runnable operation) {
        execute(dependency, () -> { operation.run(); return Boolean.TRUE; }, () -> Boolean.FALSE);
    }

    public Map<String, CircuitSnapshot> snapshots() {
        return states.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().snapshot()));
    }

    @PreDestroy
    void close() { executor.close(); }

    public record CircuitSnapshot(String state, int consecutiveFailures, Instant openUntil, int availablePermits) {}

    private static final class DependencyState {
        private final AtomicInteger failures = new AtomicInteger();
        private final Semaphore bulkhead;
        private volatile Instant openUntil;

        private DependencyState(int maxConcurrent) { this.bulkhead = new Semaphore(Math.max(1, maxConcurrent)); }

        private boolean isOpen() {
            Instant until = openUntil;
            if (until == null) return false;
            if (Instant.now().isBefore(until)) return true;
            openUntil = null;
            failures.set(0);
            return false;
        }

        private void success() { failures.set(0); openUntil = null; }

        private void failure(int threshold, long openSeconds) {
            if (failures.incrementAndGet() >= threshold) openUntil = Instant.now().plus(Duration.ofSeconds(openSeconds));
        }

        private CircuitSnapshot snapshot() {
            return new CircuitSnapshot(isOpen() ? "OPEN" : "CLOSED", failures.get(), openUntil, bulkhead.availablePermits());
        }
    }
}
