package com.devdiresto.secscanner.core;

import com.devdiresto.secscanner.model.Finding;
import com.devdiresto.secscanner.model.Severity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orquestra os analisadores de reconhecimento/varredura passiva:
 * {@link HeaderAnalyzer}, {@link CookieAnalyzer}, {@link TlsAnalyzer} e
 * {@link ExposedPathScanner}.
 * <p>
 * Todo o scanner opera em modo <b>somente leitura</b>: apenas requisições
 * GET, sem tentativas de login, sem payloads de injeção e sem qualquer
 * ação que altere estado no servidor. Isso o torna seguro para rodar
 * contra sistemas em produção, dentro do escopo de reconhecimento e
 * varredura de vulnerabilidades conhecidas.
 */
public final class ScanEngine {

    private final HttpClient httpClient;
    private final Duration timeout;

    public ScanEngine(Duration timeout) {
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<Finding> run(URI target) throws Exception {
        List<Finding> findings = new ArrayList<>();

        HttpRequest request = HttpRequest.newBuilder(target)
                .GET()
                .timeout(timeout)
                .header("User-Agent", "devdiresto-security-scanner/1.0 (authorized-recon)")
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        findings.addAll(new HeaderAnalyzer().analyze(response.headers()));
        findings.addAll(new CookieAnalyzer().analyze(response.headers()));

        response.sslSession().ifPresent(session -> findings.addAll(new TlsAnalyzer().analyze(session)));

        findings.addAll(new ExposedPathScanner(httpClient, timeout).scan(target));

        findings.sort(Comparator.comparing(Finding::severity, Comparator.comparing(Severity::ordinal).reversed()));
        return findings;
    }
}
