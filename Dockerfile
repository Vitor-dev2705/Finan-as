# Estágio de Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia apenas o pom.xml para baixar as dependências e criar CACHE
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copia o código-fonte e faz a compilação do JAR
COPY src ./src
RUN mvn package -DskipTests --batch-mode

# Estágio de Execução (Imagem leve)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]