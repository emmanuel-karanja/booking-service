package com.example.booking.config;

public record AppConfig(
        DatabaseConfig database,
        HttpServerConfig http,
        JwtConfig jwt
) {}