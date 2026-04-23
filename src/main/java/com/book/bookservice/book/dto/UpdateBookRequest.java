package com.book.bookservice.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateBookRequest(
        @NotBlank String title,
        @NotBlank String author,
        @NotNull @Positive Integer year,
        String description
) {
}
