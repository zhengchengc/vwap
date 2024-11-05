package com.exercise.vwap.service.impl;

import com.exercise.vwap.domain.Trade;
import com.exercise.vwap.domain.TradeWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemorySafeHourlyWindowManager Tests")
class MemorySafeHourlyWindowManagerTest {
    private MemorySafeHourlyWindowManager windowManager;
    private LocalDateTime baseTime;

    // Test-specific constants
    private static final int TEST_QUEUE_CAPACITY = 1000;
    private static final int TEST_MAX_CURRENCY_PAIRS = 3;
    private static final int TEST_WINDOW_RETENTION_HOURS = 2;

    @BeforeEach
    void setUp() {
        windowManager = new MemorySafeHourlyWindowManager(
                TEST_QUEUE_CAPACITY,
                TEST_MAX_CURRENCY_PAIRS,
                TEST_WINDOW_RETENTION_HOURS
        );
        baseTime = LocalDateTime.of(2024, 1, 1, 10, 0); // 10:00 AM
    }

    @Nested
    @DisplayName("Basic Trade Processing Tests")
    class BasicTradeProcessingTests {
        @Test
        @DisplayName("Should process single trade correctly")
        void processSingleTrade() {
            Trade trade = new Trade(baseTime, "EUR/USD", 1.1234, 1000000);
            windowManager.addTrade(trade);

            // Wait for async processing
            await().atMost(2, TimeUnit.SECONDS).until(() ->
                    windowManager.getWindow("EUR/USD", baseTime).getVwap() > 0);

            TradeWindow window = windowManager.getWindow("EUR/USD", baseTime);
            assertEquals(1.1234, window.getVwap(), 0.0001);
        }

        @Test
        @DisplayName("Should handle multiple trades in same window")
        void processMultipleTradesInSameWindow() {
            Trade trade1 = new Trade(baseTime, "EUR/USD", 1.1234, 1000000);
            Trade trade2 = new Trade(baseTime.plusMinutes(30), "EUR/USD", 1.1236, 2000000);

            windowManager.addTrade(trade1);
            windowManager.addTrade(trade2);

            await().atMost(2, TimeUnit.SECONDS).until(() -> {
                TradeWindow window = windowManager.getWindow("EUR/USD", baseTime);
                return Math.abs(window.getVwap() - 1.12353333) < 0.0001;
            });

            TradeWindow window = windowManager.getWindow("EUR/USD", baseTime);
            // Expected VWAP = (1.1234*1000000 + 1.1236*2000000)/(1000000 + 2000000)
            assertEquals(1.12353333, window.getVwap(), 0.0001);
        }
    }

    @Nested
    @DisplayName("Window Management Tests")
    class WindowManagementTests {
        @Test
        @DisplayName("Should create separate windows for different hours")
        void handleDifferentHourWindows() {
            Trade trade1 = new Trade(baseTime, "EUR/USD", 1.1234, 1000000);
            Trade trade2 = new Trade(baseTime.plusHours(1), "EUR/USD", 1.1236, 1000000);

            windowManager.addTrade(trade1);
            windowManager.addTrade(trade2);

            await().atMost(2, TimeUnit.SECONDS).until(() -> {
                TradeWindow window1 = windowManager.getWindow("EUR/USD", baseTime);
                TradeWindow window2 = windowManager.getWindow("EUR/USD", baseTime.plusHours(1));
                return window1.getVwap() > 0 && window2.getVwap() > 0;
            });

            TradeWindow window1 = windowManager.getWindow("EUR/USD", baseTime);
            TradeWindow window2 = windowManager.getWindow("EUR/USD", baseTime.plusHours(1));

            assertEquals(1.1234, window1.getVwap(), 0.0001);
            assertEquals(1.1236, window2.getVwap(), 0.0001);
        }

        @Test
        @DisplayName("Should handle trades from different currency pairs")
        void handleDifferentCurrencyPairs() {
            Trade trade1 = new Trade(baseTime, "EUR/USD", 1.1234, 1000000);
            Trade trade2 = new Trade(baseTime, "GBP/USD", 1.3456, 1000000);

            windowManager.addTrade(trade1);
            windowManager.addTrade(trade2);

            await().atMost(2, TimeUnit.SECONDS).until(() -> {
                TradeWindow eurWindow = windowManager.getWindow("EUR/USD", baseTime);
                TradeWindow gbpWindow = windowManager.getWindow("GBP/USD", baseTime);
                return eurWindow.getVwap() > 0 && gbpWindow.getVwap() > 0;
            });

            assertEquals(1.1234, windowManager.getWindow("EUR/USD", baseTime).getVwap(), 0.0001);
            assertEquals(1.3456, windowManager.getWindow("GBP/USD", baseTime).getVwap(), 0.0001);
        }
    }

    @Nested
    @DisplayName("Memory Management Tests")
    class MemoryManagementTests {
        @Test
        @DisplayName("Should enforce maximum currency pairs limit")
        void enforceMaxCurrencyPairs() {
            // Add trades for 4 currency pairs (max is 3)
            String[] pairs = {"EUR/USD", "GBP/USD", "AUD/USD", "USD/JPY"};
            for (String pair : pairs) {
                windowManager.addTrade(new Trade(baseTime, pair, 1.0, 1000000));
            }

            await().atMost(2, TimeUnit.SECONDS).until(() ->
                    windowManager.getWindowCounts().size() <= 3);

            assertTrue(windowManager.getWindowCounts().size() <= 3);
        }

        @Test
        @DisplayName("Should cleanup expired windows")
        void cleanupExpiredWindows() {
            // Add trades for different times
            windowManager.addTrade(new Trade(baseTime, "EUR/USD", 1.0, 1000000));
            windowManager.addTrade(new Trade(baseTime.plusHours(3), "EUR/USD", 1.0, 1000000));

            // Trigger cleanup
            windowManager.cleanupExpiredWindows(baseTime.plusHours(4));

            Map<String, Integer> windowCounts = windowManager.getWindowCounts();
            assertTrue(windowCounts.get("EUR/USD") <= 1);
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {
        @Test
        @DisplayName("Should handle concurrent trade processing")
        void handleConcurrentTrades() throws InterruptedException {
            int numThreads = 10;
            int tradesPerThread = 100;
            ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);

            for (int i = 0; i < numThreads; i++) {
                executorService.submit(() -> {
                    try {
                        for (int j = 0; j < tradesPerThread; j++) {
                            Trade trade = new Trade(baseTime, "EUR/USD", 1.1234, 1000000);
                            windowManager.addTrade(trade);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS));

            await().atMost(5, TimeUnit.SECONDS).until(() -> {
                TradeWindow window = windowManager.getWindow("EUR/USD", baseTime);
                return window.getVwap() > 0;
            });

            TradeWindow window = windowManager.getWindow("EUR/USD", baseTime);
            assertEquals(1.1234, window.getVwap(), 0.0001);
        }

        @Test
        @DisplayName("Should handle concurrent access to different currency pairs")
        void handleConcurrentCurrencyPairs() throws InterruptedException {
            int numPairs = 10;
            List<String> pairs = new ArrayList<>();
            for (int i = 0; i < numPairs; i++) {
                pairs.add("PAIR" + i + "/USD");
            }

            ExecutorService executorService = Executors.newFixedThreadPool(numPairs);
            CountDownLatch latch = new CountDownLatch(numPairs);

            for (String pair : pairs) {
                executorService.submit(() -> {
                    try {
                        Trade trade = new Trade(baseTime, pair, 1.0, 1000000);
                        windowManager.addTrade(trade);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertTrue(windowManager.getWindowCounts().size() <= 3); // Max pairs is 3
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        @Test
        @DisplayName("Should handle trades at window boundaries")
        void handleWindowBoundaries() {
            Trade trade1 = new Trade(baseTime.withMinute(59).withSecond(59), "EUR/USD", 1.0, 1000000);
            Trade trade2 = new Trade(baseTime.plusHours(1).withMinute(0).withSecond(0), "EUR/USD", 2.0, 1000000);

            windowManager.addTrade(trade1);
            windowManager.addTrade(trade2);

            await().atMost(2, TimeUnit.SECONDS).until(() -> {
                TradeWindow window1 = windowManager.getWindow("EUR/USD", baseTime);
                TradeWindow window2 = windowManager.getWindow("EUR/USD", baseTime.plusHours(1));
                return window1.getVwap() > 0 && window2.getVwap() > 0;
            });

            assertNotEquals(
                    windowManager.getWindow("EUR/USD", baseTime).getVwap(),
                    windowManager.getWindow("EUR/USD", baseTime.plusHours(1)).getVwap()
            );
        }
    }
}