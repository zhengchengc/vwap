package com.exercise.vwap.utils;

import java.time.LocalDateTime;

public class TimeParser {

    private static final int MIN_HOUR = 1;
    private static final int MAX_HOUR = 12;
    private static final int MIN_MINUTE = 0;
    private static final int MAX_MINUTE = 59;

    public static LocalDateTime parse(String timeStamp) {
        if (timeStamp == null || timeStamp.trim().isEmpty()) {
            throw new IllegalArgumentException("Time stamp cannot be null or empty");
        }

        String[] parts = timeStamp.split("[\\s:]+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid time format: " + timeStamp);
        }

        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String period = parts[2].toUpperCase();

            validateTimeComponents(hour, minute, period, timeStamp);

            return createDateTime(hour, minute, period);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time format: " + timeStamp);
        }
    }

    private static void validateTimeComponents(int hour, int minute, String period, String timeStamp) {
        if (hour < MIN_HOUR || hour > MAX_HOUR) {
            throw new IllegalArgumentException("Hour must be between 1 and 12: " + timeStamp);
        }
        if (minute < MIN_MINUTE || minute > MAX_MINUTE) {
            throw new IllegalArgumentException("Minute must be between 0 and 59: " + timeStamp);
        }
        if (!period.equals("AM") && !period.equals("PM")) {
            throw new IllegalArgumentException("Period must be AM or PM: " + timeStamp);
        }
    }

    private static LocalDateTime createDateTime(int hour, int minute, String period) {
        // Convert to 24-hour format
        int hour24 = switch (period) {
            case "AM" -> hour == 12 ? 0 : hour;
            case "PM" -> hour == 12 ? 12 : hour + 12;
            default -> throw new IllegalArgumentException("Invalid period: " + period);
        };

        return LocalDateTime.of(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getDayOfMonth(),
                hour24,
                minute
        );
    }
}
