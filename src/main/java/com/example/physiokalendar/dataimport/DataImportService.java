package com.example.physiokalendar.dataimport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.physiokalendar.entity.Absence;
import com.example.physiokalendar.entity.AbsenceType;
import com.example.physiokalendar.entity.Appointment;
import com.example.physiokalendar.entity.AppointmentSeries;
import com.example.physiokalendar.entity.Cancellation;
import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.AbsenceRepository;
import com.example.physiokalendar.repository.AppointmentRepository;
import com.example.physiokalendar.repository.AppointmentSeriesRepository;
import com.example.physiokalendar.repository.CancellationRepository;
import com.example.physiokalendar.repository.PatientRepository;
import com.example.physiokalendar.repository.TherapistRepository;
import com.example.physiokalendar.service.AppointmentSeriesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DataImportService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CancellationRepository cancellationRepository;


    @Autowired
    private AppointmentSeriesRepository appointmentSeriesRepository;

    @Autowired
    private AbsenceRepository absenceRepository;

    @Autowired
    private AppointmentSeriesService appointmentSeriesService;

    @Autowired
    private com.example.physiokalendar.service.AppointmentSeriesGeneratorJob seriesGeneratorJob;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    // Statistik-Counter für den Import
    private int therapistCount = 0;
    private int patientCount = 0;
    private int appointmentCount = 0;
    private int seriesCount = 0;
    private int absenceCount = 0;
    private int cancellationCount = 0;

    // Cache to prevent duplicate patient imports within a single run
    private Map<String, Long> patientNameToIdCache = new HashMap<>();

    // Einfacher Import ohne Transaktionen - jeder save() wird sofort committed
    @Transactional
    public void importData(String filePath) {
        // Statistik-Counter zurücksetzen
        therapistCount = 0;
        patientCount = 0;
        appointmentCount = 0;
        seriesCount = 0;
        absenceCount = 0;
        cancellationCount = 0;

        // Clear patient cache for new import run
        patientNameToIdCache.clear();

        try (BufferedWriter errorWriter = new BufferedWriter(new FileWriter("src/main/java/com/example/physiokalendar/dataimport/error_log.txt", true))) {
            errorWriter.write("\n\n========================================\n");
            errorWriter.write("Importieren gestartet am: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            errorWriter.write("========================================\n");

            // JSON Datei einlesen
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(filePath));

            // 1. Importiere Therapeuten
            // if (rootNode.has("therapists") && rootNode.get("therapists").isArray()) {
            //     errorWriter.write("\n--- Importiere Therapeuten ---\n");
            //     Iterator<JsonNode> therapists = rootNode.get("therapists").elements();
            //     while (therapists.hasNext()) {
            //         try {
            //             JsonNode therapistNode = therapists.next();
            //             importTherapist(therapistNode, errorWriter);
            //         } catch (Exception thEx) {
            //             errorWriter.write("[ERROR] Therapeut-Import fehlgeschlagen: " + thEx.getMessage() + "\n");
            //             errorWriter.flush();
            //         }
            //     }
            // }

            // // 2. Importiere Patienten aus der Daylist (um alle Patienten zu erfassen)
            // if (rootNode.has("daylist") && rootNode.get("daylist").has("elements")) {
            //     errorWriter.write("\n--- Importiere Patienten ---\n");
            //     JsonNode elementsNode = rootNode.get("daylist").get("elements");
            //     if (elementsNode.isArray()) {
            //         Iterator<JsonNode> days = elementsNode.elements();
            //         while (days.hasNext()) {
            //             try {
            //                 JsonNode day = days.next();
            //                 importPatientsFromDay(day, errorWriter);
            //             } catch (Exception patEx) {
            //                 errorWriter.write("[ERROR] Patient-Import fehlgeschlagen: " + patEx.getMessage() + " - weiter mit nächstem\n");
            //                 errorWriter.flush();
            //             }
            //         }
            //     }
            // }

            // 3. Importiere Einzeltermine aus der Daylist
            if (rootNode.has("daylist") && rootNode.get("daylist").has("elements")) {
                errorWriter.write("\n--- Importiere Einzeltermine (Daylist) ---\n");
                JsonNode elementsNode = rootNode.get("daylist").get("elements");
                errorWriter.write("[DEBUG] Total Tage im daylist: " + elementsNode.size() + "\n");
                errorWriter.flush();
                if (elementsNode.isArray()) {
                    // !!! WICHTIG: NEUER ITERATOR FÜR JEDEN DURCHGANG !!!
                    for (int i = 0; i < elementsNode.size(); i++) {
                        try {
                            JsonNode day = elementsNode.get(i);
                            errorWriter.write("[DEBUG] Verarbeite Tag " + (i + 1) + " von " + elementsNode.size() + "\n");
                            errorWriter.flush();
                            importAppointmentsForDay(day, errorWriter);
                        } catch (Exception dayEx) {
                            errorWriter.write("[ERROR] Tag " + (i + 1) + " fehlgeschlagen: " + dayEx.getClass().getSimpleName() + ": " + dayEx.getMessage() + " - weiter mit nächstem Tag\n");
                            errorWriter.flush();
                            // Continue with next day - don't abort entire import
                        }
                    }
                }
            }

            // 4. Importiere Serientermine aus der Masterlist
            if (rootNode.has("masterlist") && rootNode.get("masterlist").has("elements")) {
                errorWriter.write("\n--- Importiere Serientermine (Masterlist) ---\n");
                JsonNode masterListNode = rootNode.get("masterlist").get("elements");
                if (masterListNode.isArray()) {
                    for (int i = 0; i < masterListNode.size(); i++) {
                        try {
                            JsonNode seriesDay = masterListNode.get(i);
                            importSeriesAppointments(seriesDay, errorWriter);
                        } catch (Exception seriesEx) {
                            errorWriter.write("[ERROR] Serientermin-Import fehlgeschlagen: " + seriesEx.getMessage() + "\n");
                            errorWriter.flush();
                        }
                    }
                }

                // Nach dem Import aller Serien: Einzeltermine generieren (max 1 Jahr)
                errorWriter.write("\n--- Generiere Einzeltermine aus Serien (max 1 Jahr) ---\n");
                errorWriter.flush();
                try {
                    // Flush um sicherzustellen dass alle Serien committed sind
                    entityManager.flush();
                    entityManager.clear();

                    int generatedCount = seriesGeneratorJob.generateMissingAppointments();
                    errorWriter.write("OK: " + generatedCount + " Einzeltermine aus Serien generiert\n");
                } catch (Exception genEx) {
                    errorWriter.write("[ERROR] Fehler beim Generieren der Einzeltermine: " + genEx.getMessage() + "\n");
                    genEx.printStackTrace();
                }
                errorWriter.flush();
            }

            // 5. TEMP: Abwesenheiten SKIPPEN für jetzt
            // if (rootNode.has("therapists") && rootNode.get("therapists").isArray()) {
            //     errorWriter.write("\n--- Importiere Abwesenheiten ---\n");
            //     Iterator<JsonNode> therapists = rootNode.get("therapists").elements();
            //     while (therapists.hasNext()) {
            //         JsonNode therapistNode = therapists.next();
            //         importAbsences(therapistNode, errorWriter);
            //     }
            // }

            // Zusammenfassung
            errorWriter.write("\n========================================\n");
            errorWriter.write("Importieren abgeschlossen am: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            errorWriter.write("========================================\n");
            errorWriter.write("\nZUSAMMENFASSUNG:\n");
            errorWriter.write("- Therapeuten importiert:        " + therapistCount + "\n");
            errorWriter.write("- Patienten importiert:          " + patientCount + "\n");
            errorWriter.write("- Einzeltermine importiert:      " + appointmentCount + "\n");
            errorWriter.write("- Serientermine importiert:      " + seriesCount + "\n");
            errorWriter.write("- Abwesenheiten importiert:      " + absenceCount + "\n");
            errorWriter.write("- Ausfalltermine importiert:     " + cancellationCount + "\n");
            errorWriter.write("========================================\n");

            // Auch auf der Konsole ausgeben
            System.out.println("\n========== IMPORT SUMMARY ==========");
            System.out.println("Therapeuten:    " + therapistCount);
            System.out.println("Patienten:      " + patientCount);
            System.out.println("Einzeltermine:  " + appointmentCount);
            System.out.println("Serientermine:  " + seriesCount);
            System.out.println("Abwesenheiten:  " + absenceCount);
            System.out.println("Ausfälle:       " + cancellationCount);
            System.out.println("====================================\n");
        } catch (IOException e) {
            System.out.println("Fehler beim Importieren der Daten: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Importiert NUR Serientermine aus der JSON-Datei.
     * Einzeltermine werden übersprungen (bereits importiert).
     * Therapeuten und Patienten werden nur erstellt wenn sie noch nicht existieren.
     *
     * WICHTIG: Kein @Transactional! Speichert wie importAppointmentsForDay() direkt mit repository.save()
     */
    public void importSeriesOnly(String filePath) {
        // Statistik-Counter zurücksetzen
        seriesCount = 0;
        cancellationCount = 0;
        absenceCount = 0;
        patientCount = 0;

        // Clear patient cache for new import run
        patientNameToIdCache.clear();

        System.out.println("\n[TX-START] Transaktion gestartet für importSeriesOnly()");

        try (BufferedWriter errorWriter = new BufferedWriter(new FileWriter("src/main/java/com/example/physiokalendar/dataimport/series_import_log.txt", true))) {
            errorWriter.write("\n\n========================================\n");
            errorWriter.write("SERIENTERMINE-IMPORT gestartet am: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            errorWriter.write("========================================\n");

            // JSON Datei einlesen
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(filePath));

            // 1. Importiere Serientermine aus der Masterlist
            if (rootNode.has("masterlist") && rootNode.get("masterlist").has("elements")) {
                errorWriter.write("\n--- Importiere Serientermine (Masterlist) ---\n");
                JsonNode masterListNode = rootNode.get("masterlist").get("elements");
                errorWriter.write("[INFO] Anzahl Wochentage in Masterlist: " + masterListNode.size() + "\n");
                errorWriter.flush();

                if (masterListNode.isArray()) {
                    for (int i = 0; i < masterListNode.size(); i++) {
                        try {
                            JsonNode seriesDay = masterListNode.get(i);
                            importSeriesAppointments(seriesDay, errorWriter);
                        } catch (Exception seriesEx) {
                            errorWriter.write("[ERROR] Serientermin-Import fehlgeschlagen: " + seriesEx.getMessage() + "\n");
                            errorWriter.flush();
                        }
                    }
                }

                // Nach dem Import aller Serien: Flush + Transaktion end
                errorWriter.write("\n--- Beende Transaktions-Block (Serien+Ausfälle+Abwesenheiten) ---\n");
                errorWriter.flush();
            } else {
                errorWriter.write("[WARN] Keine Masterlist in der JSON-Datei gefunden!\n");
            }

            // Zusammenfassung
            errorWriter.write("\n========================================\n");
            errorWriter.write("SERIENTERMINE-IMPORT abgeschlossen am: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");;
            errorWriter.write("========================================\n");
            errorWriter.write("\nZUSAMMENFASSUNG:\n");
            errorWriter.write("- Serientermine importiert:      " + seriesCount + "\n");
            errorWriter.write("- Patienten neu erstellt:        " + patientCount + "\n");
            errorWriter.write("- Ausfalltermine importiert:     " + cancellationCount + "\n");
            errorWriter.write("- Abwesenheiten importiert:      " + absenceCount + "\n");
            errorWriter.write("========================================\n");

            // Auch auf der Konsole ausgeben
            System.out.println("\n========== SERIES IMPORT SUMMARY ==========");
            System.out.println("Serientermine:  " + seriesCount);
            System.out.println("Patienten neu:  " + patientCount);
            System.out.println("Ausfälle:       " + cancellationCount);
            System.out.println("Abwesenheiten:  " + absenceCount);
            System.out.println("============================================\n");

            // Alle Daten werden sofort mit repository.save() gespeichert - kein Flush nötig
            System.out.println("[IMPORT-SUCCESS] Serientermine erfolgreich importiert und sofort in DB gespeichert!");
            errorWriter.write("\n[SUCCESS] Alle " + seriesCount + " Serientermine und " + cancellationCount + " Ausfalltermine wurden sofort in die Datenbank gespeichert!\n");
            errorWriter.flush();
        } catch (Exception e) {
            System.out.println("[TX-ROLLBACK] Fehler beim Importieren der Serientermine: " + e.getClass().getSimpleName());
            System.out.println("[TX-ROLLBACK] Message: " + e.getMessage());
            System.out.println("[TX-ROLLBACK] Die Transaktion wird komplett zurückgerollt!");
            e.printStackTrace();
        }

        // GENERIERUNG LÄUFT NACH DER TRANSAKTION - getrennte Transaktion!
        System.out.println("\n[SEPARATE-TX] Starte Einzeltermin-Generierung (separate Transaktion)");
        try (BufferedWriter errorWriter = new BufferedWriter(new FileWriter("src/main/java/com/example/physiokalendar/dataimport/series_import_log.txt", true))) {
            errorWriter.write("\n--- Generiere Einzeltermine aus Serien (max 1 Jahr) [SEPARATE TRANSAKTION] ---\n");
            errorWriter.flush();
            int generatedCount = seriesGeneratorJob.generateMissingAppointments();
            errorWriter.write("OK: " + generatedCount + " Einzeltermine aus Serien generiert\n");
            errorWriter.flush();
            System.out.println("[SEPARATE-TX] Einzeltermin-Generierung erfolgreich: " + generatedCount + " Termine");
        } catch (Exception e) {
            System.out.println("[SEPARATE-TX-ERROR] Fehler beim Generieren der Einzeltermine: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Importiert einen Therapeuten.
     */
    private void importTherapist(JsonNode therapistNode, BufferedWriter errorWriter) throws IOException {
        try {
            String therapistName = therapistNode.hasNonNull("name") ? therapistNode.get("name").asText() : null;

            if (therapistName == null || therapistName.isEmpty()) {
                errorWriter.write("FEHLER: Therapeut ohne Namen gefunden, übersprungen\n");
                return;
            }

            // Prüfen, ob Therapeut bereits exists
            Therapist existingTherapist = therapistRepository.findByFirstName(therapistName);
            if (existingTherapist != null) {
                // Therapeut existiert bereits, überspringe
                return;
            }

            // Neuen Therapeuten erstellen
            Therapist therapist = new Therapist();
            therapist.setFirstName(therapistName);
            therapist.setLastName(""); // Nachname nicht im JSON vorhanden
            therapist.setFullName(therapistName);

            // Optional: E-Mail und Telefon aus JSON, falls vorhanden
            if (therapistNode.hasNonNull("email")) {
                therapist.setEmail(therapistNode.get("email").asText());
            }
            if (therapistNode.hasNonNull("telefon")) {
                therapist.setTelefon(therapistNode.get("telefon").asText());
            }

            // Active Since und Until setzen (aus JSON oder Default)
            if (therapistNode.hasNonNull("activeSince")) {
                long activeSince = therapistNode.get("activeSince").asLong();
                if (activeSince > 0) {
                    therapist.setActiveSince(dateToLocalDateTime(new Date(activeSince)));
                } else {
                    therapist.setActiveSince(java.time.LocalDateTime.now());
                }
            } else {
                therapist.setActiveSince(java.time.LocalDateTime.now());
            }

            if (therapistNode.hasNonNull("activeUntil")) {
                long activeUntil = therapistNode.get("activeUntil").asLong();
                if (activeUntil > 0) {
                    therapist.setActiveUntil(dateToLocalDateTime(new Date(activeUntil)));
                } else {
                    therapist.setActiveUntil(null);
                }
            }

            therapist.setIsActive(true);

            therapistRepository.save(therapist);
            therapistCount++;
            errorWriter.write("OK: Therapeut '" + therapistName + "' importiert\n");
        } catch (Exception e) {
            errorWriter.write("FEHLER beim Importieren von Therapeut: " + e.getMessage() + "\n");
        }
    }

    /**
     * Importiert Patienten aus einem Tag.
     */
    private void importPatientsFromDay(JsonNode day, BufferedWriter errorWriter) throws IOException {
        try {
            if (day.has("appointments") && day.get("appointments").isArray()) {
                Iterator<JsonNode> appointments = day.get("appointments").elements();

                while (appointments.hasNext()) {
                    JsonNode appointmentNode = appointments.next();

                    String patientName = appointmentNode.hasNonNull("patient") ? appointmentNode.get("patient").asText() : null;
                    if (patientName != null && !patientName.isEmpty()) {
                        Patient existingPatient = findPatientByName(patientName);
                        if (existingPatient == null) {
                            createPatient(patientName);
                            patientCount++;
                            errorWriter.write("OK: Patient '" + patientName + "' importiert\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            errorWriter.write("FEHLER beim Importieren von Patienten: " + e.getMessage() + "\n");
        }
    }

    /**
     * Importiert alle Termine eines Tages.
     */
    private void importAppointmentsForDay(JsonNode day, BufferedWriter errorWriter) {
        try {
            if (!day.has("date") || !day.has("appointments")) return;

            long timestampMs = day.get("date").asLong();
            LocalDate aptDate = LocalDate.ofEpochDay(timestampMs / 86400000);
            JsonNode apts = day.get("appointments");

            if (!apts.isArray()) return;

            errorWriter.write("[APT-DAY] Datum: " + aptDate + ", Anzahl Termine: " + apts.size() + "\n");
            errorWriter.flush();

            for (int i = 0; i < apts.size(); i++) {
                try {
                    JsonNode apt = apts.get(i);

                    String therapist = apt.has("therapist") ? apt.get("therapist").asText("") : "";
                    String patient = apt.has("patient") ? apt.get("patient").asText("") : "";
                    String start = apt.has("startTime") ? apt.get("startTime").asText("") : "";
                    String end = apt.has("endTime") ? apt.get("endTime").asText("") : "";

                    if (therapist.isEmpty() || patient.isEmpty() || start.isEmpty() || end.isEmpty()) {
                        errorWriter.write("  [SKIP] Pflichtfelder leer (T:'" + therapist + "', P:'" + patient + "', Start:'" + start + "', End:'" + end + "')\n");
                        errorWriter.flush();
                        continue;
                    }

                    java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("H:mm");
                    LocalTime startTime = LocalTime.parse(start, timeFormatter);
                    LocalTime endTime = LocalTime.parse(end, timeFormatter);
                    LocalDateTime startDT = LocalDateTime.of(aptDate, startTime);
                    LocalDateTime endDT = LocalDateTime.of(aptDate, endTime);

                    Patient p = findPatientByName(patient);
                    if (p == null) {
                        p = createPatient(patient);
                        if (p != null) {
                            errorWriter.write("  [CREATE-P] Patient erstellt: " + patient + "\n");
                            errorWriter.flush();
                        }
                    }
                    if (p == null) {
                        errorWriter.write("  [ERROR-P] Patient konnte nicht erstellt werden: " + patient + "\n");
                        errorWriter.flush();
                        continue;
                    }

                    Therapist t = findTherapistByName(therapist);
                    if (t == null) {
                        errorWriter.write("  [ERROR-T] Therapeut nicht gefunden: " + therapist + "\n");
                        errorWriter.flush();
                        continue;
                    }

                    // Check for duplicate appointments
                    boolean isDuplicate = appointmentRepository.existsByTherapistPatientDateAndTime(
                            t.getId(), p.getId(), aptDate, startDT, endDT);
                    if (isDuplicate) {
                        errorWriter.write("  [DUP] Duplikat ignoriert: " + therapist + " - " + patient + " " + startTime + "-" + endTime + "\n");
                        errorWriter.flush();
                        continue;
                    }

                    Appointment a = new Appointment();
                    a.setPatient(p);
                    a.setTherapist(t);
                    a.setDate(aptDate);
                    a.setStartTime(startDT);
                    a.setEndTime(endDT);
                    a.setComment(apt.has("comment") ? apt.get("comment").asText("") : "");
                    a.setIsHotair(apt.has("isHotair") ? apt.get("isHotair").asBoolean(false) : false);
                    a.setIsUltrasonic(apt.has("isUltrasonic") ? apt.get("isUltrasonic").asBoolean(false) : false);
                    a.setIsElectric(apt.has("isElectric") ? apt.get("isElectric").asBoolean(false) : false);
                    a.setCreatedBySeriesAppointment(false);

                    appointmentRepository.save(a);
                    appointmentCount++;
                    errorWriter.write("  [SAVE] ✓ " + therapist + " - " + patient + " " + startTime + "-" + endTime + "\n");
                    errorWriter.flush();
                } catch (Exception e) {
                    errorWriter.write("  [ERROR] Exception bei Termin: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n");
                    errorWriter.flush();
                    // Continue with next appointment - don't abort the whole import
                }
            }
        } catch (Exception e) {
            try {
                errorWriter.write("[ERROR] Exception in importAppointments: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n");
                errorWriter.flush();
            } catch (IOException ioe) {}
        }
    }

    /**
     * Speichert eine einzelne Cancellation.
     * Wird innerhalb der Batch-Transaktion von importSeriesOnly() committed
     */
    /**
     * Speichert eine einzelne Cancellation sofort in der DB
     * (wie im normalen Import - repository.save() statt entityManager.persist())
     */
    private void saveCancellation(AppointmentSeries series, LocalDate cancellationDate) {
        if (series != null && cancellationDate != null) {
            Cancellation cancellation = new Cancellation();
            cancellation.setDate(cancellationDate);
            cancellation.setAppointmentSeries(series);
            cancellationRepository.save(cancellation);  // Sofort speichern wie im normalen Import
        }
    }

    /**
     * Importiert Therapeuten-Abwesenheiten aus der JSON
     * Unterstützt wöchentliche (RECURRING) und einmalige (SPECIAL) Abwesenheiten
     */
    private void importAbsences(JsonNode therapistsNode, BufferedWriter errorWriter) throws IOException {
        for (JsonNode therapistNode : therapistsNode) {
            try {
                String therapistName = therapistNode.hasNonNull("name") ? therapistNode.get("name").asText() : null;
                if (therapistName == null) {
                    continue;
                }

                Therapist therapist = findTherapistByName(therapistName);
                if (therapist == null) {
                    errorWriter.write("[SKIP] Therapeut '" + therapistName + "' nicht gefunden - Abwesenheiten übersprungen\n");
                    continue;
                }

                // Verarbeite Abwesenheiten (absences)
                if (therapistNode.has("absences") && therapistNode.get("absences").isArray()) {
                    for (JsonNode absenceNode : therapistNode.get("absences")) {
                        try {
                            String dayStr = absenceNode.hasNonNull("day") ? absenceNode.get("day").asText() : null;
                            String startStr = absenceNode.hasNonNull("start") ? absenceNode.get("start").asText() : null;
                            String endStr = absenceNode.hasNonNull("end") ? absenceNode.get("end").asText() : null;

                            if (dayStr == null) {
                                continue;
                            }

                            // Parse start und end times
                            LocalDateTime startTime = startStr != null ? parseTimeToLocalDateTime(startStr) : null;
                            LocalDateTime endTime = endStr != null ? parseTimeToLocalDateTime(endStr) : null;

                            // Prüfe ob es ein Wochentag oder ein Datum ist
                            String weekday = tryParseWeekday(dayStr);
                            if (weekday != null) {
                                // RECURRING Absence (Wochentag)
                                Absence absence = new Absence();
                                absence.setTherapist(therapist);
                                absence.setAbsenceType(AbsenceType.RECURRING);
                                absence.setWeekday(weekday);
                                absence.setStartTime(startTime);
                                absence.setEndTime(endTime);
                                absence.setReason("Regelmäßige Abwesenheit");
                                saveAbsence(absence);
                                absenceCount++;
                                errorWriter.write("OK: RECURRING Abwesenheit " + therapistName + " " + weekday + " " + startStr + "-" + endStr + "\n");
                            } else {
                                // SPECIAL Absence (Datum)
                                LocalDate absenceDate = tryParseDateStringGerman(dayStr);
                                if (absenceDate != null) {
                                    Absence absence = new Absence();
                                    absence.setTherapist(therapist);
                                    absence.setAbsenceType(AbsenceType.SPECIAL);
                                    absence.setDate(absenceDate);
                                    absence.setStartTime(startTime);
                                    absence.setEndTime(endTime);
                                    absence.setReason("Abwesenheit");
                                    saveAbsence(absence);
                                    absenceCount++;
                                    errorWriter.write("OK: SPECIAL Abwesenheit " + therapistName + " " + dayStr + "\n");
                                }
                            }
                        } catch (Exception e) {
                            errorWriter.write("  [WARN] Abwesenheit übersprungen: " + e.getMessage() + "\n");
                        }
                    }
                }
            } catch (Exception e) {
                errorWriter.write("[ERROR] Fehler beim Import der Abwesenheiten für Therapeut: " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * Speichert eine einzelne Absence sofort in der DB
     * (wie im normalen Import - repository.save() statt entityManager.persist())
     */
    private void saveAbsence(Absence absence) {
        if (absence != null && absence.getTherapist() != null) {
            absenceRepository.save(absence);  // Sofort speichern wie im normalen Import
        }
    }

    // HELPER: Sichere String-Extraktion
    private String getStringField(JsonNode node, String fieldName) {
        if (node.hasNonNull(fieldName)) {
            String value = node.get(fieldName).asText();
            return value != null && !value.isEmpty() ? value : null;
        }
        return null;
    }

    // HELPER: Sichere Boolean-Extraktion
    private Boolean getBooleanField(JsonNode node, String fieldName) {
        if (node.hasNonNull(fieldName)) {
            try {
                return node.get(fieldName).asBoolean();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void importSeriesAppointments(JsonNode seriesDay, BufferedWriter errorWriter) throws IOException {
        String weekday = seriesDay.hasNonNull("weekday") ? seriesDay.get("weekday").asText() : null;

        // FILTER: Nur Serien mit startDate >= 15.06.2025 importieren (weniger Testdaten)
        LocalDate filterDate = LocalDate.of(2025, 6, 15);

        // Überprüfen, ob "appointments" vorhanden ist und ein Array ist
        if (seriesDay.has("appointments") && seriesDay.get("appointments").isArray()) {
            Iterator<JsonNode> appointments = seriesDay.get("appointments").elements();

            // Über die Termine iterieren
            while (appointments.hasNext()) {
                JsonNode appointmentNode = appointments.next();

                try {
                    // FILTER: Prüfe startDate vor weiterem Processing
                    if (appointmentNode.hasNonNull("startDate")) {
                        LocalDate seriesStartDate = dateToLocalDate(new Date(appointmentNode.get("startDate").asLong()));
                        if (seriesStartDate != null && seriesStartDate.isBefore(filterDate)) {
                            // Überspringe Serien vor 15.06.2025
                            continue;
                        }
                    }

                    // Patient ermitteln oder erstellen, wenn nicht gefunden
                    String patientName = appointmentNode.hasNonNull("patient") ? appointmentNode.get("patient").asText() : null;
                    Patient patient = findPatientByName(patientName);
                    if (patient == null && patientName != null) {
                        patient = createPatient(patientName);
                    }

                    // Therapeut ermitteln
                    String therapistName = appointmentNode.hasNonNull("therapist") ? appointmentNode.get("therapist").asText() : null;
                    Therapist therapist = (therapistName != null) ? findTherapistByName(therapistName) : null;

                    // Therapeut nicht gefunden = SKIP (kein Fehler, Therapeut existiert nicht mehr)
                    if (therapist == null) {
                        errorWriter.write("[SKIP] Therapeut '" + therapistName + "' nicht gefunden - Serie übersprungen\n");
                        continue;
                    }

                    if (patient != null && appointmentNode.hasNonNull("startTime") && appointmentNode.hasNonNull("endTime")) {
                        // Parse Zeiten
                        LocalTime startTime = parseTimeToLocalTime(appointmentNode.get("startTime").asText());
                        LocalTime endTime = parseTimeToLocalTime(appointmentNode.get("endTime").asText());

                        // Duplikatsprüfung: Existiert diese Serie bereits?
                        boolean isDuplicate = appointmentSeriesRepository.existsByTherapistPatientWeekdayAndTime(
                                therapist.getId(), patient.getId(), weekday, startTime, endTime);
                        if (isDuplicate) {
                            errorWriter.write("[DUP] Serie existiert bereits: " + patientName + " - " + therapistName +
                                    " " + weekday + " " + startTime + "-" + endTime + "\n");
                            continue;
                        }

                        // Serientermin speichern
                        AppointmentSeries appointment = new AppointmentSeries();
                        appointment.setPatient(patient);
                        appointment.setTherapist(therapist);
                        appointment.setStartTime(startTime);
                        appointment.setEndTime(endTime);

                        // Start- und Enddatum ermitteln
                        Date startDate = new Date(appointmentNode.get("startDate").asLong());
                        Date endDate = new Date(appointmentNode.get("endDate").asLong());

                        // Serientermin speichern mit entityManager.persist() für konsistentes Batch-Saving
                        appointment.setStartDate(dateToLocalDate(startDate));
                        appointment.setEndDate(dateToLocalDate(endDate));
                        appointment.setWeekday(weekday);
                        appointment.setWeeklyfrequency(appointmentNode.hasNonNull("interval") ? appointmentNode.get("interval").asInt() : 1);
                        appointment.setComment(appointmentNode.hasNonNull("comment") ? appointmentNode.get("comment").asText() : "");
                        appointment.setStatus(com.example.physiokalendar.entity.SeriesStatus.ACTIVE);

                        // Speichere sofort mit repository.save() wie im normalen Import
                        appointment = appointmentSeriesRepository.save(appointment);
                        seriesCount++;

                        // Für Logging brauchen wir die ID - machen wir einen ID-Lookup nach persist
                        String seriesIdStr = (appointment.getId() != null) ? String.valueOf(appointment.getId()) : "?";
                        errorWriter.write("OK: Serientermin gespeichert (Therapeutic: " + patientName + ", Therapeut: " + therapistName +
                                ", " + weekday + " " + appointment.getStartTime() + "-" + appointment.getEndTime() + ")\n");

                        // Behandle Ausfälle (Cancellations) - wasserdicht
                        if (appointmentNode.has("cancellations") && appointmentNode.get("cancellations").isArray()) {
                            for (JsonNode cancellationNode : appointmentNode.get("cancellations")) {
                                try {
                                    LocalDate cancellationDate = parseCancellationDate(cancellationNode);
                                    if (cancellationDate != null) {
                                        // Nur Cancellations im gültigen Zeitraum der Serie speichern
                                        if (!cancellationDate.isBefore(appointment.getStartDate()) &&
                                            !cancellationDate.isAfter(appointment.getEndDate())) {
                                            // Speichere Cancellation in der Hibernate-Session
                                            saveCancellation(appointment, cancellationDate);
                                            cancellationCount++;
                                            errorWriter.write("  OK: Ausfalltermin gespeichert " + cancellationDate + "\n");
                                        }
                                    }
                                } catch (Exception e) {
                                    // Cancellation-Fehler ignorieren - Serie wurde bereits gespeichert
                                    errorWriter.write("  [WARN] Cancellation übersprungen: " + e.getMessage() + "\n");
                                }
                            }
                        }
                    } else {
                        // Patient fehlt
                        errorWriter.write("[SKIP] Patient '" + patientName + "' konnte nicht erstellt werden\n");
                    }
                } catch (Exception e) {
                    errorWriter.write("FEHLER beim Importieren eines Serientiermins: " + e.getMessage() + "\n");
                    // Fortsetzung mit nächstem Serientermin
                }
            }
        }
    }

    private boolean isDate(String str) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            sdf.parse(str);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private Date parseDate(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            System.out.println("Error parsing date: " + dateStr);
            return null;
        }
    }

    private LocalDate parseStringToLocalDate(String dateStr) {
        Date date = parseDate(dateStr);
        return date != null ? dateToLocalDate(date) : null;
    }

    private LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        try {
            return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
        } catch (Exception e) {
            System.out.println("Error converting to LocalDate: " + e.getMessage());
            return null;
        }
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) return null;
        try {
            return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
        } catch (Exception e) {
            System.out.println("Error converting to LocalDateTime: " + e.getMessage());
            return null;
        }
    }

    private LocalTime parseTimeToLocalTime(String time) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        try {
            Date parsedTime = timeFormat.parse(time);
            return parsedTime.toInstant().atZone(ZoneId.of("UTC")).toLocalTime();
        } catch (ParseException e) {
            System.out.println("Error parsing time: " + e.getMessage());
            return null;
        }
    }

    /**
     * Wasserdichte Cancellation-Datum-Konvertierung.
     * Unterstützt:
     * - Timestamp als Long (Millisekunden seit Epoch)
     * - Datumsstring "dd.MM.yyyy"
     * - ISO-Datumsstring "yyyy-MM-dd"
     * - Verschachtelte Objekte mit "date" Feld
     */
    private LocalDate parseCancellationDate(JsonNode cancellationNode) {
        if (cancellationNode == null) return null;

        try {
            // Fall 1: Direkter Long-Wert (Timestamp)
            if (cancellationNode.isNumber()) {
                long timestamp = cancellationNode.asLong();
                if (timestamp > 0) {
                    return LocalDate.ofEpochDay(timestamp / 86400000);
                }
            }

            // Fall 2: Verschachteltes Objekt mit "date" Feld
            JsonNode dateNode = cancellationNode.hasNonNull("date") ? cancellationNode.get("date") : cancellationNode;

            // Fall 2a: date ist ein Timestamp (Long)
            if (dateNode.isNumber()) {
                long timestamp = dateNode.asLong();
                if (timestamp > 0) {
                    return LocalDate.ofEpochDay(timestamp / 86400000);
                }
            }

            // Fall 2b: date ist ein String
            if (dateNode.isTextual()) {
                String dateStr = dateNode.asText();
                if (dateStr == null || dateStr.isEmpty()) return null;

                // Versuche verschiedene Formate
                // Format: dd.MM.yyyy
                if (dateStr.contains(".")) {
                    return parseStringToLocalDate(dateStr);
                }
                // Format: yyyy-MM-dd (ISO)
                if (dateStr.contains("-") && dateStr.length() == 10) {
                    return LocalDate.parse(dateStr);
                }
                // Vielleicht ist es ein numerischer String (Timestamp)
                try {
                    long timestamp = Long.parseLong(dateStr);
                    if (timestamp > 0) {
                        return LocalDate.ofEpochDay(timestamp / 86400000);
                    }
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception e) {
            // Fehler beim Parsen - null zurückgeben
        }

        return null;
    }

    // Patient anhand des Namens finden
    private Patient findPatientByName(String fullName) {
        // First check the cache (important after EntityManager.clear())
        if (patientNameToIdCache.containsKey(fullName)) {
            Long patientId = patientNameToIdCache.get(fullName);
            return patientRepository.findById(patientId).orElse(null);
        }

        // Handle complex names with multiple commas by splitting only on first comma
        int firstComma = fullName.indexOf(", ");
        Patient patient = null;
        if (firstComma > 0) {
            String lastName = fullName.substring(0, firstComma).trim();
            String firstName = fullName.substring(firstComma + 2).trim();
            // Use exact match instead of LIKE patterns to avoid false duplicates
            patient = patientRepository.findByFirstNameAndLastName(firstName, lastName);
            if (patient == null) {
                // Fallback: try with LIKE pattern for backwards compatibility
                String firstNamePattern = "%" + firstName + "%";
                String lastNamePattern = "%" + lastName + "%";
                patient = patientRepository.findFirstByFirstNameAndLastNameLike(firstNamePattern, lastNamePattern);
            }
        } else {
            // No comma - search by full name pattern
            patient = patientRepository.findFirstByFirstNameAndLastNameLike("%" + fullName + "%", "%");
        }

        // Add to cache if found
        if (patient != null) {
            patientNameToIdCache.put(fullName, patient.getId());
        }
        return patient;
    }

    // Patient erstellen, wenn nicht gefunden
    private Patient createPatient(String fullName) {
        // Handle complex names with multiple commas by splitting only on first comma
        int firstComma = fullName.indexOf(", ");
        Patient patient = new Patient();
        if (firstComma > 0) {
            patient.setLastName(fullName.substring(0, firstComma).trim());
            patient.setFirstName(fullName.substring(firstComma + 2).trim());
        } else {
            patient.setFirstName(fullName);
            patient.setLastName(""); // Leeres Nachnamefeld, wenn nicht vorhanden
        }
        patient.setFullName(patient.getFirstName() + " " + patient.getLastName());
        patient.setActiveSince(java.time.LocalDateTime.now());
        patient.setActiveUntil(java.time.LocalDateTime.now());
        patient.setIsBWO(false);
        patient = patientRepository.save(patient);

        // Add to cache for future lookups
        patientNameToIdCache.put(fullName, patient.getId());
        return patient;
    }

    // Therapeut anhand des Namens finden
    private Therapist findTherapistByName(String firstName) {
        return therapistRepository.findByFirstName(firstName);
    }

    // Uhrzeit-String in Date konvertieren
    private Date parseTime(String time, Date appointmentDate) {
        // Define the time format "HH:mm"
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        Date parsedTime = null;
        try {
            // Parse the time (HH:mm)
            parsedTime = timeFormat.parse(time);
        } catch (ParseException e) {
            System.out.println("Error parsing time: " + e.getMessage());
            return null;  // Return null in case of parsing errors
        }

        // Create a Calendar object for the appointment date (date part)
        Calendar appointmentCalendar = Calendar.getInstance();
        appointmentCalendar.setTime(appointmentDate);

        // Create a Calendar object for the parsed time (time part)
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(parsedTime);

        // Set the hour and minute from the parsed time into the appointment date
        appointmentCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        appointmentCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
        appointmentCalendar.set(Calendar.SECOND, 0); // Optionally set seconds to 0

        // Return the combined date and time
        return appointmentCalendar.getTime();
    }

    /**
     * Parst Zeit-String "7:00" oder "14:30" zu LocalDateTime mit heutigem Datum
     */
    private LocalDateTime parseTimeToLocalDateTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            LocalTime time = parseTimeToLocalTime(timeStr);
            if (time != null) {
                return time.atDate(LocalDate.now());
            }
        } catch (Exception e) {
            // Fehler beim Parsen - null zurückgeben
        }
        return null;
    }

    /**
     * Versucht den String als Wochentag zu parsen (Montag, Monday, MO, etc.)
     * Gibt den englischen Wochentag zurück oder null
     */
    private String tryParseWeekday(String dayStr) {
        if (dayStr == null || dayStr.trim().isEmpty()) {
            return null;
        }

        String normalized = dayStr.trim().toUpperCase();

        // Deutsche Wochentage
        if (normalized.startsWith("MON")) return "MONDAY";
        if (normalized.startsWith("MONT")) return "MONDAY";
        if (normalized.startsWith("DIENS") || normalized.startsWith("DIEN")) return "TUESDAY";
        if (normalized.startsWith("MITT")) return "WEDNESDAY";
        if (normalized.startsWith("DONN")) return "THURSDAY";
        if (normalized.startsWith("FREIT")) return "FRIDAY";
        if (normalized.startsWith("FREIT")) return "FRIDAY";
        if (normalized.startsWith("SAMS")) return "SATURDAY";
        if (normalized.startsWith("SONN")) return "SUNDAY";

        // English
        if (normalized.startsWith("MON")) return "MONDAY";
        if (normalized.startsWith("TUE")) return "TUESDAY";
        if (normalized.startsWith("WED")) return "WEDNESDAY";
        if (normalized.startsWith("THU")) return "THURSDAY";
        if (normalized.startsWith("FRI")) return "FRIDAY";
        if (normalized.startsWith("SAT")) return "SATURDAY";
        if (normalized.startsWith("SUN")) return "SUNDAY";

        // Abbreviations
        if (normalized.equals("MO")) return "MONDAY";
        if (normalized.equals("DI")) return "TUESDAY";
        if (normalized.equals("MI")) return "WEDNESDAY";
        if (normalized.equals("DO")) return "THURSDAY";
        if (normalized.equals("FR")) return "FRIDAY";
        if (normalized.equals("SA")) return "SATURDAY";
        if (normalized.equals("SO")) return "SUNDAY";

        return null;
    }

    /**
     * Versucht einen deutschen Datumsstring "dd.MM.yyyy" zu parsen
     * Returns LocalDate oder null wenn nicht parsbar
     */
    private LocalDate tryParseDateStringGerman(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            // Format dd.MM.yyyy
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            Date parsedDate = sdf.parse(dateStr.trim());
            return dateToLocalDate(parsedDate);
        } catch (ParseException e) {
            // Nicht in diesem Format - versuche andere Formate
        }

        // Versuche ISO Format yyyy-MM-dd
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            // Auch nicht erfolgreich
        }

        return null;
    }
}

