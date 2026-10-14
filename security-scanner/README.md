# security-scanner

Ferramenta de **reconhecimento e varredura de vulnerabilidades passiva**, em
Java puro, pensada como o primeiro passo de um teste de segurança (pentest)
**autorizado** contra um site/sistema próprio ou para o qual você tenha
permissão explícita (ex.: contrato de pentest, autorização do responsável).

> ⚠️ **Uso responsável**: aponte esta ferramenta apenas para sistemas seus ou
> autorizados por escrito. Mesmo sendo somente leitura, gerar tráfego de
> reconhecimento contra um sistema de terceiros sem autorização pode
> constituir acesso não autorizado (Lei 12.737/2012). A ferramenta exige a
> flag `--confirm-owner` em toda execução, exatamente para deixar essa
> responsabilidade explícita.

## O que esta ferramenta faz (e o que NÃO faz)

Opera **inteiramente em modo somente leitura** (requisições `GET`):

✅ Analisa headers de segurança (HSTS, CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy)
✅ Analisa flags de cookies (`Secure`, `HttpOnly`, `SameSite`) — crítico para cookies de sessão PHP (`PHPSESSID`)
✅ Inspeciona a sessão TLS: protocolo negociado, cipher suite, validade do certificado
✅ Verifica exposição de caminhos sensíveis comuns (`.env`, `.git/config`, backups `.sql`/`.bak`, `phpinfo.php`, `composer.json`, etc.), com baseline anti-falso-positivo

❌ **Não** tenta login nem força bruta de credenciais
❌ **Não** injeta payloads (SQLi, XSS, etc.)
❌ **Não** envia requisições que alterem dados (sempre `GET`)
❌ **Não** gera volume de tráfego (para isso existe o módulo `load-tester`, com propósito diferente: teste de capacidade, não de segurança)

Para avançar para testes ativos (autenticação, injeção, lógica de negócio),
é necessário escopo formalmente acordado com o responsável pelo sistema —
este scanner é deliberadamente limitado ao reconhecimento, para ser seguro
de rodar até contra produção.

## Por que caminhos como `/.env` ou `/phpinfo.php`?

São os alvos clássicos de reconhecimento em qualquer stack PHP — e um dos
vazamentos mais comuns em sites reais, geralmente por engano (um arquivo de
deploy esquecido, um `.git` publicado junto com o código, um `phpinfo.php`
de debug que nunca foi removido). Detectar isso **antes** que alguém mal
intencionado o faça é literalmente o valor de um pentest de reconhecimento.

Para evitar falsos positivos (sites que respondem HTTP 200 para qualquer
caminho, por roteamento próprio), o scanner primeiro busca uma página de
baseline com um caminho inexistente e só reporta um caminho da lista se a
resposta dele divergir claramente dessa baseline.

## Build

Requer Java 21+ e Maven.

```bash
mvn clean package
```

Gera `target/security-scanner-jar-with-dependencies.jar` (standalone).

## Uso

```bash
java -jar target/security-scanner-jar-with-dependencies.jar \
  --url https://meusite.com.br \
  --confirm-owner
```

### Opções

| Flag               | Descrição                                               | Padrão |
|---------------------|----------------------------------------------------------|--------|
| `--url`             | URL de destino (obrigatório)                              | —      |
| `--confirm-owner`   | Confirma autorização para testar essa URL (obrigatório)   | —      |
| `--timeout`         | Timeout por requisição, ex.: `10s`                        | `10s`  |

## Exemplo de saída

```
=============== RELATÓRIO DE RECONHECIMENTO/VARREDURA ===============
Resumo por severidade:
  CRITICAL   : 1
  HIGH       : 2
  MEDIUM     : 4
  LOW        : 2
  INFO       : 1
-----------------------------------------------------------------------

[CRITICAL] Exposição de arquivos/caminhos — Caminho possivelmente exposto: /.env
  Detalhe        : HTTP 200, 412 bytes — Credenciais de ambiente (.env) podem estar expostas publicamente.
  Recomendação   : Remova o arquivo do diretório público, bloqueie o caminho no servidor web...

[HIGH] Cookies — Cookie 'PHPSESSID' sem flag HttpOnly
  ...
```

## Próximos passos (para evoluir o projeto)

- Exportar o relatório em JSON/HTML (ótimo exercício de serialização em Java).
- Adicionar verificação de cabeçalhos CORS mal configurados.
- Integrar com uma base de CVEs (ex.: consultar versões de software detectadas
  via fingerprinting contra o NVD) — mantendo o princípio de só *detectar*,
  nunca *explorar*.
- Um segundo módulo, separado e com escopo próprio, para testes ativos
  (autenticação/injeção) — só deve ser construído quando houver autorização
  explícita cobrindo esse tipo de teste, idealmente contra staging.
