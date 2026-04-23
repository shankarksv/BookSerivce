package com.book.bookservice.pricing.service;

import com.book.bookservice.pricing.dto.BasketItemLine;
import com.book.bookservice.pricing.service.DiscountPolicyProvider;
import com.book.bookservice.pricing.service.InMemoryDiscountPolicyProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PricingService {

    public static final BigDecimal BOOK_PRICE = new BigDecimal("50.00");
    private static final int EXACT_SOLVER_TOTAL_BOOK_THRESHOLD = 200;

    private final DiscountPolicyProvider discountPolicyProvider;

    public PricingService() {
        this(new InMemoryDiscountPolicyProvider());
    }

    public PricingService(Map<Integer, BigDecimal> discountByDistinctBooks) {
        this(new InMemoryDiscountPolicyProvider(discountByDistinctBooks));
    }

    public PricingService(DiscountPolicyProvider discountPolicyProvider) {
        this.discountPolicyProvider = discountPolicyProvider;
    }

    public BigDecimal calculateTotal(List<BasketItemLine> items) {
        validateItems(items);

        Map<Long, Integer> quantitiesByBook = items.stream()
                .collect(Collectors.toMap(
                        BasketItemLine::bookId,
                        BasketItemLine::quantity,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<Integer, BigDecimal> activeDiscounts = discountPolicyProvider.getActiveDiscounts();
        BigDecimal total;
        if (shouldUseExactSolver(quantitiesByBook)) {
            total = calculateOptimalTotal(quantitiesByBook, activeDiscounts);
        } else {
            total = calculateLayeredTotal(quantitiesByBook, activeDiscounts);
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

    private boolean shouldUseExactSolver(Map<Long, Integer> quantitiesByBook) {
        int totalBooks = quantitiesByBook.values().stream().mapToInt(Integer::intValue).sum();
        return totalBooks <= EXACT_SOLVER_TOTAL_BOOK_THRESHOLD;
    }

    private BigDecimal calculateOptimalTotal(Map<Long, Integer> quantitiesByBook, Map<Integer, BigDecimal> activeDiscounts) {
        int[] counts = quantitiesByBook.values().stream()
                .mapToInt(Integer::intValue)
                .filter(quantity -> quantity > 0)
                .boxed()
                .sorted((left, right) -> Integer.compare(right, left))
                .mapToInt(Integer::intValue)
                .toArray();

        return calculateOptimalTotal(counts, activeDiscounts, new HashMap<>());
    }

    private BigDecimal calculateOptimalTotal(int[] counts, Map<Integer, BigDecimal> activeDiscounts, Map<String, BigDecimal> memo) {
        int[] normalized = normalizeCounts(counts);
        if (normalized.length == 0) {
            return BigDecimal.ZERO;
        }

        String key = Arrays.toString(normalized) + "|" + discountKey(activeDiscounts);
        BigDecimal cached = memo.get(key);
        if (cached != null) {
            return cached;
        }

        BigDecimal bestTotal = null;
        int distinctBooks = normalized.length;

        for (int groupSize = 1; groupSize <= distinctBooks; groupSize++) {
            List<int[]> nextStates = new ArrayList<>();
            generateNextStates(normalized, groupSize, 0, new int[groupSize], 0, nextStates);

            for (int[] nextState : nextStates) {
                BigDecimal candidate = calculateGroupPrice(groupSize, activeDiscounts)
                        .add(calculateOptimalTotal(nextState, activeDiscounts, memo));
                if (bestTotal == null || candidate.compareTo(bestTotal) < 0) {
                    bestTotal = candidate;
                }
            }
        }

        memo.put(key, bestTotal);
        return bestTotal;
    }

    private void generateNextStates(int[] counts, int groupSize, int startIndex, int[] selectedIndices, int depth, List<int[]> nextStates) {
        if (depth == groupSize) {
            int[] nextState = Arrays.copyOf(counts, counts.length);
            for (int index : selectedIndices) {
                nextState[index]--;
            }
            nextStates.add(normalizeCounts(nextState));
            return;
        }

        for (int i = startIndex; i <= counts.length - (groupSize - depth); i++) {
            if (counts[i] <= 0) {
                continue;
            }

            selectedIndices[depth] = i;
            generateNextStates(counts, groupSize, i + 1, selectedIndices, depth + 1, nextStates);
        }
    }

    private int[] normalizeCounts(int[] counts) {
        return Arrays.stream(counts)
                .filter(quantity -> quantity > 0)
                .boxed()
                .sorted((left, right) -> Integer.compare(right, left))
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private String discountKey(Map<Integer, BigDecimal> activeDiscounts) {
        return activeDiscounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue().toPlainString())
                .collect(Collectors.joining(","));
    }

    private BigDecimal calculateGroupPrice(int distinctBooksInGroup, Map<Integer, BigDecimal> activeDiscounts) {
        BigDecimal basePrice = BOOK_PRICE.multiply(BigDecimal.valueOf(distinctBooksInGroup));
        BigDecimal discountPercentage = activeDiscounts.getOrDefault(distinctBooksInGroup, BigDecimal.ZERO);

        return basePrice.subtract(basePrice.multiply(discountPercentage));
    }

    private BigDecimal calculateLayeredTotal(Map<Long, Integer> quantitiesByBook, Map<Integer, BigDecimal> activeDiscounts) {
        List<Integer> remainingCounts = new ArrayList<>(quantitiesByBook.values());
        remainingCounts.sort(Integer::compareTo);

        Map<Integer, Integer> groupCountBySize = new LinkedHashMap<>();
        while (!remainingCounts.isEmpty()) {
            int distinctBooksInGroup = remainingCounts.size();
            int batchCount = remainingCounts.get(0);
            groupCountBySize.merge(distinctBooksInGroup, batchCount, Integer::sum);

            List<Integer> nextCounts = new ArrayList<>();
            for (int count : remainingCounts) {
                int reduced = count - batchCount;
                if (reduced > 0) {
                    nextCounts.add(reduced);
                }
            }
            remainingCounts = nextCounts;
        }

        rebalanceForOptimalDiscount(groupCountBySize, activeDiscounts);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> entry : groupCountBySize.entrySet()) {
            BigDecimal groupedPrice = calculateGroupPrice(entry.getKey(), activeDiscounts)
                    .multiply(BigDecimal.valueOf(entry.getValue()));
            total = total.add(groupedPrice);
        }

        return total;
    }

    private void rebalanceForOptimalDiscount(Map<Integer, Integer> groupCountBySize, Map<Integer, BigDecimal> activeDiscounts) {
        BigDecimal costFivePlusThree = calculateGroupPrice(5, activeDiscounts).add(calculateGroupPrice(3, activeDiscounts));
        BigDecimal costFourPlusFour = calculateGroupPrice(4, activeDiscounts).multiply(BigDecimal.valueOf(2));
        if (costFourPlusFour.compareTo(costFivePlusThree) >= 0) {
            return;
        }

        int fiveBookGroups = groupCountBySize.getOrDefault(5, 0);
        int threeBookGroups = groupCountBySize.getOrDefault(3, 0);
        int rebalanceCount = Math.min(fiveBookGroups, threeBookGroups);

        if (rebalanceCount == 0) {
            return;
        }

        groupCountBySize.put(5, fiveBookGroups - rebalanceCount);
        groupCountBySize.put(3, threeBookGroups - rebalanceCount);
        groupCountBySize.merge(4, rebalanceCount * 2, Integer::sum);
    }
}
