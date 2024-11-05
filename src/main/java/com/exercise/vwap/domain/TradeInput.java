package com.exercise.vwap.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeInput {
  private String timestamp;    // "9:30 AM" format
  private String currencyPair; // "EUR/USD" format
  private double price;        // 1.1234 format
  private double volume;       // 100000 format
}
