package com.example.booking.model;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Booking(
        Long id,
        Long listingId,
        Long guestId,
        @NotBlank
        LocalDate startDate,
        @NotBlank
        LocalDate endDate,
        BookingStatus status,
        OffsetDateTime createdAt
) { }