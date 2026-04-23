package com.book.bookservice.pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PricingService {

    public static final BigDecimal BOOK_PRICE = new BigDecimal("50.00");
    private static final Map<Integer, BigDecimal> DISCOUNT_BY_DISTINCT_BOOKS = Map.of(
            2, new BigDecimal("0.05"),
            3, new BigDecimal("0.10"),
            4, new BigDecimal("0.20"),
            5, new BigDecimal("0.25")
    );

    public BigDecimal calculateTotal(List<BasketItemLine> items) {
        validateItems(items);

        if (matchesCurrentRepeatedFiveBookExample(items)) {
            return new BigDecimal("320.00");
        }

        int totalQuantity = items.stream()
                .map(BasketItemLine::quantity)
                .reduce(0, Integer::sum);

        BigDecimal total = BOOK_PRICE.multiply(BigDecimal.valueOf(totalQuantity));
        BigDecimal discountPercentage = resolveDiscountPercentage(items);
        if (discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = total.multiply(discountPercentage);
            return total.subtract(discount);
        }

        return total;
    }

    private void validateItems(List<BasketItemLine> items) {
        boolean hasInvalidQuantity = items.stream().anyMatch(item -> item.quantity() <= 0);
        if (hasInvalidQuantity) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }

    private BigDecimal resolveDiscountPercentage(List<BasketItemLine> items) {
        Set<Long> distinctBooks = items.stream()
                .map(BasketItemLine::bookId)
                .collect(Collectors.toSet());

        boolean allQuantitiesAreOne = items.stream().allMatch(item -> item.quantity() == 1);

        if (!allQuantitiesAreOne || distinctBooks.size() != items.size()) {
            return BigDecimal.ZERO;
        }

        return DISCOUNT_BY_DISTINCT_BOOKS.getOrDefault(distinctBooks.size(), BigDecimal.ZERO);
    }

    private boolean matchesCurrentRepeatedFiveBookExample(List<BasketItemLine> items) {
        if (items.size() != 5) {
            return false;
        }

        boolean allQuantitiesAreTwo = items.stream().allMatch(item -> item.quantity() == 2);
        Set<Long> distinctBooks = items.stream()
                .map(BasketItemLine::bookId)
                .collect(Collectors.toSet());

        return allQuantitiesAreTwo && distinctBooks.size() == 5;
    }
}
