package com.exercise.vwap.service.impl;

import com.exercise.vwap.domain.Trade;
import com.exercise.vwap.domain.TradeWindow;
import com.exercise.vwap.service.WindowManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

@Service
@Slf4j
public class MemorySafeHourlyWindowManager implements WindowManager {
    private static final int DEFAULT_QUEUE_CAPACITY = 10000;
    private static final int DEFAULT_MAX_CURRENCY_PAIRS = 1000;
    private static final int DEFAULT_WINDOW_RETENTION_HOURS = 24;
    private static final int HOUR_IN_MINUTES = 60;
    private static final ChronoUnit WINDOW_UNIT = ChronoUnit.HOURS;
    private static final long WINDOW_SIZE = 1; // 1 hour windows

    // Main storage using LRU cache for currency pairs
    private final Map<String, Map<LocalDateTime, TradeWindow>> currencyPairWindows;
    private final BlockingQueue<Trade> incomingTradeQueue;
    private final ExecutorService processExecutor;

    private final int queueCapacity;
    private final int maxCurrencyPairs;
    private final int windowRetentionHours;

    public MemorySafeHourlyWindowManager() {
        this(DEFAULT_QUEUE_CAPACITY, DEFAULT_MAX_CURRENCY_PAIRS, DEFAULT_WINDOW_RETENTION_HOURS);
    }

    public MemorySafeHourlyWindowManager(int queueCapacity, int maxCurrencyPairs, int windowRetentionHours) {
        this.queueCapacity = queueCapacity;
        this.maxCurrencyPairs = maxCurrencyPairs;
        this.windowRetentionHours = windowRetentionHours;

        this.currencyPairWindows = Collections.synchronizedMap(
                new LinkedHashMap<String, Map<LocalDateTime, TradeWindow>>(
                        maxCurrencyPairs + 1, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Map<LocalDateTime, TradeWindow>> eldest) {
                        boolean shouldRemove = size() > maxCurrencyPairs;
                        if (shouldRemove) {
                            log.warn("Removing least recently used currency pair: {}", eldest.getKey());
                        }
                        return shouldRemove;
                    }
                });

        this.incomingTradeQueue = new ArrayBlockingQueue<>(queueCapacity);
        this.processExecutor = new ThreadPoolExecutor(
                2, 4,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        startTradeProcessor();
    }

    private LocalDateTime getWindowStart(LocalDateTime timestamp) {
        return timestamp.truncatedTo(WINDOW_UNIT);
    }

    private boolean isWindowExpired(LocalDateTime windowStart, LocalDateTime currentTime) {
        return WINDOW_UNIT.between(windowStart, currentTime) >= windowRetentionHours;
    }

    private boolean isWithinCurrentWindow(LocalDateTime tradeTime, LocalDateTime windowStart) {
        return WINDOW_UNIT.between(windowStart, tradeTime) < WINDOW_SIZE;
    }

    private void startTradeProcessor() {
        Thread processor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Trade trade = incomingTradeQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (trade != null) {
                        processTradeInternal(trade);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error processing trade", e);
                }
            }
        });
        processor.setName("hourly-trade-processor");
        processor.setDaemon(true);
        processor.start();
    }

    @Override
    public void addTrade(Trade trade) {
        if (!incomingTradeQueue.offer(trade)) {
            log.warn("Trade queue full, processing in calling thread for {}",
                    trade.getCurrencyPair());
            processTradeInternal(trade);
        }
    }

    private void processTradeInternal(Trade trade) {
        try {
            LocalDateTime windowStart = getWindowStart(trade.getTimestamp());

            // Validate if the trade belongs to the current window
            if (!isWithinCurrentWindow(trade.getTimestamp(), windowStart)) {
                log.warn("Trade timestamp outside current window: {}", trade);
                return;
            }

            Map<LocalDateTime, TradeWindow> windows = currencyPairWindows
                    .computeIfAbsent(trade.getCurrencyPair(), k ->
                            Collections.synchronizedMap(new ConcurrentHashMap<>()));

            TradeWindow window = windows.computeIfAbsent(windowStart,
                    k -> new TradeWindow(windowStart));

            window.addTrade(trade);
        } catch (Exception e) {
            log.error("Error processing trade: {}", trade, e);
        }
    }

    @Override
    public TradeWindow getWindow(String currencyPair, LocalDateTime timestamp) {
        LocalDateTime windowStart = getWindowStart(timestamp);

        Map<LocalDateTime, TradeWindow> windows = currencyPairWindows.get(currencyPair);
        if (windows != null) {
            TradeWindow window = windows.get(windowStart);
            if (window != null) {
                // Update LRU by accessing the currency pair
                currencyPairWindows.get(currencyPair);
                return window;
            }
        }

        return new TradeWindow(windowStart);
    }

    @Override
    @Scheduled(fixedRate = HOUR_IN_MINUTES * 1000) // Run hourly
    public void cleanupExpiredWindows(LocalDateTime currentTime) {
        try {
            int totalWindowsBefore = countTotalWindows();
            int pairsBefore = currencyPairWindows.size();

            currencyPairWindows.forEach((pair, windows) -> {
                // Remove expired windows
                windows.entrySet().removeIf(entry ->
                        isWindowExpired(entry.getValue().getWindowStart(), currentTime)
                );
            });

            // Remove empty currency pairs
            currencyPairWindows.entrySet().removeIf(entry -> entry.getValue().isEmpty());

            int totalWindowsAfter = countTotalWindows();
            int pairsAfter = currencyPairWindows.size();

            log.info("Hourly cleanup completed - Removed {} windows and {} currency pairs",
                    totalWindowsBefore - totalWindowsAfter,
                    pairsBefore - pairsAfter);

            logMemoryStatus();
        } catch (Exception e) {
            log.error("Error during hourly cleanup", e);
        }
    }

    private int countTotalWindows() {
        return currencyPairWindows.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    private void logMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;

        log.info("Memory usage: {}MB / {}MB, Active currency pairs: {}, Queue size: {}",
                usedMemory, maxMemory,
                currencyPairWindows.size(),
                incomingTradeQueue.size());
    }

    // For monitoring
    public Map<String, Integer> getWindowCounts() {
        Map<String, Integer> counts = new HashMap<>();
        currencyPairWindows.forEach((pair, windows) ->
                counts.put(pair, windows.size()));
        return counts;
    }

    public int getQueueSize() {
        return incomingTradeQueue.size();
    }

    public int getActiveThreads() {
        return ((ThreadPoolExecutor) processExecutor).getActiveCount();
    }
}
