# Banco de Conhecimento 📚

Sistema web em **Java + Spring Boot** com tela de **login**, tela de **gerenciamento**,
banco de dados **SQL** (H2 em arquivo), **Swagger** funcionando e armazenamento de
**fotos e vídeos dentro do banco de dados** — com **limite de tamanho por tipo**.

## Como rodar

### No terminal
```bash
cd "Projeto Banco de conhecimento"
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn spring-boot:run
```
Ou com o jar já gerado:
```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 java -jar target/banco-conhecimento-1.0.0.jar
```

### No NetBeans
1. **Arquivo → Abrir Projeto** e selecione a pasta `Projeto Banco de conhecimento`
   (o NetBeans reconhece o projeto Maven pelo `pom.xml`).
2. Confira se o **Java 21** está selecionado (Projeto → Propriedades → Sources →
   Java Platform). O JDK 21 do sistema serve: `/usr/lib/jvm/java-21-openjdk-amd64`.
3. Clique com o direito no projeto → **Run** (executar).

## Acessos

| O quê | Endereço |
|---|---|
| Tela de login | http://localhost:8180/login.html |
| Tela de gerenciamento | http://localhost:8180/app.html (após login) |
| **Swagger (documentação da API)** | http://localhost:8180/swagger-ui.html |
| Console do banco (opcional, ADMIN) | http://localhost:8180/h2-console |

> **Segurança**: qualquer URL do Swagger (`/swagger-ui.html`,
> `/swagger-ui/index.html`, `/v3/api-docs*`) redireciona para
> `/swagger-gate.html`. A liberação exige login com perfil **ADMIN** (direção) e
> a confirmação da **senha do administrador**; USER1 e USER2 não liberam. A senha
> é conferida contra o hash BCrypt do usuário logado, a liberação vale apenas
> naquela sessão e some no `logout`. O console do H2 exige sessão ADMIN e, sem
> ela, volta para a tela de login.

## Usuários e perfis (criados automaticamente na 1ª execução)

| Login | Senha | Perfil | Cargo | Pode |
|---|---|---|---|---|
| `admin` | `admin123` | **ADMIN** | Direção | Tudo (inclui, altera, exclui, vê, gerencia usuários) |
| `user1` | `user123` | **USER1** | Administrativo | Incluir, alterar e visualizar (não exclui, não gerencia usuários) |
| `user2` | `user123` | **USER2** | Operacional | Somente visualizar |

As permissões são aplicadas **no servidor** (HTTP 403 em criação/edição/exclusão
para quem não tem cargo), não apenas escondendo botões na tela. O perfil ADMIN
também tem a tela **"Usuários"** no cabeçalho para cadastrar/editar/excluir
usuários, com proteções: login duplicado é recusado, não é possível excluir a
si mesmo nem o último administrador. Senhas são armazenadas com BCrypt.

## Limites de tamanho (o que você pediu)

| Tipo | Limite | Onde mudar (`src/main/resources/application.properties`) |
|---|---|---|
| **Vídeo** | **50 MB** | `app.upload.video-max-mb=50` |
| Foto | 5 MB | `app.upload.foto-max-mb=5` |
| Outros arquivos | 10 MB | `app.upload.arquivo-max-mb=10` |

Quando o arquivo passa do limite, a API responde **HTTP 413** com a mensagem
`"Limite excedido: o video tem 55,0 MB e o maximo permitido e 50 MB."` e a tela
mostra o aviso. O limite global do multipart está em 200 MB
(`spring.servlet.multipart.max-file-size`).

## Banco de dados

Os dados ficam no **arquivo** `./data/banco-conhecimento.mv.db` (SQL/H2) —
fecha o app, abre de novo, os registros continuam lá. As fotos e vídeos são
guardados em coluna **BLOB** nas tabelas:

- `usuarios` — login, senha (BCrypt) e nome
- `itens_conhecimento` — título, categoria, descrição, autor, data
- `anexos` — nome, tipo, tamanho e **o conteúdo binário** (foto/vídeo)

### Trocar para MySQL ou PostgreSQL depois
Basta trocar no `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/banco_conhecimento
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA
```
e adicionar a dependência do driver no `pom.xml` (as tabelas são criadas
automaticamente pelo `spring.jpa.hibernate.ddl-auto=update`).

## API (resumo)

| Método | Endpoint | O quê faz |
|---|---|---|
| POST | `/api/auth/login` | Entra (login + senha JSON) e cria a sessão |
| POST | `/api/auth/logout` | Sai |
| GET | `/api/auth/me` | Usuário logado |
| GET | `/api/itens?query=&categoria=` | Lista/busca registros |
| POST | `/api/itens` | Cria registro |
| PUT | `/api/itens/{id}` | Edita |
| DELETE | `/api/itens/{id}` | Exclui (junto com anexos) |
| POST | `/api/itens/{id}/anexos` | **Upload** de foto/vídeo/arquivo (multipart) |
| GET | `/api/usuarios` | Listar usuários (**só ADMIN**) |
| POST | `/api/usuarios` | Criar usuário com perfil (**só ADMIN**) |
| PUT | `/api/usuarios/{id}` | Alterar nome/senha/perfil (**só ADMIN**) |
| DELETE | `/api/usuarios/{id}` | Excluir usuário (**só ADMIN**) |
| GET | `/api/anexos/{id}/stream` | Stream do anexo (suporta Range p/ vídeo) |
| DELETE | `/api/anexos/{id}` | Remove um anexo |

Tudo isso está documentado e testável direto no **Swagger UI**
(http://localhost:8180/swagger-ui.html) — para testar no Swagger, use o
botão "Authorize" ou faça login pela tela web antes, pois as APIs exigem sessão.

## Estrutura do projeto

```
src/main/java/com/bancoconhecimento/
├── KnowledgeBaseApplication.java   # main
├── config/       # interceptor de sessão + rotas públicas
├── model/        # Usuario, ItemConhecimento, Anexo (entidades JPA)
├── repository/   # Spring Data JPA
├── dto/          # objetos de entrada/saída da API
├── service/      # regras: CRUD, upload com limites, BCrypt
├── controller/   # REST (auth, itens, anexos/streaming)
└── exception/    # tratamento de erros (413, 404, 500)

src/main/resources/
├── application.properties          # configurações (portas, limites, banco)
└── static/                         # telas web
    ├── login.html                  # tela de login
    └── app.html                    # tela de gerenciamento (CRUD + uploads)
```

## Testado em (24/11/2026)

- Login inválido → 401 / login correto → sessão criada ✓
- API bloqueada sem login (401) ✓
- Criar/listar/editar/excluir registros ✓
- Upload de foto e vídeo no banco ✓
- **Vídeo de 55 MB recusado com aviso de limite (413)** ✓
- Stream com Range (HTTP 206) para o player de vídeo avançar/retroceder ✓
- Swagger UI com 9 endpoints ✓
- Dados persistindo no arquivo do banco ✓

---

```bash
docker build -t banco-conhecimento .
docker run -p 8180:8080 -e APP_ADMIN_SENHA=minhasenha banco-conhecimento
```
