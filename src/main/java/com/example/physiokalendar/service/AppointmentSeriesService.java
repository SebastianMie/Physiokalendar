// AppointmentSeriesService.java
package com.example.physiokalendar.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONAppointmentSeriesDTO;
import com.example.physiokalendar.dto.JSONCancellationDTO;
import com.example.physiokalendar.entity.Appointment;
import com.example.physiokalendar.entity.AppointmentSeries;
import com.example.physiokalendar.entity.Cancellation;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.AppointmentRepository;
import com.example.physiokalendar.repository.AppointmentSeriesRepository;
import com.example.physiokalendar.repository.CancellationRepository;
import com.example.physiokalendar.repository.PatientRepository;
import com.example.physiokalendar.repository.TherapistRepository;

import jakarta.transaction.Transactional;

@Service
public class AppointmentSeriesService {

    @Autowired
    private AppointmentSeriesRepository appointmentSeriesRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private CancellationRepository cancellationRepository;

    public List<AppointmentSeries> getAllAppointmentSeries() {
        return appointmentSeriesRepository.findAll();
    }

    public Optional<AppointmentSeries> getAppointmentSeriesById(Long id) {
        return appointmentSeriesRepository.findById(id);
    }

     @Transactional
    public AppointmentSeries saveAppointmentSeries(JSONAppointmentSeriesDTO appointmentSeriesDTO) {
        // Mapping DTO to Entity
        Long therapistId = appointmentSeriesDTO.getTherapist().getId();
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));

        AppointmentSeries appointmentSeries = new AppointmentSeries();
        appointmentSeries.setTherapist(therapist);
        appointmentSeries.setPatient(patientRepository.findById(appointmentSeriesDTO.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID")));
        appointmentSeries.setStartTime(appointmentSeriesDTO.getStartTime());
        appointmentSeries.setEndTime(appointmentSeriesDTO.getEndTime());
        appointmentSeries.setStartDate(appointmentSeriesDTO.getStartDate());
        appointmentSeries.setEndDate(appointmentSeriesDTO.getEndDate());
        appointmentSeries.setWeeklyfrequency(appointmentSeriesDTO.getWeeklyFrequency());
        appointmentSeries.setWeekday(appointmentSeriesDTO.getWeekday());
        appointmentSeries.setComment(appointmentSeriesDTO.getComment());

        // Speichern der AppointmentSeries
        AppointmentSeries savedSeries = appointmentSeriesRepository.save(appointmentSeries);

        // Einzeltermine erstellen
        createAppointmentsFromSeries(savedSeries, appointmentSeriesDTO.getStartTime(), appointmentSeriesDTO.getEndDate(), appointmentSeriesDTO.getWeeklyFrequency());

        return savedSeries;
    }

    public void createAppointmentsFromSeries(AppointmentSeries series, Date startDate, Date endDate, int weeklyFrequency) {
        Therapist therapist = series.getTherapist();

        // Erstellen des Kalenders für die Datumsmathematik
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);

        Calendar endTimeCalendar = Calendar.getInstance();
        endTimeCalendar.setTime(series.getEndTime());

        // Iterieren durch die Wochen, um die Einzeltermine zu erstellen
        while (startCalendar.before(endCalendar) || startCalendar.equals(endCalendar)) {
            // Erstellen des Einzeltermins
            Appointment appointment = new Appointment();
            appointment.setTherapist(therapist);
            appointment.setPatient(series.getPatient());
            Date startTime = startCalendar.getTime();
            Calendar endCalendarAppointment = (Calendar) startCalendar.clone();
            endCalendarAppointment.set(Calendar.HOUR_OF_DAY, endTimeCalendar.get(Calendar.HOUR_OF_DAY));
            endCalendarAppointment.set(Calendar.MINUTE, endTimeCalendar.get(Calendar.MINUTE));
            endCalendarAppointment.set(Calendar.SECOND, endTimeCalendar.get(Calendar.SECOND));
            Date endTime = endCalendarAppointment.getTime();
            appointment.setDate(startTime);
            appointment.setStartTime(startTime);
            appointment.setEndTime(endTime);
            appointment.setCreatedBySeriesAppointment(true);
            appointment.setIsElectric(false);
            appointment.setIsHotair(false);
            appointment.setIsUltrasonic(false);
            appointment.setComment("generiert aus SerienTermin id "+ series.getId());
            // Überprüfen auf Konflikte
            if (checkForConflicts(appointment)) {
                throw new IllegalStateException("Appointment conflicts with an existing appointment.");
            }

            // Speichern des Einzeltermins
            appointmentRepository.save(appointment);

            // Nächster Termin basierend auf der wöchentlichen Frequenz
            startCalendar.add(Calendar.WEEK_OF_YEAR, weeklyFrequency);
        }
    }

    private boolean checkForConflicts(Appointment newAppointment) {
        // Filterlogik für Konflikte, z.B. Termine des heutigen Tages
        Calendar todayCalendar = Calendar.getInstance();
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        todayCalendar.set(Calendar.MINUTE, 0);
        todayCalendar.set(Calendar.SECOND, 0);
        todayCalendar.set(Calendar.MILLISECOND, 0);
        Date today = todayCalendar.getTime();

        List<Appointment> appointments = appointmentRepository.findAll().stream()
                .filter(a -> isSameDay(a.getStartTime(), today) && a.getTherapist().getId().equals(newAppointment.getTherapist().getId()))
                .collect(Collectors.toList());

        return appointments.stream().anyMatch(existingAppointment -> isOverlapping(existingAppointment, newAppointment));
    }

    private boolean isOverlapping(Appointment a, Appointment b) {
        return a.getEndTime().after(b.getStartTime()) && b.getEndTime().after(a.getStartTime());
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Transactional
    public AppointmentSeries addCancellations(Long appointmentSeriesId, List<JSONCancellationDTO> cancellationDTOs) {
        // Holen des AppointmentSeries-Objekts anhand der ID
        AppointmentSeries appointmentSeries = appointmentSeriesRepository.findById(appointmentSeriesId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointment series ID"));

        // Durchlaufen der DTOs und Erstellen der Cancellation-Objekte
        List<Cancellation> cancellations = cancellationDTOs.stream().map(dto -> {
            Cancellation cancellation = convertDTOToEntity(dto);

            // Zuweisen des AppointmentSeries zu Cancellation
            cancellation.setAppointmentSeries(appointmentSeries);

            // Speichern der Cancellation in der Datenbank
            return cancellationRepository.save(cancellation);
        }).collect(Collectors.toList());

        // Hinzufügen der neuen Cancellations zur AppointmentSeries
        appointmentSeries.getCancellations().addAll(cancellations);

        // Speichern des aktualisierten AppointmentSeries-Objekts
        return appointmentSeriesRepository.save(appointmentSeries);
    }

    public void deleteAppointmentSeries(Long id) {
        appointmentSeriesRepository.deleteById(id);
    }

    private Cancellation convertDTOToEntity(JSONCancellationDTO dto) {
        Cancellation cancellation = new Cancellation();
        cancellation.setId(dto.getId());
        cancellation.setDate(dto.getDate());
        // Weitere Felder falls nötig
        return cancellation;
    }
}