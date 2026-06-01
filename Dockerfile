# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app/uploads

# Switch to non-root user
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]