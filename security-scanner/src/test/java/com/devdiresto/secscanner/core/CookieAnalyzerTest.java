package com.devdiresto.secscanner.core;

import com.devdiresto.secscanner.model.Finding;
import com.devdiresto.secscanner.model.Severity;
import org.junit.jupiter.api.Test;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieAnalyzerTest {

    private final CookieAnalyzer analyzer = new CookieAnalyzer();

    @Test
    void flagsSessionCookieWithoutSecureAndHttpOnlyAsHighSeverity() {
        HttpHeaders headers = HttpHeaders.of(
                Map.of("Set-Cookie", List.of("PHPSESSID=abc123; Path=/")),
                (a, b) -> true);

        List<Finding> findings = analyzer.analyze(headers);

        assertTrue(findings.stream().anyMatch(f ->
                f.title().contains("Secure") && f.severity() == Severity.HIGH));
        assertTrue(findings.stream().anyMatch(f ->
                f.title().contains("HttpOnly") && f.severity() == Severity.HIGH));
    }

    @Test
    void doesNotFlagCookieWithAllSecurityFlags() {
        HttpHeaders headers = HttpHeaders.of(
                Map.of("Set-Cookie", List.of("PHPSESSID=abc123; Secure; HttpOnly; SameSite=Strict")),
                (a, b) -> true);

        List<Finding> findings = analyzer.analyze(headers);

        assertEquals(0, findings.size());
    }
}
