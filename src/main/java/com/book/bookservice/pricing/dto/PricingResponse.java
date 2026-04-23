package com.book.bookservice.pricing.dto;

import java.math.BigDecimal;
import java.util.List;

public record PricingResponse(
        BigDecimal totalPrice,
        List<DiscountBreakdownItem> discountBreakdown,
        Invoice invoice
) {
    public record DiscountBreakdownItem(
            Integer groupSize,
            Integer groupCount,
            BigDecimal discountPercentage
    ) {
    }

    public record Invoice(
            Long userId,
            List<InvoiceItem> items
    ) {
    }

    public record InvoiceItem(
            Long bookId,
            String title,
            Integer quantity
    ) {
    }
}
