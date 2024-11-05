package com.exercise.vwap.service;

import java.time.LocalDateTime;

import com.exercise.vwap.domain.Trade;
import com.exercise.vwap.domain.TradeWindow;

public interface WindowManager {
  void addTrade(Trade trade);
  TradeWindow getWindow(String currencyPair, LocalDateTime timestamp);
  void cleanupExpiredWindows(LocalDateTime currentTime);
}
