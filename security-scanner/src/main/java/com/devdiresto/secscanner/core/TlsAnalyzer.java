package com.devdiresto.secscanner.core;

import com.devdiresto.secscanner.model.Finding;
import com.devdiresto.secscanner.model.Severity;

import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Analisa a sessão TLS estabelecida com o servidor: protocolo negociado,
 * cipher suite e validade do certificado.
 * <p>
 * Não faz nenhuma tentativa de downgrade de protocolo ou ataque ativo —
 * apenas observa o que o próprio servidor negociou com um cliente Java padrão.
 */
public final class TlsAnalyzer {

    private static final List<String> WEAK_PROTOCOLS = List.of("SSLv3", "TLSv1", "TLSv1.1");

    public List<Finding> analyze(SSLSession session) {
        List<Finding> findings = new ArrayList<>();

        String protocol = session.getProtocol();
        if (WEAK_PROTOCOLS.contains(protocol)) {
            findings.add(new Finding(Severity.HIGH, "TLS",
                    "Protocolo TLS obsoleto negociado: " + protocol,
                    "Protocolos anteriores ao TLS 1.2 têm vulnerabilidades conhecidas " +
                            "(ex.: POODLE, BEAST) e foram descontinuados pelos principais navegadores.",
                    "Desative SSLv3/TLSv1.0/TLSv1.1 no servidor web e mantenha apenas TLS 1.2+."));
        }

        String cipherSuite = session.getCipherSuite();
        if (cipherSuite.toUpperCase(Locale.ROOT).contains("RC4")
                || cipherSuite.toUpperCase(Locale.ROOT).contains("3DES")
                || cipherSuite.toUpperCase(Locale.ROOT).contains("NULL")) {
            findings.add(new Finding(Severity.HIGH, "TLS",
                    "Cipher suite fraca negociada",
                    "Cipher: " + cipherSuite,
                    "Reconfigure o servidor para priorizar cipher suites modernas " +
                            "(ex.: AES-GCM, ChaCha20-Poly1305) e desabilitar as fracas/obsoletas."));
        }

        try {
            Certificate[] chain = session.getPeerCertificates();
            if (chain.length > 0 && chain[0] instanceof X509Certificate leaf) {
                Instant expiry = leaf.getNotAfter().toInstant();
                Instant now = Instant.now();
                long daysLeft = java.time.Duration.between(now, expiry).toDays();

                if (expiry.isBefore(now)) {
                    findings.add(new Finding(Severity.CRITICAL, "TLS",
                            "Certificado TLS expirado",
                            "Expirou em " + expiry,
                            "Renove o certificado imediatamente. Visitantes verão aviso de " +
                                    "'conexão não segura' no navegador."));
                } else if (daysLeft <= 15) {
                    findings.add(new Finding(Severity.MEDIUM, "TLS",
                            "Certificado TLS próximo de expirar",
                            "Expira em " + daysLeft + " dia(s), em " + expiry,
                            "Renove o certificado com antecedência (idealmente com renovação " +
                                    "automática, ex.: Let's Encrypt + certbot)."));
                } else {
                    findings.add(new Finding(Severity.INFO, "TLS",
                            "Certificado TLS válido",
                            "Expira em " + daysLeft + " dias, em " + expiry,
                            "Nenhuma ação necessária por ora."));
                }
            }
        } catch (Exception e) {
            findings.add(new Finding(Severity.INFO, "TLS",
                    "Não foi possível inspecionar o certificado",
                    e.getMessage(),
                    "—"));
        }

        return findings;
    }
}
