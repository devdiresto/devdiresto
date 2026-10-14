package com.devdiresto.loadtester;

import com.devdiresto.loadtester.cli.CliArguments;
import com.devdiresto.loadtester.config.LoadTestConfig;
import com.devdiresto.loadtester.core.LoadTestEngine;
import com.devdiresto.loadtester.report.LoadTestStats;
import com.devdiresto.loadtester.report.ReportPrinter;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Ponto de entrada da ferramenta de teste de carga.
 * <p>
 * Uso previsto: validar, de forma controlada e autorizada, se uma aplicação
 * própria aguenta um determinado volume de requisições concorrentes — o
 * tipo de teste que precede o lançamento de uma funcionalidade em produção.
 * <p>
 * NUNCA aponte esta ferramenta para um sistema de terceiros sem autorização
 * explícita: gerar tráfego excessivo contra um serviço que não é seu, sem
 * permissão, pode constituir crime (negação de serviço) em praticamente
 * qualquer jurisdição.
 */
public final class LoadTesterApp {

    public static void main(String[] args) {
        // Garante saída em UTF-8 independentemente da codificação padrão do
        // terminal/host, para que os textos em português sejam exibidos corretamente.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        LoadTestConfig config;
        try {
            config = CliArguments.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro ao processar argumentos: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Iniciando teste de carga...");
        System.out.println("  URL         : " + config.targetUri());
        System.out.println("  Método      : " + config.method());
        System.out.println("  Concorrência: " + config.concurrency());
        if (config.isDurationBased()) {
            System.out.println("  Duração     : " + config.duration());
        } else {
            System.out.println("  Requisições : " + config.totalRequests());
        }
        System.out.println();

        LoadTestEngine engine = new LoadTestEngine(config);
        try {
            LoadTestStats.Snapshot snapshot = engine.run();
            ReportPrinter.print(snapshot);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Teste interrompido.");
            System.exit(130);
        }
    }
}
