# load-tester

Ferramenta de **teste de carga (load testing)** em Java puro, para avaliar
quantas requisições concorrentes um site/API **próprio** (ou que você tenha
autorização explícita para testar) suporta antes de degradar ou cair.

> ⚠️ **Uso responsável**: aponte esta ferramenta apenas para sistemas seus ou
> para os quais você tenha autorização por escrito. Gerar tráfego excessivo
> contra um serviço de terceiros sem permissão pode ser crime (negação de
> serviço) em praticamente qualquer jurisdição, incluindo o Brasil. A
> ferramenta exige a flag `--confirm-owner` para rodar, exatamente para
> deixar essa responsabilidade explícita a cada execução.

## Por que essa ferramenta

Um teste de carga de verdade (diferente de "spammar requisição") precisa:

- **Concorrência controlada** — não é "quanto mais rápido melhor", é "simular
  N usuários simultâneos de forma previsível".
- **Métricas que importam** — não só "quantas deram certo", mas latência
  (p50/p90/p95/p99), vazão (req/s) e taxa de erro.
- **Dois modos de execução** — por quantidade fixa de requisições, ou por
  duração (ex.: "aguente 200 req/s por 2 minutos").

Esse projeto implementa isso em Java 21 usando **virtual threads** (Project
Loom), o que permite milhares de requisições concorrentes com baixíssimo
overhead de memória, sem a complexidade de pools de threads tradicionais.

## Arquitetura

```
com.devdiresto.loadtester
├── LoadTesterApp            → ponto de entrada (main)
├── cli/CliArguments         → parsing manual de argumentos de linha de comando
├── config/
│   ├── LoadTestConfig       → configuração imutável (Builder pattern)
│   └── HttpMethod           → enum dos verbos HTTP suportados
├── core/
│   ├── LoadTestEngine       → motor de execução (virtual threads + semáforo)
│   └── RequestOutcome       → record com o resultado de uma requisição
└── report/
    ├── LoadTestStats        → agregação thread-safe das métricas
    └── ReportPrinter        → formatação do relatório final
```

## Build

Requer Java 21+ e Maven.

```bash
mvn clean package
```

Gera `target/load-tester.jar` (precisa das dependências no classpath) e
`target/load-tester-jar-with-dependencies.jar` (standalone, recomendado).

## Uso

```bash
java -jar target/load-tester-jar-with-dependencies.jar \
  --url https://meusite.com.br \
  --confirm-owner \
  --requests 5000 \
  --concurrency 100
```

Teste por duração, em vez de quantidade fixa:

```bash
java -jar target/load-tester-jar-with-dependencies.jar \
  --url https://meusite.com.br \
  --confirm-owner \
  --duration 1m \
  --concurrency 200
```

### Opções

| Flag               | Descrição                                               | Padrão   |
|--------------------|----------------------------------------------------------|----------|
| `--url`            | URL de destino (obrigatório)                              | —        |
| `--confirm-owner`  | Confirma autorização para testar essa URL (obrigatório)   | —        |
| `--method`         | `GET`, `POST`, `PUT`, `DELETE`, `HEAD`, `PATCH`            | `GET`    |
| `--requests`       | Número total de requisições (alternativa a `--duration`)  | —        |
| `--duration`       | Duração do teste, ex.: `30s`, `2m`, `1h`                   | —        |
| `--concurrency`    | Requisições simultâneas                                   | `50`     |
| `--timeout`        | Timeout por requisição                                    | `10s`    |
| `--ramp-up`        | Distribui o início das requisições gradualmente            | `0`      |
| `--header`         | Header HTTP adicional, formato `"Nome: valor"` (repetível) | —        |
| `--body`           | Corpo da requisição (para `POST`/`PUT`/`PATCH`)            | —        |

## Exemplo de saída

```
================ RESULTADO DO TESTE DE CARGA ================
Requisições totais : 5000
  Sucesso (2xx/3xx) : 4987
  Falha             : 13
Taxa de erro        : 0.26%
Vazão (RPS)         : 412.37 req/s
---------------------------------------------------------------
Latência (ms):
  mín    : 4.12
  média  : 118.44
  p50    : 62.30
  p90    : 289.17
  p95    : 401.52
  p99    : 812.90
  máx    : 1203.44
---------------------------------------------------------------
Códigos de status HTTP:
  HTTP 200 : 4987
  HTTP 503 : 13
=================================================================
```

## Próximos passos (se quiser evoluir o projeto)

- Exportar o relatório em CSV/JSON para integrar com dashboards.
- Adicionar um modo de ramp-up mais sofisticado (degraus de carga).
- Publicar métricas em tempo real (ex.: via Micrometer + Prometheus) —
  ótimo exercício para quem está migrando para o ecossistema Java backend.
- Testes de integração com WireMock simulando falhas e lentidão do servidor.
