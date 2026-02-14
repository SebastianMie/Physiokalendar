package com.example.physiokalendar.service;

import com.example.physiokalendar.dto.JSONAppointmentDTO;
import com.example.physiokalendar.entity.Absence;
import com.example.physiokalendar.entity.Appointment;
import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.AppointmentRepository;
import com.example.physiokalendar.repository.TherapistRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.physiokalendar.repository.PatientRepository;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private TherapistService therapistService;

    @Autowired
    private AbsenceService absenceService;

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    public List<Appointment> getAppointmentsForDate(Date date) {
        return appointmentRepository.findByDate(date);
    }

    public List<Appointment> getAppointmentsByCriteria(Long therapistId, Long patientId, Date date) {
        if (therapistId != null && patientId != null && date != null) {
            return appointmentRepository.findByTherapistIdAndPatientIdAndDate(therapistId, patientId, date);
        } else if (therapistId != null && date != null) {
            return appointmentRepository.findByTherapistIdAndDate(therapistId, date);
        } else if (patientId != null && date != null) {
            return appointmentRepository.findByPatientIdAndDate(patientId, date);
        } else if (date != null) {
            return appointmentRepository.findByDate(date);
        } else if (therapistId != null && patientId != null) {
            return appointmentRepository.findByTherapistIdAndPatientId(therapistId, patientId);
        } else if (therapistId != null) {
            return appointmentRepository.findByTherapistId(therapistId);
        } else if (patientId != null) {
            return appointmentRepository.findByPatientId(patientId);
        } else {
            return appointmentRepository.findAll(); // No filters applied, return all appointments
        }
    }



    public Appointment saveAppointment(JSONAppointmentDTO appointmentDTO) {
        // Mapping DTO to Entity
        Long therapistId = appointmentDTO.getTherapist().getId();
        Long patientId = appointmentDTO.getPatient().getId();

        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID"));

        Appointment appointment = new Appointment();
        appointment.setId(appointmentDTO.getId());
        appointment.setTherapist(therapist);
        appointment.setPatient(patient);
        appointment.setDate(appointmentDTO.getDate() != null ? dateToLocalDate(appointmentDTO.getDate()) : null);
        appointment.setStartTime(appointmentDTO.getStartTime() != null ? dateToLocalDateTime(appointmentDTO.getStartTime()) : null);
        appointment.setEndTime(appointmentDTO.getEndTime() != null ? dateToLocalDateTime(appointmentDTO.getEndTime()) : null);
        appointment.setComment(appointmentDTO.getComment());
        appointment.setCreatedBySeriesAppointment(appointmentDTO.getCreatedBySeriesAppointment());
        appointment.setIsElectric(appointmentDTO.getIsElectric());
        appointment.setIsHotair(appointmentDTO.getIsHotair());
        appointment.setIsUltrasonic(appointmentDTO.getIsUltrasonic());

        // Speichern und zurückgeben
        return appointmentRepository.save(appointment);
    }


    public List<Appointment> findAvailableAppointments(Long therapistId, Long patientId, int timeOfDayId, Integer duration) {
        List<Appointment> availableAppointments = new ArrayList<>();
        List<Absence> absences = absenceService.getAbsencesByTherapistId(therapistId);

        // Datum von heute als Date-Objekt
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);

        LocalTime startTime = TimeOfDayService.getStartTime(timeOfDayId);
        LocalTime endTime = TimeOfDayService.getEndTime(timeOfDayId);

        while (startTime.plusMinutes(duration).isBefore(endTime)) {
            calendar.set(Calendar.HOUR_OF_DAY, startTime.getHour());
            calendar.set(Calendar.MINUTE, startTime.getMinute());
            Date startDateTime = calendar.getTime();

            calendar.add(Calendar.MINUTE, duration);
            Date endDateTime = calendar.getTime();

            if (isSlotAvailable(therapistId, startDateTime, endDateTime) && !isTherapistAbsent(absences, startDateTime, endDateTime)) {
                Appointment potentialAppointment = new Appointment();
                potentialAppointment.setTherapist(therapistRepository.findById(therapistId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID")));

                potentialAppointment.setPatient(patientRepository.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID")));
                potentialAppointment.setStartTime(dateToLocalDateTime(startDateTime));
                potentialAppointment.setEndTime(dateToLocalDateTime(endDateTime));
                potentialAppointment.setIsElectric(false);
                potentialAppointment.setIsHotair(false);
                potentialAppointment.setIsUltrasonic(false);
                potentialAppointment.setDate(dateToLocalDate(today));

                availableAppointments.add(potentialAppointment);
            }

            startTime = startTime.plusMinutes(duration);
        }

        return availableAppointments;
    }

    private boolean isTherapistAbsent(List<Absence> absences, Date startDateTime, Date endDateTime) {
        for (Absence absence : absences) {
            if (absence.getDate() != null) {
                LocalDateTime absenceStart = absence.getStartTime();
                LocalDateTime absenceEnd = absence.getEndTime();
                LocalDateTime checkStart = dateToLocalDateTime(startDateTime);
                LocalDateTime checkEnd = dateToLocalDateTime(endDateTime);
                if (!checkStart.isAfter(absenceEnd) && !checkEnd.isBefore(absenceStart)) {
                    return true; // Überlappung gefunden
                }
            } else if (absence.getWeekday() != null && !absence.getWeekday().isEmpty() && matchesWeeklyAbsence(absence, startDateTime, endDateTime)) {
                return true; // Überlappung mit wöchentlicher Abwesenheit gefunden
            }
        }
        return false;
    }

    private boolean matchesWeeklyAbsence(Absence absence, Date start, Date end) {
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(start);
        int startDayOfWeek = calStart.get(Calendar.DAY_OF_WEEK);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(end);
        int endDayOfWeek = calEnd.get(Calendar.DAY_OF_WEEK);

        int absenceDayOfWeek = convertWeekdayStringToIndex(absence.getWeekday());
        return absenceDayOfWeek == startDayOfWeek || absenceDayOfWeek == endDayOfWeek;
    }

    private int convertWeekdayStringToIndex(String weekday) {
        switch (weekday) {
            case "Sonntag" -> {
                return Calendar.SUNDAY;
            }
            case "Montag" -> {
                return Calendar.MONDAY;
            }
            case "Dienstag" -> {
                return Calendar.TUESDAY;
            }
            case "Mittwoch" -> {
                return Calendar.WEDNESDAY;
            }
            case "Donnerstag" -> {
                return Calendar.THURSDAY;
            }
            case "Freitag" -> {
                return Calendar.FRIDAY;
            }
            case "Samstag" -> {
                return Calendar.SATURDAY;
            }
            default -> throw new IllegalArgumentException("Unbekannter Wochentag: " + weekday); // Fehler werfen bei ungültigem Wochentag
        }
    }

    private boolean isSlotAvailable(Long therapistId, Date startDateTime, Date endDateTime) {
        // Prüfen, ob der Slot Überschneidungen mit bestehenden Terminen hat
        LocalDate date = dateToLocalDate(startDateTime);
        LocalDateTime checkStart = dateToLocalDateTime(startDateTime);
        LocalDateTime checkEnd = dateToLocalDateTime(endDateTime);
        return appointmentRepository.findAll().stream()
            .filter(a -> a.getDate().equals(date) && a.getTherapist().getId().equals(therapistId))
            .noneMatch(appointment -> checkStart.isBefore(appointment.getEndTime()) &&
                       checkEnd.isAfter(appointment.getStartTime()));
    }

    public boolean checkForConflicts(Appointment newAppointment) {
        // Holen Sie alle Termine
        List<Appointment> allAppointments = appointmentRepository.findAll();

        List<Appointment> todayAppointments = allAppointments.stream()
                .filter(appointment -> appointment.getDate().equals(newAppointment.getDate()))
                .collect(Collectors.toList());

        // Filtern Sie nach Therapeut
        List<Appointment> therapistAppointments = todayAppointments.stream()
                .filter(appointment -> appointment.getTherapist().getId().equals(newAppointment.getTherapist().getId()))
                .collect(Collectors.toList());

        // Überprüfen Sie auf Überlappungen
        for (Appointment existingAppointment : therapistAppointments) {
            if (isOverlapping(existingAppointment, newAppointment)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkForConflicts(Appointment app1, Appointment app2) {
        // Überprüfe ob zwei Appointments Überlappungen erzeugten
        if (!app1.getDate().equals(app2.getDate()) ||
            !app1.getTherapist().getId().equals(app2.getTherapist().getId())) {
            return false;
        }
        return isOverlapping(app1, app2);
    }

    private boolean isOverlapping(Appointment existingAppointment, Appointment newAppointment) {
        return existingAppointment.getStartTime().isBefore(newAppointment.getEndTime()) &&
               existingAppointment.getEndTime().isAfter(newAppointment.getStartTime());
    }

    public Appointment convertDTOToEntity(JSONAppointmentDTO dto) {
        Appointment appointment = new Appointment();
        appointment.setId(dto.getId());
        appointment.setDate(dto.getDate() != null ? dateToLocalDate(dto.getDate()) : null);
        appointment.setComment(dto.getComment());
        appointment.setCreatedBySeriesAppointment(dto.getCreatedBySeriesAppointment());
        appointment.setStartTime(dto.getStartTime() != null ? dateToLocalDateTime(dto.getStartTime()) : null);
        appointment.setEndTime(dto.getEndTime() != null ? dateToLocalDateTime(dto.getEndTime()) : null);
        appointment.setIsElectric(dto.getIsElectric());
        appointment.setIsHotair(dto.getIsHotair());
        appointment.setIsUltrasonic(dto.getIsUltrasonic());
        appointment.setPatient(patientService.convertDTOToEntity(dto.getPatient()));
        appointment.setTherapist(therapistService.convertDTOToEntity(dto.getTherapist()));
        // Weitere Felder falls nötig
        return appointment;
    }

    private LocalDate dateToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public List<Appointment> getAppointmentsWithConflicts() {
        List<Appointment> allAppointments = getAllAppointments();
        List<Appointment> conflictingAppointments = new ArrayList<>();

        for (int i = 0; i < allAppointments.size(); i++) {
            Appointment app1 = allAppointments.get(i);
            for (int j = i + 1; j < allAppointments.size(); j++) {
                Appointment app2 = allAppointments.get(j);
                if (checkForConflicts(app1, app2) && !conflictingAppointments.contains(app1)) {
                    conflictingAppointments.add(app1);
                }
                if (checkForConflicts(app1, app2) && !conflictingAppointments.contains(app2)) {
                    conflictingAppointments.add(app2);
                }
            }
        }
        return conflictingAppointments;
    }

    public void deleteAppointment(Long id) {
        appointmentRepository.deleteById(id);
    }
}
