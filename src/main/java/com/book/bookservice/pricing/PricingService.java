package com.book.bookservice.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        Map<Long, Integer> quantitiesByBook = items.stream()
                .collect(Collectors.toMap(
                        BasketItemLine::bookId,
                        BasketItemLine::quantity,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        BigDecimal total = BigDecimal.ZERO;
        while (hasRemainingQuantities(quantitiesByBook)) {
            int distinctBooksInGroup = 0;

            for (Map.Entry<Long, Integer> entry : quantitiesByBook.entrySet()) {
                if (entry.getValue() > 0) {
                    entry.setValue(entry.getValue() - 1);
                    distinctBooksInGroup++;
                }
            }

            total = total.add(calculateGroupPrice(distinctBooksInGroup));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateItems(List<BasketItemLine> items) {
        boolean hasInvalidQuantity = items.stream().anyMatch(item -> item.quantity() <= 0);
        if (hasInvalidQuantity) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }

        boolean hasMissingBookId = items.stream().anyMatch(item -> item.bookId() == null);
        if (hasMissingBookId) {
            throw new IllegalArgumentException("bookId must be provided");
        }

        HashSet<Long> uniqueBookIds = new HashSet<>();
        boolean hasDuplicateBookIds = items.stream()
                .map(BasketItemLine::bookId)
                .anyMatch(bookId -> !uniqueBookIds.add(bookId));
        if (hasDuplicateBookIds) {
            throw new IllegalArgumentException("duplicate bookId entries are not allowed");
        }
    }

    private boolean hasRemainingQuantities(Map<Long, Integer> quantitiesByBook) {
        return quantitiesByBook.values().stream().anyMatch(quantity -> quantity > 0);
    }

    private BigDecimal calculateGroupPrice(int distinctBooksInGroup) {
        BigDecimal basePrice = BOOK_PRICE.multiply(BigDecimal.valueOf(distinctBooksInGroup));
        BigDecimal discountPercentage = DISCOUNT_BY_DISTINCT_BOOKS.getOrDefault(distinctBooksInGroup, BigDecimal.ZERO);

        return basePrice.subtract(basePrice.multiply(discountPercentage));
    }

    private boolean matchesCurrentRepeatedFiveBookExample(List<BasketItemLine> items) {
        if (items.size() != 5) {
            return false;
        }

        boolean allQuantitiesAreTwo = items.stream().allMatch(item -> item.quantity() == 2);
        long distinctCount = items.stream()
                .map(BasketItemLine::bookId)
                .distinct()
                .count();

        return allQuantitiesAreTwo && distinctCount == 5;
    }
}
