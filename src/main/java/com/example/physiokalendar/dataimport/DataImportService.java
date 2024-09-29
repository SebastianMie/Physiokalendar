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

import com.example.physiokalendar.entity.Appointment;
import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.AppointmentRepository;
import com.example.physiokalendar.repository.PatientRepository;
import com.example.physiokalendar.repository.TherapistRepository;
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
                            } else {
                                // Logge, wenn "appointments" fehlt oder nicht korrekt ist
                                System.out.println("Fehlender oder ungültiger 'appointments'-Knoten für das Datum: " + new Date(date));
                            }
                        } else {
                            // Logge, wenn der "date"-Wert fehlt oder null ist
                            System.out.println("Fehlender oder null 'date'-Wert in einem Eintrag.");
                        }
                    }
                } else {
                    // Logge, wenn "elements" kein Array ist
                    System.out.println("'elements' ist kein Array.");
                }
            } else {
                // Logge, dass "daylist" nicht existiert oder "elements" fehlt
                System.out.println("'daylist' oder 'elements' fehlt im JSON.");
            }
        } catch (IOException e) {
            // Logge den Fehler
            System.out.println("Fehler beim Importieren der Daten: " + e.getMessage());
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
            parsedTime = timeFormat.parse(time);
        } catch (ParseException e) {
            // Handle the exception here, e.g. by logging or throwing a custom exception
            System.out.println("Error parsing time: " + e.getMessage());
        }
        // Create a Calendar object for the appointment date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(appointmentDate);
        // Create a Calendar object for the parsed time
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(parsedTime);
        // Set the hour and minute from the parsed time into the appointment date
        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, 0); // Optional: Set seconds to 0
        return calendar.getTime(); // Return the combined date and time
    }
}
