package com.exercise.vwap.controller.v1;

import com.exercise.vwap.domain.TradeInput;
import com.exercise.vwap.service.VwapCalculatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VwapController.class)
class VwapControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VwapCalculatorService vwapCalculator;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testProcessTradesArray() throws Exception {
        // Sample trade data as per specification
        Object[][] trades = {
                {"9:30 AM", "AUD/USD", "0.6905", "106,198"},
                {"9:31 AM", "USD/JPY", "142.497", "30,995"}
        };

        mockMvc.perform(post("/api/v1/vwap/trades")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Arrays.asList(trades))))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessStructuredTrades() throws Exception {
        TradeInput trade = new TradeInput(
                "9:30 AM",
                "AUD/USD",
                0.6905,
                106198
        );

        mockMvc.perform(post("/api/v1/vwap/trades/structured")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Arrays.asList(trade))))
                .andExpect(status().isOk());
    }

    @Test
    void testGetVwap() throws Exception {
        mockMvc.perform(get("/api/v1/vwap/pair/AUD/USD")
                        .param("timestamp", "9:30 AM"))
                .andExpect(status().isOk());
    }

    @Test
    void testInvalidTradeData() throws Exception {
        Object[][] invalidTrades = {
                {"9:30 AM", "AUD/USD", "invalid", "106,198"}
        };

        mockMvc.perform(post("/api/v1/vwap/trades")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Arrays.asList(invalidTrades))))
                .andExpect(status().isOk()); // Should still return OK but log error
    }
}