package com.example.physiokalendar.dataimport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

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

    public void importData(String filePath) {
        // Fehlerprotokolldatei erstellen
        try (BufferedWriter errorWriter = new BufferedWriter(new FileWriter("src/main/java/com/example/physiokalendar/dataimport/error_log.txt", true))) {
            // JSON Datei einlesen
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(filePath));

            // Sicherstellen, dass "daylist" existiert und "elements" ein Array ist
            if (rootNode.has("daylist") && rootNode.get("daylist").has("elements")) {
                JsonNode elementsNode = rootNode.get("daylist").get("elements");

                if (elementsNode.isArray()) {
                    // Über die Elemente iterieren
                    Iterator<JsonNode> days = elementsNode.elements();
                    while (days.hasNext()) {
                        JsonNode day = days.next();
                        // Importiere die Termine
                        importAppointments(day, errorWriter);
                    }
                }
            }

            // Importiere die Serientermine aus der Masterlist
            // if (rootNode.has("masterlist") && rootNode.get("masterlist").has("elements")) {
            //     JsonNode masterListNode = rootNode.get("masterlist").get("elements");

            //     if (masterListNode.isArray()) {
            //         Iterator<JsonNode> seriesDays = masterListNode.elements();
            //         while (seriesDays.hasNext()) {
            //             JsonNode seriesDay = seriesDays.next();
            //             importSeriesAppointments(seriesDay, errorWriter);
            //         }
            //     }
            // }

            // Importiere die Abwesenheiten der Therapeuten
            // if (rootNode.has("therapists") && rootNode.get("therapists").isArray()) {
            //     Iterator<JsonNode> therapists = rootNode.get("therapists").elements();
            //     while (therapists.hasNext()) {
            //         JsonNode therapistNode = therapists.next();
            //         importAbsences(therapistNode, errorWriter);
            //     }
            // }
        } catch (IOException e) {
            // Logge den Fehler
            System.out.println("Fehler beim Importieren der Daten: " + e.getMessage());
        }
    }

    private void importAppointments(JsonNode day, BufferedWriter errorWriter) throws IOException {
        // Überprüfen, ob der "date"-Wert existiert und nicht null ist
        if (day.hasNonNull("date")) {
            long date = day.get("date").asLong();

            // Überprüfen, ob "appointments" vorhanden ist und ein Array ist
            if (day.has("appointments") && day.get("appointments").isArray()) {
                Iterator<JsonNode> appointments = day.get("appointments").elements();

                // Über die Termine iterieren
                while (appointments.hasNext()) {
                    JsonNode appointmentNode = appointments.next();

                    // Patient ermitteln oder erstellen, wenn nicht gefunden
                    String patientName = appointmentNode.hasNonNull("patient") ? appointmentNode.get("patient").asText() : null;
                    Patient patient = findPatientByName(patientName);
                    if (patient == null && patientName != null) {
                        patient = createPatient(patientName);
                    }

                    // Therapeut ermitteln
                    String therapistName = appointmentNode.hasNonNull("therapist") ? appointmentNode.get("therapist").asText() : null;
                    Therapist therapist = (therapistName != null) ? findTherapistByName(therapistName) : null;

                    if (patient != null && therapist != null) {
                        // Termin speichern
                        Appointment appointment = new Appointment();
                        appointment.setPatient(patient);
                        appointment.setTherapist(therapist);
                        appointment.setDate(new Date(date));
                        appointment.setCreatedBySeriesAppointment(false);
                        appointment.setStartTime(parseTime(appointmentNode.get("startTime").asText(), new Date(date)));
                        appointment.setEndTime(parseTime(appointmentNode.get("endTime").asText(), new Date(date)));
                        appointment.setComment(appointmentNode.hasNonNull("comment") ? appointmentNode.get("comment").asText() : "");
                        appointment.setIsHotair(appointmentNode.hasNonNull("isHotair") ? appointmentNode.get("isHotair").asBoolean() : false);
                        appointment.setIsUltrasonic(appointmentNode.hasNonNull("isUltrasonic") ? appointmentNode.get("isUltrasonic").asBoolean() : false);
                        appointment.setIsElectric(appointmentNode.hasNonNull("isElectric") ? appointmentNode.get("isElectric").asBoolean() : false);

                        appointmentRepository.save(appointment);
                    } else {
                        // Fehler loggen mit mehr Details
                        String appointmentId = appointmentNode.hasNonNull("id") ? appointmentNode.get("id").asText() : "Unbekannt";
                        String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date(date));
                        errorWriter.write("Fehler bei Termin-ID: " + appointmentId + " - Patient: " + patientName + ", Therapeut: " + therapistName + ", Datum: " + formattedDate);
                        errorWriter.newLine();
                    }
                }
            }
        }
    }

    private void importSeriesAppointments(JsonNode seriesDay, BufferedWriter errorWriter) throws IOException {
        String weekday = seriesDay.hasNonNull("weekday") ? seriesDay.get("weekday").asText() : null;
    
        // Überprüfen, ob "appointments" vorhanden ist und ein Array ist
        if (seriesDay.has("appointments") && seriesDay.get("appointments").isArray()) {
            Iterator<JsonNode> appointments = seriesDay.get("appointments").elements();
    
            // Über die Termine iterieren
            while (appointments.hasNext()) {
                JsonNode appointmentNode = appointments.next();
    
                // Patient ermitteln oder erstellen, wenn nicht gefunden
                String patientName = appointmentNode.hasNonNull("patient") ? appointmentNode.get("patient").asText() : null;
                Patient patient = findPatientByName(patientName);
                if (patient == null && patientName != null) {
                    patient = createPatient(patientName);
                }
    
                // Therapeut ermitteln
                String therapistName = appointmentNode.hasNonNull("therapist") ? appointmentNode.get("therapist").asText() : null;
                Therapist therapist = (therapistName != null) ? findTherapistByName(therapistName) : null;
    
                if (patient != null && therapist != null) {
                    // Serientermin speichern
                    AppointmentSeries appointment;
                    appointment = new AppointmentSeries();
                    appointment.setPatient(patient);
                    appointment.setTherapist(therapist);
                    appointment.setStartTime(parseTime(appointmentNode.get("startTime").asText(), new Date()));
                    appointment.setEndTime(parseTime(appointmentNode.get("endTime").asText(), new Date()));
    
                    // Start- und Enddatum ermitteln
                    Date startDate = new Date(appointmentNode.get("startDate").asLong());
                    Date endDate = new Date(appointmentNode.get("endDate").asLong());
    
                    // Datum prüfen und ggf. Enddatum auf 01.01.2026 setzen
                    Date cutoffDate = new Date(1767225600000L); // 01.01.2026 in Millisekunden
                    if (endDate.after(cutoffDate)) {
                        endDate = cutoffDate;
                    }
                    
                    appointment.setStartDate(startDate);
                    appointment.setEndDate(endDate);
                    appointment.setWeekday(weekday);
                    appointment.setWeeklyfrequency(appointmentNode.hasNonNull("interval") ? appointmentNode.get("interval").asInt() : 1);
                    appointment.setComment(appointmentNode.hasNonNull("comment") ? appointmentNode.get("comment").asText() : "");
    
                    // Erstelle die wiederkehrenden Termine anhand des Serien-Termins
                    
    
                    AppointmentSeries savedAppointment = appointmentSeriesRepository.save(appointment);

                    appointmentSeriesService.createAppointmentsFromSeries(savedAppointment, startDate, endDate, appointment.getWeeklyfrequency());
    
                    // Behandle Ausfälle (Cancellations)
                    if (appointmentNode.has("cancellations") && appointmentNode.get("cancellations").isArray()) {
                        for (JsonNode cancellationNode : appointmentNode.get("cancellations")) {
                            String cancellationDateStr = cancellationNode.hasNonNull("date") ? cancellationNode.get("date").asText() : null;
                            Date cancellationDate = parseDate(cancellationDateStr);
                            if (cancellationDate != null) {
                                Cancellation cancellation = new Cancellation();
                                cancellation.setDate(cancellationDate);
                                cancellation.setAppointmentSeries(savedAppointment);
                                cancellationRepository.save(cancellation);
                            }
                        }
                    }
                } else {
                    // Fehler loggen mit mehr Details
                    String appointmentId = appointmentNode.hasNonNull("id") ? appointmentNode.get("id").asText() : "Unbekannt";
                    errorWriter.write("Fehler bei Serientermin-ID: " + appointmentId + " - Patient: " + patientName + ", Therapeut: " + therapistName + ", Wochentag: " + weekday);
                    errorWriter.newLine();
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
                Absence absence = new Absence();
                absence.setTherapist(therapist);

                // Unterscheide zwischen Datum und Wochentag
                String day = absenceNode.hasNonNull("day") ? absenceNode.get("day").asText() : null;
                if (isDate(day)) {
                    absence.setDate(parseDate(day));
                } else {
                    absence.setWeekday(day);
                }

                absence.setStartTime(parseTime(absenceNode.get("start").asText(), new Date()));
                absence.setEndTime(parseTime(absenceNode.get("end").asText(), new Date()));

                absenceRepository.save(absence);
            }
        } else {
            errorWriter.write("Fehler bei Abwesenheiten von Therapeut: " + therapistName);
            errorWriter.newLine();
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

    // Patient anhand des Namens finden
    private Patient findPatientByName(String fullName) {
        String[] parts = fullName.split(", ");
        if (parts.length == 2) {
            String lastName = parts[0];
            String firstName = parts[1];
            String firstNamePattern = "%" + firstName + "%";
            String lastNamePattern = "%" + lastName + "%";
            return patientRepository.findFirstByFirstNameAndLastNameLike(firstNamePattern, lastNamePattern);
        } else if (parts.length == 1) {
            return patientRepository.findFirstByFirstNameAndLastNameLike("%" + fullName + "%", "%");
        }
        return null;
    }

    // Patient erstellen, wenn nicht gefunden
    private Patient createPatient(String fullName) {
        String[] parts = fullName.split(", ");
        Patient patient = new Patient();
        if (parts.length == 2) {
            patient.setFirstName(parts[1]);
            patient.setLastName(parts[0]);
        } else {
            patient.setFirstName(fullName);
            patient.setLastName(""); // Leeres Nachnamefeld, wenn nicht vorhanden
        }
        patient.setFullName(patient.getFirstName() + " " + patient.getLastName());
        patient.setActiveSince(new Date());
        patient.setActiveUntil(new Date());
        patient.setIsBWO(false);
        return patientRepository.save(patient);
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
