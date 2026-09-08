package com.achyut.operation.resilience;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FaultToleranceExecutorTest {
    private final FaultToleranceExecutor executor = configured();

    @AfterEach
    void tearDown() { executor.close(); }

    @Test
    void retriesSafeOperationThenReturnsSuccess() {
        AtomicInteger calls = new AtomicInteger();
        String result = executor.executeWithRetry("elastic", () -> {
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("temporary");
            return "ok";
        }, () -> "fallback");

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(2);
    }

    @Test
    void fallsBackAfterTimeout() {
        ReflectionTestUtils.setField(executor, "timeoutMs", 20L);
        String result = executor.execute("slow", () -> {
            Thread.sleep(200);
            return "late";
        }, () -> "fallback");

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void opensCircuitAfterConfiguredFailures() {
        ReflectionTestUtils.setField(executor, "failureThreshold", 2);
        AtomicInteger calls = new AtomicInteger();

        executor.execute("downstream", () -> { calls.incrementAndGet(); throw new IllegalStateException(); }, () -> "fallback");
        executor.execute("downstream", () -> { calls.incrementAndGet(); throw new IllegalStateException(); }, () -> "fallback");
        String third = executor.execute("downstream", () -> { calls.incrementAndGet(); return "unexpected"; }, () -> "fallback");

        assertThat(third).isEqualTo("fallback");
        assertThat(calls).hasValue(2);
        assertThat(executor.snapshots().get("downstream").state()).isEqualTo("OPEN");
    }

    private static FaultToleranceExecutor configured() {
        FaultToleranceExecutor executor = new FaultToleranceExecutor();
        ReflectionTestUtils.setField(executor, "timeoutMs", 200L);
        ReflectionTestUtils.setField(executor, "failureThreshold", 5);
        ReflectionTestUtils.setField(executor, "openSeconds", 30L);
        ReflectionTestUtils.setField(executor, "maxConcurrent", 4);
        ReflectionTestUtils.setField(executor, "maxAttempts", 2);
        ReflectionTestUtils.setField(executor, "retryBackoffMs", 1L);
        return executor;
    }
}
