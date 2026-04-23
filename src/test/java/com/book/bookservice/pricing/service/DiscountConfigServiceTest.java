package com.book.bookservice.pricing.service;

import com.book.bookservice.pricing.entity.DiscountConfig;
import com.book.bookservice.pricing.repository.DiscountConfigRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DataJpaTest
@Import(DiscountConfigService.class)
class DiscountConfigServiceTest {

    @Autowired
    private DiscountConfigRepository discountConfigRepository;

    @Autowired
    private DiscountConfigService discountConfigService;

    @Autowired
    private DiscountPolicyProvider discountPolicyProvider;

    @Test
    void shouldLoadDefaultActiveDiscountsFromDatabase() {
        assertThat(discountPolicyProvider.getActiveDiscounts()).containsExactly(
                entry(2, new BigDecimal("0.0500")),
                entry(3, new BigDecimal("0.1000")),
                entry(4, new BigDecimal("0.2000")),
                entry(5, new BigDecimal("0.2500"))
        );
    }

    @Test
    void shouldReturnOnlyActiveDiscountsOrderedByGroupSize() {
        discountConfigRepository.deleteAllInBatch();
        discountConfigRepository.flush();
        discountConfigRepository.save(new DiscountConfig(4, new BigDecimal("0.3000"), true));
        discountConfigRepository.save(new DiscountConfig(2, new BigDecimal("0.0700"), true));
        discountConfigRepository.save(new DiscountConfig(3, new BigDecimal("0.1500"), false));

        Map<Integer, BigDecimal> activeDiscounts = discountConfigService.getActiveDiscounts();

        assertThat(activeDiscounts).containsExactly(
                entry(2, new BigDecimal("0.0700")),
                entry(4, new BigDecimal("0.3000"))
        );
    }
}
