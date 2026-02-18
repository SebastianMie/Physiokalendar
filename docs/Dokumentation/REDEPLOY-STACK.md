# Redeploy: Dev → Test 🔁

Kurz: So aktualisierst du den `test`-Stack mit dem aktuellen lokalen Dev‑Stand und baust die Docker‑Container neu.

## Voraussetzungen
- Docker (Daemon) läuft lokal
- `docker compose` (v2+) verfügbar
- Node.js & npm (nur bei `--frontend`)
- Java / Maven oder `./mvnw` (nur bei `--mvn`)
- Ausführung im Repository‑Root

## Empfohlener Einzeiler (frischer Build)
```bash
./scripts/redeploy-stack.sh test --mvn --frontend --no-cache
```
Was das macht:
- `test` → Ziel‑Environment (statt `prod`)
- `--mvn` → Backend: `./mvnw -DskipTests clean package`
- `--frontend` → Frontend: `npm ci && npm run build` im `Physiokalender-v2-UI`‑Verzeichnis
- `--no-cache` → `docker compose build --no-cache` (keine Build‑Caches verwenden)

## VS Code‑Task (neu)
- Standard‑Task `Rebuild & Redeploy (test)` führt jetzt **nur die Docker‑Schritte** aus (keine lokalen Builds).
  - Verwende `Run Task` → `Rebuild & Redeploy (test)` wenn du bereits lokal in der IDE gebaut hast.
- Wenn du lokal bauen und dann redeployen willst, nutze `Rebuild & Redeploy (test, with local build)` (führt `--mvn --frontend --no-cache` aus).

### Neuer bevorzugter Workflow: `deploy` (source → target)
- Script: `scripts/deploy.sh` (bevorzugt, ersetzt nicht automatisch `redeploy-stack.sh`)
- Zweck: explizit Source‑Instanz (z. B. `dev`) und Target‑Instanz (`test` oder `prod`) wählen; **Standard**: nur Ziel‑Docker‑Container werden gebaut/restartet.
- Beispiele:
  - Deploy (dev → test, Docker‑only):
    ```bash
    ./scripts/deploy.sh --from dev --to test
    ```
  - Deploy (dev → prod, no cache):
    ```bash
    ./scripts/deploy.sh --from dev --to prod --no-cache
    ```
  - Deploy (dev → test) + local builds before image build (optional):
    ```bash
    ./scripts/deploy.sh --from dev --to test --mvn --frontend
    ```
  - Deploy from a specific git branch (uses a temporary worktree):
    ```bash
    ./scripts/deploy.sh --from dev --to test --branch feature/xyz
    ```
  - Deploy from current branch (example):
    ```bash
    ./scripts/deploy.sh --from dev --to test --branch $(git rev-parse --abbrev-ref HEAD)
    ```

Hinweis: `redeploy-stack.sh` bleibt weiterhin verfügbar; `deploy.sh` ist die klarere, zielorientierte Schnittstelle.

## Roadmap (kurz)
- Kurzfristig: `deploy.sh` verwendet lokale Dateien / Docker‑Builds wie heute.
- Mittelfristig: `deploy` kann optional Artefakte aus einem angegebenen Git‑Branch bauen und deployen (z. B. `--branch feature/xyz`).

Hinweis: Die Flags `--mvn` / `--frontend` sind optional — nutze sie nur, wenn du lokale Builds VOR dem Image‑Build möchtest.
## Schnellvarianten
- Nur neu starten / Images wiederverwenden:
  ```bash
  ./scripts/redeploy-stack.sh test
  ```
- Nur Backend neu bauen und deployen:
  ```bash
  ./scripts/redeploy-stack.sh test --mvn
  ```

**Wichtig — DB‑Verhalten**
- `redeploy-stack.sh` erstellt die **DB‑Container standardmäßig nicht neu** (Schutz gegen versehentlichen Datenverlust). Wenn du die DB explizit neu erstellen willst, nutze `--recreate-db`:
  ```bash
  ./scripts/redeploy-stack.sh test --recreate-db
  ```
- Fehler "container name "/physio-test-db" is already in use":
  - Prüfen: `docker ps -a --filter "name=physio-test-db"`
  - Entfernen (wenn beabsichtigt): `docker rm -f physio-test-db`  (Daten liegen im Volume `physio-test-db-data`)
  - Alternativ: deploy ohne DB (Standard): `./scripts/redeploy-stack.sh test`

## Windows (ohne PS‑Wrapper)
- Auf Windows am einfachsten in Git‑Bash oder WSL ausführen.
- Alternativ in PowerShell (wenn Bash verfügbar):
  ```powershell
  bash ./scripts/redeploy-stack.sh test --mvn --frontend --no-cache
  ```

> Hinweis: Das Repository enthält aktuell nur das Bash‑Script `scripts/redeploy-stack.sh`.

## Prüf‑/Fehlerbehebungsbefehle nach dem Deploy
```bash
COMPOSE_PROJECT_NAME=physio-test docker compose -f docker-compose.yml -f compose.test.yml ps
COMPOSE_PROJECT_NAME=physio-test docker compose -f docker-compose.yml -f compose.test.yml logs -f --tail=200 physio-test-backend
curl http://localhost:8081/actuator/health   # Health‑Check (Beispiel)
```

## Rollback‑Hinweise
- DB‑Restore: `backups/` (siehe `docs/` → Backup‑Anleitung)
- Start vorheriger Image‑Tags oder `docker compose up` mit vorherigem `image:`‑Tag

## Warum wird das Frontend nochmal gebaut?
- Kurz: `./scripts/redeploy-stack.sh --frontend` baut lokal die `dist/`‑Artefakte, **und** `Dockerfile.frontend` führt beim `docker compose build` ebenfalls ein `npm run build` inside der Image‑Erstellung (Multi‑Stage). Das sorgt dafür, dass das Image reproduzierbar ist — deshalb siehst du zwei Builds.

### Wie vermeide ich Doppel‑Builds?
- Wenn du nur vermeiden willst, dass das Skript lokal baut: rufe das Skript ohne `--frontend` auf:
  ```bash
  ./scripts/redeploy-stack.sh test --mvn    # kein lokales FE‑Build
  ```
- Wenn du möchtest, dass Docker **nicht** im Image noch einmal baut, ändere das Image‑Layout (z. B. `Dockerfile.frontend.local` → `COPY dist/...` statt `npm run build`) oder füge einen Compose‑Override, der eine vorgebaute `dist/` kopiert. (Sag Bescheid, ich setze das für dich um.)

## Fehlersuche: CSS‑Budget‑Fehler beim Docker‑Build
Symptom: `ng build` lokal OK, aber `docker compose build` schlägt fehl mit "exceeded maximum budget".

Ursachen & Checks:
- Docker führt die Build‑Schritte in seiner eigenen Umgebung (Node‑Version, installed deps) — das kann zu anderen Bundle‑Größen führen.
- Docker verwendet beim Build das `BUILD_ENV`‑Argument (siehe `compose.test.yml`). Prüfe lokal, ob dieselbe Konfiguration genutzt wird:
  ```bash
  npm run build -- --configuration=test
  npm run build -- --configuration=production
  node -v
  docker run --rm node:20-alpine node -v
  ```
- Reproduziere den fehlschlagenden Docker‑Build, dann siehst du die exakte Ursache:
  ```bash
  COMPOSE_PROJECT_NAME=physio-test docker compose -f docker-compose.yml -f compose.test.yml build --no-cache physio-test-frontend --progress=plain
  ```

Schnelle Behebungen:
- Temporär: baue die FE mit `development`‑Konfiguration im Image (keine Budgets): setze `BUILD_ENV=development` für den Build (compose/args).
- Dauerhaft: erhöhe/entferne die betreffenden Budgets in `angular.json` (production/test) oder optimiere die betroffenen Komponenten‑CSS.

Beispiel: Budget anpassen (angular.json → production → anyComponentStyle):
```json
{"type":"anyComponentStyle","maximumWarning":"4kb","maximumError":"6kb"}
```

---

Wenn du willst, kann ich jetzt:
1) den laufenden Fehler im Docker‑Build reproduzieren und die Logs untersuchen, oder
2) die `angular.json`‑Budgets temporär anheben, oder
3) das Projekt so anpassen, dass Docker die vorgebaute `dist/` verwendet (kein Docker‑Build der FE).

Sag mir, welche Option du möchtest — ich erledige das für dich. ✅