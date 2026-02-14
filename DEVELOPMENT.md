# 🚀 Development Setup Guide - Physiokalender

## Quick Start für lokale Entwicklung (mit Hot-Reload)

### 1. Datenbank starten (Docker)
```bash
docker-compose up -d
```

Das startet **NUR die MySQL-Datenbank** - die bleibt laufen und verwaltet die Daten persistent. Du brauchst **keine ständigen docker-compose down/up** mehr!

### 2. Backend lokal starten (mit Hot-Reload)

**Option A: Terminal/IDE**
```bash
./mvnw spring-boot:run
```

**Option B: VS Code Task**
- Drücke `Ctrl+Shift+B` und wähle "Start Spring Boot Application"

### 3. Was ist Hot-Reload?
- Sobald du eine `.java` Datei speicherst, wird sie **automatisch neu kompiliert** ✅
- Die App startet sich selbst neu - **kein Rebuild nötig!**
- Dies funktioniert dank `spring-boot-devtools` in der `pom.xml`

## 📊 Daten & Datenbank

### Datenbank lädt Migrations automatisch:
1. `V1__Create_schema.sql` - erstellt die Tabellen
2. `V2__Insert_demo_data.sql` - fügt Demo-Daten ein

Die Daten bleiben in Docker persistent - auch nach `docker-compose down`.

### Demo-Daten zurücksetzen:
```bash
# Nur wenn du die Daten löschen möchtest
docker-compose down -v
docker-compose up -d
# Backend neu starten
```

## 🛑 Datenbank vollständig neustarten
```bash
# Datenbank stoppen + Volume löschen + neu starten
docker-compose down -v
docker-compose up -d
```

## 🏗️ Backend bauen (für Production)

```bash
./mvnw clean package
```

Das erzeugt: `target/Physiokalendar-0.0.1-SNAPSHOT.jar`

## 🐳 Docker Image für Production bauen

```bash
docker build -f Dockerfile.prod -t physiokalender:latest .
```

Dann starten mit korrekter MySQL-URL:
```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/physiocalendar \
  -e SPRING_DATASOURCE_USERNAME=physiouser \
  -e SPRING_DATASOURCE_PASSWORD=password \
  physiokalender:latest
```

## 📁 Workspace-Struktur

```
├── src/main/java/        ← Backend Java Code
├── src/main/resources/    ← application.yml, DB-Migrations
├── Physiokalender-v2-UI/  ← Angular Frontend
├── docker-compose.yml     ← DEV: Nur Datenbank (nicht verändern!)
├── Dockerfile.prod        ← PROD: Multi-Stage Build
└── pom.xml               ← Maven Dependencies
```

## 🔧 Troubleshooting

### Problem: "Connection refused"
- [ ] Ist Docker laufen? `docker ps`
- [ ] Hat die DB gestartet? `docker logs physiokalendar-db-1`
- [ ] Backend wartet auf DB? Prüfe den Log nach "HikariPool initialized"

### Problem: "Port 3306 already in use"
```bash
docker-compose down
# oder auf andere Port:
docker-compose up -d
# Dann in application.yml Port ändern auf 3307
```

### Hot-Reload funktioniert nicht?
1. Stelle sicher, dass du die `.java` Datei speicherst
2. Warte 2-3 Sekunden - DevTools kompiliert im Hintergrund
3. Prüfe den Console-Log für "Completed initialization"

## 🎯 Best Practices

✅ **Richtig:**
- `docker-compose up -d` für DB
- `./mvnw spring-boot:run` für Backend = lokalBau
- `.java` Dateien speichern = Auto-Reload

❌ **Falsch:**
- `docker build .` während Entwicklung (langsam!)
- Ständig `docker-compose down/up` (Daten gehen weg!)
- Backend im Container bauen (kein Hot-Reload)

## 📝 Weitere Befehle

```bash
# Nur Tests ausführen
./mvnw test

# Spezifische Test-Klasse
./mvnw test -Dtest=TherapistControllerTest

# Maven clean ohne Test
./mvnw clean

# Dependency Check
./mvnw dependency:tree

# Datenbank-Logs anschauen
docker logs physiokalendar-db-1 -f
```

**Happy Coding! 🎉**
