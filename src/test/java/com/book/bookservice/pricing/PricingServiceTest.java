package com.book.bookservice.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    void shouldCalculateSingleBookAtFiftyEur() {
        BigDecimal total = pricingService.calculateTotal(List.of(
                new BasketItemLine(1L, 1)
        ));

        assertThat(total).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldCalculateMultipleBooksWithoutDiscountsYet() {
        BigDecimal total = pricingService.calculateTotal(List.of(
                new BasketItemLine(1L, 2),
                new BasketItemLine(2L, 1)
        ));

        assertThat(total).isEqualByComparingTo(new BigDecimal("150.00"));
    }
}
