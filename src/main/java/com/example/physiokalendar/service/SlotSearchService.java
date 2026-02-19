package com.example.physiokalendar.service;

import com.example.physiokalendar.dto.SlotSearchDTO;
import com.example.physiokalendar.dto.SlotSearchDTO.Request;
import com.example.physiokalendar.dto.SlotSearchDTO.Response;
import com.example.physiokalendar.dto.SlotSearchDTO.SlotDTO;
import com.example.physiokalendar.dto.SlotSearchDTO.SlotGroupDTO;

import com.example.physiokalendar.dto.SlotSearchDTO.DayPart;
import com.example.physiokalendar.entity.*;
import com.example.physiokalendar.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Service for finding available appointment slots.
 * Implements the slot search/terminfinder functionality.
 */
@Service
@Slf4j
public class SlotSearchService {

    // Configurable working hours
    private static final LocalTime WORK_START = LocalTime.of(7, 0);
    private static final LocalTime WORK_END = LocalTime.of(20, 0);
    private static final int SLOT_INCREMENT_MINUTES = 15; // Slot granularity

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSeriesRepository seriesRepository;
    private final AbsenceRepository absenceRepository;
    private final TherapistRepository therapistRepository;

    public SlotSearchService(
            AppointmentRepository appointmentRepository,
            AppointmentSeriesRepository seriesRepository,
            AbsenceRepository absenceRepository,
            TherapistRepository therapistRepository) {
        this.appointmentRepository = appointmentRepository;
        this.seriesRepository = seriesRepository;
        this.absenceRepository = absenceRepository;
        this.therapistRepository = therapistRepository;
    }

    /**
     * Search for available slots based on criteria.
     */
    @Transactional(readOnly = true)
    public Response searchSlots(Request request) {
        log.debug("Searching slots from {} to {} for {} minutes",
                request.getRangeFrom(), request.getRangeTo(), request.getDurationMinutes());

        // Get therapists to search
        List<Therapist> therapists;
        if (request.getTherapistId() != null) {
            therapists = therapistRepository.findById(request.getTherapistId())
                    .filter(Therapist::getIsActive)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            therapists = therapistRepository.findByIsActiveTrue();
        }

        if (therapists.isEmpty()) {
            return Response.builder()
                    .slotsByDay(List.of())
                    .totalSlotsFound(0)
                    .build();
        }

        // Collect busy intervals for each therapist and day
        Map<LocalDate, List<SlotDTO>> slotsByDay = new LinkedHashMap<>();
        int totalSlots = 0;

        LocalDate currentDate = request.getRangeFrom();
        while (!currentDate.isAfter(request.getRangeTo())) {
            // Skip weekends (optional - could be made configurable)
            if (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            List<SlotDTO> daySlots = new ArrayList<>();

            for (Therapist therapist : therapists) {
                // Get busy intervals for this therapist on this day
                List<TimeInterval> busyIntervals = getBusyIntervals(therapist.getId(), currentDate);

                // Find free slots
                List<TimeInterval> freeSlots = findFreeSlots(
                        busyIntervals,
                        request.getDurationMinutes(),
                        request.getDayParts());

                // Convert to DTOs
                for (TimeInterval slot : freeSlots) {
                    DayPart dayPart = determineDayPart(slot.start);

                    daySlots.add(SlotDTO.builder()
                            .therapistId(therapist.getId())
                            .therapistName(therapist.getFullName())
                            .date(currentDate)
                            .startTime(slot.start)
                            .endTime(slot.end)
                            .dayPart(dayPart)
                            .build());
                }
            }

            if (!daySlots.isEmpty()) {
                slotsByDay.put(currentDate, daySlots);
                totalSlots += daySlots.size();
            }

            currentDate = currentDate.plusDays(1);
        }

        // Convert to response format
        List<SlotGroupDTO> slotGroups = slotsByDay.entrySet().stream()
                .map(e -> SlotGroupDTO.builder()
                        .date(e.getKey())
                        .slots(e.getValue())
                        .build())
                .toList();

        return Response.builder()
                .slotsByDay(slotGroups)
                .totalSlotsFound(totalSlots)
                .build();
    }



    /**
     * Get all busy intervals for a therapist on a specific date.
     */
    private List<TimeInterval> getBusyIntervals(Long therapistId, LocalDate date) {
        List<TimeInterval> busy = new ArrayList<>();

        // 1. Add appointments
        List<Appointment> appointments = appointmentRepository.findByTherapistIdAndDateRange(
                therapistId, date, date);
        for (Appointment apt : appointments) {
            if (apt.getStatus() != AppointmentStatus.CANCELLED) {
                busy.add(new TimeInterval(
                        apt.getStartTime().toLocalTime(),
                        apt.getEndTime().toLocalTime()));
            }
        }

        // 2. Add series instances
        List<AppointmentSeries> series = seriesRepository.findActiveByTherapistIdAndDateRange(
                therapistId, date, date, SeriesStatus.ACTIVE);

        for (AppointmentSeries s : series) {
            DayOfWeek seriesDay = parseWeekday(s.getWeekday());
            if (seriesDay != null && date.getDayOfWeek().equals(seriesDay)) {
                // Check if cancelled
                boolean isCancelled = s.getCancellations() != null &&
                        s.getCancellations().stream()
                                .anyMatch(c -> c.getDate().equals(date));

                if (!isCancelled) {
                    // Check frequency
                    int freq = s.getWeeklyfrequency() != null ? s.getWeeklyfrequency() : 1;
                    long weeks = java.time.temporal.ChronoUnit.WEEKS.between(s.getStartDate(), date);
                    if (weeks >= 0 && weeks % freq == 0) {
                        busy.add(new TimeInterval(s.getStartTime(), s.getEndTime()));
                    }
                }
            }
        }

        // 3. Add absences
        // Special absences
        List<Absence> specialAbsences = absenceRepository.findSpecialByTherapistIdAndDateRange(
                therapistId, date, date, AbsenceType.SPECIAL);
        for (Absence absence : specialAbsences) {
            if (absence.getStartTime() != null && absence.getEndTime() != null) {
                busy.add(new TimeInterval(
                        absence.getStartTime(),
                        absence.getEndTime()));
            } else {
                // Full day absence
                busy.add(new TimeInterval(WORK_START, WORK_END));
            }
        }

        // Recurring absences
        List<Absence> recurringAbsences = absenceRepository.findRecurringByTherapistId(
                therapistId, AbsenceType.RECURRING);
        for (Absence absence : recurringAbsences) {
            DayOfWeek absenceDay = parseWeekday(absence.getWeekday());
            if (absenceDay != null && date.getDayOfWeek().equals(absenceDay)) {
                if (absence.getStartTime() != null && absence.getEndTime() != null) {
                    busy.add(new TimeInterval(
                            absence.getStartTime(),
                            absence.getEndTime()));
                } else {
                    // Full day absence
                    busy.add(new TimeInterval(WORK_START, WORK_END));
                }
            }
        }

        // Sort and merge overlapping intervals
        return mergeIntervals(busy);
    }

    /**
     * Find free slots given busy intervals.
     */
    private List<TimeInterval> findFreeSlots(
            List<TimeInterval> busyIntervals,
            int durationMinutes,
            List<DayPart> allowedDayParts) {

        List<TimeInterval> freeSlots = new ArrayList<>();

        // Invert busy intervals to get free intervals
        List<TimeInterval> freeIntervals = invertIntervals(busyIntervals, WORK_START, WORK_END);

        // Slice free intervals into slots of required duration
        for (TimeInterval free : freeIntervals) {
            LocalTime slotStart = free.start;
            while (slotStart.plusMinutes(durationMinutes).compareTo(free.end) <= 0) {
                LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);

                // Check day part filter
                DayPart dayPart = determineDayPart(slotStart);
                if (allowedDayParts == null || allowedDayParts.isEmpty() ||
                    allowedDayParts.contains(dayPart)) {
                    freeSlots.add(new TimeInterval(slotStart, slotEnd));
                }

                slotStart = slotStart.plusMinutes(SLOT_INCREMENT_MINUTES);
            }
        }

        return freeSlots;
    }

    /**
     * Merge overlapping intervals.
     */
    private List<TimeInterval> mergeIntervals(List<TimeInterval> intervals) {
        if (intervals.isEmpty()) return intervals;

        intervals.sort(Comparator.comparing(i -> i.start));
        List<TimeInterval> merged = new ArrayList<>();
        TimeInterval current = intervals.get(0);

        for (int i = 1; i < intervals.size(); i++) {
            TimeInterval next = intervals.get(i);
            if (next.start.compareTo(current.end) <= 0) {
                // Overlapping, extend current
                if (next.end.isAfter(current.end)) {
                    current = new TimeInterval(current.start, next.end);
                }
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }

    /**
     * Invert busy intervals to get free intervals within work hours.
     */
    private List<TimeInterval> invertIntervals(
            List<TimeInterval> busy, LocalTime workStart, LocalTime workEnd) {

        List<TimeInterval> free = new ArrayList<>();
        LocalTime current = workStart;

        for (TimeInterval b : busy) {
            if (b.start.isAfter(current)) {
                free.add(new TimeInterval(current, b.start));
            }
            if (b.end.isAfter(current)) {
                current = b.end;
            }
        }

        if (current.isBefore(workEnd)) {
            free.add(new TimeInterval(current, workEnd));
        }

        return free;
    }

    private DayPart determineDayPart(LocalTime time) {
        if (time.isBefore(LocalTime.of(12, 0))) {
            return DayPart.MORNING;
        } else if (time.isBefore(LocalTime.of(19, 30))) {
            return DayPart.AFTERNOON;
        } else {
            return DayPart.EVENING;
        }
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

    /**
     * Simple time interval record.
     */
    private record TimeInterval(LocalTime start, LocalTime end) {}
}
