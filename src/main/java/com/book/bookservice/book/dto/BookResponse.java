package com.book.bookservice.book.dto;

import java.math.BigDecimal;

public record BookResponse(
        Long id,
        String title,
        String author,
        Integer year,
        String description,
        BigDecimal price
) {
}
