package com.exercise.vwap.service.impl;

import com.exercise.vwap.domain.Trade;
import com.exercise.vwap.domain.VwapResult;
import com.exercise.vwap.service.VwapCalculatorService;
import com.exercise.vwap.service.strategy.VwapStrategy;
import com.exercise.vwap.service.WindowManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class VwapCalculatorServiceImpl implements VwapCalculatorService {
    private final WindowManager windowManager;
    private final VwapStrategy vwapStrategy;

    @Autowired
    public VwapCalculatorServiceImpl(WindowManager windowManager, VwapStrategy vwapStrategy) {
        this.windowManager = windowManager;
        this.vwapStrategy = vwapStrategy;
    }


    @Override
    public void processTrade(Trade trade) {
        validateTrade(trade);
        log.debug("Processing trade: {}", trade);
        windowManager.addTrade(trade);
    }

    @Override
    public VwapResult getVwap(String currencyPair, LocalDateTime timestamp) {
        validateVwapInput(currencyPair, timestamp);
        var window = windowManager.getWindow(currencyPair, timestamp);
        var vwap = vwapStrategy.calculateVwap(window);
        return new VwapResult(currencyPair, timestamp.truncatedTo(ChronoUnit.HOURS), vwap);
    }

    private void validateTrade(Trade trade) {
        if (trade == null) {
            throw new IllegalArgumentException("Trade must not be null");
        }
        if (trade.getTimestamp() == null) {
            throw new IllegalArgumentException("Trade timestamp must not be null");
        }
        if (trade.getCurrencyPair() == null || trade.getCurrencyPair().trim().isEmpty()) {
            throw new IllegalArgumentException("Trade currency pair must not be null or empty");
        }
        if (trade.getPrice() <= 0) {
            throw new IllegalArgumentException("Trade price must be positive, got: " + trade.getPrice());
        }
        if (trade.getVolume() <= 0) {
            throw new IllegalArgumentException("Trade volume must be positive, got: " + trade.getVolume());
        }
        validateCurrencyPairFormat(trade.getCurrencyPair());
    }

    private void validateVwapInput(String currencyPair, LocalDateTime timestamp) {
        if (currencyPair == null || currencyPair.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency pair must not be null or empty");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp must not be null");
        }
        validateCurrencyPairFormat(currencyPair);
    }

    private void validateCurrencyPairFormat(String currencyPair) {
        // Basic currency pair format validation (e.g., "EUR/USD")
        if (!currencyPair.matches("[A-Z]{3}/[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Invalid currency pair format. Expected format: XXX/YYY, got: " + currencyPair);
        }
    }
}
