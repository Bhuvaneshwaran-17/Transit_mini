# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the actual executable JAR
COPY --from=build /app/target/*.jar app.jar
# Ensure this matches the server.port in your properties!
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]