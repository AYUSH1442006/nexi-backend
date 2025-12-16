# Java 17 base image
FROM eclipse-temurin:17-jdk-alpine

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Expose port (Render provides PORT)
EXPOSE 8080

# Run the Spring Boot jar
CMD ["sh", "-c", "java -jar target/*.jar"]
