# API de Votação

API REST para gerenciar e participar de sessões de votação em assembleias de cooperativas.
Cada associado possui um voto e as decisões são tomadas por votação em pautas.

> O enunciado original do desafio está preservado em [DESAFIO.md](DESAFIO.md).

## Sumário

- [Funcionalidades](#funcionalidades)
- [Stack e decisões de arquitetura](#stack-e-decisões-de-arquitetura)
- [Como executar](#como-executar)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)
- [Endpoints](#endpoints)
- [Exemplo de uso ponta a ponta](#exemplo-de-uso-ponta-a-ponta)
- [Testes](#testes)
- [Tarefas bônus](#tarefas-bônus)
- [Tratamento de erros](#tratamento-de-erros)

## Funcionalidades

- ✅ Cadastrar uma nova pauta
- ✅ Abrir uma sessão de votação em uma pauta (duração informada na chamada **ou 1 minuto por default**)
- ✅ Receber votos dos associados (`SIM`/`NAO`; cada associado vota **uma única vez** por pauta)
- ✅ Contabilizar os votos e dar o resultado da votação
- ✅ Persistência (os dados sobrevivem ao restart da aplicação)
- ✅ **Bônus 1** – Integração com serviço externo de validação de CPF (client Fake)
- ✅ **Bônus 2** – Performance para centenas de milhares de votos (+ script de carga)
- ✅ **Bônus 3** – Estratégia de versionamento da API

## Stack e decisões de arquitetura

| Tema | Escolha | Por quê |
|------|---------|---------|
| Linguagem / Framework | Java 17 + Spring Boot 3.3 | Exigência do desafio; ecossistema maduro |
| Persistência | Spring Data JPA | Mapeamento objeto-relacional simples e testável |
| Banco (default) | **H2 em arquivo** (`./data`) | Persiste entre reinícios **sem nenhuma dependência externa** para o avaliador rodar |
| Banco (produção/perf) | **PostgreSQL** via profile `postgres` | Cenário realista de alto volume |
| Migrations | Flyway | Schema versionado e reproduzível (SQL padrão compatível com H2 e Postgres) |
| Documentação | springdoc-openapi (Swagger UI) | Documentação viva da API |
| Observabilidade | Spring Actuator | Health e métricas para os testes de performance |
| Boilerplate | Lombok | Menos código repetitivo nas entidades |

**Organização do código** – pacotes por *feature* (domínio), não por camada técnica. Cada
domínio concentra entidade, repositório, serviço, controller e DTOs:

```
com.desafio.votacao
├── pauta/      cadastro e consulta de pautas
├── sessao/     abertura e consulta de sessões de votação
├── voto/       registro de votos e apuração do resultado
├── cpf/        integração externa de validação de CPF (Bônus 1)
├── config/     OpenAPI e propriedades configuráveis
└── exception/  tratamento centralizado de erros (RestControllerAdvice)
```

Princípios aplicados: design simples (sem over-engineering), regras de negócio nos *services*,
controllers finos, DTOs separados das entidades, e validações tanto na borda (Bean Validation)
quanto no banco (constraints).

## Como executar

**Pré-requisitos:** Java 17+ e Maven 3.9+ (ou use o `mvnw` se preferir).

### Opção 1 — H2 em arquivo (default, recomendado para avaliar)

Nenhuma dependência externa. O banco é criado em `./data` e persiste entre reinícios.

```bash
mvn spring-boot:run
```

ou via jar:

```bash
mvn clean package
java -jar target/votacao-1.0.0.jar
```

A aplicação sobe em `http://localhost:8080`.

### Opção 2 — PostgreSQL (cenário de produção/performance)

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

As credenciais/URL podem ser sobrescritas por variáveis de ambiente
(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_SIZE`).

### Configurações relevantes (`application.yml`)

```yaml
votacao:
  sessao:
    duracao-padrao-minutos: 1      # default de 1 minuto quando a abertura não informa duração
  cpf-client:
    base-url: http://localhost:8080/fake-cpf-service   # URL de callback parametrizável
```

> A URL do serviço externo é **configurável** (conforme a dica do enunciado sobre URLs de
> callback passíveis de alteração por configuração, facilitando o teste em emulador/dispositivo).

## Documentação da API (Swagger)

Com a aplicação no ar:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Console H2 (profile default): <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:file:./data/votacao`)
- Health: <http://localhost:8080/actuator/health>

## Endpoints

Base path versionada: `/api/v1`

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `POST` | `/api/v1/pautas` | Cadastrar nova pauta |
| `GET`  | `/api/v1/pautas` | Listar pautas |
| `GET`  | `/api/v1/pautas/{id}` | Consultar pauta |
| `POST` | `/api/v1/pautas/{id}/sessao` | Abrir sessão de votação (body opcional `{ "duracaoMinutos": N }`) |
| `GET`  | `/api/v1/pautas/{id}/sessao` | Consultar sessão e seu status (`ABERTA`/`ENCERRADA`) |
| `POST` | `/api/v1/pautas/{id}/votos` | Registrar voto (`{ "associadoId": "<cpf>", "opcao": "SIM\|NAO" }`) |
| `GET`  | `/api/v1/pautas/{id}/resultado` | Contabilizar votos e obter resultado |

## Exemplo de uso ponta a ponta

```bash
# 1. Criar pauta
curl -X POST http://localhost:8080/api/v1/pautas \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Reforma do estatuto","descricao":"Aprovar reforma"}'

# 2. Abrir sessão por 5 minutos (omita o body para usar o default de 1 minuto)
curl -X POST http://localhost:8080/api/v1/pautas/1/sessao \
  -H "Content-Type: application/json" \
  -d '{"duracaoMinutos":5}'

# 3. Votar (use CPFs válidos gerados; o client Fake decide aleatoriamente a aptidão)
curl -X POST http://localhost:8080/api/v1/pautas/1/votos \
  -H "Content-Type: application/json" \
  -d '{"associadoId":"12345678909","opcao":"SIM"}'

# 4. Resultado
curl http://localhost:8080/api/v1/pautas/1/resultado
```

Resposta do resultado:

```json
{
  "pautaId": 1,
  "sessaoId": 1,
  "tituloPauta": "Reforma do estatuto",
  "sessaoEncerrada": false,
  "totalVotos": 3,
  "votosSim": 2,
  "votosNao": 1,
  "resultado": "APROVADA"
}
```

`resultado` pode ser `APROVADA`, `REPROVADA` ou `EMPATE`.

## Testes

```bash
mvn test
```

Cobertura de testes:

- **Unitários** (`VotoServiceTest`, `SessaoVotacaoServiceTest`) – regras de negócio isoladas com
  Mockito: sessão fechada, CPF inválido (404), `UNABLE_TO_VOTE`, voto duplicado, corrida de voto
  duplicado (constraint), apuração (aprovada/empate), duração default vs. informada, sessão já aberta.
- **Integração** (`VotacaoIntegrationTest`) – fluxo completo via `MockMvc` sobre o contexto real
  (H2 em memória): cadastro → abertura → votos → apuração, voto duplicado (409), CPF inválido (404),
  voto sem sessão (404) e validação de payload (400).

## Tarefas bônus

### Bônus 1 — Integração com sistema externo (validação de CPF)

`CpfValidationClient` é a abstração do serviço externo; `FakeCpfValidationClient` é a implementação
fake (`com.desafio.votacao.cpf`):

- ~30% dos CPFs são considerados **inválidos** → a API responde **HTTP 404 (Not Found)**.
- Para CPFs válidos, retorna aleatoriamente `ABLE_TO_VOTE` ou `UNABLE_TO_VOTE` — o mesmo CPF pode
  variar entre chamadas.
- `UNABLE_TO_VOTE` impede o voto com **HTTP 422**.

A interface permite trocar o fake por um client HTTP real sem tocar nas regras de votação. A URL
de callback é parametrizável (`votacao.cpf-client.base-url`).

### Bônus 2 — Performance

Decisões para suportar **centenas de milhares de votos**:

- **Apuração por agregação no banco** (`COUNT ... GROUP BY opção`) em vez de carregar votos em
  memória — custo O(1) de transferência independente do volume.
- **Índice composto** `idx_voto_sessao_opcao (sessao_id, opcao)` que cobre exatamente a query de
  contagem.
- **Unicidade garantida no banco** (`uk_voto_sessao_associado`) — evita race conditions de voto
  duplicado sob concorrência, em vez de depender só de checagem em memória.
- `open-in-view: false`, *batch inserts* do Hibernate e pool de conexões dimensionável.
- Profile **PostgreSQL** + `docker-compose.yml` para o cenário de carga.

Script de carga com **k6** em [`performance/load-test.js`](performance/load-test.js) (rampa até
200 usuários virtuais, *threshold* de p95 < 300ms). Instruções no cabeçalho do arquivo.

### Bônus 3 — Versionamento da API

**Estratégia adotada: versionamento por URI** (`/api/v1/...`).

Por que URI versioning:

- **Explícito e visível** — a versão fica clara na própria URL, fácil de testar via browser/cURL e
  de rotear em gateways/proxies.
- **Simples para o cliente mobile** — basta trocar o prefixo da URL base; sem manipular headers.
- **Cacheável** — URLs distintas por versão funcionam bem com caches HTTP/CDN.

Como evoluo na prática:

1. Mudanças **retrocompatíveis** (adicionar campos opcionais, novos endpoints) **não** sobem a
   versão — clientes antigos continuam funcionando.
2. Mudanças **quebra-contrato** (remover/renomear campos, alterar semântica) entram em uma nova
   versão `/api/v2`, mantendo `/api/v1` por um período de depreciação anunciado.
3. Controllers organizados por versão; código comum (services/domínio) é reaproveitado entre versões.

Alternativas consideradas: *header/media-type versioning* (`Accept: application/vnd.votacao.v1+json`)
— mais "purista" em REST, porém menos transparente para o cliente mobile e mais difícil de testar
manualmente; e *query param* (`?version=1`) — frágil para cache. Para o foco deste desafio
(comunicação simples e clara com o app mobile), **URI versioning** é o melhor custo-benefício.

## Tratamento de erros

Respostas de erro padronizadas via `@RestControllerAdvice`:

| Situação | HTTP |
|----------|------|
| Recurso inexistente / CPF inválido | `404 Not Found` |
| Payload inválido (Bean Validation) | `400 Bad Request` (com `fieldErrors`) |
| Regra de negócio (sessão fechada, `UNABLE_TO_VOTE`) | `422 Unprocessable Entity` |
| Associado já votou / sessão já aberta | `409 Conflict` |
| Erro inesperado | `500 Internal Server Error` |

Exemplo:

```json
{
  "timestamp": "2026-06-16T16:48:18.59-03:00",
  "status": 404,
  "error": "Not Found",
  "message": "CPF invalido: 12345678909",
  "path": "/api/v1/pautas/1/votos",
  "fieldErrors": null
}
```

## Logs

A aplicação registra os eventos relevantes (criação de pauta, abertura de sessão, registro de voto,
validação de CPF com o número mascarado, conflitos e erros) via SLF4J/Logback, facilitando a
observação do comportamento durante os testes.
