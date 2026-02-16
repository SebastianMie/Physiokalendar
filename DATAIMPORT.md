# Datenimport für Physiokalendar

## Übersicht

Das Datenimport-System ermöglicht es, Daten aus einer JSON-Datei (backup.json) in die Physiokalendar-Datenbank zu importieren. Der Import unterstützt:

- **Therapeuten** (Therapists)
- **Patienten** (Patients)
- **Einzeltermine** (Appointments aus daylist)
- **Serientermine** (Recurring Appointments aus masterlist)
- **Abwesenheiten** (Absences)

## JSON-Struktur

Die backup.json muss folgende Struktur aufweisen:

```json
{
  "therapists": [
    {
      "name": "André",
      "id": "9301eecb-4179-4b86-892b-c6f8cfa18fa4",
      "activeSince": 1770126057559,
      "activeUntil": -1,
      "absences": [
        {
          "day": "Montag" oder "01.01.2025",
          "start": "09:00",
          "end": "17:00",
          "reason": "Urlaub"
        }
      ]
    }
  ],
  "daylist": {
    "elements": [
      {
        "date": 1754956800000,
        "appointments": [
          {
            "id": "termin-123",
            "patient": "Mustermann, Max",
            "therapist": "André",
            "startTime": "09:00",
            "endTime": "10:00",
            "comment": "Physiotherapie",
            "isHotair": false,
            "isUltrasonic": false,
            "isElectric": false
          }
        ]
      }
    ]
  },
  "masterlist": {
    "elements": [
      {
        "weekday": "Montag",
        "appointments": [
          {
            "id": "serie-123",
            "patient": "Mustermann, Max",
            "therapist": "André",
            "startTime": "09:00",
            "endTime": "10:00",
            "startDate": 1754956800000,
            "endDate": 1767225600000,
            "interval": 1,
            "comment": "Regelmäßige Therapie",
            "cancellations": [
              {
                "date": "15.05.2025"
              }
            ]
          }
        ]
      }
    ]
  }
}
```

## Verwendung

### 1. API-HTTP-Request

```bash
curl -X POST http://localhost:8080/api/dataimport/import \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=src/main/java/com/example/physiokalendar/dataimport/backup.json"
```

> Hinweis: Im `test`‑Profil kann der Import automatisch beim Start ausgeführt werden — `app.dataimport.enabled=true` in `application-test.yml` (verwendet dieselbe `backup.json`).

### 2. Direkt in Java-Code

```java
@Autowired
private DataImportService dataImportService;

public void doImport() {
    dataImportService.importData("src/main/java/com/example/physiokalendar/dataimport/backup.json");
}
```

## Fehlerbehandlung

### Fehler-Log

Alle Fehler werden in folgende Datei geschrieben:
```
src/main/java/com/example/physiokalendar/dataimport/error_log.txt
```

Das Fehlerlog enthält:
- Fehlgeschlagene Therapist-Importe
- Fehlgeschlagene Patient-Importe
- Fehlgeschlagene Termin-Importe mit Details (ID, Patient, Therapeut, Datum)
- Fehlgeschlagene Serien-Termin-Importe
- Fehlgeschlagene Abwesenheits-Importe

Beispiel:
```
========================================
Importieren gestartet am: 2025-02-14 10:30:00
========================================

--- Importiere Therapeuten ---
OK: Therapeut 'André' importiert
OK: Therapeut 'Torben' importiert

--- Importiere Patienten ---
OK: Patient 'Mustermann, Max' importiert

--- Importiere Einzeltermine (Daylist) ---
OK: 150 Einzeltermine importiert

--- Importiere Serientermine (Masterlist) ---
OK: 50 Serientermine importiert
FEHLER bei Serientermin-ID: serie-999 - Patient: Unbekannt, Therapeut: André, Wochentag: Montag

--- Importiere Abwesenheiten ---
OK: 25 Abwesenheiten importiert

========================================
Importieren abgeschlossen am: 2025-02-14 10:45:00
========================================
```

## Importverhalten

### Therapeuten
- ✅ Nur neue Therapeuten werden importiert
- ✅ Existierende Therapeuten (nach Name) werden übersprungen
- ✅ `activeSince` wird aus JSON übernommen oder auf "jetzt" gesetzt
- ✅ `activeUntil` wird aus JSON übernommen oder null gelassen
- ❌ Fehlende Namen werden übersprungen

### Patienten
- ✅ Patienten werden automatisch erstellt, wenn sie nicht existieren
- ✅ Name wird im Format "Nachname, Vorname" geparst
- ✅ Vollständiger Name wird kombiniert
- ✅ Standard-Werte für `activeSince`, `isBWO` werden gesetzt
- ❌ Ungültige Namen werden übersprungen

### Einzeltermine (Daylist)
- ✅ Patient und Therapeut müssen existieren
- ✅ Uhrzeit wird aus String (HH:mm) geparst
- ✅ Behandlungsarten (Hotair, Ultrasonic, Electric) werden importiert
- ✅ Kommentare werden übernommen
- ✅ Fehlende Daten (Patient/Therapeut) führen zu Log-Eintrag

### Serientermine (Masterlist)
- ✅ Wöchentliche oder beliebig häufige Serien möglich
- ✅ Enddatum wird auf 01.01.2026 begrenzt (wenn später)
- ✅ Fällt automatisch generierte Einzeltermine
- ✅ Ausfalltermine (Cancellations) werden berücksichtigt

### Abwesenheiten (Absences)
- ✅ Unterscheidung zwischen Wochentag und Einzeldatum
- ✅ Zeitangaben sind optional
- ✅ Grund wird übernommen (falls vorhanden)
- ❌ Abwesenheiten ohne Therapeut werden übersprungen

## Datenmapping

| JSON-Feld | Entity-Feld | Typ | Notizen |
|-----------|-------------|-----|---------|
| therapist.name | Therapist.firstName | String | lastName bleibt leer |
| therapist.activeSince | Therapist.activeSince | LocalDateTime | Millisekunden → LocalDateTime |
| patient (Format: "Nachname, Vorname") | Patient.firstName, lastName | String | Name wird geparst |
| appointment.startTime | Appointment.startTime | LocalDateTime | Format "HH:mm" |
| appointment.date | Appointment.date | LocalDate | Millisekunden → LocalDate |
| appointmentSeries.interval | AppointmentSeries.weeklyfrequency | Integer | Default: 1 |
| absence.day | Absence.date oder Absence.weekday | LocalDate/String | Automatische Erkennung |

## Bekannte Einschränkungen

1. **Patienten-Matching**: Patienten werden nur nach exaktem Namen gefunden (Case-Sensitive)
2. **Therapeut-Matching**: Therapeuten werden nur nach Vornamen gefunden
3. **Enddatum-Limitierung**: Serientermine sind auf 01.01.2026 begrenzt
4. **Keine Duplikat-Erkennung**: Doppelte Einzeltermine werden nicht erkannt
5. **Keine Transaktion**: Fehler beeinflussen nicht andere Operationen

## Empfohlenes Vorgehen

1. **Datenbank Backup machen** vor dem Import
2. **error_log.txt löschen** (optional, um neue Fehler zu sehen)
3. **Import ausführen** mit der backup.json
4. **Error-Log überprüfen** auf Fehler
5. **Daten validieren** in der UI
6. **Bei Fehlern**: Fehlerhafte Einträge in backup.json korrigieren und erneut importieren

## Debugging

### Fehler beim Parsen von Datumsangaben
- Achten Sie darauf, dass Daten im Format `dd.MM.yyyy` sind
- Zeiten im Format `HH:mm`

### Patient/Therapeut nicht gefunden
- Name muss exakt übereinstimmen (Case + Spacing)
- Bei neuen Therapeuten: ZUERST Therapeuten importieren

### Keine Serientermine generiert
- Prüfen Sie, dass `startDate` < `endDate`
- Wochentag muss auf Deutsch sein (Montag, Dienstag, etc.)
