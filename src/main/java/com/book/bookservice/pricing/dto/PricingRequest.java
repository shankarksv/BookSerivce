package com.book.bookservice.pricing.dto;

import jakarta.validation.constraints.NotNull;

public record PricingRequest(
        @NotNull Long userId
) {
}
