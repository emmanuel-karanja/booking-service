package com.example.booking.exceptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {

        super(message);
    }
}
