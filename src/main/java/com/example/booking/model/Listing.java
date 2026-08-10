package com.example.booking.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Listing(
        Long id,
        Long hostId,
        @NotBlank
        String title,
        @NotBlank
        String description,
        @NotBlank
        String location,
        @Min(1)
        BigDecimal price,
        OffsetDateTime createdAt
) {
}