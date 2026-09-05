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
- [Telas do app mobile (Anexo 1)](#telas-do-app-mobile-anexo-1)
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
- ✅ Telas do app mobile no formato do **Anexo 1** (`FORMULARIO` / `SELECAO`)
- ✅ **Bônus 1** – Integração com serviço externo de validação de CPF (client Fake **ou HTTP real**)
- ✅ **Bônus 2** – Performance para centenas de milhares de votos (+ script de carga)
- ✅ **Bônus 3** – Estratégia de versionamento da API

## Stack e decisões de arquitetura

| Tema | Escolha | Por quê |
|------|---------|---------|
| Linguagem / Framework | Java 21 (LTS) + Spring Boot 4.1.1 | Exigência do desafio; versões atuais e suportadas |
| Persistência | Spring Data JPA | Mapeamento objeto-relacional simples e testável |
| Banco (default) | **H2 em arquivo** (`./data`) | Persiste entre reinícios **sem nenhuma dependência externa** para o avaliador rodar |
| Banco (produção/perf) | **PostgreSQL** via profile `postgres` | Cenário realista de alto volume |
| Migrations | Flyway | Schema versionado e reproduzível (SQL padrão compatível com H2 e Postgres) |
| Documentação | springdoc-openapi (Swagger UI) | Documentação viva da API |
| Observabilidade | Spring Actuator | Health e métricas para os testes de performance |
| Boilerplate | Lombok | Menos código repetitivo nas entidades |
| Qualidade | JaCoCo | Relatório de cobertura (`target/site/jacoco/index.html`) |

**Organização do código** – pacotes por *feature* (domínio), não por camada técnica. Cada
domínio concentra entidade, repositório, serviço, controller e DTOs:

```
com.desafio.votacao
├── pauta/      cadastro e consulta de pautas
├── sessao/     abertura e consulta de sessões de votação
├── voto/       registro de votos e apuração do resultado
├── tela/       telas JSON consumidas pelo app mobile (Anexo 1)
├── cpf/        integração externa de validação de CPF (Bônus 1)
├── config/     OpenAPI e propriedades configuráveis
└── exception/  tratamento centralizado de erros (RestControllerAdvice)
```

Princípios aplicados: design simples (sem over-engineering), regras de negócio nos *services*,
controllers finos, DTOs separados das entidades, e validações tanto na borda (Bean Validation)
quanto no banco (constraints).

## Como executar

**Pré-requisitos:** Java 21+ e Maven 3.9+ (ou use o `mvnw`/`mvnw.cmd` incluído, sem instalar Maven).
Quem usa `asdf`/`mise` já tem a versão fixada em [`.tool-versions`](.tool-versions).
Para a Opção 2, também Docker + Docker Compose.

A aplicação pode rodar com **H2** (default, zero dependência externa) ou **PostgreSQL** (via Docker).
Escolha conforme o cenário:

| | H2 em arquivo (default) | PostgreSQL (profile `postgres`) |
|---|---|---|
| Dependência externa | Nenhuma | Docker |
| Persistência | Arquivo em `./data` | Volume Docker |
| Indicado para | Avaliar/rodar rápido | Produção e testes de performance |

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

### Opção 2 — PostgreSQL (via Docker Compose)

O arquivo [`docker-compose.yml`](docker-compose.yml) sobe um PostgreSQL 16 já configurado
(database/usuário/senha `votacao`) com volume persistente e healthcheck.

```bash
# 1. Subir o banco
docker compose up -d

# 2. Rodar a aplicação no profile postgres
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
#   ou: java -jar target/votacao-1.0.0.jar --spring.profiles.active=postgres

# 3. Ao terminar, parar o banco (use -v para também apagar os dados)
docker compose down
```

As credenciais/URL têm valores default e podem ser sobrescritas por variáveis de ambiente:

| Variável | Default |
|----------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/votacao` |
| `DB_USERNAME` | `votacao` |
| `DB_PASSWORD` | `votacao` |
| `DB_POOL_SIZE` | `20` |
| `POSTGRES_PORT` | `5432` (porta publicada pelo `docker compose`; use outra se a 5432 já estiver ocupada) |

> O mesmo schema (Flyway, SQL padrão) é aplicado automaticamente em ambos os bancos, então não há
> passo manual de criação de tabelas em nenhuma das opções.

### Configurações relevantes (`application.yml`)

```yaml
votacao:
  sessao:
    duracao-padrao-minutos: 1        # default de 1 minuto quando a abertura não informa duração
  cpf-client:
    modo: fake                       # fake (default, sem rede) | http (chama o serviço real)
    base-url: https://user-info.herokuapp.com
  ui:
    base-url: ""                     # vazio = deriva da própria requisição
```

Todas têm variável de ambiente equivalente:

| Variável | Default | Para quê |
|----------|---------|----------|
| `CPF_CLIENT_MODO` | `fake` | `http` ativa a chamada real a `{base-url}/users/{cpf}` |
| `CPF_CLIENT_BASE_URL` | `https://user-info.herokuapp.com` | domínio do serviço externo |
| `UI_BASE_URL` | *(vazio)* | domínio usado nas URLs de callback das telas |

**URLs de callback parametrizáveis** (conforme a dica do enunciado). Com `UI_BASE_URL` vazio
(default), o domínio das URLs das telas é derivado da própria requisição — o mesmo servidor atende
emulador e dispositivo físico sem reconfiguração:

| Requisição chega em | URL devolvida nas telas |
|---------------------|-------------------------|
| `localhost:8080` | `http://localhost:8080/api/v1/ui/votacao` |
| `10.0.2.2:8080` (emulador Android) | `http://10.0.2.2:8080/api/v1/ui/votacao` |
| `192.168.0.42:8080` (dispositivo físico) | `http://192.168.0.42:8080/api/v1/ui/votacao` |

Definindo `UI_BASE_URL`, ele passa a valer para todas as requisições — o caso de estar atrás de um
proxy/gateway com domínio próprio:

```bash
UI_BASE_URL=https://votacao.cooperativa.com.br java -jar target/votacao-1.0.0.jar
# telas passam a apontar para https://votacao.cooperativa.com.br/api/v1/ui/...
```

O mesmo vale para o serviço externo de CPF, via `CPF_CLIENT_BASE_URL`.

## Documentação da API (Swagger)

Com a aplicação no ar:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Console H2 (profile default): <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:file:./data/votacao`)
- Health: <http://localhost:8080/actuator/health>

A documentação cobre as 12 operações (REST + telas), agrupadas por tag, com os **status de erro
de cada endpoint** e o schema do corpo — `ApiError` nas rotas REST e `TelaFormulario` nas rotas de
tela, que devolvem erro como tela. Os códigos e descrições ficam nos controllers; o corpo é
preenchido uma vez só, por um `OpenApiCustomizer`, em vez de repetido em cada anotação.

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

Cada pauta tem **uma única** sessão de votação (garantido por constraint no banco): reabrir a
votação descartaria a apuração e permitiria que o mesmo associado votasse duas vezes na pauta.

## Telas do app mobile (Anexo 1)

O foco da avaliação é a comunicação com o app mobile: o servidor devolve **a tela pronta**, já com
a URL e o `body` que o app deve enviar ao acionar cada botão/item. O app não precisa conhecer
rotas — a navegação é dirigida pelo servidor.

| Método | Caminho | Tela devolvida |
|--------|---------|----------------|
| `GET`/`POST` | `/api/v1/ui/pautas` | `SELECAO` — pautas em deliberação (entrada do fluxo) |
| `POST` | `/api/v1/ui/votacao` | `FORMULARIO` — votação da pauta (ou o resultado, se a sessão já encerrou) |
| `POST` | `/api/v1/ui/votos` | `FORMULARIO` — confirmação do voto |
| `POST` | `/api/v1/ui/resultado` | `FORMULARIO` — apuração |

Fluxo: `GET /ui/pautas` → o usuário toca numa pauta (o app faz `POST` na `url` do item com o
`body` dele) → tela de votação → o usuário digita o CPF e toca **Sim**/**Não** (o app envia o
`body` do botão **acrescido** do campo preenchido) → confirmação → resultado.

```bash
curl http://localhost:8080/api/v1/ui/pautas
```

```json
{
  "tipo": "SELECAO",
  "titulo": "Pautas em deliberacao",
  "itens": [
    {
      "titulo": "Reforma do estatuto",
      "descricao": "Aprovar reforma",
      "url": "http://localhost:8080/api/v1/ui/votacao",
      "body": { "pautaId": 1 }
    }
  ]
}
```

```bash
curl -X POST http://localhost:8080/api/v1/ui/votacao \
  -H "Content-Type: application/json" -d '{"pautaId":1}'
```

```json
{
  "tipo": "FORMULARIO",
  "titulo": "Reforma do estatuto",
  "itens": [
    { "tipo": "LABEL", "titulo": "Descricao", "valor": "Aprovar reforma" },
    { "tipo": "LABEL", "titulo": "Sessao aberta ate", "valor": "05/09/2026 09:27:12" },
    { "tipo": "TEXTO", "id": "associadoId", "titulo": "Informe seu CPF" }
  ],
  "botoes": [
    { "titulo": "Sim", "url": "http://localhost:8080/api/v1/ui/votos", "body": { "pautaId": 1, "opcao": "SIM" } },
    { "titulo": "Nao", "url": "http://localhost:8080/api/v1/ui/votos", "body": { "pautaId": 1, "opcao": "NAO" } }
  ]
}
```

Ao tocar em **Sim**, o app envia o `body` do botão somado ao campo digitado:

```bash
curl -X POST http://localhost:8080/api/v1/ui/votos \
  -H "Content-Type: application/json" \
  -d '{"pautaId":1,"opcao":"SIM","associadoId":"12345678909"}'
```

Tipos de campo suportados: `TEXTO`, `NUMERICO`, `DATA` (entrada do usuário) e `LABEL`
(somente leitura). **Erros também voltam como tela** `FORMULARIO` (com o status HTTP correto),
já que o app só sabe renderizar telas:

```json
{
  "tipo": "FORMULARIO",
  "titulo": "Nao foi possivel concluir",
  "itens": [{ "tipo": "LABEL", "titulo": "Motivo", "valor": "Associado ja votou nesta pauta" }],
  "botoes": [{ "titulo": "Voltar", "url": "http://localhost:8080/api/v1/ui/pautas", "body": {} }]
}
```

> As rotas REST de `/api/v1/pautas` continuam disponíveis para qualquer outro cliente; as telas
> são uma camada de apresentação sobre o mesmo domínio, sem duplicar regra de negócio.

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

Relatório de cobertura em `target/site/jacoco/index.html`: **97,8% de linhas** e **80% de branches**
(53 testes).

- **Unitários** (`VotoServiceTest`, `SessaoVotacaoServiceTest`) – regras de negócio isoladas com
  Mockito: sessão fechada, CPF inválido (404), `UNABLE_TO_VOTE`, voto duplicado, corrida de voto
  duplicado (constraint), apuração (aprovada/empate/sem votos), duração default vs. informada,
  sessão já aberta e tentativa de reabertura.
- **Client externo** (`HttpCpfValidationClientTest`) – `MockRestServiceServer`: resposta de sucesso,
  404 JSON do serviço → `CpfInvalidoException`, 404 HTML de gateway → 503, indisponibilidade → 503.
- **Integração** (`VotacaoIntegrationTest`) – fluxo completo via `MockMvc` sobre o contexto real
  (H2 em memória): cadastro → abertura → votos → apuração, duração default de 1 minuto, voto
  duplicado (409), CPF inválido (404), `UNABLE_TO_VOTE` (422), voto sem sessão (404), segunda
  sessão na mesma pauta (409) e os caminhos de erro de borda (400/404/405).
- **Telas** (`TelaIntegrationTest`) – contrato do Anexo 1: tipo da tela, campos, URLs e `body` dos
  botões, fluxo voto → resultado e erro devolvido como tela.

### Análise estática (SonarQube)

O `docker-compose.yml` traz um SonarQube em um profile separado, para não subir junto no uso comum:

```bash
# 1. Subir o SonarQube (leva ~1 min para ficar UP)
docker compose --profile qualidade up -d sonarqube
#   porta configurável: SONAR_PORT=9002 docker compose --profile qualidade up -d sonarqube

# 2. Gerar o relatório de cobertura e enviar a análise
mvn clean verify
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<TOKEN>

# 3. Dashboard: http://localhost:9000/dashboard?id=desafio-votacao
```

O token sai em *My Account → Security → Generate Token* (login inicial `admin`/`admin`). O
`sonar-maven-plugin` e o caminho do relatório do JaCoCo já estão configurados no `pom.xml`.

Resultado da última análise:

| Métrica | Valor |
|---------|-------|
| Quality Gate | **OK** |
| Bugs | 0 — Reliability **A** |
| Vulnerabilidades / Hotspots | 0 / 0 — Security **A** |
| Code smells | 0 — Maintainability **A** (dívida técnica 0 min) |
| Cobertura | 95,3% |
| Duplicação | 0,0% |

A primeira análise apontou 3 bugs e 27 code smells; o que veio de lá:

- **Relógio injetável.** Havia oito `LocalDateTime.now()` espalhados, sem fuso explícito. Agora um
  bean `Clock` é injetado e as entidades recebem o instante em vez de capturá-lo — o que também
  permitiu testar a janela da sessão com `Clock.fixed`, sem depender do relógio da máquina.
- **Rota de tela duplicada.** O caminho das telas estava escrito no `@RequestMapping` do controller
  *e* no service que monta a URL de callback. Mudar um lado faria as telas apontarem para uma rota
  inexistente, sem erro de compilação. Unificado em `TelaRotas`.
- **`@Transactional` sem efeito.** `buscarPorPauta` chamava `encontrarPorPauta` via `this`:
  auto-invocação não passa pelo proxy do Spring, então a anotação era decorativa.

## Tarefas bônus

### Bônus 1 — Integração com sistema externo (validação de CPF)

`CpfValidationClient` é a abstração do serviço externo, com duas implementações selecionadas por
configuração (`votacao.cpf-client.modo`), sem `if` espalhado pelo domínio:

| Modo | Implementação | Comportamento |
|------|---------------|---------------|
| `fake` (default) | `FakeCpfValidationClient` | Aleatório e local — roda sem rede, para o avaliador |
| `http` | `HttpCpfValidationClient` | `GET {base-url}/users/{cpf}` via `RestClient` |

```bash
CPF_CLIENT_MODO=http CPF_CLIENT_BASE_URL=https://user-info.herokuapp.com \
  java -jar target/votacao-1.0.0.jar
```

No modo `http`: **404** do serviço → 404 na API; `UNABLE_TO_VOTE` → 422; serviço fora do ar ou
timeout → **503** (`ExternalServiceException`), com timeouts em `spring.http.clients.*`.

> ⚠️ **O serviço do enunciado está fora do ar.** `https://user-info.herokuapp.com` responde
> `404` com o HTML `No such app` do `heroku-router` (os dynos gratuitos foram desligados pela
> Heroku). Por isso o modo default é `fake` — a aplicação roda e é avaliável sem essa dependência.
>
> Um detalhe que isso expõe: **404 nem sempre significa "CPF inválido"**. Se o client confiasse no
> status cru, um serviço inexistente reprovaria *todos* os associados como CPF inválido,
> silenciosamente e para sempre. O client só trata 404 como CPF inválido quando a resposta é JSON
> (veio do serviço); 404 de gateway/roteador vira **503** com a URL no erro.

#### Serviço fake hospedado (`fake-cpf`)

Como o endereço do enunciado não existe mais, foi criado um serviço com o **mesmo contrato**, para
que a integração possa ser exercitada de verdade: **`fake-cpf`** (Python, sem dependências),
hospedado em <https://fake-cpf.vercel.app>.

```bash
CPF_CLIENT_MODO=http \
CPF_CLIENT_BASE_URL=https://fake-cpf.vercel.app \
  java -jar target/votacao-1.0.0.jar
```

```bash
curl https://fake-cpf.vercel.app/users/94198571317
# {"status":"ABLE_TO_VOTE"}
```

> Nenhuma linha de código muda para trocar de serviço: só a variável de ambiente. É a mesma
> abstração `CpfValidationClient` atendendo fake local, `fake-cpf` hospedado ou o serviço real,
> caso um dia volte ao ar.

Ele expõe `GET /users/{cpf}` devolvendo `{"status":"ABLE_TO_VOTE"}`, `{"status":"UNABLE_TO_VOTE"}`
ou `404`, com CPFs fixos para teste determinístico (todos gerados, com DV válido, não pertencem a
ninguém):

| CPF | Resposta do fake | Resposta da API de Votação |
|-----|------------------|----------------------------|
| `94198571317` | `200 ABLE_TO_VOTE` | `201 Created` — voto registrado |
| `04320930010` | `200 ABLE_TO_VOTE` | `201 Created` |
| `39923590186` | `200 ABLE_TO_VOTE` | `201 Created` |
| `60416909027` | `200 ABLE_TO_VOTE` | `201 Created` |
| `08610283925` | `200 ABLE_TO_VOTE` | `201 Created` |
| `81468381032` | `200 UNABLE_TO_VOTE` | `422 Unprocessable Entity` |
| `36855860257` | `200 UNABLE_TO_VOTE` | `422` |
| `14838847246` | `200 UNABLE_TO_VOTE` | `422` |
| `77976601075` | `200 UNABLE_TO_VOTE` | `422` |
| `89661867934` | `404` (DV inválido) | `404 Not Found` |
| `49634608231` | `404` | `404` |
| `33110010446` | `404` | `404` |
| `11845814879` | `404` | `404` |

E CPFs que **simulam o serviço com problema**, para exercitar o tratamento de erro sem derrubar nada:

| CPF | O que o fake faz | Resposta da API de Votação |
|-----|------------------|----------------------------|
| `26982080820` | `503` indisponível | `503 Service Unavailable` |
| `12162240050` | `503` indisponível | `503` |
| `85830229765` | `404` em **HTML** (`No such app`) | `503` — não confunde com CPF inválido |
| `36334378511` | `404` em **HTML** | `503` |
| `75622539943` | Demora 8s | `503` — estoura o read-timeout de 5s |
| `95335180579` | Demora 8s | `503` |

Qualquer outro CPF é simulado: DV inválido → `404`; DV válido → sorteia `ABLE_TO_VOTE` /
`UNABLE_TO_VOTE` a cada chamada, como o serviço original.

Os 19 casos acima foram verificados ponta a ponta com a aplicação apontando para o serviço
publicado (`CPF_CLIENT_BASE_URL=https://fake-cpf.vercel.app`) — todos conforme o esperado:

```
CPF           ESPERADO                         OBTIDO
94198571317   201 ABLE_TO_VOTE                 201 (0.696101s)
81468381032   422 UNABLE_TO_VOTE               422 (0.237140s)
89661867934   404 CPF invalido                 404 (0.218354s)
26982080820   503 fora do ar                   503 (0.249758s)
85830229765   503 gateway 404 HTML             503 (0.309220s)
75622539943   503 read-timeout                 503 (5.012604s)
```

O último caso mostra o read-timeout cortando em 5s um serviço que levaria 8s — sem ele, uma
dependência lenta seguraria threads do servidor indefinidamente.

### Bônus 2 — Performance

Decisões para suportar **centenas de milhares de votos**:

- **Apuração por agregação no banco** — uma única query `COUNT ... GROUP BY opcao` (não duas), em
  vez de carregar votos em memória: custo de transferência constante, independente do volume.
- **Índice composto** `idx_voto_sessao_opcao (sessao_id, opcao)` que cobre exatamente a query de
  contagem.
- **Unicidade garantida no banco** (`uk_voto_sessao_associado`) — evita race conditions de voto
  duplicado sob concorrência, em vez de depender só de checagem em memória.
- `open-in-view: false`, *batch inserts* do Hibernate e pool de conexões dimensionável.
- Profile **PostgreSQL** + `docker-compose.yml` para o cenário de carga.

Script de carga com **k6** em [`performance/load-test.js`](performance/load-test.js) (rampa até
200 usuários virtuais, *threshold* de p95 < 300ms). Instruções no cabeçalho do arquivo.

Medições feitas neste projeto (PostgreSQL 16 em Docker, notebook):

| Cenário | Resultado |
|---------|-----------|
| `GET /resultado` com **300.004 votos** na sessão | ~50 ms |
| 100 chamadas concorrentes a `/resultado` (300k votos) | ~1,6 s no total |
| 30 votos idênticos simultâneos do mesmo associado | exatamente **1 × 201** e **29 × 409** |

O último confirma que a unicidade não depende da checagem em memória: sob concorrência real quem
garante é a constraint `uk_voto_sessao_associado`.

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
   As telas do app (`/api/v1/ui/...`) seguem o mesmo prefixo, então o cliente mobile troca uma
   única URL base ao migrar de versão.

## Tratamento de erros

Respostas de erro padronizadas via `@RestControllerAdvice`:

| Situação | HTTP |
|----------|------|
| Recurso inexistente / CPF inválido | `404 Not Found` |
| Payload inválido (Bean Validation) | `400 Bad Request` (com `fieldErrors`) |
| Regra de negócio (sessão fechada, `UNABLE_TO_VOTE`) | `422 Unprocessable Entity` |
| Associado já votou / pauta já tem sessão | `409 Conflict` |
| JSON mal formado, enum inválido, id não numérico | `400 Bad Request` |
| Método HTTP não suportado na rota | `405 Method Not Allowed` |
| Serviço externo de CPF indisponível | `503 Service Unavailable` |
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

## Por que cada escolha

Resumo das decisões tomadas, na ordem em que aparecem no código.

| Decisão | Motivo |
|---|---|
| **Pacotes por feature** (`pauta`, `sessao`, `voto`, `tela`, `cpf`) e não por camada | Cada mudança fica contida em uma pasta; evita o vaivém entre `controllers/`, `services/`, `repositories/` |
| **H2 em arquivo como default**, PostgreSQL por profile | O avaliador roda `java -jar` e tem persistência real, sem instalar nada. O Postgres existe para o cenário de volume |
| **Flyway** em vez de `ddl-auto: update` | Schema versionado e idêntico nos dois bancos; `ddl-auto: validate` garante que a entidade não divergiu da tabela |
| **Regras no service**, controllers finos, DTOs separados das entidades | O controller traduz HTTP, o service decide, a entidade não vaza para o contrato |
| **Unicidade do voto no banco** (`uk_voto_sessao_associado`), não só em memória | Checagem em memória não sobrevive à concorrência: 30 votos simultâneos passariam todos pelo `exists` antes do primeiro `insert` commitar. Verificado: 1× 201, 29× 409 |
| **Uma sessão por pauta** (`uk_sessao_pauta`) | Reabrir a votação descartaria a apuração e liberaria o mesmo associado para votar de novo, já que a unicidade é por sessão |
| **Apuração por `GROUP BY` no banco** | O custo de resposta não cresce com o número de votos: ~50 ms com 300 mil votos |
| **Client de CPF por trás de uma interface**, escolhido por configuração | Trocar fake ↔ HTTP real é variável de ambiente, não alteração de código. O `VotoService` não sabe qual está ativo |
| **404 só é "CPF inválido" se a resposta for JSON** | Um 404 de gateway (serviço fora do ar) reprovaria todo associado silenciosamente. Vira 503, que aponta a causa certa |
| **Telas montadas no servidor** (Anexo 1) | O app recebe a tela pronta, com URL e `body` de cada ação: não conhece rotas nem regra de negócio, e mudanças de fluxo não exigem republicar o app |
| **Erro das telas devolvido como tela**, com o status HTTP correto | O app só sabe renderizar telas; um `ApiError` ali seria uma tela em branco |
| **URL de callback derivada da requisição** por padrão | O mesmo servidor atende emulador e dispositivo físico sem reconfiguração; `UI_BASE_URL` sobrepõe quando há proxy/domínio próprio |
| **Versionamento por URI** (`/api/v1`) | Visível, fácil de rotear e de testar; o cliente mobile troca só o prefixo |
| **Lombok apenas nas entidades** | Corta getter/setter onde é repetitivo, sem esconder lógica |
| **Sem cache, sem fila, sem evento** | Não há requisito que os justifique; o gargalo previsto (apuração) foi resolvido com índice e agregação |
