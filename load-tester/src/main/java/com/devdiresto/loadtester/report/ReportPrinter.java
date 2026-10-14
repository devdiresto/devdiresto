package com.devdiresto.loadtester.report;

import java.util.Locale;
import java.util.Map;

/**
 * Formata e imprime no console o relatório final de um teste de carga.
 */
public final class ReportPrinter {

    private ReportPrinter() {
    }

    public static void print(LoadTestStats.Snapshot snapshot) {
        System.out.println();
        System.out.println("================ RESULTADO DO TESTE DE CARGA ================");
        System.out.printf(Locale.US, "Requisições totais : %d%n", snapshot.totalRequests());
        System.out.printf(Locale.US, "  Sucesso (2xx/3xx) : %d%n", snapshot.successCount());
        System.out.printf(Locale.US, "  Falha             : %d%n", snapshot.failureCount());
        System.out.printf(Locale.US, "Taxa de erro        : %.2f%%%n", snapshot.errorRatePercent());
        System.out.printf(Locale.US, "Vazão (RPS)         : %.2f req/s%n", snapshot.requestsPerSecond());
        System.out.println("---------------------------------------------------------------");
        System.out.println("Latência (ms):");
        System.out.printf(Locale.US, "  mín    : %.2f%n", snapshot.minMillis());
        System.out.printf(Locale.US, "  média  : %.2f%n", snapshot.averageMillis());
        System.out.printf(Locale.US, "  p50    : %.2f%n", snapshot.percentileMillis(50));
        System.out.printf(Locale.US, "  p90    : %.2f%n", snapshot.percentileMillis(90));
        System.out.printf(Locale.US, "  p95    : %.2f%n", snapshot.percentileMillis(95));
        System.out.printf(Locale.US, "  p99    : %.2f%n", snapshot.percentileMillis(99));
        System.out.printf(Locale.US, "  máx    : %.2f%n", snapshot.maxMillis());

        if (!snapshot.statusCodeCounts().isEmpty()) {
            System.out.println("---------------------------------------------------------------");
            System.out.println("Códigos de status HTTP:");
            for (Map.Entry<Integer, Long> entry : snapshot.statusCodeCounts().entrySet()) {
                System.out.printf(Locale.US, "  HTTP %d : %d%n", entry.getKey(), entry.getValue());
            }
        }

        if (!snapshot.errorCounts().isEmpty()) {
            System.out.println("---------------------------------------------------------------");
            System.out.println("Erros de conexão/timeout:");
            for (Map.Entry<String, Long> entry : snapshot.errorCounts().entrySet()) {
                System.out.printf(Locale.US, "  %s : %d%n", entry.getKey(), entry.getValue());
            }
        }
        System.out.println("=================================================================");
    }
}
