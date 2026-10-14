package com.devdiresto.loadtester.report;

import com.devdiresto.loadtester.core.RequestOutcome;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

/**
 * Coletor thread-safe de métricas durante a execução do teste de carga.
 * <p>
 * Cada thread virtual chama {@link #record(RequestOutcome)} ao concluir uma
 * requisição; os cálculos agregados (percentis, taxa de erro) são feitos ao
 * final, em {@link #snapshot(long)}.
 */
public final class LoadTestStats {

    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final ConcurrentLinkedQueue<Long> latenciesNanos = new ConcurrentLinkedQueue<>();
    private final Map<Integer, LongAdder> statusCodeCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> errorCounts = new ConcurrentHashMap<>();

    public void record(RequestOutcome outcome) {
        latenciesNanos.add(outcome.latencyNanos());
        if (outcome.success()) {
            successCount.increment();
        } else {
            failureCount.increment();
        }
        if (outcome.statusCode() > 0) {
            statusCodeCounts.computeIfAbsent(outcome.statusCode(), k -> new LongAdder()).increment();
        }
        if (outcome.errorMessage() != null && outcome.statusCode() == 0) {
            errorCounts.computeIfAbsent(outcome.errorMessage(), k -> new LongAdder()).increment();
        }
    }

    public long totalRequests() {
        return successCount.sum() + failureCount.sum();
    }

    /**
     * Congela as métricas coletadas em um relatório calculado.
     *
     * @param wallClockNanos tempo total de parede (wall-clock) da execução, em nanossegundos
     */
    public Snapshot snapshot(long wallClockNanos) {
        long[] sorted = latenciesNanos.stream().mapToLong(Long::longValue).toArray();
        Arrays.sort(sorted);

        Map<Integer, Long> statusCodes = new TreeMap<>();
        statusCodeCounts.forEach((code, count) -> statusCodes.put(code, count.sum()));

        Map<String, Long> errors = new TreeMap<>();
        errorCounts.forEach((msg, count) -> errors.put(msg, count.sum()));

        return new Snapshot(
                successCount.sum(),
                failureCount.sum(),
                sorted,
                statusCodes,
                errors,
                wallClockNanos
        );
    }

    /**
     * Resultado final e imutável de uma execução, com os percentis já calculados.
     */
    public record Snapshot(
            long successCount,
            long failureCount,
            long[] sortedLatenciesNanos,
            Map<Integer, Long> statusCodeCounts,
            Map<String, Long> errorCounts,
            long wallClockNanos
    ) {

        public long totalRequests() {
            return successCount + failureCount;
        }

        public double errorRatePercent() {
            long total = totalRequests();
            return total == 0 ? 0.0 : (failureCount * 100.0) / total;
        }

        public double requestsPerSecond() {
            double seconds = wallClockNanos / 1_000_000_000.0;
            return seconds <= 0 ? 0.0 : totalRequests() / seconds;
        }

        public double percentileMillis(double percentile) {
            if (sortedLatenciesNanos.length == 0) {
                return 0.0;
            }
            int index = (int) Math.ceil(percentile / 100.0 * sortedLatenciesNanos.length) - 1;
            index = Math.clamp(index, 0, sortedLatenciesNanos.length - 1);
            return sortedLatenciesNanos[index] / 1_000_000.0;
        }

        public double minMillis() {
            return sortedLatenciesNanos.length == 0 ? 0.0 : sortedLatenciesNanos[0] / 1_000_000.0;
        }

        public double maxMillis() {
            return sortedLatenciesNanos.length == 0 ? 0.0
                    : sortedLatenciesNanos[sortedLatenciesNanos.length - 1] / 1_000_000.0;
        }

        public double averageMillis() {
            if (sortedLatenciesNanos.length == 0) {
                return 0.0;
            }
            long sum = 0;
            for (long value : sortedLatenciesNanos) {
                sum += value;
            }
            return (sum / (double) sortedLatenciesNanos.length) / 1_000_000.0;
        }
    }
}
