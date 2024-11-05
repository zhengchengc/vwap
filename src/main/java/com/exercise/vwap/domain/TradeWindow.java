package com.exercise.vwap.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AtomicDouble;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Getter
@Slf4j
public class TradeWindow {
  private final LocalDateTime windowStart;
  private final AtomicDouble sumPriceVolume = new AtomicDouble(0.0);
  private final AtomicDouble sumVolume = new AtomicDouble(0.0);
  private final AtomicInteger tradeCount = new AtomicInteger(0);

  private static final int MAX_TRADES_PER_WINDOW = 1_000_000;

  public void addTrade(Trade trade) {
    if (tradeCount.incrementAndGet() <= MAX_TRADES_PER_WINDOW) {
      sumPriceVolume.addAndGet(trade.getPrice() * trade.getVolume());
      sumVolume.addAndGet(trade.getVolume());
    } else {
      tradeCount.decrementAndGet();
      log.warn("Maximum trades per window reached for window starting at: {}", windowStart);
    }
  }

  public double getVwap() {
    double volume = sumVolume.get();
    return volume == 0 ? 0.0 : sumPriceVolume.get() / volume;
  }

  public boolean isExpired(LocalDateTime currentTime) {
    return ChronoUnit.HOURS.between(windowStart, currentTime) >= 1;
  }

  public void merge(TradeWindow other) {
    sumPriceVolume.addAndGet(other.sumPriceVolume.get());
    sumVolume.addAndGet(other.sumVolume.get());
    tradeCount.addAndGet(other.tradeCount.get());
  }
}
