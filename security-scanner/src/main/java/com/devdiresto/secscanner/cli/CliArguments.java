package com.devdiresto.secscanner.cli;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Parser de argumentos de linha de comando para o {@code security-scanner}.
 */
public final class CliArguments {

    private CliArguments() {
    }

    public record Options(URI targetUri, Duration timeout) {
    }

    public static Options parse(String[] args) {
        if (args.length == 0 || hasFlag(args, "--help") || hasFlag(args, "-h")) {
            printUsage();
            System.exit(args.length == 0 ? 1 : 0);
        }

        Deque<String> queue = new ArrayDeque<>();
        for (String arg : args) {
            queue.add(arg);
        }

        URI targetUri = null;
        Duration timeout = Duration.ofSeconds(10);
        boolean ownershipConfirmed = false;

        while (!queue.isEmpty()) {
            String token = queue.poll();
            switch (token) {
                case "--url" -> targetUri = URI.create(requireValue(queue, token));
                case "--timeout" -> timeout = parseDuration(requireValue(queue, token));
                case "--confirm-owner" -> ownershipConfirmed = true;
                default -> throw new IllegalArgumentException("Argumento desconhecido: " + token);
            }
        }

        if (targetUri == null) {
            throw new IllegalArgumentException("Informe --url <URL>");
        }

        if (!ownershipConfirmed) {
            System.err.println();
            System.err.println("ERRO: faltou a flag --confirm-owner.");
            System.err.println();
            System.err.println("Esta ferramenta é somente leitura (GET), mas ainda assim envia tráfego");
            System.err.println("de reconhecimento para o host informado. Use-a apenas contra sistemas");
            System.err.println("seus ou para os quais você tenha autorização explícita (ex.: contrato de");
            System.err.println("pentest). Passe --confirm-owner para declarar essa autorização.");
            System.err.println();
            System.exit(2);
        }

        return new Options(targetUri, timeout);
    }

    private static Duration parseDuration(String raw) {
        String value = raw.trim().toLowerCase();
        try {
            if (value.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
            } else if (value.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
            }
            return Duration.ofSeconds(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Duração inválida: " + raw, e);
        }
    }

    private static String requireValue(Deque<String> queue, String flag) {
        String value = queue.poll();
        if (value == null) {
            throw new IllegalArgumentException("Faltou valor para o argumento " + flag);
        }
        return value;
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("""
                security-scanner — reconhecimento e varredura de vulnerabilidades (passivo)

                USO:
                  java -jar security-scanner.jar --url <URL> --confirm-owner [opções]

                O QUE ESTE SCANNER FAZ (somente leitura, requisições GET):
                  - Analisa headers de segurança (HSTS, CSP, X-Frame-Options, etc.)
                  - Analisa flags de cookies (Secure, HttpOnly, SameSite)
                  - Inspeciona a sessão TLS (protocolo, cipher suite, validade do certificado)
                  - Verifica exposição de caminhos sensíveis comuns (.env, .git, backups, etc.)

                O QUE ESTE SCANNER NÃO FAZ:
                  - Não tenta login nem força bruta de credenciais
                  - Não injeta payloads (SQLi, XSS, etc.)
                  - Não envia requisições que alterem dados (sempre GET)
                  - Não gera volume de tráfego (não é uma ferramenta de carga)

                OPÇÕES:
                  --url <url>            URL de destino (obrigatório)
                  --confirm-owner        Confirma autorização para testar essa URL (obrigatório)
                  --timeout <tempo>      Timeout por requisição (padrão: 10s)

                EXEMPLO:
                  java -jar security-scanner.jar --url https://meusite.com.br --confirm-owner
                """);
    }
}
