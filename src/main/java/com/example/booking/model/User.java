package com.example.booking.model;

import jakarta.validation.constraints.NotEmpty;

import java.time.OffsetDateTime;

public record User(
        Long id,
        @NotEmpty
        String email,
        @NotEmpty
        String password,
        String passwordHash,
        Role role,
        OffsetDateTime createdAt
) {
}