// AppointmentSeriesService.java
package com.example.physiokalendar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
        // Support both nested therapist object and flat therapistId
        Long therapistId = appointmentSeriesDTO.getTherapistId() != null
                ? appointmentSeriesDTO.getTherapistId()
                : (appointmentSeriesDTO.getTherapist() != null ? appointmentSeriesDTO.getTherapist().getId() : null);
        if (therapistId == null) {
            throw new IllegalArgumentException("Therapist ID is required");
        }
        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));

        AppointmentSeries appointmentSeries = new AppointmentSeries();
        appointmentSeries.setTherapist(therapist);
        appointmentSeries.setPatient(patientRepository.findById(appointmentSeriesDTO.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID")));

        // Konvertiere Date zu LocalTime für Start- und Endzeit
        appointmentSeries.setStartTime(appointmentSeriesDTO.getStartTime() != null ? dateToLocalTime(appointmentSeriesDTO.getStartTime()) : null);
        appointmentSeries.setEndTime(appointmentSeriesDTO.getEndTime() != null ? dateToLocalTime(appointmentSeriesDTO.getEndTime()) : null);

        // Konvertiere Date zu LocalDate für Start- und Enddatum
        appointmentSeries.setStartDate(appointmentSeriesDTO.getStartDate() != null ? dateToLocalDate(appointmentSeriesDTO.getStartDate()) : null);
        appointmentSeries.setEndDate(appointmentSeriesDTO.getEndDate() != null ? dateToLocalDate(appointmentSeriesDTO.getEndDate()) : null);

        appointmentSeries.setWeeklyfrequency(appointmentSeriesDTO.getWeeklyFrequency());
        appointmentSeries.setWeekday(appointmentSeriesDTO.getWeekday());
        appointmentSeries.setComment(appointmentSeriesDTO.getComment());

        // Speichern der AppointmentSeries
        AppointmentSeries savedSeries = appointmentSeriesRepository.save(appointmentSeries);

        // Einzeltermine erstellen
        createAppointmentsFromSeries(savedSeries, savedSeries.getStartDate(), savedSeries.getEndDate(),
                savedSeries.getWeeklyfrequency(),
                Boolean.TRUE.equals(appointmentSeriesDTO.getIsHotair()),
                Boolean.TRUE.equals(appointmentSeriesDTO.getIsUltrasonic()),
                Boolean.TRUE.equals(appointmentSeriesDTO.getIsElectric()));

        return savedSeries;
    }

    public void createAppointmentsFromSeries(AppointmentSeries series, LocalDate startDate, LocalDate endDate,
                                              int weeklyFrequency, boolean isHotair, boolean isUltrasonic, boolean isElectric) {
        Therapist therapist = series.getTherapist();

        // Limit end date to 1 year in the future to prevent excessive appointment creation
        LocalDate maxEndDate = LocalDate.now().plusYears(1);
        LocalDate effectiveEndDate = endDate.isBefore(maxEndDate) ? endDate : maxEndDate;

        // Start from today if startDate is in the past
        LocalDate effectiveStartDate = startDate.isBefore(LocalDate.now()) ? LocalDate.now() : startDate;

        // Erstellen des Kalenders für die Datumsmathematik
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(localDateToDate(effectiveStartDate));
        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(localDateToDate(effectiveEndDate));

        LocalTime startTime = series.getStartTime();
        LocalTime endTime = series.getEndTime();

        // Iterieren durch die Wochen, um die Einzeltermine zu erstellen
        while (startCalendar.before(endCalendar) || startCalendar.equals(endCalendar)) {
            // Erstellen des Einzeltermins
            Appointment appointment = new Appointment();
            appointment.setTherapist(therapist);
            appointment.setPatient(series.getPatient());

            // Kombiniere Datum mit Start- und Endzeit
            LocalDate appointmentDate = dateToLocalDate(startCalendar.getTime());
            LocalDateTime startDateTime = appointmentDate.atTime(startTime);
            LocalDateTime endDateTime = appointmentDate.atTime(endTime);

            appointment.setAppointmentSeries(series);
            appointment.setDate(appointmentDate);
            appointment.setStartTime(startDateTime);
            appointment.setEndTime(endDateTime);
            appointment.setCreatedBySeriesAppointment(true);
            appointment.setIsElectric(isElectric);
            appointment.setIsHotair(isHotair);
            appointment.setIsUltrasonic(isUltrasonic);
            appointment.setComment("");

            // Speichern des Einzeltermins
            appointmentRepository.save(appointment);

            // Nächster Termin basierend auf der wöchentlichen Frequenz
            startCalendar.add(Calendar.WEEK_OF_YEAR, weeklyFrequency);
        }
    }

    // private boolean checkForConflicts(Appointment newAppointment) {
    //     // Filterlogik für Konflikte, z.B. Termine des heutigen Tages
    //     Calendar todayCalendar = Calendar.getInstance();
    //     todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
    //     todayCalendar.set(Calendar.MINUTE, 0);
    //     todayCalendar.set(Calendar.SECOND, 0);
    //     todayCalendar.set(Calendar.MILLISECOND, 0);
    //     Date today = todayCalendar.getTime();

    //     List<Appointment> appointments = appointmentRepository.findAll().stream()
    //             .filter(a -> isSameDay(a.getStartTime(), today) && a.getTherapist().getId().equals(newAppointment.getTherapist().getId()))
    //             .collect(Collectors.toList());

    //     return appointments.stream().anyMatch(existingAppointment -> isOverlapping(existingAppointment, newAppointment));
    // }

    // private boolean isOverlapping(Appointment a, Appointment b) {
    //     return a.getEndTime().after(b.getStartTime()) && b.getEndTime().after(a.getStartTime());
    // }

    // private boolean isSameDay(Date d1, Date d2) {
    //     Calendar cal1 = Calendar.getInstance();
    //     cal1.setTime(d1);
    //     Calendar cal2 = Calendar.getInstance();
    //     cal2.setTime(d2);
    //     return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
    //            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    // }

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

    /**
     * Updates an existing appointment series (master data only).
     * Updates times and treatment flags for the series master.
     * Future appointments generated from this series are also updated.
     */
    @Transactional
    public AppointmentSeries updateAppointmentSeries(Long id, JSONAppointmentSeriesDTO dto) {
        AppointmentSeries series = appointmentSeriesRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment series not found: " + id));

        // Update times if provided
        if (dto.getStartTime() != null) {
            series.setStartTime(dateToLocalTime(dto.getStartTime()));
        }
        if (dto.getEndTime() != null) {
            series.setEndTime(dateToLocalTime(dto.getEndTime()));
        }

        // Update comment if provided
        if (dto.getComment() != null) {
            series.setComment(dto.getComment());
        }

        // Update endDate if provided
        if (dto.getEndDate() != null) {
            series.setEndDate(dateToLocalDate(dto.getEndDate()));
        }

        // Update weeklyFrequency if provided
        if (dto.getWeeklyFrequency() != null) {
            series.setWeeklyfrequency(dto.getWeeklyFrequency());
        }

        AppointmentSeries savedSeries = appointmentSeriesRepository.save(series);

        // Update all future appointments in this series
        LocalDate today = LocalDate.now();
        List<Appointment> futureAppointments = appointmentRepository.findBySeriesId(id).stream()
                .filter(a -> !a.getDate().isBefore(today))
                .toList();

        for (Appointment apt : futureAppointments) {
            if (dto.getStartTime() != null) {
                apt.setStartTime(apt.getDate().atTime(savedSeries.getStartTime()));
            }
            if (dto.getEndTime() != null) {
                apt.setEndTime(apt.getDate().atTime(savedSeries.getEndTime()));
            }
            if (dto.getComment() != null) {
                apt.setComment(dto.getComment());
            }
            if (dto.getIsHotair() != null) {
                apt.setIsHotair(dto.getIsHotair());
            }
            if (dto.getIsUltrasonic() != null) {
                apt.setIsUltrasonic(dto.getIsUltrasonic());
            }
            if (dto.getIsElectric() != null) {
                apt.setIsElectric(dto.getIsElectric());
            }
            appointmentRepository.save(apt);
        }

        return savedSeries;
    }

    public void deleteAppointmentSeries(Long id) {
        appointmentSeriesRepository.deleteById(id);
    }

    /**
     * Deletes a cancellation from a series.
     * This can be used to restore a previously cancelled appointment.
     */
    @Transactional
    public AppointmentSeries deleteCancellation(Long seriesId, Long cancellationId) {
        AppointmentSeries series = appointmentSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new IllegalArgumentException("Series not found: " + seriesId));

        Cancellation cancellation = cancellationRepository.findById(cancellationId)
                .orElseThrow(() -> new IllegalArgumentException("Cancellation not found: " + cancellationId));

        // Verify the cancellation belongs to this series
        if (!cancellation.getAppointmentSeries().getId().equals(seriesId)) {
            throw new IllegalArgumentException("Cancellation does not belong to this series");
        }

        // Remove from the series list and delete
        series.getCancellations().removeIf(c -> c.getId().equals(cancellationId));
        cancellationRepository.deleteById(cancellationId);

        return appointmentSeriesRepository.save(series);
    }

    private Cancellation convertDTOToEntity(JSONCancellationDTO dto) {
        Cancellation cancellation = new Cancellation();
        cancellation.setId(dto.getId());
        cancellation.setDate(dto.getDate() != null ? dateToLocalDate(dto.getDate()) : null);
        // Weitere Felder falls nötig
        return cancellation;
    }

    private LocalDate dateToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
    }

    private LocalTime dateToLocalTime(Date date) {
        return date.toInstant().atZone(ZoneId.of("UTC")).toLocalTime();
    }

    private Date localDateToDate(LocalDate localDate) {
        return java.sql.Date.valueOf(localDate);
    }
}