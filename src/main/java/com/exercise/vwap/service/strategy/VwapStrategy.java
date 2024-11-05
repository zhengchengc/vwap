package com.exercise.vwap.service.strategy;

import com.exercise.vwap.domain.TradeWindow;

public interface VwapStrategy {
  double calculateVwap(TradeWindow window);
}
