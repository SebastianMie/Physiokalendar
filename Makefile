# Convenience make targets for local dev/test maintenance
.PHONY: help backup-dev import-latest-to-test start-test-stack start-backup

help:
	@echo "Available targets: backup-dev, import-latest-to-test, start-test-stack, start-backup"

backup-dev:
	@echo "Create dev DB dump (uses container 'physiokalendar-db-1' or 'db-1')"
	@bash -c "mkdir -p backups && docker exec -i physiokalendar-db-1 sh -c 'exec mysqldump -uphysiouser -p\"devpassword\" --single-transaction --quick --no-tablespaces --routines --triggers --events physiocalendar' | gzip > backups/dev_dump_\$$(date +%Y%m%d_%H%M%S).sql.gz"

import-latest-to-test:
	@echo "Import the latest dev dump into physio-test-db (runs scripts/import_latest_to_test.sh -y)"
	@bash scripts/import_latest_to_test.sh -y

start-test-stack:
	docker compose -f compose.test.yml --env-file .env.test up -d --build

start-backup:
	docker compose -f compose.test.yml --env-file .env.test up -d physio-test-backup
