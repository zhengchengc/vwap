package com.exercise.vwap.service.strategy.impl;

import com.exercise.vwap.domain.TradeWindow;
import com.exercise.vwap.service.strategy.VwapStrategy;
import org.springframework.stereotype.Component;

@Component
public class SimpleVwapStrategy implements VwapStrategy {
    @Override
    public double calculateVwap(TradeWindow window) {
        return window.getSumVolume().doubleValue() == 0.0 ? 0 :
                window.getSumPriceVolume().doubleValue() / window.getSumVolume().doubleValue();
    }
}
