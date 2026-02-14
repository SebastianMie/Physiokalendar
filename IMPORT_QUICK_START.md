# 🚀 Datenimport - Schnellstart

## Option 1: AUTOMATISCHER IMPORT beim Startup ⭐ (EINFACHSTE)

### Schritt 1: application.properties editieren
Datei: `src/main/resources/application.properties`

Füge am Ende diese Zeilen ein:
```properties
# Datenimport beim Startup aktivieren
app.dataimport.enabled=true
app.dataimport.filepath=src/main/java/com/example/physiokalendar/dataimport/backup.json
```

### Schritt 2: Spring Boot starten
```bash
./mvnw spring-boot:run
```

### Schritt 3: Warten und fertig! ✓
Die Daten werden **automatisch** beim Start importiert:
```
╔════════════════════════════════════════╗
║   DATENIMPORT - AUTOMATISCHER START    ║
╚════════════════════════════════════════╝

Datei: src/main/java/com/example/physiokalendar/dataimport/backup.json
Status: Importiere Daten...

✓ Import erfolgreich abgeschlossen!
  Error-Log: src/main/java/com/example/physiokalendar/dataimport/error_log.txt
```

---

## Option 2: REST-API (HTTP-Request)

### cURL-Befehl
```bash
curl -X POST http://localhost:8080/api/dataimport/import \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=src/main/java/com/example/physiokalendar/dataimport/backup.json"
```

### Postman
1. **Neue Request erstellen**
   - Typ: `POST`
   - URL: `http://localhost:8080/api/dataimport/import`

2. **Body** → `x-www-form-urlencoded`
   - Key: `filePath`
   - Value: `src/main/java/com/example/physiokalendar/dataimport/backup.json`

3. **Send** klicken

### Antwort
```
Import abgeschlossen
```

---

## Option 3: Java-TestKlasse (für Entwickler)

Datei: `src/test/java/com/example/physiokalendar/DataImportTest.java`

```java
package com.example.physiokalendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.physiokalendar.dataimport.DataImportService;

@SpringBootTest
class DataImportTest {

    @Autowired
    private DataImportService dataImportService;

    @Test
    void importBackupData() {
        String filePath = "src/main/java/com/example/physiokalendar/dataimport/backup.json";
        dataImportService.importData(filePath);
        System.out.println("✓ Import erfolgreich!");
    }
}
```

Ausführen:
```bash
./mvnw test -Dtest=DataImportTest
```

---

## 📊 Welche Option sollte ich verwenden?

| Option | Wann | Vorteile | Nachteile |
|--------|------|----------|----------|
| **1: Auto-Import** | ✅ Erste Daten laden | Automatisch, einfach | Immer beim Start |
| **2: REST-API** | ✅ Mehrmals importieren | Flexibel, jederzeit | Manuell aufrufen |
| **3: TestKlasse** | 🧪 Entwicklung/Testing | Gutes Debugging | Nur für Tests |

### 🎯 Empfehlung
**Option 1** zum initialen Laden, dann **Option 2** Falls Sie neu importieren möchten.

---

## ⚙️ Konfiguration (Optional)

### In application-dev.properties (Entwicklung)
```properties
app.dataimport.enabled=true
app.dataimport.filepath=src/main/java/com/example/physiokalendar/dataimport/backup.json
```

### In application-prod.properties (Produktion)
```properties
app.dataimport.enabled=false
```

### Umgebungsvariablen (Docker/Container)
```bash
export APP_DATAIMPORT_ENABLED=true
export APP_DATAIMPORT_FILEPATH=/app/data/backup.json
```

---

## ❓ Häufig gestellte Fragen

### F: Kann ich die Fehler sehen?
A: Ja! Überprüfe die Datei:
```
src/main/java/com/example/physiokalendar/dataimport/error_log.txt
```

### F: Kann ich den Import deaktivieren?
A: Ja, in application.properties ändern:
```properties
app.dataimport.enabled=false
```

### F: Wo ist die backup.json?
A: Die Datei sollte hier liegen:
```
src/main/java/com/example/physiokalendar/dataimport/backup.json
```

### F: Wie lange dauert der Import?
A: Abhängig von der Datenmenge, typisch 5-10 Minuten.

---

## ✅ Checkliste

- [ ] backup.json ist vorhanden in `src/main/java/com/example/physiokalendar/dataimport/`
- [ ] Spring Boot läuft: `./mvnw spring-boot:run`
- [ ] Für Auto-Import: application.properties konfiguriert
- [ ] Import gestartet (eine der 3 Optionen)
- [ ] Error-Log überprüft (optional)
- [ ] Daten in DB vorhanden
- [ ] UI überprüft auf neue Therapeuten/Patienten/Termine

---

## 🔗 Weitere Links

- [Detaillierte Dokumentation](DATAIMPORT.md)
- [Implementierungs-Übersicht](IMPORT_SUMMARY.md)
- [Error-Log](src/main/java/com/example/physiokalendar/dataimport/error_log.txt)
