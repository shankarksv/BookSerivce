package com.book.bookservice.basket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateBasketItemRequest(
        @NotNull @Positive Integer quantity
) {
}
