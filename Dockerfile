
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package

# Use an OpenJDK base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy JAR to image
COPY target/urakata-0.0.1-SNAPSHOT.jar urakata-app.jar

# Expose the port (change to your actual port)
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java", "-jar", "urakata-app.jar"]
