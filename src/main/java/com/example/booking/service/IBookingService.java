package com.example.booking.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface IBookingService {

    Future<JsonObject> create(
            Long guestId,
            Long listingId,
            JsonObject body);

    Future<JsonObject> findByGuest(
            Long guestId);

    Future<JsonObject> findById(
            Long bookingId);

    Future<Void> cancel(
            Long bookingId,
            Long guestId);

    Future<JsonObject> findByListing(
            Long listingId,
            Long hostId);

    Future<JsonObject> findByHost(Long hostId);
}