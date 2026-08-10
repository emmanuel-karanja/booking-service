package com.example.booking.config;

public record DatabaseConfig(
        String host,
        int port,
        String database,
        String user,
        String password,
        int poolSize
) {}