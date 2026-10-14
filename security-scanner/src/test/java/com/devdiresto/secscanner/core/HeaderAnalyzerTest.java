package com.devdiresto.secscanner.core;

import com.devdiresto.secscanner.model.Finding;
import org.junit.jupiter.api.Test;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderAnalyzerTest {

    private final HeaderAnalyzer analyzer = new HeaderAnalyzer();

    @Test
    void flagsAllMissingSecurityHeaders() {
        HttpHeaders headers = HttpHeaders.of(Map.of("Content-Type", List.of("text/html")), (a, b) -> true);

        List<Finding> findings = analyzer.analyze(headers);

        assertTrue(containsTitleContaining(findings, "HSTS ausente"));
        assertTrue(containsTitleContaining(findings, "X-Frame-Options ausente"));
        assertTrue(containsTitleContaining(findings, "Content-Security-Policy ausente"));
    }

    @Test
    void doesNotFlagHeadersThatArePresent() {
        HttpHeaders headers = HttpHeaders.of(Map.of(
                "Strict-Transport-Security", List.of("max-age=31536000"),
                "X-Frame-Options", List.of("DENY"),
                "Content-Security-Policy", List.of("default-src 'self'"),
                "X-Content-Type-Options", List.of("nosniff"),
                "Referrer-Policy", List.of("strict-origin-when-cross-origin")
        ), (a, b) -> true);

        List<Finding> findings = analyzer.analyze(headers);

        assertFalse(containsTitleContaining(findings, "HSTS ausente"));
        assertFalse(containsTitleContaining(findings, "X-Frame-Options ausente"));
        assertFalse(containsTitleContaining(findings, "Content-Security-Policy ausente"));
    }

    @Test
    void flagsServerAndXPoweredByAsInfoLevelFingerprinting() {
        HttpHeaders headers = HttpHeaders.of(Map.of(
                "Server", List.of("Apache/2.4.41"),
                "X-Powered-By", List.of("PHP/7.4.3")
        ), (a, b) -> true);

        List<Finding> findings = analyzer.analyze(headers);

        assertTrue(containsTitleContaining(findings, "'Server' expõe"));
        assertTrue(containsTitleContaining(findings, "'X-Powered-By' expõe"));
    }

    private static boolean containsTitleContaining(List<Finding> findings, String snippet) {
        return findings.stream().anyMatch(f -> f.title().contains(snippet));
    }
}
