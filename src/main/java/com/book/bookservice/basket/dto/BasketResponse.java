package com.book.bookservice.basket.dto;

import java.util.List;

public record BasketResponse(
        Long userId,
        String status,
        List<BasketItemResponse> items
) {
}
