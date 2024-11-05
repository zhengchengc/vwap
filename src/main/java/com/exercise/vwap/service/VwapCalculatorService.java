package com.exercise.vwap.service;

import java.time.LocalDateTime;

import com.exercise.vwap.domain.Trade;
import com.exercise.vwap.domain.VwapResult;

public interface VwapCalculatorService {
  void processTrade(Trade trade);
  VwapResult getVwap(String currencyPair, LocalDateTime timestamp);
}
