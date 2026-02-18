# Self‑hosted GitHub Actions Runner — Windows (Docker Desktop)

Kurz: Installiere einen **self‑hosted runner** auf dem Windows‑Rechner, auf dem Docker Desktop läuft. GitHub Actions kann dann Jobs auf *deinem* Rechner ausführen (z. B. `docker compose up`) und so deine lokalen Container direkt aktualisieren.

Vorteile
- Direkter Zugriff auf Docker Desktop / lokale Volumes
- Keine SSH‑Server/EXTRA‑Exposition notwendig
- Workflows können `docker`/`docker compose` ausführen

Schnellstart (Schritte)

1) Repo → Settings → Actions → Runners → "New self-hosted runner" → Platform: **Windows**
   - Kopiere die `config`‑Zeile (Token temporär).

2) Auf deinem Windows‑Rechner (PowerShell, Admin):

```powershell
# Arbeitsverzeichnis
mkdir C:\actions-runner -Force
cd C:\actions-runner

# Download (aktuelle Version automatisch bei GitHub anzeigen)
Invoke-WebRequest -Uri "https://github.com/actions/runner/releases/latest/download/actions-runner-win-x64.zip" -OutFile actions-runner.zip
Expand-Archive actions-runner.zip -DestinationPath .

# Konfiguration (ersetze URL + TOKEN durch die Werte aus GitHub UI)
.\config.cmd --url https://github.com/<OWNER>/<REPO> --token <TOKEN> --name physio-desktop-runner --labels self-hosted,windows,docker

# Manuell testen (interaktiver Modus)
.\run.cmd
```

3) Runner dauerhaft laufen lassen
- Option A (einfach): Erzeuge eine geplante Aufgabe (Task Scheduler) die `run.cmd` bei Login/Startup ausführt.
  - Beispiel (PowerShell als Admin):
    schtasks /Create /SC ONLOGON /TN "GitHub Actions Runner - physio-desktop" /TR "C:\actions-runner\run.cmd" /RL HIGHEST /F
- Option B: NSSM oder Windows Service (wenn du bereits Tools dafür nutzt).

4) Voraussetzungen auf dem Host
- Docker Desktop installiert und läuft; `docker` im PATH
- Ports die Compose verwendet dürfen frei sein (z. B. 8081, 4201, 3307 für `compose.test.yml`)
- Der Runner‑Account muss Rechte haben, Docker zu benutzen (lokaler Anwender mit Docker‑Group / Admin)

5) Testen: In GitHub → Actions → wähle den Workflow `Deploy → Docker Desktop` → `Run workflow` → Stack: `test` oder `dev`.

Wichtiges zur Sicherheit
- Der Runner hat Zugriff auf deinen Rechner — nur auf vertrauenswürdigen Maschinen installieren.
- Entferne den Runner aus GitHub (Settings → Actions → Runners), wenn du den Rechner wechselst oder offline nimmst.

Support‑Tipps
- Falls ein Job nicht startet: prüfe ob Runner online ist (Settings → Actions → Runners)
- Logs: `C:\actions-runner\_diag` enthält Runner‑Logs

---

Wenn du möchtest, richte ich den Workflow `Deploy → Docker Desktop` in deinem Repo ein (ich habe die Datei bereits vorbereitet). Soll ich dir helfen, den Runner live zu registrieren?