# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src ./src
RUN mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built JAR from the build stage
# The name is based on artifactId and version in pom.xml
COPY --from=build /app/target/librarymanager-0.0.1-jar-with-dependencies.jar app.jar

# Expose the port Ktor is configured to use
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
