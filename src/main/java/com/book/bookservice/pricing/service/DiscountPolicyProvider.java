package com.book.bookservice.pricing.service;

import java.math.BigDecimal;
import java.util.Map;

public interface DiscountPolicyProvider {

    Map<Integer, BigDecimal> getActiveDiscounts();
}
