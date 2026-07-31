# ============================================
# STAGE 1: Build com Maven
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copia apenas o pom.xml primeiro para cachear as dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código-fonte e compila
COPY src ./src
RUN mvn clean package -DskipTests -q

# ============================================
# STAGE 2: Runtime com JRE leve
# ============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia o fat JAR gerado pelo Maven Shade Plugin
COPY --from=build /app/target/financas-voz-bot-1.0-SNAPSHOT.jar app.jar

# Variáveis de ambiente (definidas no docker-compose ou via -e)
ENV DB_URL="" \
    DB_USER="" \
    DB_PASS="" \
    TELEGRAM_BOT_TOKEN="" \
    GROQ_API_KEY="" \
    SPRING_AI_OPENAI_BASE_URL="https://api.groq.com/openai" \
    SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL="llama-3.3-70b-versatile" \
    SPRING_AI_OPENAI_AUDIO_TRANSCRIPTION_OPTIONS_MODEL="whisper-large-v3"

# Executa o bot
CMD ["java", "-jar", "app.jar"]
