package com.example.physiokalendar.service;

import com.example.physiokalendar.dto.CalendarRangeDTO;
import com.example.physiokalendar.dto.CalendarRangeDTO.*;
import com.example.physiokalendar.entity.*;
import com.example.physiokalendar.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calendar range aggregation.
 * Provides unified view of appointments, series instances, and absences.
 */
@Service
@Slf4j
public class CalendarService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeriesRepository seriesRepository;
    private final AbsenceRepository absenceRepository;
    private final CancellationRepository cancellationRepository;
    private final TherapistRepository therapistRepository;

    public CalendarService(
            AppointmentRepository appointmentRepository,
            AppointmentSeriesRepository seriesRepository,
            AbsenceRepository absenceRepository,
            CancellationRepository cancellationRepository,
            TherapistRepository therapistRepository) {
        this.appointmentRepository = appointmentRepository;
        this.seriesRepository = seriesRepository;
        this.absenceRepository = absenceRepository;
        this.cancellationRepository = cancellationRepository;
        this.therapistRepository = therapistRepository;
    }

    /**
     * Get complete calendar data for a date range.
     * Returns appointments, computed series instances, and absence blocks.
     */
    @Transactional(readOnly = true)
    public CalendarRangeDTO getCalendarRange(LocalDate from, LocalDate to, Long therapistId) {
        log.debug("Getting calendar range from {} to {} for therapist {}", from, to, therapistId);

        // Get therapists
        List<Therapist> therapists;
        if (therapistId != null) {
            therapists = therapistRepository.findById(therapistId)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            therapists = therapistRepository.findByIsActiveTrue();
        }

        // Get appointments
        List<Appointment> appointments;
        if (therapistId != null) {
            appointments = appointmentRepository.findByTherapistIdAndDateRange(therapistId, from, to);
        } else {
            appointments = appointmentRepository.findByDateRange(from, to);
        }

        // Get series and compute instances
        List<AppointmentSeries> series;
        if (therapistId != null) {
            series = seriesRepository.findActiveByTherapistIdAndDateRange(
                    therapistId, from, to, SeriesStatus.ACTIVE);
        } else {
            series = seriesRepository.findActiveByDateRange(from, to, SeriesStatus.ACTIVE);
        }
        List<CalendarSeriesInstanceDTO> seriesInstances = computeSeriesInstances(series, from, to);

        // Get absences (blocks)
        List<CalendarBlockDTO> blocks = getAbsenceBlocks(from, to, therapistId);

        return CalendarRangeDTO.builder()
                .from(from)
                .to(to)
                .therapists(therapists.stream().map(this::toTherapistSummary).toList())
                .appointments(appointments.stream().map(this::toCalendarAppointment).toList())
                .seriesInstances(seriesInstances)
                .blocks(blocks)
                .build();
    }

    /**
     * Compute all series instances within the date range.
     * Applies cancellations and exceptions.
     */
    private List<CalendarSeriesInstanceDTO> computeSeriesInstances(
            List<AppointmentSeries> seriesList, LocalDate from, LocalDate to) {

        List<CalendarSeriesInstanceDTO> instances = new ArrayList<>();

        for (AppointmentSeries series : seriesList) {
            // Get cancellations for this series
            Set<LocalDate> cancelledDates = series.getCancellations() != null
                    ? series.getCancellations().stream()
                        .map(Cancellation::getDate)
                        .collect(Collectors.toSet())
                    : Set.of();

            // Determine the weekday
            DayOfWeek seriesDay = parseWeekday(series.getWeekday());
            if (seriesDay == null) {
                log.warn("Invalid weekday {} for series {}", series.getWeekday(), series.getId());
                continue;
            }

            // Iterate through the date range
            LocalDate effectiveStart = series.getStartDate().isAfter(from) ? series.getStartDate() : from;
            LocalDate effectiveEnd = series.getEndDate().isBefore(to) ? series.getEndDate() : to;

            LocalDate current = effectiveStart;
            int instanceIndex = 0;
            int weekCounter = 0;

            // Find first occurrence of the weekday
            while (!current.getDayOfWeek().equals(seriesDay) && !current.isAfter(effectiveEnd)) {
                current = current.plusDays(1);
            }

            // Calculate initial week offset from series start
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(series.getStartDate(), current);
            weekCounter = (int) (daysBetween / 7);

            while (!current.isAfter(effectiveEnd)) {
                // Check if this week matches the frequency
                int frequency = series.getWeeklyfrequency() != null ? series.getWeeklyfrequency() : 1;
                if (weekCounter % frequency == 0) {
                    boolean isCancelled = cancelledDates.contains(current);

                    instances.add(CalendarSeriesInstanceDTO.builder()
                            .seriesId(series.getId())
                            .therapistId(series.getTherapist().getId())
                            .therapistName(series.getTherapist().getFullName())
                            .patientId(series.getPatient().getId())
                            .patientName(series.getPatient().getFullName())
                            .date(current)
                            .startTime(series.getStartTime())
                            .endTime(series.getEndTime())
                            .comment(series.getComment())
                            .isCancelled(isCancelled)
                            .isException(false) // TODO: implement exception handling
                            .instanceIndex(instanceIndex++)
                            .build());
                }

                current = current.plusWeeks(1);
                weekCounter++;
            }
        }

        return instances;
    }

    /**
     * Get absence blocks for the date range.
     * Includes both special (one-time) and recurring absences.
     */
    private List<CalendarBlockDTO> getAbsenceBlocks(LocalDate from, LocalDate to, Long therapistId) {
        List<CalendarBlockDTO> blocks = new ArrayList<>();

        // Get special absences
        List<Absence> specialAbsences;
        if (therapistId != null) {
            specialAbsences = absenceRepository.findSpecialByTherapistIdAndDateRange(
                    therapistId, from, to, AbsenceType.SPECIAL);
        } else {
            specialAbsences = absenceRepository.findSpecialByDateRange(from, to, AbsenceType.SPECIAL);
        }

        for (Absence absence : specialAbsences) {
            blocks.add(toCalendarBlock(absence, false));
        }

        // Get recurring absences and expand them
        List<Absence> recurringAbsences;
        if (therapistId != null) {
            recurringAbsences = absenceRepository.findRecurringByTherapistId(therapistId, AbsenceType.RECURRING);
        } else {
            recurringAbsences = absenceRepository.findAllRecurring(AbsenceType.RECURRING);
        }

        for (Absence absence : recurringAbsences) {
            DayOfWeek weekday = parseWeekday(absence.getWeekday());
            if (weekday == null) continue;

            LocalDate current = from;
            while (!current.getDayOfWeek().equals(weekday)) {
                current = current.plusDays(1);
            }

            while (!current.isAfter(to)) {
                CalendarBlockDTO block = CalendarBlockDTO.builder()
                        .id(absence.getId())
                        .therapistId(absence.getTherapist().getId())
                        .therapistName(absence.getTherapist().getFullName())
                        .date(current)
                        .startTime(absence.getStartTime())
                        .endTime(absence.getEndTime())
                        .reason(absence.getReason())
                        .blockType("RECURRING")
                        .isRecurring(true)
                        .build();
                blocks.add(block);
                current = current.plusWeeks(1);
            }
        }

        return blocks;
    }

    private DayOfWeek parseWeekday(String weekday) {
        if (weekday == null) return null;
        try {
            // Support both English and German weekday names
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

    private TherapistSummaryDTO toTherapistSummary(Therapist therapist) {
        return TherapistSummaryDTO.builder()
                .id(therapist.getId())
                .fullName(therapist.getFullName())
                .color(null) // TODO: add color to therapist entity
                .build();
    }

    private CalendarAppointmentDTO toCalendarAppointment(Appointment appointment) {
        return CalendarAppointmentDTO.builder()
                .id(appointment.getId())
                .therapistId(appointment.getTherapist().getId())
                .therapistName(appointment.getTherapist().getFullName())
                .patientId(appointment.getPatient().getId())
                .patientName(appointment.getPatient().getFullName())
                .date(appointment.getDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus() != null ? appointment.getStatus().name() : "SCHEDULED")
                .comment(appointment.getComment())
                .isHotair(appointment.getIsHotair())
                .isUltrasonic(appointment.getIsUltrasonic())
                .isElectric(appointment.getIsElectric())
                .isFromSeries(appointment.getCreatedBySeriesAppointment())
                .seriesId(appointment.getAppointmentSeries() != null
                        ? appointment.getAppointmentSeries().getId() : null)
                .build();
    }

    private CalendarBlockDTO toCalendarBlock(Absence absence, boolean isRecurring) {
        return CalendarBlockDTO.builder()
                .id(absence.getId())
                .therapistId(absence.getTherapist().getId())
                .therapistName(absence.getTherapist().getFullName())
                .date(absence.getDate())
                .startTime(absence.getStartTime())
                .endTime(absence.getEndTime())
                .reason(absence.getReason())
                .blockType(isRecurring ? "RECURRING" : "SPECIAL")
                .isRecurring(isRecurring)
                .build();
    }
}
