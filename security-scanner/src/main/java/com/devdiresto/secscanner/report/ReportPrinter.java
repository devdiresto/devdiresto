package com.devdiresto.secscanner.report;

import com.devdiresto.secscanner.model.Finding;
import com.devdiresto.secscanner.model.Severity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Formata e imprime no console o relatório de achados do scanner.
 */
public final class ReportPrinter {

    private ReportPrinter() {
    }

    public static void print(List<Finding> findings) {
        System.out.println();
        System.out.println("=============== RELATÓRIO DE RECONHECIMENTO/VARREDURA ===============");

        if (findings.isEmpty()) {
            System.out.println("Nenhum achado. (Isso não significa 'sem vulnerabilidades' — apenas que " +
                    "este conjunto específico de verificações passivas não encontrou nada.)");
            return;
        }

        Map<Severity, Long> countsBySeverity = new TreeMap<>(
                Comparator.comparing(Severity::ordinal).reversed());
        for (Finding f : findings) {
            countsBySeverity.merge(f.severity(), 1L, Long::sum);
        }

        System.out.println("Resumo por severidade:");
        for (Map.Entry<Severity, Long> entry : countsBySeverity.entrySet()) {
            System.out.printf("  %-10s : %d%n", entry.getKey(), entry.getValue());
        }
        System.out.println("-----------------------------------------------------------------------");

        for (Finding finding : findings) {
            System.out.println();
            System.out.printf("[%s] %s — %s%n", finding.severity(), finding.category(), finding.title());
            System.out.println("  Detalhe        : " + finding.detail());
            System.out.println("  Recomendação   : " + finding.recommendation());
        }

        System.out.println();
        System.out.println("=========================================================================");
        System.out.println("Lembrete: este scanner cobre apenas reconhecimento e verificações passivas.");
        System.out.println("Não substitui um pentest completo (autenticação, injeção, lógica de negócio).");
    }
}
