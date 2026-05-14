# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Bound the JVM heap used by Maven and the Kotlin compiler so the build
# does not get OOM-killed inside a memory-constrained build container
# (e.g. Coolify build runners). Kotlin compile is memory-hungry; without
# this the build can be silently killed with exit code 255.
ENV MAVEN_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=512m -XX:+ExitOnOutOfMemoryError"
ENV KOTLIN_DAEMON_JVM_OPTIONS="-Xmx1024m"

# Copy pom.xml and download dependencies (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application.
# Disable Kotlin incremental compilation: it is "experimental", uses extra
# memory, and provides no benefit in a clean one-shot Docker build.
COPY src ./src
RUN mvn package -DskipTests -B -Dkotlin.compiler.incremental=false

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from the build stage
# The name is based on artifactId and version in pom.xml
COPY --from=build /app/target/librarymanager-0.0.1.jar app.jar

# Expose the port Ktor is configured to use
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
