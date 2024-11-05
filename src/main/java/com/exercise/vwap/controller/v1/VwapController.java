package com.exercise.vwap.controller.v1;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.exercise.vwap.domain.Trade;
import com.exercise.vwap.domain.TradeInput;
import com.exercise.vwap.domain.VwapResult;
import com.exercise.vwap.service.VwapCalculatorService;
import com.exercise.vwap.utils.TimeParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/vwap")
@RequiredArgsConstructor
@Slf4j
public class VwapController {

    private final VwapCalculatorService vwapCalculator;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a");

    @PostMapping("/trades")
    public ResponseEntity<Void> processTrades(@RequestBody List<Object[]> trades) {
        log.info("Processing {} trades", trades.size());
        try {
            for (Object[] tradeData : trades) {
                if (tradeData.length != 4) {
                    log.error("Invalid trade data format: {}", (Object)tradeData);
                    continue;
                }

                try {
                    LocalDateTime timestamp = TimeParser.parse(tradeData[0].toString());

                    // Parse currency pair
                    String currencyPair = tradeData[1].toString();

                    // Parse price and volume
                    double price = Double.parseDouble(tradeData[2].toString()
                            .replace(",", ""));
                    double volume = Double.parseDouble(tradeData[3].toString()
                            .replace(",", ""));

                    Trade trade = new Trade(timestamp, currencyPair, price, volume);
                    log.info("Processing trade: {}", trade);
                    vwapCalculator.processTrade(trade);
                    log.info("Finished processing trades");
                } catch (Exception e) {
                    log.error("Error processing trade data: {}", (Object)tradeData, e);
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing trades batch", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Alternative endpoint for structured input
    @PostMapping("/trades/structured")
    public ResponseEntity<Void> processStructuredTrades(@RequestBody List<TradeInput> trades) {
        try {
            trades.forEach(tradeInput -> {
                try {
                    LocalDateTime timestamp = TimeParser.parse(tradeInput.getTimestamp());

                    Trade trade = new Trade(
                            timestamp,
                            tradeInput.getCurrencyPair(),
                            tradeInput.getPrice(),
                            tradeInput.getVolume()
                    );
                    vwapCalculator.processTrade(trade);
                } catch (Exception e) {
                    log.error("Error processing trade: {}", tradeInput, e);
                }
            });
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing structured trades batch", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/pair/{base}/{quote}")
    public ResponseEntity<VwapResult> getVwap(
            @PathVariable String base,
            @PathVariable String quote,
            @RequestParam(required = false) String timestamp) {
        try {
            String currencyPair = base + "/" + quote;
            LocalDateTime time = timestamp != null ?
                    TimeParser.parse(timestamp) :
                    LocalDateTime.now();

            VwapResult vwap = vwapCalculator.getVwap(currencyPair, time);
            return ResponseEntity.ok(vwap);
        } catch (Exception e) {
            log.error("Error getting VWAP for {}/{}", base, quote, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Alternative endpoint using encoded currency pair
    @GetMapping(value = "/pair", params = "currencyPair")
    public ResponseEntity<VwapResult> getVwapByPair(
            @RequestParam String currencyPair,
            @RequestParam(required = false) String timestamp) {
        try {
            String decodedPair = URLDecoder.decode(currencyPair, StandardCharsets.UTF_8);
            LocalDateTime time = timestamp != null ?
                    TimeParser.parse(timestamp) :
                    LocalDateTime.now();

            VwapResult vwap = vwapCalculator.getVwap(decodedPair, time);
            return ResponseEntity.ok(vwap);
        } catch (Exception e) {
            log.error("Error getting VWAP for {}", currencyPair, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
