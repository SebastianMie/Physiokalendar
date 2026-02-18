# Docker — Start / Stop / Rebuild (Dev & Test)

Kurzbeschreibung
- Ziel: Einzelne Container starten, stoppen und neu bauen für `dev` und `test` Umgebungen.
- Wichtige Dateien: `compose.dev.yml`, `compose.test.yml`, `.env.dev`, `.env.test`, `Dockerfile`, `Dockerfile.frontend`, `Dockerfile.backup`, `mysql_backup.sh`, `Makefile`.

---

## 🧩 Übersicht (Services)
- Test-Stack (compose.test.yml): `physio-test-db`, `physio-test-backend`, `physio-test-frontend`, `physio-test-backup` ✅

> Container names are generated from `COMPOSE_PROJECT_NAME` (example: `container_name: ${COMPOSE_PROJECT_NAME}-db` → `physio-test-db`).
- Dev-Stack (compose.dev.yml): `physio-dev-db`, `physio-dev-backend`, `physio-dev-frontend` (oder: nur DB via top-level `docker-compose.yml` und Backend lokal für Hot-Reload)

### 🏷️ Namenskonvention (kurz)
- Container / Service: service names are simple (`db`, `backend`, `frontend`, `backup`) and `container_name` uses `COMPOSE_PROJECT_NAME` to remain unique across projects: `${COMPOSE_PROJECT_NAME}-<service>` (e.g. `physio-test-backend`)
- Image name / tag: `physio-<env>-<service>:<tag>` (z. B. `physio-test-backend:latest`)
- Network: `physio-<env>-network`
- Volume (DB): `physio-<env>-db-data` (z. B. `physio-test-db-data`)
- Env file: `.env.<env>` must contain `COMPOSE_PROJECT_NAME=physio-<env>`

> Hinweis: `compose.dev.yml` entfernte das alte `physiokalendar_db_data` — das ist veraltet; neue Standard‑Volume‑Name ist `physio-dev-db-data`. Falls du noch das alte Volume brauchst, behalte es manuell oder migriere Daten (siehe Abschnitt "Volume-Export").

> Ports (Standard aus repo): DB test=3307, Backend test=8081, Frontend test=4201. (Siehe `.env.test` / `.env.dev`)

---

## ⚡ Schnellbefehle — Start / Stop / Rebuild
- Start vollständiger Test-Stack (build + up):

```bash
# aus Projekt-Root
docker compose -f compose.test.yml --env-file .env.test up -d --build
# oder Makefile-Target
make start-test-stack
```

Hinweis: Wenn du `--build` ohne Service‑Angabe ausführst, baut Docker Compose **alle** Dienste aus der Compose‑Datei (DB, Backend, Frontend, Backup). Möchtest du nur einen Dienst neu bauen, füge den Service‑Namen an oder verwende `docker compose build <service>`.

Beispiel — nur Frontend neu bauen:

```bash
# Standard: neu bauen und starten (verwendet ggf. Docker‑Cache)
docker compose -f compose.test.yml --env-file .env.test up -d --build physio-test-frontend

# Build (kein Cache):
COMPOSE_PROJECT_NAME=physio-test docker compose -f docker-compose.yml -f compose.test.yml --env-file .env.test build --no-cache physio-test-backend physio-test-frontend
# Recreate (ohne Dependencies → DB bleibt unangetastet):
COMPOSE_PROJECT_NAME=physio-test docker compose -f docker-compose.yml -f compose.test.yml --env-file .env.test up -d --no-deps --force-recreate physio-test-backend physio-test-frontend
```

# Falls Änderungen nach normalem Rebuild NICHT sichtbar sind (häufige Ursachen: Docker‑Layer‑Cache oder Browser‑Cache):

```bash
# 1) Komplett ohne Docker‑Layer‑Cache bauen
docker compose -f compose.test.yml --env-file .env.test build --no-cache physio-test-frontend

# 2) Container aus neuem Image erzwingen (keine Abhängigkeiten neu starten)
docker compose -f compose.test.yml --env-file .env.test up -d --no-deps --force-recreate physio-test-frontend

# 1‑Zeiler (build ohne Cache + erzwinge Recreate)
docker compose -f compose.test.yml --env-file .env.test build --no-cache physio-test-frontend && \
  docker compose -f compose.test.yml --env-file .env.test up -d --no-deps --force-recreate physio-test-frontend
```

- Start / Stop / Restart eines einzelnen Services (Test):

```bash
# Start / (re)build + start
docker compose -f compose.test.yml --env-file .env.test up -d --build physio-test-backend
# Stop
docker compose -f compose.test.yml --env-file .env.test stop physio-test-backend
# Restart
docker compose -f compose.test.yml --env-file .env.test restart physio-test-backend
```

- Logs anzeigen (z. B. Backend):

```bash
docker compose -f compose.test.yml --env-file .env.test logs -f --tail=200 physio-test-backend
```

- Frontend local (schnell):
```bash
cd Physiokalender-v2-UI && npm run dev   # dev server (faster iteration)
```

---

## 🔁 Empfohlener Ablauf nach Code-Änderungen (Backend / Frontend)
1. Code ändern / testen lokal.
2. Für Docker-Test: neu bauen & nur den Service neu starten:
   - Backend:
     ```bash
     docker compose -f compose.test.yml --env-file .env.test build physio-test-backend
     docker compose -f compose.test.yml --env-file .env.test up -d physio-test-backend
     ```
   - Oder in einem Schritt:
     ```bash
     docker compose -f compose.test.yml --env-file .env.test up -d --build physio-test-backend
     ```
   - Frontend:
     ```bash
    # Normaler Rebuild + Start
    docker compose -f compose.test.yml --env-file .env.test up -d --build physio-test-frontend
    ```

    **Wenn du die Änderung nach dem Rebuild nicht siehst:**

    - Ursache 1: Docker hat veraltete Layer verwendet → benutze `build --no-cache`.
    - Ursache 2: Browser cached alte JS/CSS (nginx setzt lange Cache‑Header) → `Ctrl+F5` oder Inkognito öffnen.
    - Fallback‑Workflow (sicher):
      ```bash
      docker compose -f compose.test.yml --env-file .env.test build --no-cache physio-test-frontend && \
        docker compose -f compose.test.yml --env-file .env.test up -d --no-deps --force-recreate physio-test-frontend
      ```
Tipp: Für schnelle Java-Iterationen nutze `mvn spring-boot:run` lokal (IDE) statt Image-Build.

---

## 💾 Varianten, damit keine Daten in der DB verloren gehen
1. Persistente Docker-Volumes (Standard in repo)
   - `compose.test.yml` nutzt `physio-test-db-data:/var/lib/mysql` — lösche dieses Volume **nicht**.
   - Niemals `docker compose down -v` / `docker volume rm <name>` wenn du Daten behalten willst.
   - Prüfen: `docker volume ls | grep physio-test-db-data`

2. Backup-Container (automatisch + manuell)
   - `physio-test-backup` nutzt `mysql_backup.sh` und schreibt nach `./backups`.
   - Manuell triggern:
     ```bash
     docker compose -f compose.test.yml --env-file .env.test up -d physio-test-backup
     docker compose -f compose.test.yml --env-file .env.test exec -T physio-test-backup /usr/local/bin/mysql_backup.sh
     ```

3. Manueller SQL-Dump (schnell, bevor du risky ops machst)
   - Backup (auf Host):
     ```bash
     docker exec -i physio-test-db sh -c 'exec mysqldump -u$DB_USER -p"$DB_PASSWORD" $DB_NAME' | gzip > backups/pre-change_$(date +%Y%m%d_%H%M%S).sql.gz
     ```
   - Restore (Host → Container):
     ```bash
     gunzip < backups/pre-change_YYYYMMDD_....sql.gz | docker exec -i physio-test-db sh -c 'mysql -u$DB_USER -p"$DB_PASSWORD" $DB_NAME'
     ```
   - (Variablen kommen aus `.env.test`)

4. Volume-Export / Snapshot (wenn du das Volume komplett sichern willst)
   - Unix-example:
     ```bash
     docker run --rm \
       -v physio-test-db-data:/data \
       -v $(pwd)/backups:/backup \
       alpine sh -c "cd /data && tar czf /backup/vol_physio-test-db-data_$(date +%Y%m%d).tgz ."
     ```
   - PowerShell/Windows: passe `$(pwd)` / Datumssyntax an; oder nutze die `mysqldump` Methode (am zuverlässigsten).

---

## ⚠️ Wichtige Hinweise / Fallen
- `docker compose down -v` löscht Volumes und damit die DB-Daten — vermeide das, wenn du Daten behalten willst.
- `compose.test.yml` hat `SPRING_FLYWAY_ENABLED: "false"` (Test-Backend). Bei Schema-Änderungen Flyway beachten (evtl. temporär aktivieren oder Migration manuell ausführen).
- Backup-Ordner `./backups` ist im Repo vorhanden und wird vom Backup-Container verwendet.

---

## ✅ Beispiel-Workflows (Kurz)
- Schnell neu bauen & deploy Backend (Test):
  1) `git pull && mvn -DskipTests package`
  2) `docker compose -f compose.test.yml --env-file .env.test up -d --build physio-test-backend`
- Sichere DB vor riskanten Änderungen:
  1) `docker exec -i physio-test-db sh -c 'exec mysqldump -uroot -p"$DB_ROOT_PASSWORD" $DB_NAME' | gzip > backups/pre-change.sql.gz`
  2) Führe Änderungen durch und teste
  3) Wenn nötig: restore (siehe oben)

---

## 🔧 Nützliche Kommandos (Übersicht)
| Aufgabe | Befehl |
|---|---|
| Start Test-Stack | `docker compose -f compose.test.yml --env-file .env.test up -d --build` |
| Start einzelner Test-Service | `docker compose -f compose.test.yml --env-file .env.test up -d physio-test-backend` |
| Rebuild + Start Service | `docker compose -f compose.test.yml --env-file .env.test up -d --build <service>`

  Use when a normal rebuild is sufficient.
  For a full fresh image (no Docker cache) + forced container recreate use:
  `docker compose -f compose.test.yml --env-file .env.test build --no-cache <service> && docker compose -f compose.test.yml --env-file .env.test up -d --no-deps --force-recreate <service>` |
| Stop Service | `docker compose -f compose.test.yml --env-file .env.test stop <service>` |
| Logs | `docker compose -f compose.test.yml --env-file .env.test logs -f <service>` |
| Manuelles Backup (backup container) | `docker compose -f compose.test.yml --env-file .env.test exec -T physio-test-backup /usr/local/bin/mysql_backup.sh` |
| DB Dump (host) | siehe Abschnitt "Manueller SQL-Dump" |

---

## Quellen / Referenzen im Repo
- `compose.test.yml`, `.env.test` — Test-Stack-Konfiguration
  - Uses `Dockerfile.test` (multi-stage) for faster, smaller test backend image.
  - Adds `logging` + `deploy.resources` blocks (useful for CI / swarm). Note: `deploy.resources` is ignored by local docker-compose but kept for stack deployments.
  - `compose.override.yml` is available as an optional local‑dev override to mount sources and run dev servers.
  - Uses named / external network & volume (avoid duplicate resources across projects). Ensure `COMPOSE_PROJECT_NAME` in `.env.test` matches the stack (default: `physio-test`).
  - Healthchecks added for backend & frontend; compose will no longer auto-start DB/backup unless requested.
- `compose.dev.yml`, `.env.dev` — Dev-Stack
- `Dockerfile.frontend`, `Dockerfile`, `Dockerfile.backup` — Build/Runtime
- `mysql_backup.sh` — Backup-Logik (full / incremental)
- `Makefile` — hilfreiche Targets (`start-test-stack`, `start-backup`, `backup-dev`)

---

## 🚀 Praktischer Plan: Dev → Test (konkret)
Kurz, präzise Schritte, damit Änderungen aus deinem lokalen Coding‑Space sauber in die Test‑Instanz gelangen.

### Ziel
- Lokal entwickeln mit Hot‑Reload (VS Code / `./mvnw spring-boot:run` + `npm run dev`).
- Bei „Promote to test“: lokalen Build erstellen, Images bauen, Test‑Compose neu starten (DB + Backup unangetastet).

### 1) Lokale Entwicklung (Hot‑Reload) — empfohlen
- Backend lokal: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (IDE/VS Code)
- Frontend lokal: `cd Physiokalender-v2-UI && npm run dev`
- Docker nur für Infrastruktur: `COMPOSE_PROJECT_NAME=physio-test docker compose -f docker-compose.yml -f compose.test.yml up -d physio-test-db physio-test-backup`

Vorteil: sofortiges Hot‑Reload, persistente DB bleibt in Compose.

### 2) Manuelle Promotion (Dev → Test) — minimal, sicher
1. Option (lokal build): `./mvnw -DskipTests clean package`
2. Frontend build: `cd Physiokalender-v2-UI && npm ci && npm run build`
3. Build (Compose) + Deploy:
   ```bash
   COMPOSE_PROJECT_NAME=physio-test docker compose -f docker-compose.yml -f compose.test.yml up -d --remove-orphans --force-recreate --build physio-test-backend physio-test-frontend
   ```
4. Optional: Health‑Check / Smoke Test (z. B. `curl http://localhost:8081/actuator/health`)
5. Wenn Probleme: restore DB aus `backups/` oder rollback auf vorheriges Image (siehe unten).

One‑liner (alles nacheinander):
```bash
./mvnw -DskipTests clean package && cd Physiokalender-v2-UI && npm ci && npm run build && \
  COMPOSE_PROJECT_NAME=physio-test docker compose -f docker-compose.yml -f compose.test.yml up -d --remove-orphans --force-recreate --build physio-test-backend physio-test-frontend
```

### 3) Script‑Unterstützung (lokal, ohne Git‑CI)
- Neu: `scripts/redeploy-stack.sh` (bash) und `scripts/redeploy-stack.ps1` (PowerShell)
- Zweck: `build` (optional MVN + FE), `docker compose build` + `up -d --force-recreate`, Health‑check‑Hinweis.
- Beispiel (bash):
  - `./scripts/redeploy-stack.sh test --mvn --frontend --no-cache`
- Beispiel (PowerShell):
  - `.\	ools\redeploy-stack.ps1 -env test -Maven -Frontend -NoCache`

> Die Skripte sind in `scripts/` abgelegt — sie führen keine Git‑Operationen aus (lokal, ohne Registry).

### 4) Rollback kurz
- Halte das vorherige Image lokal (oder verwende Image‑Tags).
- Rollback durch Start des vorherigen Tags oder vorherigen Compose‑Up (z. B. `docker compose up -d --no-deps --force-recreate <service>` nachdem du das `image:`-Tag angepasst hast).

### 5) Kurzplan für Self‑hosted Runner / spätere Automation (Phase 1 → 3)
- Phase 1 (sofort): On push to `test` branch → runner macht `./mvnw -DskipTests package`, `npm run build`, `docker compose -f docker-compose.yml -f compose.test.yml up -d --build` (local runner auf deinem Dev‑Rechner). No registry required.
- Phase 2: füge Unit/Integration tests hinzu, deploy nur bei grün.
- Phase 3: Release tags → tag images → deploy prod stack.

---

### Dateien: Scripts (bereits angelegt)
- `scripts/redeploy-stack.sh` — Bash (Windows/Git‑Bash / Linux / macOS)
- `scripts/redeploy-stack.ps1` — PowerShell (Windows)

Beispiele und Usage stehen direkt in den Skripten. Willst du, dass ich die VS Code‑Tasks (`tasks.json`) so anpasse, dass ein Task `Rebuild & Deploy → test` die neuen Skripte aufruft? 🔧

