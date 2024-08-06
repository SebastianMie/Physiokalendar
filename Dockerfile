# Verwenden Sie ein Basis-Image mit Java 17
FROM openjdk:17-jdk-slim

# Setzen Sie das Arbeitsverzeichnis im Container
WORKDIR /usr/src/app

# Installieren Sie Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Kopieren Sie die Anwendung und die notwendigen Dateien in das Arbeitsverzeichnis
COPY pom.xml ./
COPY src ./src

# Installieren Sie die Abhängigkeiten und kompilieren Sie die Anwendung
RUN mvn dependency:go-offline -B

# Der Befehl zum Starten der Anwendung
CMD ["mvn", "spring-boot:run"]
