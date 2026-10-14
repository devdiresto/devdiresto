package com.devdiresto.loadtester.core;

import com.devdiresto.loadtester.config.LoadTestConfig;
import com.devdiresto.loadtester.report.LoadTestStats;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/**
 * Motor de teste de carga: dispara requisições HTTP concorrentes contra um
 * alvo, limitando a concorrência com threads virtuais (Project Loom) e um
 * semáforo, e agrega as métricas em {@link LoadTestStats}.
 * <p>
 * Suporta dois modos de execução:
 * <ul>
 *     <li><b>Por quantidade:</b> envia um número fixo de requisições.</li>
 *     <li><b>Por duração:</b> envia requisições continuamente até o tempo configurado se esgotar.</li>
 * </ul>
 */
public final class LoadTestEngine {

    private final LoadTestConfig config;
    private final HttpClient httpClient;

    public LoadTestEngine(LoadTestConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.requestTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * Executa o teste de carga de forma síncrona (bloqueia até terminar).
     *
     * @return estatísticas agregadas da execução
     */
    public LoadTestStats.Snapshot run() throws InterruptedException {
        LoadTestStats stats = new LoadTestStats();
        Semaphore concurrencyLimiter = new Semaphore(config.concurrency());

        Instant start = Instant.now();

        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            if (config.isDurationBased()) {
                runForDuration(virtualThreadExecutor, concurrencyLimiter, stats);
            } else {
                runForFixedCount(virtualThreadExecutor, concurrencyLimiter, stats);
            }
        }

        Duration elapsed = Duration.between(start, Instant.now());
        return stats.snapshot(elapsed.toNanos());
    }

    private void runForFixedCount(ExecutorService executor, Semaphore limiter, LoadTestStats stats)
            throws InterruptedException {
        long total = config.totalRequests();
        List<Future<?>> futures = new ArrayList<>((int) Math.min(total, Integer.MAX_VALUE));
        applyRampUpDelay(0, total);

        for (long i = 0; i < total; i++) {
            limiter.acquire();
            long requestIndex = i;
            futures.add(executor.submit(() -> {
                try {
                    stats.record(sendOneRequest());
                } finally {
                    limiter.release();
                }
            }));
            applyRampUpDelay(requestIndex, total);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                // sendOneRequest já captura exceções internamente; não deveria ocorrer.
            }
        }
    }

    private void runForDuration(ExecutorService executor, Semaphore limiter, LoadTestStats stats)
            throws InterruptedException {
        Instant deadline = Instant.now().plus(config.duration());
        List<Future<?>> futures = new CopyOnWriteArrayList<>();
        BooleanSupplier stillRunning = () -> Instant.now().isBefore(deadline);

        while (stillRunning.getAsBoolean()) {
            if (!limiter.tryAcquire(50, TimeUnit.MILLISECONDS)) {
                continue;
            }
            futures.add(executor.submit(() -> {
                try {
                    stats.record(sendOneRequest());
                } finally {
                    limiter.release();
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get(config.requestTimeout().toMillis() + 5_000, TimeUnit.MILLISECONDS);
            } catch (ExecutionException | TimeoutException e) {
                // requisições penduradas são contabilizadas via timeout do HttpClient.
            }
        }
    }

    /** Distribui o início das requisições ao longo da janela de ramp-up, se configurada. */
    private void applyRampUpDelay(long requestIndex, long total) throws InterruptedException {
        if (config.rampUp().isZero() || total <= 1) {
            return;
        }
        long delayMillisPerRequest = config.rampUp().toMillis() / total;
        if (delayMillisPerRequest > 0) {
            Thread.sleep(delayMillisPerRequest);
        }
    }

    private RequestOutcome sendOneRequest() {
        long startNanos = System.nanoTime();
        try {
            HttpRequest request = buildRequest();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long elapsedNanos = System.nanoTime() - startNanos;
            return RequestOutcome.ofSuccess(response.statusCode(), elapsedNanos);
        } catch (IOException e) {
            long elapsedNanos = System.nanoTime() - startNanos;
            return RequestOutcome.ofFailure(elapsedNanos, describeException(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long elapsedNanos = System.nanoTime() - startNanos;
            return RequestOutcome.ofFailure(elapsedNanos, "Interrompido");
        }
    }

    private HttpRequest buildRequest() {
        URI uri = config.targetUri();
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(config.requestTimeout());

        config.headers().forEach(builder::header);

        String body = config.body() == null ? "" : config.body();
        HttpRequest.BodyPublisher bodyPublisher = body.isEmpty()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);

        switch (config.method()) {
            case GET -> builder.GET();
            case HEAD -> builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
            case DELETE -> builder.DELETE();
            case POST -> builder.POST(bodyPublisher);
            case PUT -> builder.PUT(bodyPublisher);
            case PATCH -> builder.method("PATCH", bodyPublisher);
        }

        return builder.build();
    }

    private static String describeException(IOException e) {
        String simpleName = e.getClass().getSimpleName();
        String message = e.getMessage();
        return message == null ? simpleName : simpleName + ": " + message;
    }
}
