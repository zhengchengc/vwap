package com.exercise.vwap.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.LocalDateTime;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TimeParser Tests")
class TimeParserTest {

    @Nested
    @DisplayName("Valid Time Format Tests")
    class ValidTimeFormatTests {
        @Test
        @DisplayName("Should parse AM time correctly")
        void shouldParseAMTimeCorrectly() {
            LocalDateTime result = TimeParser.parse("9:30 AM");
            LocalDateTime expected = LocalDateTime.now()
                    .with(LocalDateTime.of(
                            LocalDate.now().getYear(),
                            LocalDate.now().getMonth(),
                            LocalDate.now().getDayOfMonth(),
                            9,
                            30,
                            0,
                            0).toLocalTime());

            assertEquals(expected.getHour(), result.getHour());
            assertEquals(expected.getMinute(), result.getMinute());
            assertEquals(expected.toLocalDate(), result.toLocalDate());
        }

        @Test
        @DisplayName("Should parse PM time correctly")
        void shouldParsePMTimeCorrectly() {
            LocalDateTime result = TimeParser.parse("2:30 PM");
            LocalDateTime expected = LocalDateTime.now()
                    .with(LocalDateTime.of(
                            LocalDate.now().getYear(),
                            LocalDate.now().getMonth(),
                            LocalDate.now().getDayOfMonth(),
                            14,
                            30,
                            0,
                            0).toLocalTime());

            assertEquals(expected.getHour(), result.getHour());
            assertEquals(expected.getMinute(), result.getMinute());
            assertEquals(expected.toLocalDate(), result.toLocalDate());
        }

        @Test
        @DisplayName("Should parse 12 AM correctly")
        void shouldParse12AMCorrectly() {
            LocalDateTime result = TimeParser.parse("12:00 AM");

            assertEquals(0, result.getHour());
            assertEquals(0, result.getMinute());
        }

        @Test
        @DisplayName("Should parse 12 PM correctly")
        void shouldParse12PMCorrectly() {
            LocalDateTime result = TimeParser.parse("12:00 PM");

            assertEquals(12, result.getHour());
            assertEquals(0, result.getMinute());
        }

        @Test
        @DisplayName("Should parse single digit hours correctly")
        void shouldParseSingleDigitHoursCorrectly() {
            LocalDateTime result = TimeParser.parse("9:05 AM");

            assertEquals(9, result.getHour());
            assertEquals(5, result.getMinute());
        }

        @Test
        @DisplayName("Should parse double digit hours correctly")
        void shouldParseDoubleDigitHoursCorrectly() {
            LocalDateTime result = TimeParser.parse("11:59 AM");

            assertEquals(11, result.getHour());
            assertEquals(59, result.getMinute());
        }
    }

    @Nested
    @DisplayName("Invalid Time Format Tests")
    class InvalidTimeFormatTests {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
                " ",
                "  ",
                "\t",
                "\n"
        })
        @DisplayName("Should throw exception for null or empty input")
        void shouldThrowExceptionForNullOrEmptyInput(String input) {
            assertThrows(IllegalArgumentException.class, () ->
                    TimeParser.parse(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "9:30",            // Missing AM/PM
                "9:30AM",          // Missing space
                "9:30:00 AM",      // Extra seconds
                "9-30 AM",         // Wrong separator
                "930 AM",          // Missing separator
                "9:3O AM",         // Letter O instead of zero
                "nine:30 AM",      // Text instead of number
                ":30 AM",          // Missing hour
                "9: AM",           // Missing minute
                "09:30AM PM"       // Multiple periods
        })
        @DisplayName("Should throw exception for invalid time format")
        void shouldThrowExceptionForInvalidTimeFormat(String invalidTime) {
            Exception exception = assertThrows(IllegalArgumentException.class, () ->
                    TimeParser.parse(invalidTime));
            assertTrue(exception.getMessage().contains("Invalid time format"));
        }

        @Test
        @DisplayName("Should throw exception for out of range hours in AM")
        void shouldThrowExceptionForOutOfRangeHoursAM() {
            assertThrows(IllegalArgumentException.class, () ->
                    TimeParser.parse("13:00 AM"));
        }

        @Test
        @DisplayName("Should throw exception for out of range hours in PM")
        void shouldThrowExceptionForOutOfRangeHoursPM() {
            assertThrows(IllegalArgumentException.class, () ->
                    TimeParser.parse("13:00 PM"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "-1:30 AM",    // Negative hour
                "9:-30 AM",    // Negative minute
                "99:30 AM",    // Hour > 12
                "9:99 AM"      // Minute > 59
        })
        @DisplayName("Should throw exception for out of range values")
        void shouldThrowExceptionForOutOfRangeValues(String invalidTime) {
            assertThrows(IllegalArgumentException.class, () ->
                    TimeParser.parse(invalidTime));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        @Test
        @DisplayName("Should handle multiple spaces between parts")
        void shouldHandleMultipleSpaces() {
            LocalDateTime result = TimeParser.parse("9:30     AM");

            assertEquals(9, result.getHour());
            assertEquals(30, result.getMinute());
        }

        @Test
        @DisplayName("Should maintain date parts from current time")
        void shouldMaintainDateParts() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime result = TimeParser.parse("9:30 AM");

            assertEquals(now.getYear(), result.getYear());
            assertEquals(now.getMonth(), result.getMonth());
            assertEquals(now.getDayOfMonth(), result.getDayOfMonth());
        }

        @Test
        @DisplayName("Should handle leading zeros in hours")
        void shouldHandleLeadingZerosInHours() {
            LocalDateTime result = TimeParser.parse("09:30 AM");

            assertEquals(9, result.getHour());
            assertEquals(30, result.getMinute());
        }

        @Test
        @DisplayName("Should handle leading zeros in minutes")
        void shouldHandleLeadingZerosInMinutes() {
            LocalDateTime result = TimeParser.parse("9:05 AM");

            assertEquals(9, result.getHour());
            assertEquals(5, result.getMinute());
        }
    }

    @Nested
    @DisplayName("Time Range Tests")
    class TimeRangeTests {
        @Test
        @DisplayName("Should correctly convert 11:59 PM")
        void shouldConvert11_59PM() {
            LocalDateTime result = TimeParser.parse("11:59 PM");
            assertEquals(23, result.getHour());
            assertEquals(59, result.getMinute());
        }

        @Test
        @DisplayName("Should correctly convert 1:00 PM")
        void shouldConvert1_00PM() {
            LocalDateTime result = TimeParser.parse("1:00 PM");
            assertEquals(13, result.getHour());
            assertEquals(0, result.getMinute());
        }
    }
}