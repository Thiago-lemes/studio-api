# EduManager API (studio-api)

API de gestão para estúdio/escola de cursos: alunos e responsáveis, professores, salas, turmas com controle de choque de horário, matrículas e financeiro (contas a pagar/receber, pagamentos, dashboard e cobrança automática mensal).

Kotlin + Spring Boot, single-tenant, banco PostgreSQL com migrations versionadas.

---

## Índice

- [Stack](#stack)
- [Começando](#começando)
- [Criando o primeiro usuário](#criando-o-primeiro-usuário)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)
- [Autenticação e perfis](#autenticação-e-perfis)
- [Arquitetura](#arquitetura)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Módulos e regras de negócio](#módulos-e-regras-de-negócio)
- [Endpoints](#endpoints)
- [Banco de dados e migrations](#banco-de-dados-e-migrations)
- [Jobs agendados](#jobs-agendados)
- [Testes](#testes)
- [Configuração](#configuração)
- [O que ainda não existe](#o-que-ainda-não-existe)
- [Documentos de referência](#documentos-de-referência)

---

## Stack

| Item | Versão / observação |
|---|---|
| Kotlin | 2.2.21 |
| Java | **21** (obrigatório — ver [Testes](#testes)) |
| Spring Boot | 4.0.8-SNAPSHOT (repositório `spring-snapshots` habilitado no `pom.xml`) |
| Banco | PostgreSQL 16 |
| Migrations | Flyway |
| Segurança | Spring Security + JWT (jjwt 0.12.6) |
| Docs | springdoc-openapi 3.0.2 |
| Testes | JUnit 5, `mockito-kotlin`, Testcontainers |
| Mensageria | RabbitMQ — **provisionado, mas ainda sem nenhum producer/consumer no código** |

---

## Começando

**Pré-requisitos:** JDK 21, Docker (para o banco e para os testes).

```bash
# 1. sobe Postgres e RabbitMQ
docker compose up -d

# 2. roda a aplicação (Flyway aplica as migrations na subida)
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

O `docker-compose.yaml` expõe Postgres em `5432` (banco `creative_studio`, usuário/senha `postgres`) e RabbitMQ em `5672`, com a UI de management em `15672` (`guest`/`guest`).

**Alternativa sem subir o compose** — o Spring Boot levanta Postgres e RabbitMQ descartáveis via Testcontainers:

```bash
./mvnw spring-boot:test-run
```

> Este comando baixa o `spring-boot-maven-plugin` do repositório de snapshots, então não funciona com a flag `-o` (offline).

---

## Criando o primeiro usuário

**Atenção:** o banco nasce sem nenhum usuário, e `POST /usuarios` exige um token de ADMIN. Num banco novo não há como se autenticar pela API — o primeiro ADMIN precisa ser inserido direto no banco.

```sql
-- senha: admin123
INSERT INTO usuario (nome, email, senha, role)
VALUES ('Admin', 'admin@edumanager.local',
        '$2a$10$GHau2l2h44cOCAhrlhEt4OHAfhF17GR7Vz0wdQGAXrN3rIiuLx7Ya',
        'ADMIN');
```

O hash acima é BCrypt e corresponde a `admin123` — troque a senha depois de entrar. Para gerar outro hash, use o `BCryptPasswordEncoder` (o mesmo bean que a aplicação usa para conferir a senha no login).

Em seguida:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@edumanager.local","senha":"admin123"}'
```

A resposta traz `token`, que vai no header `Authorization: Bearer <token>` das demais chamadas.

---

## Documentação da API (Swagger)

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Spec OpenAPI | http://localhost:8080/v3/api-docs |

São 42 operações documentadas, agrupadas em 12 tags, cada uma com os códigos de resposta que realmente devolve.

**Como autenticar na UI:** `POST /auth/login` → copie o `token` da resposta → botão **Authorize** → cole **só o token** (o `Bearer ` é adicionado pelo próprio Swagger).

A documentação é pública (a UI não carrega se o `/v3/api-docs` estiver protegido). Ela expõe apenas o contrato, nunca dado — mas se a API for exposta fora da rede interna, vale fechar essa regra por ambiente.

---

## Autenticação e perfis

Autenticação por JWT assinado em HMAC. O token carrega `sub` (id do usuário), `papel` (a role) e, quando o usuário é professor, `professorId`. Validade padrão: 8 horas.

`POST /auth/login` é o **único endpoint público** da API (junto com as rotas do Swagger).

| Módulo | ADMIN | SECRETARIA | PROFESSOR |
|---|:--:|:--:|:--:|
| Alunos, Professores, Turmas, Salas — leitura (GET) | ✅ | ✅ | ✅ |
| Alunos, Professores, Turmas, Salas — escrita | ✅ | ✅ | ❌ |
| Responsáveis, Matrículas, Usuários | ✅ | ✅ | ❌ |
| Financeiro (contas a pagar/receber, pagamentos, dashboard) | ✅ | ✅ | ❌ (nem leitura) |

Um usuário PROFESSOR só consegue consultar `GET /turmas/{id}/alunos` das turmas em que ele próprio é o professor — caso contrário recebe 403 com mensagem específica.

> 403 pode vir de duas origens: do `SecurityFilterChain` (bloqueio antes de qualquer controller, sem corpo customizado) ou de uma `AcessoNegadoException` lançada dentro do controller (com mensagem). Ao depurar um 403, vale confirmar qual dos dois é.

---

## Arquitetura

**MVC em camadas:** `Controller → Service → Repository`. O projeto já foi hexagonal e a indireção (ports/adapters, mapper Domain↔Entity) não se pagava para o porte dele — hoje a classe `@Entity` é domínio e persistência ao mesmo tempo, e não existe interface + `Impl` por padrão.

| Camada | Responsabilidade |
|---|---|
| `Entity` | Tabela e invariantes simples (`nullable = false`). Regras que dependem de consultar o banco ficam no Service, para não acoplar negócio ao ciclo de hidratação do Hibernate. |
| `Repository` | `JpaRepository<T, UUID>`, com buscas resolvidas por nomenclatura Spring Data. |
| `Service` | Toda a regra de negócio e a orquestração entre repositories de módulos diferentes. |
| `Controller` | Recebe DTO, desestrutura em parâmetros soltos para o Service, converte o retorno com `toResponse()`. |
| `dto/request`, `dto/response` | Contratos HTTP. O Service **nunca** recebe um tipo `XxxRequest` — sempre parâmetros soltos, para continuar chamável fora do contexto HTTP (jobs, consumers). |

### Hierarquia de exceções

Toda exceção de negócio herda de uma das quatro categorias abstratas em `shared/exception/`, e o `GlobalExceptionHandler` tem um handler por categoria — não um por exceção:

| Categoria base | HTTP |
|---|---|
| `NaoEncontradoException` | 404 |
| `RecursoJaExisteException` | 409 |
| `RegraDeNegocioException` | 400 |
| `AcessoNegadoException` | 403 |
| `CredenciaisInvalidasException` (fora da hierarquia, caso único de autenticação) | 401 |
| `MethodArgumentNotValidException` (erro de `@Valid`) | 400 |

**Ao criar uma exceção nova, basta herdar da categoria certa — o `GlobalExceptionHandler` nunca precisa ser tocado de novo.**

---

## Estrutura de pastas

Package by feature: cada módulo carrega suas próprias camadas.

```
src/main/kotlin/com/crative/studio_api/
├── usuario/        -> autenticação, contas de acesso, JWT
├── aluno/          -> AlunoEntity + ResponsavelEntity
├── professor/
├── academico/
│   ├── sala/
│   ├── turma/
│   └── matricula/  -> submódulo, inclui CalculoMensalidade.kt
├── financeiro/     -> contas a pagar/receber, pagamentos, dashboard, jobs
└── shared/
    ├── config/     -> SecurityConfig, JwtConfig, OpenApiConfig, SchedulingConfig
    ├── security/   -> JwtService, JwtAuthenticationFilter, AuthenticatedUserDetails
    ├── exception/  -> as 4 categorias-base
    └── web/        -> ErroResponse, GlobalExceptionHandler
```

---

## Módulos e regras de negócio

### Aluno e Responsável
CPF obrigatório e único; data de nascimento não pode ser futura. Idade e menoridade são **calculadas on-the-fly**, nunca persistidas. Inativação é soft delete (`ativo = false`), preservando histórico de matrículas.

Os responsáveis ficam em `/responsaveis/aluno/{alunoId}` em vez do aninhamento REST usual `/alunos/{id}/responsaveis` — decisão explícita de simplicidade.

### Professor
Duplicidade é checada por **nome + telefone** (por isso telefone é obrigatório: sem ele, dois professores sem telefone e de mesmo nome colidiriam indevidamente). A comparação é literal — "João" e "joão" ainda não são detectados como duplicata.

### Sala
CRUD simples, mas os paths **destoam do resto da API**: `/salas/listar`, `/salas/buscar/{id}`, `/salas/atualizar/{id}`, `/salas/delete/{id}`. A exclusão aqui é física, não soft delete.

### Turma
Criar ou editar uma turma dispara **duas** validações de choque de horário, ambas devolvendo 409:

1. **Sala** — nenhuma outra turma ativa pode ocupar a mesma sala, no mesmo dia, com horário sobreposto.
2. **Professor** — o mesmo professor não pode estar escalado em duas turmas sobrepostas, mesmo em salas diferentes.

A sobreposição é estrita (`inicio < fimExistente && inicioExistente < fim`): uma turma terminando às 14h e outra começando às 14h **não** conflitam. Na edição, a turma é excluída da própria checagem para não conflitar consigo mesma.

Toda turma retorna enriquecida com nome do professor, nome da sala e `vagasDisponiveis` (capacidade máxima menos as matrículas ATIVA).

### Matrícula
Vínculo aluno↔turma com os dados financeiros do contrato. Validações na criação:

- aluno existe e está ativo
- aluno menor de 18 anos precisa de **ao menos um responsável cadastrado**
- turma existe, está ativa e tem vaga
- não pode haver matrícula ATIVA ou TRANCADA do mesmo aluno na mesma turma (409)
- mensalidade > 0, dia de vencimento entre 1 e 28, desconto entre 0 e 100

**Rematrícula:** a tabela tem `UNIQUE(aluno_id, turma_id)`, então matricular de novo um aluno cuja matrícula naquela turma está CANCELADA **reaproveita a linha existente** com os novos valores, em vez de inserir outra (o que violaria a constraint).

**Transições de status:** ATIVA ↔ TRANCADA, e ambas → CANCELADA. CANCELADA é terminal — voltar a estudar exige criar a matrícula de novo. Cancelar grava a data de fim; reativar exige turma ativa e vaga livre.

Criar uma matrícula **já gera a conta a receber** da competência de início, na mesma transação.

### Financeiro
`valorEfetivo = valorMensalidade × (1 − desconto/100)`, arredondado em 2 casas. O valor é **copiado** para a conta a receber no momento em que a cobrança é gerada, para que uma cobrança já emitida não mude retroativamente se a mensalidade for renegociada.

**Contas a receber não têm POST** — só nascem pela criação da matrícula ou pelo job mensal. Uma conta cadastrada (a pagar) com vencimento já passado nasce ATRASADO, já que o job diário só alcança as que vencem depois de cadastradas.

**Pagamentos:** registrar um pagamento quita a conta no mesmo commit. Não há pagamento parcial — qualquer valor registrado leva a conta para PAGO.

**Dashboard** (`GET /financeiro/dashboard?referencia=AAAA-MM`):

| Campo | Definição |
|---|---|
| `totalReceberMes` | tudo que **vence** no mês, em qualquer status (o previsto) |
| `totalRecebidoMes` | pagamentos com data no mês — **por caixa, não por competência** (mensalidade de agosto paga em setembro entra em setembro) |
| `totalAtrasado` | todas as contas ATRASADO, de qualquer competência |
| `quantidadeAlunosInadimplentes` | alunos distintos por trás dessas contas |
| `saldoProjetadoMes` | `totalReceberMes − totalPagarMes` |

---

## Endpoints

42 operações. A lista completa e navegável está no [Swagger](#documentação-da-api-swagger).

| Módulo | Endpoints |
|---|---|
| **Auth** | `POST /auth/login` |
| **Usuários** | `POST /usuarios` · `GET /usuarios/{id}` |
| **Alunos** | `POST /alunos` · `GET /alunos` · `GET /alunos/{id}` · `PUT /alunos/{id}` · `DELETE /alunos/{id}` · `PATCH /alunos/{id}/status` |
| **Responsáveis** | `POST /responsaveis/aluno/{alunoId}` · `GET /responsaveis/aluno/{alunoId}` · `PUT /responsaveis/{id}` |
| **Professores** | `POST /professores` · `GET /professores` · `GET /professores/{id}` · `PUT /professores/{id}` · `DELETE /professores/{id}` · `PATCH /professores/{id}/status` |
| **Salas** | `POST /salas` · `GET /salas/listar` · `GET /salas/buscar/{id}` · `PATCH /salas/atualizar/{id}` · `DELETE /salas/delete/{id}` |
| **Turmas** | `POST /turmas` · `GET /turmas` · `GET /turmas/{id}` · `PUT /turmas/{id}` · `GET /turmas/{id}/alunos` |
| **Matrículas** | `POST /matriculas` · `GET /matriculas` (`?alunoId=` `?turmaId=` `?status=`) · `GET /matriculas/{id}` · `PATCH /matriculas/{id}/status` |
| **Contas a pagar** | `POST /contas-pagar` · `GET /contas-pagar` (`?status=`) · `GET /contas-pagar/{id}` · `PATCH /contas-pagar/{id}/pagar` |
| **Contas a receber** | `GET /contas-receber` (`?status=` `?matriculaId=`) · `GET /contas-receber/{id}` |
| **Pagamentos** | `POST /pagamentos` · `GET /pagamentos/{id}` · `GET /pagamentos?contaReceberId=` |
| **Dashboard** | `GET /financeiro/dashboard` (`?referencia=AAAA-MM`) |

Nos filtros de `GET /matriculas` os parâmetros são **exclusivos**, avaliados nesta ordem: `alunoId`, `turmaId`, `status`. Sem nenhum filtro, devolve as matrículas ATIVA.

---

## Banco de dados e migrations

Flyway em `src/main/resources/db/migration/`:

| Migration | Conteúdo |
|---|---|
| `V1` | professor |
| `V2` | usuario |
| `V3` | aluno |
| `V4` | responsavel |
| `V5` | sala |
| `V6` | turma + turma_dia_semana |
| `V7` | matricula |
| `V8` | conta_pagar, conta_receber, pagamento |

O JPA roda com **`ddl-auto: validate`** — o Hibernate nunca altera o schema, ele só confere que as entidades batem com o que a migration criou. Uma divergência (tipo de coluna, nome, nulidade) derruba a aplicação na subida, o que é proposital: pega o erro antes de virar bug em produção.

Ao adicionar uma coluna, altere **sempre os dois lados**: nova migration `V{n}` **e** a entidade. Rode `./mvnw test` depois — o teste de contexto sobe um Postgres real e valida o schema inteiro.

---

## Jobs agendados

`@EnableScheduling` fica em `shared/config/SchedulingConfig.kt`.

| Job | Quando | O que faz |
|---|---|---|
| `GerarMensalidadesJob` | dia 1, às 03:00 | Gera a cobrança do mês para cada matrícula ATIVA |
| `MarcarContasAtrasadasJob` | diário, às 02:00 | Marca como ATRASADO toda conta a receber PENDENTE já vencida |

A geração de mensalidade é **idempotente** graças ao `UNIQUE(matricula_id, referencia)`: rodar duas vezes no mesmo mês não duplica cobrança — e é isso que evita a duplicata quando uma matrícula nasce no dia 1 e o job roda logo depois.

Uma matrícula problemática é logada e **não aborta as demais** (o tratamento é por item, não uma transação única). O método `gerarPara(referencia)` é público e sem `@Scheduled`, o que permite disparo manual ou backfill de uma competência específica.

---

## Testes

```bash
export JAVA_HOME="$HOME/.jdks/openjdk-21.0.2"   # ajuste para o seu caminho
./mvnw test
```

> **Pegadinha conhecida:** se o `java` do seu PATH for mais antigo que o 21, o `mvnw compile` passa mas o `mvnw test` quebra com *"has been compiled by a more recent version of the Java Runtime"*. Não é bug do código — é o surefire rodando num JRE velho. Exporte o `JAVA_HOME` apontando para um JDK 21.

Os testes usam Testcontainers, então **o Docker precisa estar rodando**. O `StudioApiApplicationTests.contextLoads` sobe a aplicação inteira contra um Postgres real: valida o wiring de beans (incluindo ciclos de dependência) e, por causa do `ddl-auto: validate`, também pega divergências entre entidade JPA e migration.

Cobertura atual: `usuario` e `shared` (JWT, autenticação). **Matrícula e Financeiro ainda não têm testes.**

---

## Configuração

`src/main/resources/application.yaml`:

| Chave | Default | Observação |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/creative_studio` | |
| `spring.jpa.hibernate.ddl-auto` | `validate` | não mude para `update` — as migrations é que mandam no schema |
| `jwt.secret` | via env `JWT_SECRET` | **defina em produção**; o default é só para desenvolvimento |
| `jwt.expiration-ms` | `28800000` (8h) | |
| `server.port` | `8080` | |

> Ao mexer neste arquivo, cuidado com a indentação: `springdoc` e `server` são blocos **de nível raiz**, irmãos de `spring`, não filhos dele. Aninhados por engano, a configuração é silenciosamente ignorada.

---

## O que ainda não existe

- **Módulo Evento** e **Comunicação** — o `MarcarContasAtrasadasJob` já é o ponto natural de disparo do aviso de inadimplência
- **Fluxo de autocadastro de professor** com aprovação pelo ADMIN (por isso `POST /usuarios` rejeita `role = PROFESSOR`)
- **Endpoint de troca de senha**
- **Seed do primeiro ADMIN** — hoje é inserção manual no banco (ver [Criando o primeiro usuário](#criando-o-primeiro-usuário))
- **Pagamento parcial** de conta a receber
- **Testes** de Matrícula e Financeiro
- **RabbitMQ** está provisionado mas sem uso no código
- Mover a regra "professor só vê a própria turma" do Controller para o Service (débito técnico consciente)
- Normalizar nome (uppercase) na checagem de duplicidade de professor

---

## Documentos de referência

| Arquivo | Conteúdo |
|---|---|
| [`.doc/edumanager-api-design.md`](.doc/edumanager-api-design.md) | Design detalhado: decisões de arquitetura, o porquê de cada regra e os débitos registrados |
| [`.doc/edumanager-schema.sql`](.doc/edumanager-schema.sql) | Schema de referência |
| [`.doc/edumanager-frontend-guia.md`](.doc/edumanager-frontend-guia.md) | Guia para o frontend |
