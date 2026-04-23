package com.book.bookservice.basket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddBasketItemRequest(
        @NotNull Long bookId,
        @NotNull @Positive Integer quantity
) {
}
