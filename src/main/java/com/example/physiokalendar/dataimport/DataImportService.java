package com.example.physiokalendar.dataimport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import com.example.physiokalendar.entity.Absence;
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
            if (rootNode.has("therapists") && rootNode.get("therapists").isArray()) {
                errorWriter.write("\n--- Importiere Therapeuten ---\n");
                Iterator<JsonNode> therapists = rootNode.get("therapists").elements();
                while (therapists.hasNext()) {
                    try {
                        JsonNode therapistNode = therapists.next();
                        importTherapist(therapistNode, errorWriter);
                    } catch (Exception thEx) {
                        errorWriter.write("[ERROR] Therapeut-Import fehlgeschlagen: " + thEx.getMessage() + "\n");
                        errorWriter.flush();
                    }
                }
            }

            // 2. Importiere Patienten aus der Daylist (um alle Patienten zu erfassen)
            if (rootNode.has("daylist") && rootNode.get("daylist").has("elements")) {
                errorWriter.write("\n--- Importiere Patienten ---\n");
                JsonNode elementsNode = rootNode.get("daylist").get("elements");
                if (elementsNode.isArray()) {
                    Iterator<JsonNode> days = elementsNode.elements();
                    while (days.hasNext()) {
                        try {
                            JsonNode day = days.next();
                            importPatientsFromDay(day, errorWriter);
                        } catch (Exception patEx) {
                            errorWriter.write("[ERROR] Patient-Import fehlgeschlagen: " + patEx.getMessage() + " - weiter mit nächstem\n");
                            errorWriter.flush();
                        }
                    }
                }
            }

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

            // 4. TEMP: Serientermine SKIPPEN für jetzt
            // if (rootNode.has("masterlist") && rootNode.get("masterlist").has("elements")) {
            //     errorWriter.write("\n--- Importiere Serientermine (Masterlist) ---\n");
            //     JsonNode masterListNode = rootNode.get("masterlist").get("elements");
            //     if (masterListNode.isArray()) {
            //         Iterator<JsonNode> seriesDays = masterListNode.elements();
            //         while (seriesDays.hasNext()) {
            //             JsonNode seriesDay = seriesDays.next();
            //             importSeriesAppointments(seriesDay, errorWriter);
            //         }
            //     }
            // }

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

        // Überprüfen, ob "appointments" vorhanden ist und ein Array ist
        if (seriesDay.has("appointments") && seriesDay.get("appointments").isArray()) {
            Iterator<JsonNode> appointments = seriesDay.get("appointments").elements();

            // Über die Termine iterieren
            while (appointments.hasNext()) {
                JsonNode appointmentNode = appointments.next();

                try {
                    // Patient ermitteln oder erstellen, wenn nicht gefunden
                    String patientName = appointmentNode.hasNonNull("patient") ? appointmentNode.get("patient").asText() : null;
                    Patient patient = findPatientByName(patientName);
                    if (patient == null && patientName != null) {
                        patient = createPatient(patientName);
                    }

                    // Therapeut ermitteln
                    String therapistName = appointmentNode.hasNonNull("therapist") ? appointmentNode.get("therapist").asText() : null;
                    Therapist therapist = (therapistName != null) ? findTherapistByName(therapistName) : null;

                    if (patient != null && therapist != null && appointmentNode.hasNonNull("startTime") && appointmentNode.hasNonNull("endTime")) {
                        // Serientermin speichern
                        AppointmentSeries appointment = new AppointmentSeries();
                        appointment.setPatient(patient);
                        appointment.setTherapist(therapist);
                        appointment.setStartTime(parseTimeToLocalTime(appointmentNode.get("startTime").asText()));
                        appointment.setEndTime(parseTimeToLocalTime(appointmentNode.get("endTime").asText()));

                        // Start- und Enddatum ermitteln
                        Date startDate = new Date(appointmentNode.get("startDate").asLong());
                        Date endDate = new Date(appointmentNode.get("endDate").asLong());

                        // Datum prüfen und ggf. Enddatum auf 01.01.2026 setzen
                        Date cutoffDate = new Date(1767225600000L); // 01.01.2026 in Millisekunden
                        if (endDate.after(cutoffDate)) {
                            endDate = cutoffDate;
                        }

                        appointment.setStartDate(dateToLocalDate(startDate));
                        appointment.setEndDate(dateToLocalDate(endDate));
                        appointment.setWeekday(weekday);
                        appointment.setWeeklyfrequency(appointmentNode.hasNonNull("interval") ? appointmentNode.get("interval").asInt() : 1);
                        appointment.setComment(appointmentNode.hasNonNull("comment") ? appointmentNode.get("comment").asText() : "");

                        // Erstelle die wiederkehrenden Termine anhand des Serien-Termins
                        AppointmentSeries savedAppointment = appointmentSeriesRepository.save(appointment);
                        seriesCount++;

                        // Standardmäßig kein Hotair, Ultrasonic oder Electric für Serientermine
                        appointmentSeriesService.createAppointmentsFromSeries(savedAppointment,
                            dateToLocalDate(startDate),
                            dateToLocalDate(endDate),
                            appointment.getWeeklyfrequency(),
                            false, false, false);

                        // Behandle Ausfälle (Cancellations)
                        if (appointmentNode.has("cancellations") && appointmentNode.get("cancellations").isArray()) {
                            for (JsonNode cancellationNode : appointmentNode.get("cancellations")) {
                                try {
                                    String cancellationDateStr = cancellationNode.hasNonNull("date") ? cancellationNode.get("date").asText() : null;
                                    LocalDate cancellationDate = parseStringToLocalDate(cancellationDateStr);
                                    if (cancellationDate != null) {
                                        Cancellation cancellation = new Cancellation();
                                        cancellation.setDate(cancellationDate);
                                        cancellation.setAppointmentSeries(savedAppointment);
                                        cancellationRepository.save(cancellation);
                                        cancellationCount++;
                                    }
                                } catch (Exception e) {
                                    errorWriter.write("FEHLER beim Importieren einer Cancellation: " + e.getMessage() + "\n");
                                }
                            }
                        }
                    } else {
                        // Fehler loggen mit mehr Details
                        String appointmentId = appointmentNode.hasNonNull("id") ? appointmentNode.get("id").asText() : "Unbekannt";
                        errorWriter.write("FEHLER bei Serientermin-ID: " + appointmentId + " - Patient: " + patientName + ", Therapeut: " + therapistName + ", Wochentag: " + weekday + "\n");
                    }
                } catch (Exception e) {
                    errorWriter.write("FEHLER beim Importieren eines Serientiermins: " + e.getMessage() + "\n");
                    // Fortsetzung mit nächstem Serientermin
                }
            }
        }
    }

    private void importAbsences(JsonNode therapistNode, BufferedWriter errorWriter) throws IOException {
        // Therapeut ermitteln
        String therapistName = therapistNode.hasNonNull("name") ? therapistNode.get("name").asText() : null;
        Therapist therapist = (therapistName != null) ? findTherapistByName(therapistName) : null;

        if (therapist != null && therapistNode.has("absences") && therapistNode.get("absences").isArray()) {
            Iterator<JsonNode> absences = therapistNode.get("absences").elements();

            // Über die Abwesenheiten iterieren
            while (absences.hasNext()) {
                JsonNode absenceNode = absences.next();

                try {
                    Absence absence = new Absence();
                    absence.setTherapist(therapist);

                    // Unterscheide zwischen Datum und Wochentag
                    String day = absenceNode.hasNonNull("day") ? absenceNode.get("day").asText() : null;
                    if (isDate(day)) {
                        absence.setDate(parseStringToLocalDate(day));
                    } else if (day != null) {
                        absence.setWeekday(day);
                    }

                    // Zeiten setzen wenn vorhanden
                    if (absenceNode.hasNonNull("start")) {
                        absence.setStartTime(dateToLocalDateTime(parseTime(absenceNode.get("start").asText(), new Date())));
                    }
                    if (absenceNode.hasNonNull("end")) {
                        absence.setEndTime(dateToLocalDateTime(parseTime(absenceNode.get("end").asText(), new Date())));
                    }

                    // Grund setzen wenn vorhanden
                    if (absenceNode.hasNonNull("reason")) {
                        absence.setReason(absenceNode.get("reason").asText());
                    }

                    absenceRepository.save(absence);
                    absenceCount++;
                } catch (Exception e) {
                    errorWriter.write("FEHLER beim Importieren einer Abwesenheit für Therapeut '" + therapistName + "': " + e.getMessage() + "\n");
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
}
