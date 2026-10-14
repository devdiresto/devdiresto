package com.devdiresto.loadtester.config;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuração imutável de uma execução de teste de carga.
 * <p>
 * Construída via {@link Builder}, garantindo que a instância final seja
 * válida e thread-safe (apenas leitura durante a execução do teste).
 */
public final class LoadTestConfig {

    private final URI targetUri;
    private final HttpMethod method;
    private final int concurrency;
    private final Long totalRequests; // null quando o modo é por duração
    private final Duration duration;  // null quando o modo é por quantidade
    private final Duration requestTimeout;
    private final Duration rampUp;
    private final Map<String, String> headers;
    private final String body;

    private LoadTestConfig(Builder builder) {
        this.targetUri = Objects.requireNonNull(builder.targetUri, "URL de destino é obrigatória");
        this.method = builder.method;
        this.concurrency = builder.concurrency;
        this.totalRequests = builder.totalRequests;
        this.duration = builder.duration;
        this.requestTimeout = builder.requestTimeout;
        this.rampUp = builder.rampUp;
        this.headers = Map.copyOf(builder.headers);
        this.body = builder.body;

        if (totalRequests == null && duration == null) {
            throw new IllegalStateException("Defina --requests ou --duration");
        }
        if (concurrency < 1) {
            throw new IllegalStateException("Concorrência deve ser >= 1");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public URI targetUri() {
        return targetUri;
    }

    public HttpMethod method() {
        return method;
    }

    public int concurrency() {
        return concurrency;
    }

    /** @return número total de requisições a enviar, ou {@code null} se o modo é por duração. */
    public Long totalRequests() {
        return totalRequests;
    }

    /** @return duração total do teste, ou {@code null} se o modo é por quantidade de requisições. */
    public Duration duration() {
        return duration;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public Duration rampUp() {
        return rampUp;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String body() {
        return body;
    }

    public boolean isDurationBased() {
        return duration != null;
    }

    public static final class Builder {
        private URI targetUri;
        private HttpMethod method = HttpMethod.GET;
        private int concurrency = 50;
        private Long totalRequests;
        private Duration duration;
        private Duration requestTimeout = Duration.ofSeconds(10);
        private Duration rampUp = Duration.ZERO;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String body;

        public Builder targetUri(URI targetUri) {
            this.targetUri = targetUri;
            return this;
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder concurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        public Builder totalRequests(long totalRequests) {
            this.totalRequests = totalRequests;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder rampUp(Duration rampUp) {
            this.rampUp = rampUp;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public LoadTestConfig build() {
            return new LoadTestConfig(this);
        }
    }
}
