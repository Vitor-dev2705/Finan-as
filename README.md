# 🤖 Finanças Bot — Controle Financeiro por Voz no Telegram

Bot do Telegram que permite registrar gastos apenas **enviando um áudio de voz**. O bot transcreve o áudio, extrai o valor e a categoria automaticamente usando inteligência artificial, e salva a transação no banco de dados.

---

## 🎯 O que o Bot faz?

1. **Recebe um áudio de voz** no Telegram (ex: *"Gastei 35 reais no almoço"*)
2. **Transcreve o áudio para texto** usando Groq Whisper (`whisper-large-v3`)
3. **Extrai valor e categoria** da transcrição usando Groq Llama (`llama-3.3-70b-versatile`)
4. **Salva a transação** no banco de dados PostgreSQL (NeonDB)
5. **Confirma o registro** com uma mensagem formatada no Telegram

---

## 🏗️ Arquitetura

```
┌─────────────────┐     ┌──────────────────────────────────┐     ┌──────────────────┐
│   📱 Telegram   │────▶│         🤖 Finanças Bot          │────▶│  🐘 PostgreSQL   │
│  (Áudio de Voz) │     │                                  │     │    (NeonDB)      │
└─────────────────┘     │  1. Baixa áudio (.ogg)           │     └──────────────────┘
                        │  2. Envia para Groq Whisper      │
                        │  3. Envia texto para Groq Llama  │     ┌──────────────────┐
                        │  4. Salva no banco de dados      │────▶│  🧠 Groq API     │
                        │  5. Responde no Telegram         │     │  Whisper + Llama │
                        └──────────────────────────────────┘     └──────────────────┘
```

---

## 🛠️ Tecnologias Utilizadas

| Tecnologia | Uso |
|---|---|
| **Java 17** | Linguagem principal |
| **Maven** | Gerenciador de dependências e build |
| **Telegram Bots API** (`6.9.7.1`) | Integração com o Telegram (Long Polling) |
| **Groq API** — Whisper | Transcrição de áudio para texto (STT) |
| **Groq API** — Llama 3.3 70B | Extração inteligente de valor e categoria |
| **PostgreSQL** (NeonDB) | Banco de dados serverless na nuvem |
| **Jackson** | Parsing de JSON das APIs |
| **dotenv-java** | Leitura de variáveis do arquivo `.env` |
| **Docker** | Containerização para deploy |

---

## 📁 Estrutura do Projeto

```
src/main/java/com/financasbot/
├── Main.java                   # Ponto de entrada — registra o bot
├── bot/
│   └── TelegramVoiceBot.java   # Recebe áudios e orquestra o fluxo
├── config/
│   ├── DatabaseConfig.java     # Conexão JDBC com PostgreSQL
│   └── EnvConfig.java          # Leitura de variáveis de ambiente
├── model/
│   └── Transaction.java        # Modelo de dados da transação
└── service/
    ├── FinanceService.java     # CRUD de transações no banco
    └── GroqService.java        # Transcrição (Whisper) + IA (Llama)
```

---

## ⚙️ Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto (ou configure no seu serviço de deploy):

| Variável | Descrição |
|---|---|
| `DB_URL` | URL JDBC do PostgreSQL (NeonDB) |
| `DB_USER` | Usuário do banco de dados |
| `DB_PASS` | Senha do banco de dados |
| `TELEGRAM_BOT_TOKEN` | Token do bot obtido no [@BotFather](https://t.me/BotFather) |
| `GROQ_API_KEY` | Chave de API da [Groq](https://console.groq.com/) |

> 📄 Veja o arquivo `.env.example` para um modelo com todas as variáveis.

---

## 🚀 Como Executar

### Pré-requisitos
- Java 17+
- Maven 3.8+
- Conta no Telegram com bot criado via [@BotFather](https://t.me/BotFather)
- Conta na [Groq](https://console.groq.com/) (API key gratuita)
- Banco PostgreSQL (ex: [NeonDB](https://neon.tech/) — gratuito)

### Banco de Dados
Crie a tabela no seu PostgreSQL:
```sql
CREATE TABLE IF NOT EXISTS transacoes (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    category VARCHAR(100),
    "date" DATE DEFAULT CURRENT_DATE
);
```

### Rodando Localmente
```bash
# 1. Clone o repositório
git clone https://github.com/SEU_USUARIO/Finan-as.git
cd Finan-as

# 2. Configure as variáveis de ambiente
cp .env.example .env
# Edite o .env com seus tokens e chaves reais

# 3. Compile e execute
mvn clean compile exec:java
```

### 🐳 Rodando com Docker
```bash
# 1. Configure o .env
cp .env.example .env
# Edite o .env com seus tokens

# 2. Build e execute com um único comando
docker compose up -d --build

# Ver logs
docker compose logs -f

# Parar
docker compose down
```

---

## 📝 Exemplo de Uso

1. Abra o Telegram e encontre seu bot
2. Envie um **áudio de voz** dizendo, por exemplo:
   > *"Gastei quarenta e nove reais e noventa centavos no supermercado"*
3. O bot responde:
   ```
   ✅ Gastos registrados!
   💰 Valor: R$ 49.90
   🏷️ Categoria: Supermercado
   ```

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.
