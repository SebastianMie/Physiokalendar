package com.example.physiokalendar.service;

import com.example.physiokalendar.dto.AppointmentDraftDTO;
import com.example.physiokalendar.dto.ConflictCheckDTO;
import com.example.physiokalendar.dto.ConflictCheckDTO.ConflictDTO;
import com.example.physiokalendar.entity.*;
import com.example.physiokalendar.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for conflict detection.
 * Checks for overlapping appointments, series instances, and absences.
 */
@Service
@Slf4j
public class ConflictService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeriesRepository seriesRepository;
    private final AbsenceRepository absenceRepository;
    private final CancellationRepository cancellationRepository;

    public ConflictService(
            AppointmentRepository appointmentRepository,
            AppointmentSeriesRepository seriesRepository,
            AbsenceRepository absenceRepository,
            CancellationRepository cancellationRepository) {
        this.appointmentRepository = appointmentRepository;
        this.seriesRepository = seriesRepository;
        this.absenceRepository = absenceRepository;
        this.cancellationRepository = cancellationRepository;
    }

    /**
     * Check for conflicts with a proposed appointment.
     * Returns conflict details if any are found.
     */
    @Transactional(readOnly = true)
    public ConflictCheckDTO checkConflicts(AppointmentDraftDTO draft) {
        List<ConflictDTO> conflicts = new ArrayList<>();

        // 1. Check overlapping appointments
        List<Appointment> overlappingAppointments;
        if (draft.getId() != null) {
            overlappingAppointments = appointmentRepository.findOverlappingExcludingId(
                    draft.getTherapistId(),
                    draft.getDate(),
                    draft.getStartTime(),
                    draft.getEndTime(),
                    draft.getId(),
                    AppointmentStatus.CANCELLED);
        } else {
            overlappingAppointments = appointmentRepository.findOverlapping(
                    draft.getTherapistId(),
                    draft.getDate(),
                    draft.getStartTime(),
                    draft.getEndTime(),
                    AppointmentStatus.CANCELLED);
        }

        for (Appointment apt : overlappingAppointments) {
            conflicts.add(ConflictDTO.builder()
                    .type("APPOINTMENT")
                    .id(apt.getId())
                    .therapistId(apt.getTherapist().getId())
                    .therapistName(apt.getTherapist().getFullName())
                    .patientId(apt.getPatient().getId())
                    .patientName(apt.getPatient().getFullName())
                    .date(apt.getDate())
                    .startTime(apt.getStartTime())
                    .endTime(apt.getEndTime())
                    .description("Überschneidung mit bestehendem Termin")
                    .build());
        }

        // 2. Check overlapping series instances
        List<ConflictDTO> seriesConflicts = checkSeriesConflicts(draft);
        conflicts.addAll(seriesConflicts);

        // 3. Check absence blocks
        List<ConflictDTO> absenceConflicts = checkAbsenceConflicts(draft);
        conflicts.addAll(absenceConflicts);

        return ConflictCheckDTO.builder()
                .hasConflict(!conflicts.isEmpty())
                .conflicts(conflicts)
                .build();
    }

    /**
     * Check for conflicts with series instances.
     */
    private List<ConflictDTO> checkSeriesConflicts(AppointmentDraftDTO draft) {
        List<ConflictDTO> conflicts = new ArrayList<>();

        // If this is an existing appointment, check if it belongs to a series
        Long excludeSeriesId = null;
        if (draft.getId() != null) {
            Appointment existingApt = appointmentRepository.findById(draft.getId()).orElse(null);
            if (existingApt != null && existingApt.getAppointmentSeries() != null) {
                excludeSeriesId = existingApt.getAppointmentSeries().getId();
            }
        }

        // Find active series for the therapist that could have an instance on this date
        List<AppointmentSeries> seriesList = seriesRepository.findActiveByTherapistIdAndDateRange(
                draft.getTherapistId(),
                draft.getDate(),
                draft.getDate(),
                SeriesStatus.ACTIVE);

        for (AppointmentSeries series : seriesList) {
            // Skip if this is the series that the appointment being edited belongs to
            if (excludeSeriesId != null && series.getId().equals(excludeSeriesId)) {
                continue;
            }

            // Check if this series has an instance on the draft date
            DayOfWeek seriesDay = parseWeekday(series.getWeekday());
            if (seriesDay == null || !draft.getDate().getDayOfWeek().equals(seriesDay)) {
                continue;
            }

            // Check if this instance is cancelled
            Set<LocalDate> cancelledDates = series.getCancellations() != null
                    ? series.getCancellations().stream()
                        .map(Cancellation::getDate)
                        .collect(Collectors.toSet())
                    : Set.of();

            if (cancelledDates.contains(draft.getDate())) {
                continue; // Instance is cancelled, no conflict
            }

            // Check frequency
            int frequency = series.getWeeklyfrequency() != null ? series.getWeeklyfrequency() : 1;
            long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(series.getStartDate(), draft.getDate());
            if (weeksBetween < 0 || weeksBetween % frequency != 0) {
                continue; // Not an occurrence week
            }

            // Check time overlap
            LocalDateTime seriesStart = LocalDateTime.of(draft.getDate(), series.getStartTime());
            LocalDateTime seriesEnd = LocalDateTime.of(draft.getDate(), series.getEndTime());

            if (timesOverlap(draft.getStartTime(), draft.getEndTime(), seriesStart, seriesEnd)) {
                conflicts.add(ConflictDTO.builder()
                        .type("SERIES_INSTANCE")
                        .id(series.getId())
                        .therapistId(series.getTherapist().getId())
                        .therapistName(series.getTherapist().getFullName())
                        .patientId(series.getPatient().getId())
                        .patientName(series.getPatient().getFullName())
                        .date(draft.getDate())
                        .startTime(seriesStart)
                        .endTime(seriesEnd)
                        .description("Überschneidung mit Serientermin")
                        .build());
            }
        }

        return conflicts;
    }

    /**
     * Check for conflicts with absence blocks.
     */
    private List<ConflictDTO> checkAbsenceConflicts(AppointmentDraftDTO draft) {
        List<ConflictDTO> conflicts = new ArrayList<>();

        // Check special absences
        List<Absence> specialAbsences = absenceRepository.findSpecialByTherapistIdAndDateRange(
                draft.getTherapistId(),
                draft.getDate(),
                draft.getDate(),
                AbsenceType.SPECIAL);

        for (Absence absence : specialAbsences) {
            if (timesOverlap(draft.getStartTime(), draft.getEndTime(),
                        absence.getStartTime().atDate(draft.getStartTime().toLocalDate()),
                        absence.getEndTime().atDate(draft.getStartTime().toLocalDate()))) {
                conflicts.add(ConflictDTO.builder()
                        .type("ABSENCE")
                        .id(absence.getId())
                        .therapistId(absence.getTherapist().getId())
                        .therapistName(absence.getTherapist().getFullName())
                        .date(absence.getDate())
                        .startTime(absence.getStartTime().atDate(draft.getStartTime().toLocalDate()))
                        .endTime(absence.getEndTime().atDate(draft.getStartTime().toLocalDate()))
                        .description("Therapeut ist abwesend: " +
                                (absence.getReason() != null ? absence.getReason() : "Keine Angabe"))
                        .build());
            }
        }

        // Check recurring absences
        List<Absence> recurringAbsences = absenceRepository.findRecurringByTherapistId(
                draft.getTherapistId(), AbsenceType.RECURRING);

        for (Absence absence : recurringAbsences) {
            DayOfWeek absenceDay = parseWeekday(absence.getWeekday());
            if (absenceDay == null || !draft.getDate().getDayOfWeek().equals(absenceDay)) {
                continue;
            }

            // Recurring absence on this weekday
            LocalDateTime absenceStart = absence.getStartTime().atDate(LocalDate.of(2000, 1, 1));
            LocalDateTime absenceEnd = absence.getEndTime().atDate(LocalDate.of(2000, 1, 1));

            // If times are not set, it's a full-day absence
            if (absenceStart == null || absenceEnd == null) {
                conflicts.add(ConflictDTO.builder()
                        .type("ABSENCE")
                        .id(absence.getId())
                        .therapistId(absence.getTherapist().getId())
                        .therapistName(absence.getTherapist().getFullName())
                        .date(draft.getDate())
                        .startTime(LocalDateTime.of(draft.getDate(), LocalTime.of(0, 0)))
                        .endTime(LocalDateTime.of(draft.getDate(), LocalTime.of(23, 59)))
                        .description("Therapeut ist an diesem Wochentag nicht verfügbar: " +
                                (absence.getReason() != null ? absence.getReason() : "Wöchentliche Abwesenheit"))
                        .build());
            } else if (timesOverlap(draft.getStartTime(), draft.getEndTime(), absenceStart, absenceEnd)) {
                conflicts.add(ConflictDTO.builder()
                        .type("ABSENCE")
                        .id(absence.getId())
                        .therapistId(absence.getTherapist().getId())
                        .therapistName(absence.getTherapist().getFullName())
                        .date(draft.getDate())
                        .startTime(absenceStart)
                        .endTime(absenceEnd)
                        .description("Therapeut ist zu dieser Zeit nicht verfügbar: " +
                                (absence.getReason() != null ? absence.getReason() : "Wöchentliche Abwesenheit"))
                        .build());
            }
        }

        return conflicts;
    }

    /**
     * Check if two time ranges overlap.
     */
    private boolean timesOverlap(LocalDateTime start1, LocalDateTime end1,
                                  LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private DayOfWeek parseWeekday(String weekday) {
        if (weekday == null) return null;
        try {
            return switch (weekday.toUpperCase()) {
                case "MONDAY", "MONTAG", "MO" -> DayOfWeek.MONDAY;
                case "TUESDAY", "DIENSTAG", "DI" -> DayOfWeek.TUESDAY;
                case "WEDNESDAY", "MITTWOCH", "MI" -> DayOfWeek.WEDNESDAY;
                case "THURSDAY", "DONNERSTAG", "DO" -> DayOfWeek.THURSDAY;
                case "FRIDAY", "FREITAG", "FR" -> DayOfWeek.FRIDAY;
                case "SATURDAY", "SAMSTAG", "SA" -> DayOfWeek.SATURDAY;
                case "SUNDAY", "SONNTAG", "SO" -> DayOfWeek.SUNDAY;
                default -> DayOfWeek.valueOf(weekday.toUpperCase());
            };
        } catch (Exception e) {
            return null;
        }
    }
}
