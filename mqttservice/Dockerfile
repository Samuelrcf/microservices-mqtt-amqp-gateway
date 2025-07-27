FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/mqttservice.jar app.jar
ENTRYPOINT ["java", "--add-opens", "java.base/java.net=ALL-UNNAMED", "-jar", "app.jar"]
