# Stage 1: Build the application using a Maven image
FROM maven:3.9.5-eclipse-temurin-21-alpine AS build
WORKDIR /app
# Copy the pom.xml and source code
COPY pom.xml .
COPY src /app/src
# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Create the final image using a smaller JRE (Java Runtime Environment)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the built JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar
# Set the entry point to run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]