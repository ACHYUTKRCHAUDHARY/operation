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
    @Value("${app.fault-tolerance.max-attempts:2}")
    private int maxAttempts;
    @Value("${app.fault-tolerance.retry-backoff-ms:75}")
    private long retryBackoffMs;

    public <T> T execute(String dependency, Callable<T> operation, Supplier<T> fallback) {
        return executeInternal(dependency, operation, fallback, 1);
    }

    public <T> T executeWithRetry(String dependency, Callable<T> operation, Supplier<T> fallback) {
        return executeInternal(dependency, operation, fallback, Math.max(1, maxAttempts));
    }

    public void run(String dependency, Runnable operation) {
        execute(dependency, () -> { operation.run(); return Boolean.TRUE; }, () -> Boolean.FALSE);
    }

    public void runWithRetry(String dependency, Runnable operation) {
        executeWithRetry(dependency, () -> { operation.run(); return Boolean.TRUE; }, () -> Boolean.FALSE);
    }

    private <T> T executeInternal(String dependency, Callable<T> operation, Supplier<T> fallback, int attempts) {
        DependencyState state = states.computeIfAbsent(dependency, ignored -> new DependencyState(maxConcurrent));
        if (state.isOpen()) return fallback.get();
        if (!state.bulkhead.tryAcquire()) return fallback.get();
        try {
            for (int attempt = 1; attempt <= attempts; attempt++) {
                AttemptResult<T> result = invoke(operation);
                if (result.success()) {
                    state.success();
                    return result.value();
                }
                if (result.interrupted()) {
                    state.failure(failureThreshold, openSeconds);
                    return fallback.get();
                }
                if (attempt < attempts && !sleepBackoff(attempt)) {
                    state.failure(failureThreshold, openSeconds);
                    return fallback.get();
                }
            }
            state.failure(failureThreshold, openSeconds);
            return fallback.get();
        } finally {
            state.bulkhead.release();
        }
    }

    private <T> AttemptResult<T> invoke(Callable<T> operation) {
        Future<T> future = executor.submit(operation);
        try {
            return AttemptResult.success(future.get(timeoutMs, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
            return AttemptResult.failure(false);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            return AttemptResult.failure(true);
        } catch (ExecutionException e) {
            return AttemptResult.failure(false);
        }
    }

    private boolean sleepBackoff(int attempt) {
        try {
            Thread.sleep(Math.max(0, retryBackoffMs) * attempt);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public Map<String, CircuitSnapshot> snapshots() {
        return states.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().snapshot()));
    }

    @PreDestroy
    void close() { executor.close(); }

    public record CircuitSnapshot(String state, int consecutiveFailures, Instant openUntil, int availablePermits) {}

    private record AttemptResult<T>(boolean success, boolean interrupted, T value) {
        private static <T> AttemptResult<T> success(T value) { return new AttemptResult<>(true, false, value); }
        private static <T> AttemptResult<T> failure(boolean interrupted) { return new AttemptResult<>(false, interrupted, null); }
    }

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
