# CI/CD Plan — GitHub Actions (Dev → Test → Prod)

Ziel: Vollautomatische Backend‑Pipeline (Build → Tests → Container Image → Deploy → Healthcheck). Prod‑Deploy bleibt durch manuelle Freigabe (Approval) geschützt. Frontend‑Build bleibt vorerst manuell (auf Wunsch später in CI integrieren).

Übersicht (Priorität)
1. CI: Build & Unit‑Tests (GitHub Actions)
2. Image build & push (GHCR) für Backend (Dockerfile.test / Dockerfile.prod)
3. Deploy → Test (workflow_dispatch; SSH zur Test‑VM oder Runner)
4. Deploy → Prod (protected environment + manual approval)
5. Smoke tests / healthchecks nach Deploy
6. Dokumentation + Secrets‑Setup
7. Optional: CI‑Frontend (später)

Akzeptanzkriterien
- `push` / `pull_request` löst Build + Tests aus und schlägt bei Fehlern fehl.
- Main‑Branch erzeugt und pusht Backend‑Image zu GHCR (Tag: `sha` + `latest`).
- Test‑Deployment kann per `workflow_dispatch` getriggert werden und führt `./scripts/redeploy-stack.sh test` remote aus.
- Prod‑Deploy erfordert manuelle Bestätigung (GitHub environment protection).
- Healthcheck (/actuator/health) wird nach Deploy überprüft.

Secrets (in GitHub Repository Settings > Secrets)
- `GHCR_PAT` (optional) oder benutzen von `GITHUB_TOKEN` mit Package write‑Berechtigung
- `DEPLOY_HOST_TEST`, `DEPLOY_USER_TEST`, `SSH_PRIVATE_KEY_TEST`
- `DEPLOY_HOST_PROD`, `DEPLOY_USER_PROD`, `SSH_PRIVATE_KEY_PROD`

Dateien, die ich erstelle
- `.github/workflows/ci.yml` — Build & Test
- `.github/workflows/build-and-push.yml` — Build & Push Container
- `.github/workflows/deploy-test.yml` — Deploy → Test (workflow_dispatch, SSH)
- `.github/workflows/deploy-prod.yml` — Deploy → Prod (environment: production)
- `docs/CI-CD-PLAN.md` — diese Datei (Plan + Tasks)

Zeit & Aufwandsabschätzung
- Erstimplementation (erstellte Workflows + Docs): 2–3 Stunden
- Integration SSH/Deploy und Secrets konfigurieren: 30–60 min (abhängig von Zugang)
- Frontend‑CI hinzufügen: 1–2 Stunden

Nächste Schritte
1. Workflows und Plan in repo anlegen (erledigt)
2. Du konfigurierst die benötigten Secrets in GitHub
3. Optional: Branch/PR erstellen oder direkt in main committen
4. Trigger: Push/PR → CI läuft; manuelles `Deploy → Test` starten

Änderungsrückverfolgbarkeit / Rollback
- Deploy‑Scripts nutzen vorhandene `scripts/redeploy-stack.sh` und `deploy.sh`.
- Ich empfehle zusätzlich einen `--rollback <tag>` Flag im `reploy-stack.sh` (kann ich hinzufügen).

---

Wenn du willst, committe ich die Änderungen jetzt in einen Feature‑Branch (`ci/cicd-github-actions`) und öffne einen Draft‑PR zur Review. Oder ich committe direkt auf `main` — sag mir kurz, welche Variante du bevorzugst.