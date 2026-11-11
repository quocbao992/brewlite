FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./mvnw && ./mvnw clean package -pl apps/backend -am -DskipTests

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring
COPY --from=builder /app/apps/backend/target/brewlite-backend-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
