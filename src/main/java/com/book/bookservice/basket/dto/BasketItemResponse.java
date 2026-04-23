package com.book.bookservice.basket.dto;

public record BasketItemResponse(
        Long bookId,
        String title,
        Integer quantity
) {
}
