# 🏥 Physiokalender - Complete Development & Deployment Guide

Comprehensive documentation for Physiokalender (Spring Boot + Angular + MySQL + Docker).
All documentation consolidated into one file. Straightforward & easy to navigate.

---

## 🚀 Quick Start (5 min)

### Development with Hot-Reload
```bash
# Terminal 1: Database
docker compose -f compose.dev.yml --env-file .env.dev up -d

# Terminal 2: Backend
./mvnw spring-boot:run

# Terminal 3: Frontend
cd Physiokalender-v2-UI && npm run dev
```
- Backend: http://localhost:8080
- Frontend: http://localhost:4200
- Database: localhost:3306

### Test Environment (Containerized)
```bash
docker compose -f compose.test.yml --env-file .env.test up -d --build
```
- Frontend: http://localhost:4201
- Backend: http://localhost:8081
- Database: localhost:3307

---

## 📋 Environment Stages

| Stage | Backend | Frontend | Database | Purpose |
|-------|---------|----------|----------|---------|
| **dev** | Local IDE | npm dev | Docker | Development with hot-reload |
| **test** | Docker | Docker | Docker | Pre-deployment testing & CI/CD |
| **prod** | Docker | Docker | Remote/Cloud | Production environment |

---

## 🛠️ Development Setup

### Prerequisites
- Java 21 (JDK)
- Node.js 20+
- Docker & Docker Compose
- Maven 3.8+

### Step 1: Start Database
```bash
docker compose -f compose.dev.yml --env-file .env.dev up -d
docker logs physio-dev-db
# Should show: "Ready for connections"
```

### Step 2: Start Backend
```bash
./mvnw spring-boot:run
# OR VS Code: Ctrl+Shift+B → Start Spring Boot Application
```
✅ http://localhost:8080

### Step 3: Start Frontend
```bash
cd Physiokalender-v2-UI
npm install  # First time only
npm run dev
```
✅ http://localhost:4200

### Hot-Reload
- **Backend:** Save `.java` files → auto-recompile & restart (spring-boot-devtools)
- **Frontend:** Save `.ts/.html/.scss` → auto-reload in browser

### Reset Development Data
```bash
docker compose -f compose.dev.yml --env-file .env.dev down -v
docker compose -f compose.dev.yml --env-file .env.dev up -d
./mvnw spring-boot:run  # Re-seeds data
```

---

## 🧪 Test Environment

Everything runs in containers, isolated from dev/prod with separate database & ports.

### Start Test Stack
```bash
docker compose -f compose.test.yml --env-file .env.test up -d --build
docker compose -f compose.test.yml --env-file .env.test logs -f

# Access
# Frontend: http://localhost:4201
# Backend: http://localhost:8081
# Database: localhost:3307 (user: physiouser, password: testpassword)
```

### Stop Test Stack
```bash
docker compose -f compose.test.yml --env-file .env.test down
# Or with volume cleanup: add -v flag (WARNING: deletes data!)
```

### Run Tests
```bash
# Java tests (JUnit)
./mvnw test

# Frontend tests
cd Physiokalender-v2-UI && npm test

# Build binaries
./mvnw clean package
cd Physiokalender-v2-UI && npm run build
```

---

## 🚀 Production Deployment

### Quick Deploy
```bash
# Deploy to production (recommended)
./scripts/deploy.sh --from dev --to prod --no-cache
```

### Deploy Script Options
```bash
# Dev → Test (fresh build)
./scripts/deploy.sh --from dev --to test --mvn --frontend --no-cache

# Dev → Test (Docker only, use existing binaries)
./scripts/deploy.sh --from dev --to test

# Dev → Prod (production)
./scripts/deploy.sh --from dev --to prod --no-cache

# From specific Git branch
./scripts/deploy.sh --from dev --to test --branch feature/xyz
```

### Alternative: Redeploy Script
```bash
# Test environment
./scripts/redeploy-stack.sh test --mvn --frontend --no-cache

# Production
./scripts/redeploy-stack.sh prod --mvn --frontend --no-cache

# Recreate database (WARNING: deletes all data!)
./scripts/redeploy-stack.sh test --recreate-db
```

### Production Configuration
Create `.env.prod`:
```properties
COMPOSE_PROJECT_NAME=physio-prod
DB_HOST=your-remote-db-host
DB_PORT=3306
DB_NAME=physiocalendar_prod
DB_USER=physiouser
DB_PASSWORD=your-secure-password
FRONTEND_IMAGE=ghcr.io/your-org/physiokalender-frontend:latest
```

### Database Migrations
Automatically run on backend startup via Flyway:
- `V1__Create_schema.sql` - Creates tables
- `V2__Insert_demo_data.sql` - Seeds demo data (dev/test only)

---

## 🐳 Docker & Containers

### Basic Commands

**Build & Start Service**
```bash
docker compose -f compose.test.yml --env-file .env.test up -d --build physio-test-backend
```

**Start Only**
```bash
docker compose -f compose.test.yml --env-file .env.test up -d physio-test-backend
```

**Stop**
```bash
docker compose -f compose.test.yml --env-file .env.test stop physio-test-backend
```

**Restart**
```bash
docker compose -f compose.test.yml --env-file .env.test restart physio-test-backend
```

**View Logs**
```bash
docker compose -f compose.test.yml --env-file .env.test logs -f --tail=200 physio-test-backend
```

**Build Without Cache**
```bash
docker compose -f compose.test.yml --env-file .env.test build --no-cache physio-test-backend
```

**Force Recreate** (after build)
```bash
docker compose -f compose.test.yml --env-file .env.test up -d --no-deps --force-recreate physio-test-backend
```

**One-Liner: Build + Recreate**
```bash
docker compose -f compose.test.yml --env-file .env.test build --no-cache physio-test-frontend && \
  docker compose -f compose.test.yml --env-file .env.test up -d --no-deps --force-recreate physio-test-frontend
```

### Container Naming
Container names: `${COMPOSE_PROJECT_NAME}-<service>`
- Example: `physio-test-backend`, `physio-test-db`

### Test Environment Ports
- Database: 3307
- Backend: 8081
- Frontend: 4201

---

## 💾 Database & Backups

### Manual Backup
```bash
# Backup dev database
docker exec -i physio-dev-db sh -c 'exec mysqldump -u$DB_USER -p"$DB_PASSWORD" $DB_NAME' | gzip > backups/backup_$(date +%Y%m%d_%H%M%S).sql.gz

# Backup test database
docker exec -i physio-test-db sh -c 'exec mysqldump -u$DB_USER -p"$DB_PASSWORD" $DB_NAME' | gzip > backups/backup_$(date +%Y%m%d_%H%M%S).sql.gz
```

### Restore Backup
```bash
# Restore to test database
gunzip < backups/backup_YYYYMMDD_HHMMSS.sql.gz | docker exec -i physio-test-db sh -c 'mysql -u$DB_USER -p"$DB_PASSWORD" $DB_NAME'
```

### Import Production Data to Test
```bash
# Export production dump (from prod server/client)
mysqldump -h prod-host -P 3306 -u physiouser -p physiocalendar > backups/prod_backup.sql

# Backup test database first
docker exec -i physio-test-db sh -c 'exec mysqldump -u$DB_USER -p"$DB_PASSWORD" $DB_NAME' | gzip > backups/test_pre_import.sql.gz

# Import production data
mysql -h localhost -P 3307 -u physiouser -ptestpassword physiocalendar_test < backups/prod_backup.sql

# Verify
docker exec -it physio-test-db mysql -u physiouser -p physiocalendar_test -e "SHOW TABLES;"
```

### Automated Backup (Sidecar Container)
The `physio-test-backup` container runs `mysql_backup.sh` via cron (default: 02:00 daily).

**Check logs:**
```bash
docker exec -it physio-test-backup tail -n 200 /var/log/mysql_backup.log
docker logs physio-test-backup --since "24h"
```

**Manually trigger:**
```bash
docker exec -it physio-test-backup /bin/sh -c "/usr/local/bin/mysql_backup.sh"
```

**Check binary log status:**
```bash
docker exec -it physio-test-db mysql -uroot -p${DB_ROOT_PASSWORD:-testrootpassword} \
  -e "SHOW VARIABLES LIKE 'log_bin'; SELECT @@GLOBAL.binlog_format;"
```

**Restore from backups:**
```bash
ls -lh backups/ | grep -E "(full|inc)_"
docker exec -i physio-test-db sh -c 'mysql -u$DB_USER -p"$DB_PASSWORD" $DB_NAME' < <(gunzip -c backups/full_YYYY-MM-DD*.sql.gz)
```

---

## 📥 Data Import

### Automatic Import on Startup
Edit `src/main/resources/application.properties`:
```properties
app.dataimport.enabled=true
app.dataimport.filepath=src/main/java/com/example/physiokalendar/dataimport/backup.json
```

Start backend:
```bash
./mvnw spring-boot:run
```
Data imports automatically. Check logs for status.

### Manual Import via REST API
```bash
curl -X POST http://localhost:8080/api/dataimport/import \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=src/main/java/com/example/physiokalendar/dataimport/backup.json"
```

### Import via Test
```bash
./mvnw test -Dtest=DataImportTest
```

### Supported Data
- Therapeuten (Therapists)
- Patienten (Patients)
- Einzeltermine (Appointments)
- Serientermine (Recurring Appointments)
- Abwesenheiten (Absences)

---

## 🔄 Frontend Separation

Frontend lives in separate repository: `Physiokalender-v2-UI/`

### Separate CI/CD
Frontend has own GitHub Actions workflow:
- `.github/workflows/build-and-push-frontend.yml`
- Builds & pushes image to GHCR (GitHub Container Registry)
- `FRONTEND_IMAGE` in `.env.test` / `.env.prod` references the image

### Setup
1. Push frontend CI workflows to `Physiokalender-v2-UI` repository
2. Configure GitHub secrets for GHCR PAT (or use GITHUB_TOKEN with packages:write)
3. Set `FRONTEND_IMAGE` in `.env.test` / `.env.prod`:
   ```properties
   FRONTEND_IMAGE=ghcr.io/your-org/physiokalender-frontend:latest
   ```

### Local Development
```bash
cd Physiokalender-v2-UI
npm run dev   # Hot-reload during development
```

---

## 🔨 Frontend CSS Refactoring

Migrate from inline component styles to global CSS utility classes from `global-extended.scss`.

### Global Utility Classes
```scss
// Layout & Flexbox
.flex, .flex-col, .flex-center, .gap-md, .gap-lg

// Padding & Margin
.p-sm, .p-md, .p-lg    // padding
.mx-auto               // margin

// Colors & Text
.bg-white, .bg-light, .bg-primary
.text-error, .text-success, .text-info
.text-center, .text-bold

// Borders & Shadows
.border, .border-light
.rounded-md, .rounded-lg
.shadow-md, .shadow-lg

// Components
.card, .btn, .btn-primary, .form-input
.badge, .badge-success, .badge-error
.tab, .modal, .modal-actions
```

### Migration Example

**Before (Inline Styles):**
```typescript
@Component({
  selector: 'app-login',
  template: `<div class="login-container">...</div>`,
  styles: [`
    .login-container {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }
  `]
})
```

**After (Global Classes):**
```typescript
@Component({
  selector: 'app-login',
  template: `<div class="flex-center card p-lg">...</div>`
  // No styles needed!
})
export class LoginComponent {}
```

### Refactoring Priority
**Phase 1 (Critical):**
1. `login.component.ts` (15 min)
2. `patient-list.component.ts` (45 min) ⭐ LARGEST
3. `dashboard.component.ts` (20 min)

**Phase 2 (Core Features):**
- Feature components (appointments, therapists, absences, etc.)

**Phase 3 (Nice to Have):**
- Admin components, settings, utility components

---

## 🔄 CI/CD Pipeline

### GitHub Actions Workflows
- `.github/workflows/ci.yml` - Build tests on push/PR
- `.github/workflows/build-and-push.yml` - Build & push backend image to GHCR
- `.github/workflows/deploy-test.yml` - Deploy to test (manual trigger)
- `.github/workflows/deploy-prod.yml` - Deploy to prod (requires approval)

### Required GitHub Secrets
```
GHCR_PAT              # GitHub Container Registry PAT
DEPLOY_HOST_TEST      # Test environment hostname
DEPLOY_USER_TEST      # SSH user (test)
SSH_PRIVATE_KEY_TEST  # SSH private key (test)
DEPLOY_HOST_PROD      # Production hostname
DEPLOY_USER_PROD      # SSH user (prod)
SSH_PRIVATE_KEY_PROD  # SSH private key (prod)
```

### Self-Hosted Runner (Windows)

Run GitHub Actions on your local Windows machine with Docker Desktop.

**Install:**
```powershell
mkdir C:\actions-runner -Force
cd C:\actions-runner

# Download latest runner
Invoke-WebRequest -Uri "https://github.com/actions/runner/releases/latest/download/actions-runner-win-x64.zip" -OutFile actions-runner.zip
Expand-Archive actions-runner.zip -DestinationPath .

# Configure (replace URL + TOKEN from GitHub UI)
.\config.cmd --url https://github.com/<OWNER>/<REPO> --token <TOKEN> --name physio-desktop-runner --labels self-hosted,windows,docker

# Test
.\run.cmd
```

**Run as Service (Task Scheduler):**
```powershell
schtasks /Create /SC ONLOGON /TN "GitHub Actions Runner" /TR "C:\actions-runner\run.cmd" /RL HIGHEST /F
```

**Verify it's running:**
- Go to GitHub repo → Settings → Actions → Runners
- Should see "physio-desktop-runner" with green "Idle" status

---

## 📁 Project Structure

```
Physiokalender/
├── src/main/java/com/example/physiokalendar/  ← Backend code
├── src/main/resources/
│   ├── db/migration/           ← Flyway migrations
│   ├── application.properties
│   ├── application-dev.yml
│   ├── application-test.yml
│   ├── application-prod.yml
│
├── Physiokalender-v2-UI/       ← Frontend (Angular)
│   ├── src/app/
│   ├── src/styles/
│   │   ├── global.scss         ← Base styles
│   │   ├── global-extended.scss ← Utility classes (USE THIS!)
│   ├── angular.json
│   ├── package.json
│
├── scripts/
│   ├── deploy.sh               ← Deploy script (recommended)
│   ├── redeploy-stack.sh       ← Alternative deploy
│   ├── mysql_backup.sh         ← Database backup
│   ├── import_latest_to_test.sh
│   ├── sync_dev_to_test.sh
│
├── compose.dev.yml             ← Dev: Database only
├── compose.test.yml            ← Test: All containerized
├── compose.prod.yml            ← Production
├── Dockerfile                  ← Backend image
├── Dockerfile.prod
├── Dockerfile.test
├── pom.xml                     ← Backend dependencies
└── README.md                   ← This file
```

---

## ⚡ Common Commands

| Task | Command |
|------|---------|
| Start dev stack | `docker compose -f compose.dev.yml --env-file .env.dev up -d && ./mvnw spring-boot:run` |
| Start test stack | `docker compose -f compose.test.yml --env-file .env.test up -d --build` |
| Deploy to test | `./scripts/deploy.sh --from dev --to test` |
| Deploy to prod | `./scripts/deploy.sh --from dev --to prod --no-cache` |
| Run backend tests | `./mvnw test` |
| Run frontend tests | `cd Physiokalender-v2-UI && npm test` |
| Build backend | `./mvnw clean package` |
| Build frontend | `cd Physiokalender-v2-UI && npm run build` |
| View backend logs | `./mvnw spring-boot:run` (shows in terminal) |
| View container logs | `docker compose -f compose.test.yml logs -f` |
| Backup database | `docker exec -i physio-test-db mysqldump ... \| gzip > backup.sql.gz` |
| Restore database | `gunzip < backup.sql.gz \| docker exec -i physio-test-db mysql ...` |
| Health check | `curl http://localhost:8080/actuator/health` |
| Stop containers | `docker compose -f compose.test.yml down` |
| Clean up everything | `docker system prune -a --volumes` |

---

## 🐛 Troubleshooting

| Problem | Solution |
|---------|----------|
| "Connection refused: localhost:3306" | Run: `docker compose -f compose.dev.yml --env-file .env.dev up -d` |
| "Port 8080 already in use" | Change port in `application-dev.properties` or kill process: `lsof -i :8080 \| grep LISTEN \| awk '{print $2}' \| xargs kill -9` |
| "Frontend shows old version" | Browser cache: Press Ctrl+F5 or open in Incognito mode |
| "Container name already in use" | Remove: `docker rm -f container-name` (data stays in volume) |
| "DB exits immediately" | Check logs: `docker logs physio-test-db` |
| ".java file won't compile" | Check for syntax errors, save again |
| "Hot-reload not working" | Save `.java` file, wait 2-3s, check logs for "Completed initialization" |
| "Migrations failed" | Check: `docker exec physio-test-db mysql -uroot -ppassword -e "SELECT * FROM flyway_schema_history;"` |

---

## ✅ Quick Checks

**Is Docker running?**
```bash
docker ps
```

**Is database ready?**
```bash
docker logs physio-test-db | grep "ready"
```

**Is backend healthy?**
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health  # test
```

**Is frontend accessible?**
```bash
curl http://localhost:4200
curl http://localhost:4201  # test
```

**Database connection test:**
```bash
mysql -h localhost -P 3306 -u physiouser -p physiocalendar_dev
mysql -h localhost -P 3307 -u physiouser -p physiocalendar_test
```

---

## 📚 Environment Variables

### Development (.env.dev)
```properties
COMPOSE_PROJECT_NAME=physio-dev
DB_NAME=physiocalendar_dev
DB_USER=physiouser
DB_PASSWORD=devpassword
DB_ROOT_PASSWORD=devrootpassword
DB_PORT=3306
```

### Test (.env.test)
```properties
COMPOSE_PROJECT_NAME=physio-test
DB_NAME=physiocalendar_test
DB_USER=physiouser
DB_PASSWORD=testpassword
DB_ROOT_PASSWORD=testrootpassword
DB_PORT=3307
BACKEND_PORT=8081
FRONTEND_PORT=4201
```

### Production (.env.prod)
```properties
COMPOSE_PROJECT_NAME=physio-prod
DB_NAME=physiocalendar_prod
DB_USER=physiouser
DB_PASSWORD=<secure-password>
DB_ROOT_PASSWORD=<secure-password>
DB_HOST=external-db.example.com
FRONTEND_IMAGE=ghcr.io/your-org/physiokalender-frontend:latest
```

---

## 📊 Technology Stack

- **Backend:** Spring Boot 3.x, Java 21
- **Frontend:** Angular 18+, TypeScript
- **Database:** MySQL 8.0.36
- **Containerization:** Docker & Docker Compose
- **CI/CD:** GitHub Actions
- **Build:** Maven (backend), npm/ng (frontend)
- **ORM:** JPA/Hibernate
- **API:** REST (Spring Web)
- **Database Migrations:** Flyway

---

## 🎯 Deployment Checklist

**Before Production Deployment:**
- [ ] All tests passing locally (`./mvnw test`)
- [ ] All frontend tests passing (`npm test` in UI folder)
- [ ] Docker image builds successfully
- [ ] `.env.prod` configured with production values
- [ ] SSH keys & GitHub secrets configured
- [ ] Database backups taken
- [ ] Rollback plan ready

**Deployment Steps:**
```bash
./scripts/deploy.sh --from dev --to prod --no-cache
```

**Post-Deployment:**
- [ ] Check application logs: `docker logs physio-prod-backend`
- [ ] Health check: `curl https://yourapp.com/actuator/health`
- [ ] Frontend loads: Open in browser
- [ ] Database connected: Check logs for "HikariPool initialized"

---

## 🆘 Support & Help

**For issues:**
1. Check Docker status: `docker ps`
2. Check logs: `docker logs <container-name>`
3. Verify configuration: `.env` files
4. Search existing GitHub Issues
5. Check error message in console/browser DevTools

**Performance Tips:**
- Use local npm cache: `npm ci` instead of `npm install`
- Leverage Docker layer cache: don't rebuild unless needed
- Use `--no-cache` flag only when absolutely necessary

---

**Last Updated:** February 2026
**Status:** Complete & Consolidated
**Maintained By:** Development Team
