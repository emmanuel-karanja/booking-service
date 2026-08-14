package com.example.booking.config;

public record JwtConfig(
        String secret,
        String algorithm,
        int expiresInMinutes
)
{}