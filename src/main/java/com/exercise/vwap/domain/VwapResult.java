package com.exercise.vwap.domain;

import java.time.LocalDateTime;

import lombok.Value;

@Value
public class VwapResult {
  String currencyPair;
  LocalDateTime windowStart;
  double vwap;
}
