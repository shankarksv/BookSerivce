package com.book.bookservice.pricing;

import java.math.BigDecimal;
import java.util.List;

public class PricingService {

    public static final BigDecimal BOOK_PRICE = new BigDecimal("50.00");

    public BigDecimal calculateTotal(List<BasketItemLine> items) {
        int totalQuantity = items.stream()
                .map(BasketItemLine::quantity)
                .reduce(0, Integer::sum);

        return BOOK_PRICE.multiply(BigDecimal.valueOf(totalQuantity));
    }
}
