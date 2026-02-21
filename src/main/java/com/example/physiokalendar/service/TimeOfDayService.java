package com.example.physiokalendar.service;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class TimeOfDayService {
    private static final Map<Integer, TimeRange> timeOfDayMap = new HashMap<>();
    private static final Map<Integer, String> timeOfDayLabels = new HashMap<>();

    static {
        // Define 4 main time slots
        timeOfDayMap.put(1, new TimeRange(LocalTime.of(7, 0), LocalTime.of(10, 0)));   // Morgens
        timeOfDayMap.put(2, new TimeRange(LocalTime.of(10, 0), LocalTime.of(13, 0))); // Vormittags
        timeOfDayMap.put(3, new TimeRange(LocalTime.of(13, 0), LocalTime.of(17, 0))); // Nachmittags
        timeOfDayMap.put(4, new TimeRange(LocalTime.of(17, 0), LocalTime.of(20, 0))); // Abends

        // Labels for UI display
        timeOfDayLabels.put(1, "Morgens (7:00 - 10:00 Uhr)");
        timeOfDayLabels.put(2, "Vormittags (10:00 - 13:00 Uhr)");
        timeOfDayLabels.put(3, "Nachmittags (13:00 - 17:00 Uhr)");
        timeOfDayLabels.put(4, "Abends (17:00 - 20:00 Uhr)");
    }

    public static LocalTime getStartTime(int id) {
        TimeRange range = timeOfDayMap.get(id);
        if (range != null) {
            return range.getStart();
        }
        throw new IllegalArgumentException("Invalid time of day ID");
    }

    public static LocalTime getEndTime(int id) {
        TimeRange range = timeOfDayMap.get(id);
        if (range != null) {
            return range.getEnd();
        }
        throw new IllegalArgumentException("Invalid time of day ID");
    }

    /**
     * Get the display label for a time of day ID.
     *
     * @param id Time of day ID (1-4)
     * @return Display label in German
     */
    public static String getLabel(int id) {
        String label = timeOfDayLabels.get(id);
        if (label != null) {
            return label;
        }
        throw new IllegalArgumentException("Invalid time of day ID");
    }

    /**
     * Get all available time of day options.
     *
     * @return Map of ID to label
     */
    public static Map<Integer, String> getAllTimeOfDayOptions() {
        return new HashMap<>(timeOfDayLabels);
    }

    private static class TimeRange {
        private final LocalTime start;
        private final LocalTime end;

        TimeRange(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        public LocalTime getStart() {
            return start;
        }

        public LocalTime getEnd() {
            return end;
        }
    }
}
