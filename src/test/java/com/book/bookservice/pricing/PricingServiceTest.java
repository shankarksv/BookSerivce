package com.book.bookservice.pricing;

import com.book.bookservice.pricing.dto.BasketItemLine;
import com.book.bookservice.pricing.service.PricingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldCalculateSingleBookAtFiftyEur() {
        BigDecimal total = pricingService.calculateTotal(List.of(
                new BasketItemLine(1L, 1)));

        assertThat(total).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldCalculateMultipleBooksWithoutDiscountsYet() {
        BigDecimal total = pricingService.calculateTotal(List.of(
                new BasketItemLine(1L, 2),
                new BasketItemLine(2L, 1)));

        assertThat(total).isEqualByComparingTo(new BigDecimal("145.00"));
    }

    @Test
    void shouldApplyFivePercentDiscountForTwoDistinctBooks() {
        BigDecimal total = pricingService.calculateTotal(List.of(
                new BasketItemLine(1L, 1),
                new BasketItemLine(2L, 1)));

        assertThat(total).isEqualByComparingTo("95.00");
    }

    @Test
    void shouldRejectZeroQuantity() {
        assertThatThrownBy(() -> pricingService.calculateTotal(List.of(
                new BasketItemLine(1L, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThatThrownBy(() -> pricingService.calculateTotal(List.of(
                new BasketItemLine(1L, -1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void shouldReturnZeroForEmptyBasket() {
        BigDecimal total = pricingService.calculateTotal(List.of());

        assertThat(total).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRejectNullQuantityEntryInBasket() {
        assertThatThrownBy(() -> pricingService.calculateTotal(List.of(
                new BasketItemLine(null, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bookId");
    }

    @Test
    void shouldRejectDuplicateBookLinesForSameBookId() {
        assertThatThrownBy(() -> pricingService.calculateTotal(List.of(
                new BasketItemLine(1L, 1),
                new BasketItemLine(1L, 2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void shouldUseCustomDiscountTableForSameBasket() {
        PricingService customPricingService = new PricingService(
                java.util.Map.of(
                        2, new BigDecimal("0.05"),
                        3, new BigDecimal("0.10"),
                        4, new BigDecimal("0.30"),
                        5, new BigDecimal("0.25")
                )
        );

        BigDecimal total = customPricingService.calculateTotal(List.of(
                new BasketItemLine(1L, 2),
                new BasketItemLine(2L, 2),
                new BasketItemLine(3L, 2),
                new BasketItemLine(4L, 1),
                new BasketItemLine(5L, 1)
        ));

        assertThat(total).isEqualByComparingTo("280.00");
    }

    @Test
    void shouldFallBackToZeroDiscountWhenGroupSizeIsMissing() {
        PricingService customPricingService = new PricingService(
                java.util.Map.of()
        );

        BigDecimal total = customPricingService.calculateTotal(List.of(
                new BasketItemLine(1L, 1),
                new BasketItemLine(2L, 1),
                new BasketItemLine(3L, 1)
        ));

        assertThat(total).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldProduceDifferentTotalsForDifferentDiscountTables() {
        PricingService defaultPricingService = new PricingService();
        PricingService customPricingService = new PricingService(
                java.util.Map.of(
                        2, new BigDecimal("0.05"),
                        3, new BigDecimal("0.10"),
                        4, new BigDecimal("0.10"),
                        5, new BigDecimal("0.25")
                )
        );

        List<BasketItemLine> basket = List.of(
                new BasketItemLine(1L, 1),
                new BasketItemLine(2L, 1),
                new BasketItemLine(3L, 1),
                new BasketItemLine(4L, 1)
        );

        BigDecimal defaultTotal = defaultPricingService.calculateTotal(basket);
        BigDecimal customTotal = customPricingService.calculateTotal(basket);

        assertThat(defaultTotal).isEqualByComparingTo("160.00");
        assertThat(customTotal).isEqualByComparingTo("180.00");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pricingCasesFromJson")
    void TestCalculatePriceFromJsonInput(PricingTestCase testCase) {
        PricingService effectivePricingService = testCase.discountTable() == null
                ? pricingService
                : new PricingService(testCase.discountTable().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> Integer.parseInt(entry.getKey()),
                        java.util.Map.Entry::getValue
                )));

        List<BasketItemLine> items = testCase.items().stream()
                .map(item -> new BasketItemLine(item.bookId(), item.quantity()))
                .toList();

        BigDecimal total = effectivePricingService.calculateTotal(items);

        assertThat(total).isEqualByComparingTo(testCase.expectedTotal());
    }

    static Stream<PricingTestCase> pricingCasesFromJson() {
        try (InputStream inputStream = PricingServiceTest.class.getResourceAsStream(
                "/pricing/pricing-test-cases.json")) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing test resource: /pricing/pricing-test-cases.json");
            }

            List<PricingTestCase> testCases = OBJECT_MAPPER.readValue(
                    inputStream,
                    new TypeReference<List<PricingTestCase>>() {
                    });

            return testCases.stream();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read pricing test cases from JSON", exception);
        }
    }

    record PricingTestCase(String name, List<PricingInputItem> items, String expectedTotal,
                           java.util.Map<String, BigDecimal> discountTable) {
        @Override
        public String toString() {
            return name;
        }
    }

    record PricingInputItem(Long bookId, int quantity) {
    }
}
