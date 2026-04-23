package com.book.bookservice.pricing.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryDiscountPolicyProvider implements DiscountPolicyProvider {

    private final Map<Integer, BigDecimal> activeDiscounts;

    public InMemoryDiscountPolicyProvider() {
        this(Map.of(
                2, new BigDecimal("0.05"),
                3, new BigDecimal("0.10"),
                4, new BigDecimal("0.20"),
                5, new BigDecimal("0.25")
        ));
    }

    public InMemoryDiscountPolicyProvider(Map<Integer, BigDecimal> activeDiscounts) {
        this.activeDiscounts = new LinkedHashMap<>(activeDiscounts);
    }

    @Override
    public Map<Integer, BigDecimal> getActiveDiscounts() {
        return Map.copyOf(activeDiscounts);
    }
}
