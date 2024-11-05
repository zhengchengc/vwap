package com.exercise.vwap.service.impl;

import com.exercise.vwap.domain.Trade;
import com.exercise.vwap.domain.TradeWindow;
import com.exercise.vwap.domain.VwapResult;
import com.exercise.vwap.service.VwapCalculatorService;
import com.exercise.vwap.service.WindowManager;
import com.exercise.vwap.service.strategy.VwapStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VwapCalculatorService Tests")
class VwapCalculatorServiceImplTest {
    @Mock
    private WindowManager windowManager;

    @Mock
    private VwapStrategy vwapStrategy;

    private VwapCalculatorService vwapCalculatorService;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        vwapCalculatorService = new VwapCalculatorServiceImpl(windowManager, vwapStrategy);
        baseTime = LocalDateTime.of(2024, 1, 1, 10, 30); // 10:30 AM
    }

    @Nested
    @DisplayName("VWAP Input Validation Tests")
    class VwapInputValidationTests {
        @Test
        @DisplayName("Should throw exception for null currency pair in getVwap")
        void shouldThrowExceptionForNullCurrencyPairInGetVwap() {
            assertThrows(IllegalArgumentException.class,
                    () -> vwapCalculatorService.getVwap(null, baseTime));
        }

        @Test
        @DisplayName("Should throw exception for empty currency pair in getVwap")
        void shouldThrowExceptionForEmptyCurrencyPairInGetVwap() {
            assertThrows(IllegalArgumentException.class,
                    () -> vwapCalculatorService.getVwap("  ", baseTime));
        }

        @Test
        @DisplayName("Should throw exception for null timestamp in getVwap")
        void shouldThrowExceptionForNullTimestampInGetVwap() {
            assertThrows(IllegalArgumentException.class,
                    () -> vwapCalculatorService.getVwap("EUR/USD", null));
        }
    }

    @Nested
    @DisplayName("Valid Trade Processing Tests")
    class ValidTradeProcessingTests {
        @Test
        @DisplayName("Should process valid trade successfully")
        void shouldProcessValidTrade() {
            Trade trade = new Trade(baseTime, "EUR/USD", 1.1234, 1000000);
            vwapCalculatorService.processTrade(trade);
            verify(windowManager).addTrade(trade);
        }

        @Test
        @DisplayName("Should handle valid VWAP request successfully")
        void shouldHandleValidVwapRequest() {
            TradeWindow mockWindow = mock(TradeWindow.class);
            when(windowManager.getWindow("EUR/USD", baseTime)).thenReturn(mockWindow);
            when(vwapStrategy.calculateVwap(mockWindow)).thenReturn(1.1234);

            VwapResult result = vwapCalculatorService.getVwap("EUR/USD", baseTime);

            assertNotNull(result);
            assertEquals("EUR/USD", result.getCurrencyPair());
            assertEquals(1.1234, result.getVwap());
        }
    }

    @Nested
    @DisplayName("Trade Processing Tests")
    class TradeProcessingTests {
        @Test
        @DisplayName("Should delegate trade processing to window manager")
        void shouldDelegateTradeProcessing() {
            // Arrange
            Trade trade = new Trade(baseTime, "EUR/USD", 1.1234, 1000000);

            // Act
            vwapCalculatorService.processTrade(trade);

            // Assert
            verify(windowManager).addTrade(trade);
            verifyNoMoreInteractions(vwapStrategy); // Strategy shouldn't be involved in processing
        }
    }

    @Nested
    @DisplayName("VWAP Calculation Tests")
    class VwapCalculationTests {
        @Test
        @DisplayName("Should calculate VWAP correctly")
        void shouldCalculateVwap() {
            // Arrange
            String currencyPair = "EUR/USD";
            TradeWindow mockWindow = mock(TradeWindow.class);
            double expectedVwap = 1.1234;

            when(windowManager.getWindow(currencyPair, baseTime)).thenReturn(mockWindow);
            when(vwapStrategy.calculateVwap(mockWindow)).thenReturn(expectedVwap);

            // Act
            VwapResult result = vwapCalculatorService.getVwap(currencyPair, baseTime);

            // Assert
            assertNotNull(result);
            assertEquals(currencyPair, result.getCurrencyPair());
            assertEquals(baseTime.truncatedTo(ChronoUnit.HOURS), result.getWindowStart());
            assertEquals(expectedVwap, result.getVwap());

            verify(windowManager).getWindow(currencyPair, baseTime);
            verify(vwapStrategy).calculateVwap(mockWindow);
        }

        @Test
        @DisplayName("Should handle empty window")
        void shouldHandleEmptyWindow() {
            // Arrange
            String currencyPair = "EUR/USD";
            TradeWindow emptyWindow = mock(TradeWindow.class);

            when(windowManager.getWindow(currencyPair, baseTime)).thenReturn(emptyWindow);
            when(vwapStrategy.calculateVwap(emptyWindow)).thenReturn(0.0);

            // Act
            VwapResult result = vwapCalculatorService.getVwap(currencyPair, baseTime);

            // Assert
            assertNotNull(result);
            assertEquals(0.0, result.getVwap());
        }

        @Test
        @DisplayName("Should truncate timestamp to hour")
        void shouldTruncateTimestamp() {
            // Arrange
            String currencyPair = "EUR/USD";
            LocalDateTime time = baseTime.withMinute(45).withSecond(30);
            LocalDateTime expectedWindowStart = time.truncatedTo(ChronoUnit.HOURS);
            TradeWindow mockWindow = mock(TradeWindow.class);

            when(windowManager.getWindow(currencyPair, time)).thenReturn(mockWindow);
            when(vwapStrategy.calculateVwap(mockWindow)).thenReturn(1.0);

            // Act
            VwapResult result = vwapCalculatorService.getVwap(currencyPair, time);

            // Assert
            assertEquals(expectedWindowStart, result.getWindowStart());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        @Test
        @DisplayName("Should handle null currency pair")
        void shouldHandleNullCurrencyPair() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    vwapCalculatorService.getVwap(null, baseTime));

            assertEquals("Currency pair must not be null or empty", exception.getMessage());
            verifyNoInteractions(windowManager);
            verifyNoInteractions(vwapStrategy);
        }

        @Test
        @DisplayName("Should handle null timestamp")
        void shouldHandleNullTimestamp() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    vwapCalculatorService.getVwap("EUR/USD", null));

            assertEquals("Timestamp must not be null", exception.getMessage());
            verifyNoInteractions(windowManager);
            verifyNoInteractions(vwapStrategy);
        }

        @Test
        @DisplayName("Should handle window manager exceptions")
        void shouldHandleWindowManagerException() {
            // Arrange
            String currencyPair = "EUR/USD";
            when(windowManager.getWindow(currencyPair, baseTime))
                    .thenThrow(new RuntimeException("Window manager error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    vwapCalculatorService.getVwap(currencyPair, baseTime));
        }

        @Test
        @DisplayName("Should handle strategy calculation exceptions")
        void shouldHandleStrategyException() {
            // Arrange
            String currencyPair = "EUR/USD";
            TradeWindow mockWindow = mock(TradeWindow.class);

            when(windowManager.getWindow(currencyPair, baseTime)).thenReturn(mockWindow);
            when(vwapStrategy.calculateVwap(mockWindow))
                    .thenThrow(new RuntimeException("Strategy calculation error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                    vwapCalculatorService.getVwap(currencyPair, baseTime));
        }

        @Test
        @DisplayName("Should handle empty currency pair")
        void shouldHandleEmptyCurrencyPair() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    vwapCalculatorService.getVwap("  ", baseTime));

            assertEquals("Currency pair must not be null or empty", exception.getMessage());
            verifyNoInteractions(windowManager);
            verifyNoInteractions(vwapStrategy);
        }

        @Test
        @DisplayName("Should handle invalid currency pair format")
        void shouldHandleInvalidCurrencyPairFormat() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    vwapCalculatorService.getVwap("EURUSD", baseTime));

            assertTrue(exception.getMessage().startsWith("Invalid currency pair format"));
            verifyNoInteractions(windowManager);
            verifyNoInteractions(vwapStrategy);
        }
    }

    @Nested
    @DisplayName("Integration Scenario Tests")
    class IntegrationScenarioTests {
        @Test
        @DisplayName("Should handle full workflow correctly")
        void shouldHandleFullWorkflow() {
            // Arrange
            String currencyPair = "EUR/USD";
            Trade trade = new Trade(baseTime, currencyPair, 1.1234, 1000000);
            TradeWindow mockWindow = mock(TradeWindow.class);
            double expectedVwap = 1.1234;

            when(windowManager.getWindow(currencyPair, baseTime)).thenReturn(mockWindow);
            when(vwapStrategy.calculateVwap(mockWindow)).thenReturn(expectedVwap);

            // Act
            vwapCalculatorService.processTrade(trade);
            VwapResult result = vwapCalculatorService.getVwap(currencyPair, baseTime);

            // Assert
            verify(windowManager).addTrade(trade);
            verify(windowManager).getWindow(currencyPair, baseTime);
            verify(vwapStrategy).calculateVwap(mockWindow);

            assertEquals(expectedVwap, result.getVwap());
        }

        @Test
        @DisplayName("Should maintain consistency across multiple trades")
        void shouldMaintainConsistency() {
            // Arrange
            String currencyPair = "EUR/USD";
            TradeWindow mockWindow = mock(TradeWindow.class);
            double expectedVwap = 1.1235;

            when(windowManager.getWindow(currencyPair, baseTime)).thenReturn(mockWindow);
            when(vwapStrategy.calculateVwap(mockWindow)).thenReturn(expectedVwap);

            // Act
            for (int i = 0; i < 5; i++) {
                Trade trade = new Trade(baseTime, currencyPair, 1.1234 + i * 0.0001, 1000000);
                vwapCalculatorService.processTrade(trade);
            }
            VwapResult result = vwapCalculatorService.getVwap(currencyPair, baseTime);

            // Assert
            verify(windowManager, times(5)).addTrade(any(Trade.class));
            assertEquals(expectedVwap, result.getVwap());
        }
    }
}