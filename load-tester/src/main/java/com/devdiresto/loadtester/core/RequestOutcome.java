package com.devdiresto.loadtester.core;

/**
 * Resultado de uma única requisição HTTP disparada pelo motor de carga.
 *
 * @param statusCode    código HTTP retornado (0 se a requisição falhou antes de obter resposta)
 * @param latencyNanos  tempo total da requisição, em nanossegundos
 * @param success       {@code true} se a requisição completou com status 2xx ou 3xx
 * @param errorMessage  mensagem de erro (ex.: timeout, conexão recusada), ou {@code null} em caso de sucesso
 */
public record RequestOutcome(int statusCode, long latencyNanos, boolean success, String errorMessage) {

    public static RequestOutcome ofSuccess(int statusCode, long latencyNanos) {
        boolean ok = statusCode >= 200 && statusCode < 400;
        return new RequestOutcome(statusCode, latencyNanos, ok, ok ? null : "HTTP " + statusCode);
    }

    public static RequestOutcome ofFailure(long latencyNanos, String errorMessage) {
        return new RequestOutcome(0, latencyNanos, false, errorMessage);
    }
}
