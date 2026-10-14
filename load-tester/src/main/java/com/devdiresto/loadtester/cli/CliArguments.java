package com.devdiresto.loadtester.cli;

import com.devdiresto.loadtester.config.HttpMethod;
import com.devdiresto.loadtester.config.LoadTestConfig;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Parser simples de argumentos de linha de comando para o {@code load-tester}.
 * <p>
 * Não usa nenhuma biblioteca externa de propósito: é um bom exercício de
 * parsing manual, útil para entender como ferramentas CLI reais (curl, ab,
 * wrk) processam seus argumentos.
 */
public final class CliArguments {

    private CliArguments() {
    }

    public static LoadTestConfig parse(String[] args) {
        if (args.length == 0 || hasFlag(args, "--help") || hasFlag(args, "-h")) {
            printUsage();
            System.exit(args.length == 0 ? 1 : 0);
        }

        Deque<String> queue = new ArrayDeque<>();
        for (String arg : args) {
            queue.add(arg);
        }

        LoadTestConfig.Builder builder = LoadTestConfig.builder();
        boolean ownershipConfirmed = false;

        while (!queue.isEmpty()) {
            String token = queue.poll();
            switch (token) {
                case "--url" -> builder.targetUri(URI.create(requireValue(queue, token)));
                case "--method" -> builder.method(HttpMethod.fromString(requireValue(queue, token)));
                case "--concurrency" -> builder.concurrency(Integer.parseInt(requireValue(queue, token)));
                case "--requests" -> builder.totalRequests(Long.parseLong(requireValue(queue, token)));
                case "--duration" -> builder.duration(parseDuration(requireValue(queue, token)));
                case "--timeout" -> builder.requestTimeout(parseDuration(requireValue(queue, token)));
                case "--ramp-up" -> builder.rampUp(parseDuration(requireValue(queue, token)));
                case "--header" -> addHeader(builder, requireValue(queue, token));
                case "--body" -> builder.body(requireValue(queue, token));
                case "--confirm-owner" -> ownershipConfirmed = true;
                default -> throw new IllegalArgumentException("Argumento desconhecido: " + token);
            }
        }

        if (!ownershipConfirmed) {
            System.err.println();
            System.err.println("ERRO: faltou a flag --confirm-owner.");
            System.err.println();
            System.err.println("Esta ferramenta gera carga real de requisições HTTP. Use-a apenas contra");
            System.err.println("sites/APIs que você possui ou tem autorização explícita para testar.");
            System.err.println("Passe --confirm-owner para declarar que você está autorizado a rodar");
            System.err.println("este teste contra a URL informada.");
            System.err.println();
            System.exit(2);
        }

        return builder.build();
    }

    private static void addHeader(LoadTestConfig.Builder builder, String raw) {
        int separator = raw.indexOf(':');
        if (separator < 0) {
            throw new IllegalArgumentException("Header inválido (use 'Nome: valor'): " + raw);
        }
        String name = raw.substring(0, separator).trim();
        String value = raw.substring(separator + 1).trim();
        builder.header(name, value);
    }

    /**
     * Converte durações simples como {@code "30s"}, {@code "2m"}, {@code "500ms"}.
     */
    private static Duration parseDuration(String raw) {
        String value = raw.trim().toLowerCase();
        try {
            if (value.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
            } else if (value.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
            } else if (value.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1)));
            } else if (value.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(value.substring(0, value.length() - 1)));
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
                load-tester — ferramenta de teste de carga HTTP em Java

                USO:
                  java -jar load-tester.jar --url <URL> --confirm-owner [opções]

                OPÇÕES:
                  --url <url>            URL de destino (obrigatório)
                  --confirm-owner        Confirma que você é dono/autorizado a testar essa URL (obrigatório)
                  --method <verbo>       GET, POST, PUT, DELETE, HEAD, PATCH (padrão: GET)
                  --requests <n>         Número total de requisições a enviar
                  --duration <tempo>     Duração do teste (ex.: 30s, 2m) — alternativa a --requests
                  --concurrency <n>      Número de requisições simultâneas (padrão: 50)
                  --timeout <tempo>      Timeout por requisição (padrão: 10s)
                  --ramp-up <tempo>      Tempo para distribuir o início das requisições gradualmente
                  --header "Nome: valor" Header HTTP adicional (pode repetir)
                  --body <texto>         Corpo da requisição (para POST/PUT/PATCH)

                EXEMPLOS:
                  # 5000 requisições, 100 simultâneas, contra seu próprio site
                  java -jar load-tester.jar --url https://meusite.com.br --confirm-owner \\
                       --requests 5000 --concurrency 100

                  # Teste de 1 minuto, 200 de concorrência
                  java -jar load-tester.jar --url https://meusite.com.br --confirm-owner \\
                       --duration 1m --concurrency 200
                """);
    }
}
