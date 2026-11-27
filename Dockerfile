# ---------- etapa 1: compilar com Maven + JDK 21 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- etapa 2: imagem final leve (so o JRE) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/banco-conhecimento-1.0.0.jar app.jar

# o Render define a variavel PORT; o app le via ${PORT:8180}
ENV PORT=8080
EXPOSE 8080

# HEALTHCHECK simples (Render tambem faz o proprio)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s \
  CMD curl -fs http://localhost:$PORT/login.html || exit 1

CMD ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]
