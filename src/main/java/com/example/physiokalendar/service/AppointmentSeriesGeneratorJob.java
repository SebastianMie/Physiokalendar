package com.example.physiokalendar.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.physiokalendar.entity.Appointment;
import com.example.physiokalendar.entity.AppointmentSeries;
import com.example.physiokalendar.entity.Cancellation;
import com.example.physiokalendar.entity.SeriesStatus;
import com.example.physiokalendar.repository.AppointmentRepository;
import com.example.physiokalendar.repository.AppointmentSeriesRepository;

/**
 * Service that ensures appointments are generated for all active series
 * up to 1 year in the future. Runs at application startup and can be
 * triggered manually or via scheduled job.
 */
@Service
public class AppointmentSeriesGeneratorJob {

    private static final Logger log = LoggerFactory.getLogger(AppointmentSeriesGeneratorJob.class);

    // Maximum time horizon for generating appointments (1 year)
    private static final int MAX_FUTURE_YEARS = 1;

    private final AppointmentSeriesRepository seriesRepository;
    private final AppointmentRepository appointmentRepository;
    private final HolidayService holidayService;

    public AppointmentSeriesGeneratorJob(
            AppointmentSeriesRepository seriesRepository,
            AppointmentRepository appointmentRepository,
            HolidayService holidayService) {
        this.seriesRepository = seriesRepository;
        this.appointmentRepository = appointmentRepository;
        this.holidayService = holidayService;
    }

    /**
     * Runs at application startup to ensure all series have appointments generated.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("AppointmentSeriesGeneratorJob: Starting series appointment generation check...");
        try {
            int generatedCount = generateMissingAppointments();
            log.info("AppointmentSeriesGeneratorJob: Completed. Generated {} new appointments.", generatedCount);
        } catch (Exception e) {
            log.error("AppointmentSeriesGeneratorJob: Error during appointment generation", e);
        }
    }

    /**
     * Scheduled job that runs weekly to extend series appointments.
     * Cron: Every Monday at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * MON")
    public void scheduledGeneration() {
        log.info("Scheduled series appointment generation started");
        try {
            int generatedCount = generateMissingAppointments();
            log.info("Scheduled generation completed. Generated {} new appointments.", generatedCount);
        } catch (Exception e) {
            log.error("Error during scheduled appointment generation", e);
        }
    }

    /**
     * Generates missing appointments for all active series.
     * WICHTIG: Kein @Transactional - speichert direkt mit repository.save()
     */
    public int generateMissingAppointments() {
        LocalDate today = LocalDate.now();
        LocalDate maxFutureDate = today.plusYears(MAX_FUTURE_YEARS);

        // Hole ALLE Serien (nicht nur aktive)
        List<AppointmentSeries> allSeries = seriesRepository.findAll();

        log.info("Found {} series total", allSeries.size());

        // Filter: Serien die in unserem Generierungsfenster überlappen
        // aber NUR die, die noch nicht vollständig generiert sind
        List<AppointmentSeries> relevantSeries = allSeries.stream()
                .filter(s -> s.getStartDate() != null && s.getEndDate() != null)
                // Serie muss in Zukunft liegen oder noch nicht vollständig generiert sein
                .filter(s -> !s.getEndDate().isBefore(today)) // endDate >= today
                // Generiere alle Serien deren Start vor maxFutureDate liegt
                .filter(s -> !s.getStartDate().isAfter(maxFutureDate)) // startDate <= maxFutureDate
                .toList();

        log.info("After filtering: {} series need appointment generation", relevantSeries.size());

        int totalGenerated = 0;
        for (AppointmentSeries series : relevantSeries) {
            try {
                int generated = generateAppointmentsForSeries(series, today, maxFutureDate);
                totalGenerated += generated;
                if (generated > 0) {
                    log.info("Generated {} appointments for series {} (Patient: {}, Therapist: {}, {})",
                            generated, series.getId(), series.getPatientName(), series.getTherapistName(), series.getWeekday());
                }
            } catch (Exception e) {
                log.error("Error generating appointments for series {}: {}", series.getId(), e.getMessage(), e);
            }
        }

        return totalGenerated;
    }

    /**
     * Generates missing appointments for a specific series.
     * WICHTIG: Kein @Transactional - speichert direkt mit repository.save()
     */
    public int generateAppointmentsForSeries(AppointmentSeries series, LocalDate fromDate, LocalDate maxDate) {
        // Calculate effective date range
        LocalDate effectiveStartDate = series.getStartDate().isBefore(fromDate) ? fromDate : series.getStartDate();
        LocalDate effectiveEndDate = series.getEndDate().isBefore(maxDate) ? series.getEndDate() : maxDate;

        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            return 0; // Nothing to generate
        }

        // Get the target weekday for this series
        DayOfWeek targetWeekday = parseWeekday(series.getWeekday());
        if (targetWeekday == null) {
            log.warn("Series {} has invalid weekday: {}", series.getId(), series.getWeekday());
            return 0;
        }

        // Get all existing appointment dates for this series
        List<Appointment> existingAppointments = appointmentRepository.findBySeriesId(series.getId());
        Set<LocalDate> existingDates = existingAppointments.stream()
                .map(Appointment::getDate)
                .collect(Collectors.toSet());

        // Get cancelled dates from the series
        Set<LocalDate> cancelledDates = series.getCancellations() != null
                ? series.getCancellations().stream()
                    .map(Cancellation::getDate)
                    .collect(Collectors.toSet())
                : Set.of();

        // Get all holiday dates to exclude from generation
        Set<LocalDate> holidayDates = holidayService.getHolidayDates();

        // Find the first occurrence of the target weekday from effectiveStartDate
        LocalDate currentDate = effectiveStartDate;
        while (currentDate.getDayOfWeek() != targetWeekday) {
            currentDate = currentDate.plusDays(1);
        }

        int generatedCount = 0;
        int weeklyFrequency = series.getWeeklyfrequency() != null ? series.getWeeklyfrequency() : 1;
        LocalTime startTime = series.getStartTime();
        LocalTime endTime = series.getEndTime();

        while (!currentDate.isAfter(effectiveEndDate)) {
            // Skip if appointment already exists, date is cancelled, or date is a holiday
            if (!existingDates.contains(currentDate) && !cancelledDates.contains(currentDate) && !holidayDates.contains(currentDate)) {
                // Create the appointment
                Appointment appointment = new Appointment();
                appointment.setTherapist(series.getTherapist());
                appointment.setPatient(series.getPatient());
                appointment.setDate(currentDate);
                appointment.setStartTime(LocalDateTime.of(currentDate, startTime));
                appointment.setEndTime(LocalDateTime.of(currentDate, endTime));
                appointment.setAppointmentSeries(series);
                appointment.setCreatedBySeriesAppointment(true);
                // Hinweis: Die Series-ID ist über setAppointmentSeries() gespeichert
                appointment.setIsHotair(false);
                appointment.setIsUltrasonic(false);
                appointment.setIsElectric(false);

                appointmentRepository.save(appointment);
                existingDates.add(currentDate); // Add to set to prevent duplicates within same run
                generatedCount++;
            }

            // Move to next occurrence based on weekly frequency
            currentDate = currentDate.plusWeeks(weeklyFrequency);
        }

        return generatedCount;
    }

    /**
     * Parses German weekday names to DayOfWeek.
     */
    private DayOfWeek parseWeekday(String weekday) {
        if (weekday == null || weekday.isEmpty()) {
            return null;
        }

        String normalized = weekday.toLowerCase(Locale.GERMAN).trim();

        return switch (normalized) {
            case "montag", "mo", "monday" -> DayOfWeek.MONDAY;
            case "dienstag", "di", "tuesday" -> DayOfWeek.TUESDAY;
            case "mittwoch", "mi", "wednesday" -> DayOfWeek.WEDNESDAY;
            case "donnerstag", "do", "thursday" -> DayOfWeek.THURSDAY;
            case "freitag", "fr", "friday" -> DayOfWeek.FRIDAY;
            case "samstag", "sa", "saturday" -> DayOfWeek.SATURDAY;
            case "sonntag", "so", "sunday" -> DayOfWeek.SUNDAY;
            default -> {
                // Try to match partial names
                if (normalized.startsWith("mo")) yield DayOfWeek.MONDAY;
                if (normalized.startsWith("di")) yield DayOfWeek.TUESDAY;
                if (normalized.startsWith("mi")) yield DayOfWeek.WEDNESDAY;
                if (normalized.startsWith("do")) yield DayOfWeek.THURSDAY;
                if (normalized.startsWith("fr")) yield DayOfWeek.FRIDAY;
                if (normalized.startsWith("sa")) yield DayOfWeek.SATURDAY;
                if (normalized.startsWith("so")) yield DayOfWeek.SUNDAY;
                yield null;
            }
        };
    }
}
