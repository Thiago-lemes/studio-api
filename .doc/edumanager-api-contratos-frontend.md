# EduManager API — Endpoints e Contratos

Documento de integração para o front-end. Reflete o código em `main` (commit `5708bd6`).

- **Base URL (dev):** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html` · spec crua em `/v3/api-docs`
- **Content-Type:** `application/json` em todo request com body
- **Encoding de datas:** `LocalDate` → `"2026-09-18"`, `LocalTime` → `"14:00:00"`, `LocalDateTime` → `"2026-09-18T14:00:00"`

---

## 1. Autenticação

Todos os endpoints exigem JWT, **exceto** `POST /auth/login`.

Envie em cada chamada:

```
Authorization: Bearer <token>
```

O token expira em **8 horas** (28.800.000 ms). Não há refresh token — quando expirar, refaça o login.

### Claims do token (se você quiser ler no front)

| Claim | Conteúdo |
|---|---|
| `sub` | id do usuário (UUID) |
| `papel` | `ADMIN` \| `SECRETARIA` \| `PROFESSOR` |
| `professorId` | UUID do professor — **só existe quando o papel é PROFESSOR** |

> Prefira usar o objeto `usuario` devolvido no login em vez de decodificar o token.

### `POST /auth/login` — público

**Request**
```json
{
  "email": "admin@studio.com",
  "senha": "senha123"
}
```

**200 OK**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "usuario": {
    "id": "3f2a...",
    "nome": "Maria Admin",
    "email": "admin@studio.com",
    "role": "ADMIN",
    "professorId": null
  }
}
```

**Erros:** `400` payload inválido · `401` e-mail inexistente, senha incorreta ou usuário inativo.

---

## 2. Perfis e permissões

| Recurso | ADMIN | SECRETARIA | PROFESSOR |
|---|:---:|:---:|:---:|
| `GET` em `/alunos`, `/professores`, `/turmas`, `/salas` | ✅ | ✅ | ✅ |
| Escrita em `/alunos`, `/professores`, `/turmas`, `/salas` | ✅ | ✅ | ❌ |
| `/responsaveis` (inclusive GET) | ✅ | ✅ | ❌ |
| `/matriculas` (inclusive GET) | ✅ | ✅ | ❌ |
| `/contas-pagar`, `/contas-receber`, `/pagamentos`, `/financeiro` | ✅ | ✅ | ❌ |
| `/usuarios` | ✅ | ✅ | ❌ |
| `POST /usuarios/convites` (convidar) | ✅ | ❌ | ❌ |

Regra adicional em `GET /turmas/{id}/alunos`: um usuário `PROFESSOR` só enxerga as turmas em que **ele próprio** é o professor — turma de outro professor devolve `403`.

**Implicação prática para o menu do front:** com papel `PROFESSOR`, esconda Financeiro, Matrículas, Responsáveis, Usuários e todos os botões de criar/editar/excluir. Ele só lê alunos, professores, turmas e salas.

---

## 3. Formato de erro

Toda resposta de erro tratada tem o mesmo shape:

```json
{
  "mensagem": "Aluno não encontrado",
  "status": 404
}
```

| Status | Significado |
|---|---|
| `400` | Payload inválido (bean validation) ou regra de negócio violada |
| `401` | Sem token, token expirado/inválido, ou credenciais erradas no login |
| `403` | Autenticado, mas o papel não tem acesso ao recurso |
| `404` | Recurso não encontrado |
| `409` | Conflito — duplicidade ou choque de horário |

Em erro de validação (`400`), `mensagem` vem concatenada campo a campo:

```json
{
  "mensagem": "nome: must not be blank; email: Email inválido",
  "status": 400
}
```

---

## 4. Enums

| Enum | Valores |
|---|---|
| `RoleType` | `ADMIN`, `SECRETARIA`, `PROFESSOR` |
| `DiaSemanaType` | `SEG`, `TER`, `QUA`, `QUI`, `SEX`, `SAB`, `DOM` |
| `StatusMatriculaType` | `ATIVA`, `TRANCADA`, `CANCELADA` |
| `StatusContaType` | `PENDENTE`, `PAGO`, `ATRASADO` |
| `FormaPagamentoType` | `PIX`, `CARTAO`, `BOLETO`, `DINHEIRO` |
| `CategoriaDespesaType` | `ALUGUEL`, `SALARIO`, `MATERIAL`, `MARKETING`, `OUTROS` |

---

## 5. Usuários

> `POST /usuarios` (criação direta com senha escolhida pelo ADMIN) **não existe mais**. Usuário agora
> nasce pelo fluxo de convite abaixo, para os três papéis. Spec: [`spec-convite-cadastro-usuario.md`](./spec-convite-cadastro-usuario.md).

### `POST /usuarios/convites` → 201 (ADMIN)

**Request**
```json
{ "email": "joao@studio.com", "role": "SECRETARIA" }
```
Só e-mail e papel — inclusive para `PROFESSOR`. A ficha do professor (nome, telefone, especialidade)
é criada quando a própria pessoa completa o cadastro, então **o ADMIN não precisa cadastrar o professor antes**.

**201**
```json
{
  "id": "uuid",
  "email": "joao@studio.com",
  "role": "SECRETARIA",
  "status": "PENDENTE",
  "expiraEm": "2026-09-20T22:15:00",
  "linkConvite": "http://localhost:4200/completar-cadastro?token=8f3c...",
  "token": "8f3c..."
}
```

⚠️ `linkConvite` e `token` só existem **enquanto não há envio automático de e-mail**: nesta fase o ADMIN
copia o link e repassa. Os dois campos somem quando o notification-service entrar — a tela deve exibi-los
como "copie e envie", não depender deles em regime permanente.

Convidar um e-mail que já tem convite `PENDENTE` **não é erro**: o anterior é invalidado e vale o novo (reenvio).

**Erros:** `400` payload inválido · `401` sem token · `403` não é ADMIN · `409` e-mail já tem usuário.

### `GET /usuarios/convites/{token}` → 200 (público)

Chamado pela tela `/completar-cadastro?token=xxx` ao carregar.

```json
{ "email": "joao@studio.com", "role": "SECRETARIA" }
```

**Erros:** `404` token inexistente · `400` convite já utilizado ou expirado (mesma tela de erro nos dois casos:
"este link não é mais válido, peça um novo convite ao administrador").

### `POST /usuarios/convites/{token}/completar` → 201 (público)

**Request**
```json
{ "nome": "Marina Alves", "senha": "senhaSegura1", "telefone": "11999990000", "especialidade": "Ballet" }
```
`senha` com no mínimo 8 caracteres. **E-mail não vai no corpo** — vem do convite, e a tela só o exibe,
nunca editável.

Quando o `GET` do token devolve `role: "PROFESSOR"`, a tela precisa pedir **telefone** (obrigatório) e
**especialidade** (opcional), e é esse envio que cria a ficha do professor. Nos demais papéis os dois
campos podem ser omitidos.

Se já existir uma ficha com o mesmo **nome + telefone**, ela é reaproveitada em vez de duplicada — o caso
do professor que já dava aula e só agora está ganhando login. Se essa ficha já tiver um acesso, vem `409`
("já existe um acesso vinculado a este professor") e a pessoa precisa falar com o ADMIN.

**201 sem corpo.** Nenhum JWT é emitido aqui: redirecione para `/login`. Pode falhar com `400` mesmo depois
de o `GET` ter dado 200, se o convite expirar entre carregar a página e enviar o formulário.

**Erros:** `400` payload inválido, telefone ausente em convite de PROFESSOR, convite utilizado ou expirado ·
`404` token inexistente · `409` e-mail passou a ter usuário, ou ficha de professor já tem acesso.

### `GET /usuarios/{id}` → 200

```json
{ "id": "uuid", "nome": "João", "email": "joao@studio.com", "role": "SECRETARIA", "ativo": true }
```
`404` se não existir.

---

## 6. Alunos

`idade` e `menorDeIdade` são **calculados na resposta**, nunca enviados no request.

### Shape de resposta — `AlunoResponse`
```json
{
  "id": "uuid",
  "nome": "Ana Silva",
  "telefone": "11999998888",
  "dataNascimento": "2010-04-22",
  "cpf": "12345678901",
  "idade": 15,
  "menorDeIdade": true,
  "ativo": true
}
```

### `POST /alunos` → 201
```json
{
  "nome": "Ana Silva",
  "telefone": "11999998888",
  "dataNascimento": "2010-04-22",
  "cpf": "12345678901"
}
```
`nome` obrigatório · `telefone` opcional (nullable) · `dataNascimento` obrigatória e **não pode ser futura** · `cpf` obrigatório e único.

**Erros:** `400` payload inválido ou data futura · `409` CPF já cadastrado.

### `GET /alunos` → 200
Lista `AlunoResponse[]`. **Só alunos ativos** — inativados não aparecem.

### `GET /alunos/{id}` → 200 · `404`
Busca por id (traz também os inativos).

### `PUT /alunos/{id}` → 200
```json
{ "nome": "Ana Silva Souza", "telefone": "11999997777" }
```
Só nome e telefone. CPF e data de nascimento **não são editáveis** por aqui. `404` se não existir.

### `DELETE /alunos/{id}` → 204
Soft delete: marca `ativo = false`, preserva histórico de matrículas. `404` se não existir.

### `PATCH /alunos/{id}/status` → 200
```json
{ "ativo": true }
```
Diferente do DELETE, permite **reativar**. Devolve `AlunoResponse`.

---

## 7. Responsáveis

Aluno **menor de 18 anos precisa de ao menos um responsável** cadastrado antes de ser matriculado — a matrícula falha com `400` se não houver.

### Shape de resposta — `ResponsavelResponse`
```json
{
  "id": "uuid",
  "alunoId": "uuid",
  "nome": "Carlos Silva",
  "telefone": "11988887777",
  "cpf": "98765432100",
  "parentesco": "Pai"
}
```

### `POST /responsaveis/aluno/{alunoId}` → 201
```json
{
  "nome": "Carlos Silva",
  "telefone": "11988887777",
  "cpf": "98765432100",
  "parentesco": "Pai"
}
```
`nome` (máx. 150), `telefone` (máx. 20) e `parentesco` (máx. 50) obrigatórios · `cpf` opcional (máx. 14).

**Erros:** `400` payload inválido · `404` aluno não encontrado.

### `GET /responsaveis/aluno/{alunoId}` → 200
Lista `ResponsavelResponse[]` do aluno. `404` se o aluno não existir.

### `PUT /responsaveis/{id}` → 200
Mesmo body do POST (todos os campos, `cpf` opcional). `404` se o responsável não existir.

---

## 8. Professores

### Shape de resposta — `ProfessorResponse`
```json
{
  "id": "uuid",
  "nome": "Paula Ribeiro",
  "telefone": "11977776666",
  "especialidade": "Ballet clássico",
  "ativo": true
}
```

### `POST /professores` → 201
```json
{ "nome": "Paula Ribeiro", "telefone": "11977776666", "especialidade": "Ballet clássico" }
```
`nome` obrigatório · `telefone` obrigatório (é usado no controle de duplicidade) · `especialidade` opcional.

> A checagem de duplicidade é **nome + telefone**, comparação literal: `"João"` e `"joão"` hoje **não** colidem. Se quiser evitar isso na UI, normalize antes de enviar.

**Erros:** `400` payload inválido · `409` já existe professor com esse nome e telefone.

### `GET /professores` → 200
Lista `ProfessorResponse[]` — **só os ativos**.

### `GET /professores/{id}` → 200 · `404`

### `PUT /professores/{id}` → 200
Mesmo body do POST. `404` se não existir.

### `DELETE /professores/{id}` → 204
Soft delete (`ativo = false`). Turmas já criadas com esse professor continuam existindo. `404` se não existir.

### `PATCH /professores/{id}/status` → 200
```json
{ "ativo": true }
```

---

## 9. Salas

> ⚠️ **Os paths deste módulo destoam do resto da API** — usam `/listar`, `/buscar/{id}`, `/atualizar/{id}`, `/delete/{id}` em vez do path base REST. Atenção ao montar o client.

### Shape de resposta — `SalaResponse`
```json
{ "id": "uuid", "nome": "Sala 1", "capacidade": 20 }
```

### `POST /salas` → 201
```json
{ "nome": "Sala 1", "capacidade": 20 }
```
`nome` obrigatório e único · `capacidade` precisa ser **maior que zero**.

**Erros:** `400` nome em branco ou capacidade ≤ 0 · `409` já existe sala com esse nome.

### `GET /salas/listar` → 200
Lista `SalaResponse[]` (todas — não há filtro de ativo aqui).

### `GET /salas/buscar/{id}` → 200
**Atenção:** sala inexistente devolve `400`, não `404`.

### `PATCH /salas/atualizar/{id}` → 200
Apesar do verbo `PATCH`, **exige o payload completo** (`nome` e `capacidade`).
```json
{ "nome": "Sala 1 - Espelhada", "capacidade": 25 }
```
**Erros:** `400` sala não encontrada ou payload inválido · `409` nome já usado por outra sala.

### `DELETE /salas/delete/{id}` → 204
Diferente de aluno e professor, aqui a **exclusão é física** (não é soft delete). `400` se não encontrar.

---

## 10. Turmas

Toda criação ou edição valida **choque de horário em duas frentes**: a sala não pode estar ocupada e o professor não pode estar escalado em outra turma no mesmo intervalo.

> Turmas que se encostam **não** conflitam: uma terminando às 14h e outra começando às 14h são aceitas (a sobreposição é comparada de forma estrita).

### Shape de resposta — `TurmaResponse`
```json
{
  "id": "uuid",
  "modalidade": "Ballet Infantil",
  "professorId": "uuid",
  "professorNome": "Paula Ribeiro",
  "salaId": "uuid",
  "salaNome": "Sala 1",
  "diasSemana": ["SEG", "QUA"],
  "horarioInicio": "14:00:00",
  "horarioFim": "15:00:00",
  "capacidadeMaxima": 20,
  "vagasDisponiveis": 17,
  "ativa": true
}
```
`vagasDisponiveis` = `capacidadeMaxima` − matrículas com status `ATIVA`. Já vem pronto: **não calcule no front**.

### `POST /turmas` → 201
```json
{
  "modalidade": "Ballet Infantil",
  "professorId": "uuid",
  "salaId": "uuid",
  "diasSemana": ["SEG", "QUA"],
  "horarioInicio": "14:00:00",
  "horarioFim": "15:00:00",
  "capacidadeMaxima": 20
}
```
`modalidade` obrigatória · `diasSemana` não pode ser vazio (é um **Set** — valores repetidos são colapsados) · `capacidadeMaxima` precisa ser positiva.

**Erros:** `400` payload inválido · `404` professor ou sala não encontrados · `409` choque de horário na sala ou do professor.

### `GET /turmas` → 200
Lista `TurmaResponse[]` — **só as ativas**, já enriquecidas com nome do professor, nome da sala e vagas.

### `GET /turmas/{id}` → 200 · `404`

### `PUT /turmas/{id}` → 200
Mesmo body do POST, **sem `modalidade`** (não é editável):
```json
{
  "professorId": "uuid",
  "salaId": "uuid",
  "diasSemana": ["SEG", "QUA", "SEX"],
  "horarioInicio": "14:00:00",
  "horarioFim": "15:00:00",
  "capacidadeMaxima": 25
}
```
A revalidação de choque ignora a própria turma. **Erros:** `404` turma/professor/sala não encontrados · `409` choque de horário.

### `GET /turmas/{id}/alunos` → 200
Só matrículas com status `ATIVA`.
```json
[
  {
    "alunoId": "uuid",
    "nomeAluno": "Ana Silva",
    "matriculaId": "uuid",
    "statusMatricula": "ATIVA"
  }
]
```
**Erros:** `403` professor consultando turma de outro professor · `404` turma não encontrada.

---

## 11. Matrículas

Vínculo aluno ↔ turma com os dados financeiros do contrato. **Criar uma matrícula já gera a conta a receber** da competência de início.

### Shape de resposta — `MatriculaResponse`
```json
{
  "id": "uuid",
  "alunoId": "uuid",
  "nomeAluno": "Ana Silva",
  "turmaId": "uuid",
  "modalidadeTurma": "Ballet Infantil",
  "status": "ATIVA",
  "valorMensalidade": 300.00,
  "descontoPercentual": 10.00,
  "valorEfetivo": 270.00,
  "diaVencimento": 10,
  "dataInicio": "2026-09-01",
  "dataFim": null
}
```
`valorEfetivo` = mensalidade já com desconto aplicado, 2 casas, HALF_UP. **Não é persistido** — é calculado na resposta e copiado para a conta a receber no momento em que a cobrança é gerada, para a cobrança não mudar de valor retroativamente. Use esse campo na UI, não refaça a conta.

`dataFim` só é preenchida quando a matrícula é **cancelada**.

### `POST /matriculas` → 201
```json
{
  "alunoId": "uuid",
  "turmaId": "uuid",
  "valorMensalidade": 300.00,
  "diaVencimento": 10,
  "descontoPercentual": 10.00,
  "dataInicio": "2026-09-01"
}
```

| Campo | Regra |
|---|---|
| `valorMensalidade` | obrigatório, mínimo `0.01` |
| `diaVencimento` | inteiro de **1 a 28** (evita problema com mês curto) |
| `descontoPercentual` | opcional, default `0`; entre `0.00` e `100.00` |
| `dataInicio` | obrigatória |

**Validações de negócio:** aluno ativo, turma ativa, vaga disponível e responsável cadastrado quando o aluno é menor de 18 anos.

> Rematricular um aluno cuja matrícula naquela turma está `CANCELADA` **reaproveita o registro existente** com os novos valores — o id da matrícula volta a ser o mesmo de antes.

**Erros:** `400` aluno/turma inativos, turma sem vaga, menor sem responsável ou dados financeiros inválidos · `404` aluno ou turma não encontrados · `409` aluno já tem matrícula `ATIVA` ou `TRANCADA` nessa turma.

### `GET /matriculas` → 200

Query params — **os filtros são exclusivos e avaliados nesta ordem**: `alunoId` → `turmaId` → `status`. Mandar dois juntos não combina: só o primeiro da ordem vale.

| Param | Tipo | Efeito |
|---|---|---|
| `alunoId` | UUID | matrículas do aluno |
| `turmaId` | UUID | matrículas da turma |
| `status` | `StatusMatriculaType` | matrículas naquele status |
| *(nenhum)* | — | devolve as matrículas `ATIVA` |

Retorna `MatriculaResponse[]`. `404` se o aluno/turma do filtro não existir.

### `GET /matriculas/{id}` → 200 · `404`

### `PATCH /matriculas/{id}/status` → 200
```json
{ "status": "TRANCADA" }
```

Transições permitidas:

```
ATIVA  ⇄  TRANCADA
  ↓         ↓
    CANCELADA   (terminal)
```

- `CANCELADA` é **terminal** — voltar a estudar exige criar a matrícula de novo (que reaproveita o registro, ver POST).
- Cancelar grava a `dataFim`.
- Reativar (`→ ATIVA`) exige **turma ativa e vaga livre**.

**Erros:** `400` transição inválida, status repetido, turma inativa ou sem vaga para reativar · `404` matrícula não encontrada.

---

## 12. Financeiro — Contas a receber (mensalidades)

**Somente leitura: não existe POST.** A cobrança nasce junto com a matrícula ou pelo job mensal que roda todo dia 1. Um segundo job diário marca como `ATRASADO` as contas vencidas.

### Shape de resposta — `ContaReceberResponse`
```json
{
  "id": "uuid",
  "matriculaId": "uuid",
  "alunoId": "uuid",
  "nomeAluno": "Ana Silva",
  "referencia": "2026-09",
  "valor": 270.00,
  "vencimento": "2026-09-10",
  "status": "ATRASADO",
  "diasEmAtraso": 8
}
```
`referencia` é a competência no formato `AAAA-MM`. `diasEmAtraso` vem **`null` quando o status não é `ATRASADO`**.

### `GET /contas-receber` → 200
Query params **combináveis** (diferente de `/matriculas`):

| Param | Tipo |
|---|---|
| `status` | `PENDENTE` \| `PAGO` \| `ATRASADO` |
| `matriculaId` | UUID |

### `GET /contas-receber/{id}` → 200 · `404`

---

## 13. Financeiro — Pagamentos

### Shape de resposta — `PagamentoResponse`
```json
{
  "id": "uuid",
  "contaReceberId": "uuid",
  "valorPago": 270.00,
  "formaPagamento": "PIX",
  "pagoEm": "2026-09-18T10:32:00"
}
```

### `POST /pagamentos` → 201
```json
{
  "contaReceberId": "uuid",
  "valorPago": 270.00,
  "formaPagamento": "PIX"
}
```
Registra o pagamento e **quita a conta no mesmo commit**.

> **Não há pagamento parcial:** qualquer valor registrado leva a conta para `PAGO`. Não monte UI de "pagar parcialmente".

`valorPago` mínimo `0.01`.

**Erros:** `400` valor ≤ 0 · `404` conta a receber não encontrada · `409` conta já está paga.

### `GET /pagamentos?contaReceberId={uuid}` → 200
`contaReceberId` é **obrigatório**. Retorna `PagamentoResponse[]`.

### `GET /pagamentos/{id}` → 200 · `404`

---

## 14. Financeiro — Contas a pagar (despesas)

### Shape de resposta — `ContaPagarResponse`
```json
{
  "id": "uuid",
  "descricao": "Aluguel setembro",
  "categoria": "ALUGUEL",
  "valor": 3500.00,
  "vencimento": "2026-09-05",
  "status": "PENDENTE",
  "pagoEm": null
}
```

### `POST /contas-pagar` → 201
```json
{
  "descricao": "Aluguel setembro",
  "categoria": "ALUGUEL",
  "valor": 3500.00,
  "vencimento": "2026-09-05"
}
```
`descricao` obrigatória · `categoria` obrigatória (enum) · `valor` mínimo `0.01` · `vencimento` obrigatório.

> Conta cadastrada com vencimento já passado **nasce `ATRASADO`** — o job diário só alcança as que vencem depois de cadastradas.

**Erros:** `400` descrição em branco ou valor ≤ 0.

### `GET /contas-pagar` → 200
Param opcional `status` (`StatusContaType`). Sem filtro, devolve todas.

### `GET /contas-pagar/{id}` → 200 · `404`

### `PATCH /contas-pagar/{id}/pagar` → 200
**Body opcional.** Sem ele, a data de pagamento é o instante da chamada:
```json
{ "dataPagamento": "2026-09-18T10:00:00" }
```
**Erros:** `404` conta não encontrada · `409` conta já está quitada.

---

## 15. Financeiro — Dashboard

### `GET /financeiro/dashboard?referencia=2026-09` → 200

`referencia` é opcional no formato `AAAA-MM`; sem ela, usa o mês corrente. Formato errado → `400`.

```json
{
  "referencia": "2026-09",
  "totalReceberMes": 12000.00,
  "totalRecebidoMes": 9500.00,
  "totalAtrasado": 1800.00,
  "quantidadeAlunosInadimplentes": 6,
  "totalPagarMes": 7200.00,
  "saldoProjetadoMes": 4800.00
}
```

Definições que importam para rotular os cards corretamente:

| Campo | O que é |
|---|---|
| `totalReceberMes` | tudo que **vence** no mês, em qualquer status — é o **previsto** |
| `totalRecebidoMes` | pagamentos com **data no mês** — por caixa, não por competência (mensalidade de agosto paga em setembro entra em setembro) |
| `totalAtrasado` | todas as contas `ATRASADO` de **qualquer competência**, não só do mês |
| `quantidadeAlunosInadimplentes` | alunos **distintos** por trás dessas contas atrasadas |
| `totalPagarMes` | despesas que vencem no mês |
| `saldoProjetadoMes` | `totalReceberMes` − `totalPagarMes` (previsto, não realizado) |

⚠️ `totalRecebidoMes` e `totalReceberMes` **não são comparáveis diretamente** (caixa vs. competência). Evite montar um "% recebido" dividindo um pelo outro.

---

## 16. Pontos de atenção para o front

1. **CORS não está configurado na API.** Chamada direta do browser em outra origem vai ser bloqueada. Até isso ser resolvido no backend, use proxy no dev server (Vite/Next `rewrites`) ou peça a liberação.
2. **`/auth/registrar-professor` está liberado no Security mas não existe controller.** Chamar esse path devolve `404`. Não implemente tela de autocadastro de professor ainda.
3. **Sem paginação.** Todos os `GET` de lista devolvem array puro, sem envelope `content`/`totalPages`. Se o volume crescer, isso vai mudar — encapsule as listas numa camada de serviço no front para o dia em que virar paginado.
4. **Sem refresh token.** Trate `401` globalmente: limpe a sessão e mande para o login.
5. **Paths de `/salas` são irregulares** (`/listar`, `/buscar/{id}`, `/atualizar/{id}`, `/delete/{id}`) e sala inexistente devolve `400` em vez de `404`.
6. **Valores monetários são `BigDecimal`** e chegam como número JSON (ex.: `270.00`). Se precisar de precisão exata em cálculo, trate como string/decimal — não como `float`.
7. **Não recalcule no front:** `idade`, `menorDeIdade`, `vagasDisponiveis`, `valorEfetivo` e `diasEmAtraso` já vêm prontos da API.
8. **Filtros de `/matriculas` são exclusivos**, os de `/contas-receber` são combináveis. Não trate os dois da mesma forma.

---

## 17. Fluxo de referência (happy path)

```
1. POST /auth/login                          → guarda token + usuario
2. POST /alunos                              → cria o aluno
3. POST /responsaveis/aluno/{alunoId}        → obrigatório se menorDeIdade
4. POST /professores  +  POST /salas         → pré-requisitos da turma
5. POST /turmas                              → valida choque de horário
6. POST /matriculas                          → já gera a 1ª conta a receber
7. GET  /contas-receber?matriculaId={id}     → lista as cobranças
8. POST /pagamentos                          → quita a conta
9. GET  /financeiro/dashboard                → consolidado do mês
```
