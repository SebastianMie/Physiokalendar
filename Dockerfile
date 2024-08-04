# Base image
FROM openjdk:11-jre-slim

# Set the working directory
WORKDIR /usr/src/app

# Copy the Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Ensure the Maven wrapper has executable permissions
RUN chmod +x ./mvnw

# Install dependencies and package the application
# First, download the dependencies, then copy the rest of the application
COPY src ./src
RUN ./mvnw dependency:go-offline -B
RUN ./mvnw package -DskipTests -B

# Define the command to run the application
CMD ["java", "-jar", "target/yourapp.jar"]
