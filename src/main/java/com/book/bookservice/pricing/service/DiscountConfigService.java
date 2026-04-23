package com.book.bookservice.pricing.service;

import com.book.bookservice.pricing.entity.DiscountConfig;
import com.book.bookservice.pricing.repository.DiscountConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DiscountConfigService implements DiscountPolicyProvider {

    private final DiscountConfigRepository discountConfigRepository;

    public DiscountConfigService(DiscountConfigRepository discountConfigRepository) {
        this.discountConfigRepository = discountConfigRepository;
    }

    @Override
    public Map<Integer, BigDecimal> getActiveDiscounts() {
        Map<Integer, BigDecimal> activeDiscounts = new LinkedHashMap<>();

        for (DiscountConfig config : discountConfigRepository.findByActiveTrueOrderByGroupSizeAsc()) {
            activeDiscounts.put(config.getGroupSize(), config.getDiscountPercentage());
        }

        return activeDiscounts;
    }
}
