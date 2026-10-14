package com.devdiresto.loadtester.report;

import com.devdiresto.loadtester.core.RequestOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadTestStatsTest {

    @Test
    void computesSuccessAndFailureCounts() {
        LoadTestStats stats = new LoadTestStats();
        stats.record(RequestOutcome.ofSuccess(200, 10_000_000));
        stats.record(RequestOutcome.ofSuccess(200, 20_000_000));
        stats.record(RequestOutcome.ofFailure(5_000_000, "Connection refused"));

        LoadTestStats.Snapshot snapshot = stats.snapshot(1_000_000_000L);

        assertEquals(3, snapshot.totalRequests());
        assertEquals(2, snapshot.successCount());
        assertEquals(1, snapshot.failureCount());
        assertTrue(snapshot.errorRatePercent() > 0);
    }

    @Test
    void computesLatencyPercentilesFromSortedSamples() {
        LoadTestStats stats = new LoadTestStats();
        for (int i = 1; i <= 100; i++) {
            stats.record(RequestOutcome.ofSuccess(200, i * 1_000_000L)); // i milissegundos
        }

        LoadTestStats.Snapshot snapshot = stats.snapshot(1_000_000_000L);

        assertEquals(1.0, snapshot.minMillis(), 0.001);
        assertEquals(100.0, snapshot.maxMillis(), 0.001);
        assertEquals(50.0, snapshot.percentileMillis(50), 0.001);
        assertEquals(95.0, snapshot.percentileMillis(95), 0.001);
        assertEquals(99.0, snapshot.percentileMillis(99), 0.001);
    }

    @Test
    void returnsZeroedMetricsWhenNoRequestsRecorded() {
        LoadTestStats stats = new LoadTestStats();
        LoadTestStats.Snapshot snapshot = stats.snapshot(1_000_000_000L);

        assertEquals(0, snapshot.totalRequests());
        assertEquals(0.0, snapshot.errorRatePercent());
        assertEquals(0.0, snapshot.averageMillis());
    }

    @Test
    void requestsPerSecondReflectsWallClockDuration() {
        LoadTestStats stats = new LoadTestStats();
        for (int i = 0; i < 100; i++) {
            stats.record(RequestOutcome.ofSuccess(200, 1_000_000L));
        }

        // 100 requisições em 2 segundos de parede => 50 req/s
        LoadTestStats.Snapshot snapshot = stats.snapshot(2_000_000_000L);
        assertEquals(50.0, snapshot.requestsPerSecond(), 0.001);
    }
}
