package com.devdiresto.secscanner.core;

import com.devdiresto.secscanner.model.Finding;
import com.devdiresto.secscanner.model.Severity;

import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Analisa os cookies enviados pelo servidor (header {@code Set-Cookie}) em
 * busca de flags de segurança ausentes: {@code Secure}, {@code HttpOnly} e
 * {@code SameSite}.
 * <p>
 * Especialmente relevante para o cookie de sessão de um sistema PHP
 * (tipicamente {@code PHPSESSID}), já que uma sessão sem essas flags é um
 * alvo fácil para sequestro de sessão (session hijacking) via XSS ou
 * interceptação em rede.
 */
public final class CookieAnalyzer {

    public List<Finding> analyze(HttpHeaders headers) {
        List<Finding> findings = new ArrayList<>();
        List<String> cookies = headers.allValues("Set-Cookie");

        for (String cookie : cookies) {
            String name = cookie.split("=", 2)[0].trim();
            String lower = cookie.toLowerCase(Locale.ROOT);
            boolean isSessionCookie = name.toLowerCase(Locale.ROOT).contains("sess")
                    || name.equalsIgnoreCase("PHPSESSID");

            if (!lower.contains("secure")) {
                findings.add(new Finding(
                        isSessionCookie ? Severity.HIGH : Severity.MEDIUM,
                        "Cookies",
                        "Cookie '" + name + "' sem flag Secure",
                        "O cookie pode ser transmitido também por HTTP puro, se o site aceitar, " +
                                "expondo-o a interceptação de rede.",
                        "Marque o cookie com 'Secure' (só é enviado em conexões HTTPS)."));
            }

            if (!lower.contains("httponly")) {
                findings.add(new Finding(
                        isSessionCookie ? Severity.HIGH : Severity.LOW,
                        "Cookies",
                        "Cookie '" + name + "' sem flag HttpOnly",
                        "JavaScript no navegador consegue ler esse cookie via document.cookie, " +
                                "o que amplia o impacto de uma vulnerabilidade de XSS para roubo de sessão.",
                        "Marque o cookie com 'HttpOnly' (no PHP: session.cookie_httponly = 1)."));
            }

            if (!lower.contains("samesite")) {
                findings.add(new Finding(Severity.MEDIUM, "Cookies",
                        "Cookie '" + name + "' sem atributo SameSite",
                        "Sem SameSite, o cookie pode ser enviado em requisições cross-site, " +
                                "facilitando ataques de CSRF.",
                        "Defina 'SameSite=Lax' ou 'SameSite=Strict' (no PHP 7.3+: via " +
                                "session_set_cookie_params)."));
            }
        }

        return findings;
    }
}
