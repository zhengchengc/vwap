package com.exercise.vwap.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDateTime;

@Getter
public class Trade {
    @NotNull(message = "Timestamp must not be null")
    private final LocalDateTime timestamp;

    @NotNull(message = "Currency pair must not be null")
    @Pattern(
            regexp = "[A-Z]{3}/[A-Z]{3}",
            message = "Currency pair must be in format XXX/YYY where X and Y are capital letters"
    )
    private final String currencyPair;

    @Positive(message = "Price must be positive")
    private final double price;

    @Positive(message = "Volume must be positive")
    private final double volume;

    public Trade(LocalDateTime timestamp, String currencyPair, double price, double volume) {
        validateInputs(timestamp, currencyPair, price, volume);
        this.timestamp = timestamp;
        this.currencyPair = currencyPair;
        this.price = price;
        this.volume = volume;
    }

    private void validateInputs(LocalDateTime timestamp, String currencyPair, double price, double volume) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp must not be null");
        }
        if (currencyPair == null || currencyPair.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency pair must not be null or empty");
        }
        if (!currencyPair.matches("[A-Z]{3}/[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Invalid currency pair format. Expected format: XXX/YYY, got: " + currencyPair);
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be positive, got: " + price);
        }
        if (volume <= 0) {
            throw new IllegalArgumentException("Volume must be positive, got: " + volume);
        }
    }
}
