package com.devdiresto.secscanner.model;

/**
 * Um achado individual do scanner: uma observação de segurança sobre o alvo,
 * já classificada por severidade e categoria.
 *
 * @param severity     impacto potencial do achado
 * @param category     categoria (ex.: "Headers", "TLS", "Exposição de arquivos")
 * @param title        resumo de uma linha
 * @param detail       detalhe/evidência (ex.: valor do header, caminho encontrado)
 * @param recommendation como corrigir
 */
public record Finding(Severity severity, String category, String title, String detail, String recommendation) {
}
