package com.example.physiokalendar.service;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class TimeOfDayService {
    private static final Map<Integer, TimeRange> timeOfDayMap = new HashMap<>();

    static {
        timeOfDayMap.put(1, new TimeRange(LocalTime.of(7, 0), LocalTime.of(10, 0)));  // Morgen
        timeOfDayMap.put(2, new TimeRange(LocalTime.of(10, 0), LocalTime.of(12, 0))); // Spätvormittag
        timeOfDayMap.put(3, new TimeRange(LocalTime.of(12, 0), LocalTime.of(15, 0))); // Mittag
        timeOfDayMap.put(4, new TimeRange(LocalTime.of(15, 0), LocalTime.of(18, 0))); // Nachmittag
        timeOfDayMap.put(5, new TimeRange(LocalTime.of(18, 0), LocalTime.of(20, 0))); // Abend
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
