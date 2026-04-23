package com.book.bookservice.pricing;

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

        assertThat(total).isEqualByComparingTo(new BigDecimal("150.00"));
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("pricingCasesFromJson")
    void TestCalculatePriceFromJsonInput(PricingTestCase testCase) {
        List<BasketItemLine> items = testCase.items().stream()
                .map(item -> new BasketItemLine(item.bookId(), item.quantity()))
                .toList();

        BigDecimal total = pricingService.calculateTotal(items);

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

    record PricingTestCase(String name, List<PricingInputItem> items, String expectedTotal) {
        @Override
        public String toString() {
            return name;
        }
    }

    record PricingInputItem(Long bookId, int quantity) {
    }
}
