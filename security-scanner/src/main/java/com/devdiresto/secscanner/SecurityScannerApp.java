package com.devdiresto.secscanner;

import com.devdiresto.secscanner.cli.CliArguments;
import com.devdiresto.secscanner.core.ScanEngine;
import com.devdiresto.secscanner.model.Finding;
import com.devdiresto.secscanner.report.ReportPrinter;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Ponto de entrada do {@code security-scanner}.
 * <p>
 * Ferramenta de <b>reconhecimento e varredura de vulnerabilidades passiva</b>,
 * pensada para o primeiro passo de um teste de segurança autorizado (pentest)
 * contra um sistema próprio ou com autorização explícita do responsável.
 * <p>
 * Opera inteiramente em modo somente leitura (requisições GET): não realiza
 * força bruta de login, não injeta payloads de ataque e não altera nenhum
 * dado no servidor de destino. Para testes ativos (autenticação, injeção,
 * etc.), é necessário escopo e ferramental adicionais — este scanner é
 * deliberadamente limitado ao reconhecimento.
 * <p>
 * NUNCA aponte esta ferramenta para um sistema de terceiros sem autorização
 * explícita.
 */
public final class SecurityScannerApp {

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        CliArguments.Options options;
        try {
            options = CliArguments.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro ao processar argumentos: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Iniciando reconhecimento/varredura passiva...");
        System.out.println("  URL     : " + options.targetUri());
        System.out.println("  Timeout : " + options.timeout());
        System.out.println();

        try {
            ScanEngine engine = new ScanEngine(options.timeout());
            List<Finding> findings = engine.run(options.targetUri());
            ReportPrinter.print(findings);
        } catch (Exception e) {
            System.err.println("Falha ao executar o scan: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            System.exit(1);
        }
    }
}
