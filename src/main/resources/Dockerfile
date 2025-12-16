# Use Java 17
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy everything
COPY . .

# Build the application
RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests

# Expose port (Render injects PORT)
EXPOSE 8080

# Run the jar
CMD ["sh", "-c", "java -jar target/*.jar"]
