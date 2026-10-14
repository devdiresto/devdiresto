package com.devdiresto.secscanner.core;

import com.devdiresto.secscanner.model.Finding;
import com.devdiresto.secscanner.model.Severity;

import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Analisa os headers de resposta HTTP em busca de proteções de segurança
 * ausentes ou mal configuradas (OWASP Secure Headers Project).
 * <p>
 * Puramente analítico: não envia nenhuma requisição, apenas interpreta
 * headers já obtidos por quem chama.
 */
public final class HeaderAnalyzer {

    public List<Finding> analyze(HttpHeaders headers) {
        List<Finding> findings = new ArrayList<>();

        checkMissing(headers, "Strict-Transport-Security", Severity.MEDIUM,
                "HSTS ausente",
                "Sem Strict-Transport-Security, o navegador pode ser induzido a acessar o " +
                        "site via HTTP, abrindo espaço para downgrade/MITM.",
                "Adicione 'Strict-Transport-Security: max-age=31536000; includeSubDomains'.")
                .ifPresent(findings::add);

        checkMissing(headers, "X-Content-Type-Options", Severity.LOW,
                "X-Content-Type-Options ausente",
                "Sem esse header, navegadores antigos podem tentar 'adivinhar' o tipo de " +
                        "conteúdo (MIME sniffing), o que pode ser abusado em certos ataques de XSS.",
                "Adicione 'X-Content-Type-Options: nosniff'.")
                .ifPresent(findings::add);

        checkMissing(headers, "X-Frame-Options", Severity.MEDIUM,
                "X-Frame-Options ausente (risco de clickjacking)",
                "Sem X-Frame-Options (ou uma CSP com frame-ancestors), a página pode ser " +
                        "embutida em um <iframe> de outro site e usada em ataques de clickjacking.",
                "Adicione 'X-Frame-Options: DENY' ou uma CSP com 'frame-ancestors 'none''.")
                .ifPresent(findings::add);

        checkMissing(headers, "Content-Security-Policy", Severity.MEDIUM,
                "Content-Security-Policy ausente",
                "Sem CSP, o navegador não tem uma camada extra de defesa contra XSS e " +
                        "injeção de scripts/recursos de origens não confiáveis.",
                "Defina uma Content-Security-Policy restritiva (ao menos default-src 'self').")
                .ifPresent(findings::add);

        checkMissing(headers, "Referrer-Policy", Severity.LOW,
                "Referrer-Policy ausente",
                "Sem essa política, URLs internas (às vezes com tokens/parâmetros sensíveis) " +
                        "podem ser enviadas para sites de terceiros via header Referer.",
                "Adicione 'Referrer-Policy: strict-origin-when-cross-origin' (ou mais restritivo).")
                .ifPresent(findings::add);

        headers.firstValue("Server").ifPresent(value -> {
            if (!value.isBlank()) {
                findings.add(new Finding(Severity.INFO, "Fingerprinting",
                        "Header 'Server' expõe o software/versão do servidor",
                        "Server: " + value,
                        "Considere ocultar ou genericizar o header Server (ex.: via configuração " +
                                "do Apache/Nginx) para dificultar o reconhecimento de versões vulneráveis."));
            }
        });

        headers.firstValue("X-Powered-By").ifPresent(value -> findings.add(new Finding(Severity.INFO,
                "Fingerprinting",
                "Header 'X-Powered-By' expõe a tecnologia do backend",
                "X-Powered-By: " + value,
                "Desative esse header (ex.: 'expose_php = Off' no php.ini) para não revelar a " +
                        "versão exata do PHP/framework, que pode ser usada para buscar CVEs conhecidas.")));

        return findings;
    }

    private Optional<Finding> checkMissing(HttpHeaders headers, String headerName, Severity severity,
                                            String title, String detail, String recommendation) {
        if (headers.firstValue(headerName).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new Finding(severity, "Headers de segurança", title, detail, recommendation));
    }
}
