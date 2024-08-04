# Base image
FROM openjdk:17-jre-slim

# Set the working directory
WORKDIR /usr/src/app

# Copy the Maven wrapper and source code
COPY . .

# Install dependencies and package the application
RUN ./mvnw package -DskipTests

# Define the command to run the application
CMD ["java", "-jar", "target/yourapp.jar"]
