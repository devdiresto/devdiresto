package com.devdiresto.loadtester.config;

/**
 * Métodos HTTP suportados pela ferramenta.
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    HEAD,
    PATCH;

    /**
     * Converte uma string (case-insensitive) para o enum correspondente.
     *
     * @param value texto informado pelo usuário, ex.: "get", "POST"
     * @return o {@link HttpMethod} correspondente
     * @throws IllegalArgumentException se o método não for suportado
     */
    public static HttpMethod fromString(String value) {
        try {
            return HttpMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Método HTTP não suportado: " + value, ex);
        }
    }
}
