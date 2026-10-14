package com.devdiresto.secscanner.core;

import com.devdiresto.secscanner.model.Finding;
import com.devdiresto.secscanner.model.Severity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifica, de forma passiva (apenas requisições GET de leitura), se caminhos
 * comumente sensíveis estão publicamente acessíveis — arquivos de
 * configuração, backups, repositório .git exposto, painéis administrativos
 * sem proteção, etc.
 * <p>
 * Para evitar falsos positivos em sites que respondem 200 para qualquer
 * caminho (soft-404, comum em apps PHP com roteamento próprio), primeiro é
 * obtida uma resposta de <b>baseline</b> para um caminho inexistente; um
 * caminho da lista só é reportado se a resposta dele divergir claramente
 * dessa baseline.
 * <p>
 * Esta classe NUNCA envia payloads de ataque, nunca tenta autenticação e
 * nunca envia métodos que alterem estado (sempre GET).
 */
public final class ExposedPathScanner {

    /** Caminhos comumente sensíveis em stacks PHP, verificados apenas por leitura (GET). */
    private static final List<PathCheck> CANDIDATE_PATHS = List.of(
            new PathCheck("/.env", Severity.CRITICAL,
                    "Credenciais de ambiente (.env) podem estar expostas publicamente."),
            new PathCheck("/.git/config", Severity.CRITICAL,
                    "Repositório .git exposto permite reconstruir todo o código-fonte e histórico."),
            new PathCheck("/.git/HEAD", Severity.CRITICAL,
                    "Repositório .git exposto permite reconstruir todo o código-fonte e histórico."),
            new PathCheck("/composer.json", Severity.LOW,
                    "Lista de dependências PHP exposta — útil para atacante mapear versões vulneráveis."),
            new PathCheck("/composer.lock", Severity.MEDIUM,
                    "Versões exatas de dependências expostas — facilita buscar CVEs específicas."),
            new PathCheck("/phpinfo.php", Severity.HIGH,
                    "phpinfo() exposto revela versão do PHP, módulos, caminhos do servidor e configs."),
            new PathCheck("/info.php", Severity.HIGH,
                    "phpinfo() exposto revela versão do PHP, módulos, caminhos do servidor e configs."),
            new PathCheck("/config.php.bak", Severity.CRITICAL,
                    "Backup de arquivo de configuração pode conter credenciais de banco de dados."),
            new PathCheck("/backup.sql", Severity.CRITICAL,
                    "Dump de banco de dados pode estar publicamente acessível."),
            new PathCheck("/database.sql", Severity.CRITICAL,
                    "Dump de banco de dados pode estar publicamente acessível."),
            new PathCheck("/.htaccess", Severity.MEDIUM,
                    "Regras de .htaccess expostas podem revelar detalhes da configuração interna."),
            new PathCheck("/phpmyadmin/", Severity.MEDIUM,
                    "Painel phpMyAdmin acessível publicamente é um alvo comum de força bruta."),
            new PathCheck("/server-status", Severity.MEDIUM,
                    "mod_status do Apache exposto pode revelar requisições de outros usuários em tempo real."),
            new PathCheck("/error_log", Severity.MEDIUM,
                    "Log de erros exposto pode revelar caminhos internos, stack traces e versões."),
            new PathCheck("/debug.log", Severity.MEDIUM,
                    "Log de debug exposto pode revelar informações sensíveis da aplicação."),
            new PathCheck("/cliente/", Severity.LOW,
                    "Verifica se há listagem de diretório habilitada na área de clientes.")
    );

    private final HttpClient httpClient;
    private final Duration timeout;

    public ExposedPathScanner(HttpClient httpClient, Duration timeout) {
        this.httpClient = httpClient;
        this.timeout = timeout;
    }

    public List<Finding> scan(URI baseUri) throws Exception {
        List<Finding> findings = new ArrayList<>();

        Baseline baseline = fetchBaseline(baseUri);

        for (PathCheck check : CANDIDATE_PATHS) {
            URI target = baseUri.resolve(check.path());
            Snapshot snapshot = fetch(target);

            if (looksExposed(snapshot, baseline)) {
                findings.add(new Finding(check.severity(), "Exposição de arquivos/caminhos",
                        "Caminho possivelmente exposto: " + check.path(),
                        "HTTP " + snapshot.statusCode() + ", " + snapshot.bodyLength() + " bytes — " + check.risk(),
                        "Remova o arquivo do diretório público, bloqueie o caminho no servidor web " +
                                "(ex.: regra de Nginx/Apache) ou restrinja o acesso por autenticação."));
            }
        }

        return findings;
    }

    private boolean looksExposed(Snapshot snapshot, Baseline baseline) {
        if (snapshot.statusCode() != 200) {
            return false;
        }
        if (baseline.statusCode() != 200) {
            // Baseline não encontra nada (comportamento normal) -> um 200 real aqui é significativo.
            return true;
        }
        // Site responde 200 para tudo (soft-404): só reporta se o corpo for claramente diferente
        // em tamanho do corpo da página de "não encontrado" (heurística conservadora).
        long diff = Math.abs(snapshot.bodyLength() - baseline.bodyLength());
        double relativeDiff = baseline.bodyLength() == 0 ? 1.0 : diff / (double) baseline.bodyLength();
        return relativeDiff > 0.15;
    }

    private Baseline fetchBaseline(URI baseUri) throws Exception {
        URI probe = baseUri.resolve("/este-caminho-nao-deveria-existir-" + System.nanoTime() + ".txt");
        Snapshot snapshot = fetch(probe);
        return new Baseline(snapshot.statusCode(), snapshot.bodyLength());
    }

    private Snapshot fetch(URI uri) throws Exception {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(timeout)
                    .header("User-Agent", "devdiresto-security-scanner/1.0 (authorized-recon)")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new Snapshot(response.statusCode(), response.body().length());
        } catch (Exception e) {
            return new Snapshot(0, 0);
        }
    }

    private record PathCheck(String path, Severity severity, String risk) {
    }

    private record Snapshot(int statusCode, int bodyLength) {
    }

    private record Baseline(int statusCode, int bodyLength) {
    }
}
