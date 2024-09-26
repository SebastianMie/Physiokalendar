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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.ZoneId;
import java.util.Date;

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
        appointment.setDate(appointmentDTO.getDate());
        appointment.setStartTime(appointmentDTO.getStartTime());
        appointment.setEndTime(appointmentDTO.getEndTime());
        appointment.setComment(appointmentDTO.getComment());
        appointment.setCreatedBySeriesAppointment(appointmentDTO.getCreatedBySeriesAppointment());
        appointment.setIsHotair(appointmentDTO.getIsHotair());
        appointment.setIsUltrasonic(appointmentDTO.getIsUltrasonic());
        appointment.setIsElectric(appointmentDTO.getIsElectric());

        return appointmentRepository.save(appointment);
    }

    public void deleteAppointment(Long id) {
        appointmentRepository.deleteById(id);
    }

    public List<Appointment> getAppointmentsWithConflicts() {
        List<Appointment> allAppointments = this.getAllAppointments();
        List<Appointment> conflictingAppointments = new ArrayList<>();
        Set<Long> addedAppointments = new HashSet<>();
    
        for (Appointment currentAppointment : allAppointments) {
            if (addedAppointments.contains(currentAppointment.getId())) {
                // Überspringe dieses Appointment, wenn es bereits als konfliktbehaftet markiert wurde
                continue;
            }
            for (Appointment compareAppointment : allAppointments) {
                if (!currentAppointment.getId().equals(compareAppointment.getId()) &&
                    this.checkForConflicts(currentAppointment, compareAppointment)) {
    
                    // Füge nur das erste gefundene konfliktbehaftete Appointment hinzu
                    conflictingAppointments.add(currentAppointment);
                    // Markiere beide Termine als bearbeitet
                    addedAppointments.add(currentAppointment.getId());
                    addedAppointments.add(compareAppointment.getId());
                    break;
                }
            }
        }
        return conflictingAppointments;
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
        //ZoneId systemTimeZone = ZoneId.systemDefault(); // System-Zeitzone

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
                potentialAppointment.setStartTime(startDateTime);
                potentialAppointment.setEndTime(endDateTime);
                potentialAppointment.setIsElectric(false);
                potentialAppointment.setIsHotair(false);
                potentialAppointment.setIsUltrasonic(false);
                potentialAppointment.setDate(today); // Das Datum ohne Zeitkomponente

                availableAppointments.add(potentialAppointment);
            }

            startTime = startTime.plusMinutes(duration); // Update startTime für den nächsten Durchlauf
        }

        return availableAppointments;
    }

    private boolean isTherapistAbsent(List<Absence> absences, Date startDateTime, Date endDateTime) {
        for (Absence absence : absences) {
            if (absence.getDate() != null) {
                Date absenceStart = absence.getStartTime();
                Date absenceEnd = absence.getEndTime();
                if (!absenceStart.after(endDateTime) && !absenceEnd.before(startDateTime)) {
                    return true; // Überlappung gefunden
                }
            } else if (!absence.getWeekday().isEmpty() && matchesWeeklyAbsence(absence, startDateTime, endDateTime)) {
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
        LocalDate date = startDateTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return appointmentRepository.findAllByTherapistIdAndDate(therapistId, Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())).stream()
            .noneMatch(appointment ->
                startDateTime.before(appointment.getEndTime()) && 
                endDateTime.after(appointment.getStartTime())
            );
    }

    private boolean checkForConflicts(Appointment app1, Appointment app2) {
        return app1.getTherapist().getId().equals(app2.getTherapist().getId()) &&
               app1.getDate().equals(app2.getDate()) &&
               app1.getStartTime().before(app2.getEndTime()) &&
               app1.getEndTime().after(app2.getStartTime());
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

    private boolean isOverlapping(Appointment existingAppointment, Appointment newAppointment) {
        return existingAppointment.getStartTime().before(newAppointment.getEndTime()) &&
               existingAppointment.getEndTime().after(newAppointment.getStartTime());
    }

     public Appointment convertDTOToEntity(JSONAppointmentDTO dto) {
        Appointment appointment = new Appointment();
        appointment.setId(dto.getId());
        appointment.setDate(dto.getDate());
        appointment.setComment(dto.getComment());
        appointment.setCreatedBySeriesAppointment(dto.getCreatedBySeriesAppointment());
        appointment.setAppointmentSeriesId(dto.getAppointmentSeriesId());
        appointment.setStartTime(dto.getStartTime());
        appointment.setEndTime(dto.getEndTime());
        appointment.setIsElectric(dto.getIsElectric());
        appointment.setIsHotair(dto.getIsHotair());
        appointment.setIsUltrasonic(dto.getIsUltrasonic());
        appointment.setPatient(patientService.convertDTOToEntity(dto.getPatient()));
        appointment.setTherapist(therapistService.convertDTOToEntity(dto.getTherapist()));
        // Weitere Felder falls nötig
        return appointment;
    }
}
