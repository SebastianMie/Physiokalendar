package com.example.physiokalendar.service;

import com.example.physiokalendar.entity.*;
import com.example.physiokalendar.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service for seeding test/demo data.
 * Only runs when app.seed-data-enabled=true and environment is not production.
 */
@Service
@Slf4j
public class SeedDataService {

    @Value("${app.seed-data-enabled:false}")
    private boolean seedDataEnabled;

    @Value("${app.environment:dev}")
    private String environment;

    private final TherapistRepository therapistRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeriesRepository seriesRepository;
    private final AbsenceRepository absenceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataService(
            TherapistRepository therapistRepository,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            AppointmentSeriesRepository seriesRepository,
            AbsenceRepository absenceRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.therapistRepository = therapistRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.seriesRepository = seriesRepository;
        this.absenceRepository = absenceRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedData() {
        // Safety checks
        if ("prod".equalsIgnoreCase(environment)) {
            log.info("Skipping seed data - production environment detected");
            return;
        }

        if (!seedDataEnabled) {
            log.info("Skipping seed data - seed-data-enabled is false");
            return;
        }

        // Check if data already exists
        if (therapistRepository.count() > 2 || patientRepository.count() > 0) {
            log.info("Skipping seed data - data already exists");
            return;
        }

        log.info("Starting seed data generation for {} environment...", environment);

        try {
            seedTherapists();
            seedPatients();
            seedAppointments();
            seedAppointmentSeries();
            seedAbsences();

            log.info("Seed data generation completed successfully");
        } catch (Exception e) {
            log.error("Error generating seed data: {}", e.getMessage(), e);
        }
    }

    private void seedTherapists() {
        log.info("Seeding therapists...");

        String[][] therapistData = {
            {"Max", "Mustermann"},
            {"Anna", "Schmidt"},
            {"Thomas", "Müller"},
            {"Lisa", "Weber"},
            {"Michael", "Fischer"}
        };

        for (String[] data : therapistData) {
            if (therapistRepository.findByEmail(data[0].toLowerCase() + "." + data[1].toLowerCase() + "@physio.de").isEmpty()) {
                Therapist therapist = new Therapist();
                therapist.setFirstName(data[0]);
                therapist.setLastName(data[1]);
                therapist.setFullName(data[0] + " " + data[1]);
                therapist.setEmail(data[0].toLowerCase() + "." + data[1].toLowerCase() + "@physio.de");
                therapist.setTelefon("0123-" + (4567890 + new Random().nextInt(1000)));
                therapist.setIsActive(true);
                therapist.setActiveSince(LocalDateTime.now().minusYears(1));
                therapistRepository.save(therapist);
            }
        }
    }

    private void seedPatients() {
        log.info("Seeding patients...");

        String[][] patientData = {
            {"Hans", "Meier", "Hauptstraße", "12", "12345", "Berlin"},
            {"Petra", "Schulz", "Nebenweg", "3", "23456", "Hamburg"},
            {"Klaus", "Becker", "Am Park", "7", "34567", "München"},
            {"Sabine", "Wagner", "Waldstraße", "22", "45678", "Köln"},
            {"Jürgen", "Hoffmann", "Lindenallee", "5", "56789", "Frankfurt"},
            {"Monika", "Koch", "Gartenweg", "15", "67890", "Stuttgart"},
            {"Wolfgang", "Richter", "Bergstraße", "8", "78901", "Düsseldorf"},
            {"Ursula", "Klein", "Seeweg", "30", "89012", "Leipzig"},
            {"Dieter", "Wolf", "Sonnenplatz", "1", "90123", "Dresden"},
            {"Renate", "Schröder", "Mondstraße", "18", "01234", "Hannover"}
        };

        for (String[] data : patientData) {
            if (patientRepository.findByEmail(data[0].toLowerCase() + "." + data[1].toLowerCase() + "@patient.de").isEmpty()) {
                Patient patient = new Patient();
                patient.setFirstName(data[0]);
                patient.setLastName(data[1]);
                patient.setFullName(data[0] + " " + data[1]);
                patient.setEmail(data[0].toLowerCase() + "." + data[1].toLowerCase() + "@patient.de");
                patient.setTelefon("0152-" + (1234567 + new Random().nextInt(10000)));
                patient.setStreet(data[2]);
                patient.setHouseNumber(data[3]);
                patient.setPostalCode(data[4]);
                patient.setCity(data[5]);
                patient.setIsBWO(new Random().nextBoolean());
                patient.setActiveSince(LocalDateTime.now().minusMonths(new Random().nextInt(24)));
                patientRepository.save(patient);
            }
        }
    }

    private void seedAppointments() {
        log.info("Seeding appointments...");

        List<Therapist> therapists = therapistRepository.findByIsActiveTrue();
        List<Patient> patients = patientRepository.findAll();

        if (therapists.isEmpty() || patients.isEmpty()) {
            log.warn("Cannot seed appointments - no therapists or patients");
            return;
        }

        Random random = new Random(42); // Fixed seed for reproducibility
        LocalDate today = LocalDate.now();

        // Create appointments for the next 14 days
        for (int day = 0; day < 14; day++) {
            LocalDate date = today.plusDays(day);

            // Skip weekends
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }

            for (Therapist therapist : therapists) {
                // 3-5 appointments per therapist per day
                int appointmentCount = 3 + random.nextInt(3);
                List<LocalTime> usedTimes = new ArrayList<>();

                for (int i = 0; i < appointmentCount; i++) {
                    LocalTime startTime;
                    boolean timeConflict;
                    do {
                        int hour = 8 + random.nextInt(10); // 8:00 - 17:00
                        int minute = random.nextInt(4) * 15; // 0, 15, 30, 45
                        startTime = LocalTime.of(hour, minute);
                        final LocalTime checkTime = startTime;
                        timeConflict = usedTimes.stream().anyMatch(t ->
                            Math.abs(t.toSecondOfDay() - checkTime.toSecondOfDay()) < 1800); // 30 min apart
                    } while (timeConflict);

                    usedTimes.add(startTime);

                    int duration = (30 + random.nextInt(3) * 15); // 30, 45, or 60 minutes
                    LocalTime endTime = startTime.plusMinutes(duration);

                    Patient patient = patients.get(random.nextInt(patients.size()));

                    Appointment appointment = new Appointment();
                    appointment.setTherapist(therapist);
                    appointment.setPatient(patient);
                    appointment.setDate(date);
                    appointment.setStartTime(LocalDateTime.of(date, startTime));
                    appointment.setEndTime(LocalDateTime.of(date, endTime));
                    appointment.setStatus(AppointmentStatus.SCHEDULED);
                    appointment.setIsHotair(random.nextDouble() < 0.3);
                    appointment.setIsUltrasonic(random.nextDouble() < 0.2);
                    appointment.setIsElectric(random.nextDouble() < 0.15);
                    appointment.setCreatedBySeriesAppointment(false);

                    appointmentRepository.save(appointment);
                }
            }
        }
    }

    private void seedAppointmentSeries() {
        log.info("Seeding appointment series...");

        List<Therapist> therapists = therapistRepository.findByIsActiveTrue();
        List<Patient> patients = patientRepository.findAll();

        if (therapists.size() < 2 || patients.size() < 4) {
            log.warn("Not enough therapists/patients for series seeding");
            return;
        }

        Random random = new Random(123);
        LocalDate startDate = LocalDate.now().plusDays(7);
        LocalDate endDate = startDate.plusMonths(3);

        String[] weekdays = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};

        for (int i = 0; i < 5; i++) {
            Therapist therapist = therapists.get(i % therapists.size());
            Patient patient = patients.get(i % patients.size());

            int hour = 9 + random.nextInt(8);
            int minute = random.nextInt(4) * 15;

            AppointmentSeries series = new AppointmentSeries();
            series.setTherapist(therapist);
            series.setPatient(patient);
            series.setStartDate(startDate.plusDays(i));
            series.setEndDate(endDate);
            series.setWeekday(weekdays[i % 5]);
            series.setWeeklyfrequency(i % 2 == 0 ? 1 : 2); // Weekly or bi-weekly
            series.setStartTime(LocalTime.of(hour, minute));
            series.setEndTime(LocalTime.of(hour, minute).plusMinutes(45));
            series.setStatus(SeriesStatus.ACTIVE);
            series.setComment("Regelmäßige Behandlung");

            seriesRepository.save(series);
        }
    }

    private void seedAbsences() {
        log.info("Seeding absences...");

        List<Therapist> therapists = therapistRepository.findByIsActiveTrue();
        if (therapists.isEmpty()) return;

        // Add some special absences
        for (int i = 0; i < 3; i++) {
            Therapist therapist = therapists.get(i % therapists.size());
            LocalDate date = LocalDate.now().plusDays(5 + i * 3);

            Absence absence = new Absence();
            absence.setTherapist(therapist);
            absence.setDate(date);
            absence.setAbsenceType(AbsenceType.SPECIAL);
            absence.setStartTime(LocalDateTime.of(date, LocalTime.of(8, 0)));
            absence.setEndTime(LocalDateTime.of(date, LocalTime.of(18, 0)));
            absence.setReason("Fortbildung");

            absenceRepository.save(absence);
        }

        // Add some recurring absences (lunch breaks, etc.)
        for (Therapist therapist : therapists) {
            Absence lunchBreak = new Absence();
            lunchBreak.setTherapist(therapist);
            lunchBreak.setAbsenceType(AbsenceType.RECURRING);
            lunchBreak.setWeekday("MONDAY");
            lunchBreak.setStartTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 0)));
            lunchBreak.setEndTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(13, 0)));
            lunchBreak.setReason("Mittagspause");

            absenceRepository.save(lunchBreak);
        }
    }
}
