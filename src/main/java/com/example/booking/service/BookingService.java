package com.example.booking.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class BookingService implements IBookingService {

    private final EventBus _eventBus;

    public BookingService(Vertx vertx) {
        _eventBus = vertx.eventBus();
    }

    public Future<JsonObject> create(
            Long guestId,
            Long listingId,
            JsonObject body) {

        JsonObject request = body.copy()
                .put("guestId", guestId)
                .put("listingId", listingId);

        return _eventBus.<JsonObject>request(
                        "booking.overlap.check",
                        request
                )
                .compose(overlapCheck ->
                        _eventBus.<JsonObject>request(
                                "booking.create",
                                request
                        )
                )
                .map(Message::body);
    }

    public Future<JsonObject> findByHost(Long hostId) {

        return _eventBus.<JsonObject>request(
                "booking.find.by.host",
                new JsonObject()
                        .put("hostId", hostId)
        ).map(Message::body);
    }

    public Future<JsonObject> findByGuest(Long guestId) {

        return _eventBus.<JsonObject>request(
                "booking.find.by.guest",
                new JsonObject()
                        .put("guestId", guestId)
        ).map(Message::body);
    }

    public Future<JsonObject> findById(Long bookingId) {

        return _eventBus.<JsonObject>request(
                "booking.find.by.id",
                new JsonObject()
                        .put("bookingId", bookingId)
        ).map(Message::body);
    }

    public Future<Void> cancel(
            Long bookingId,
            Long guestId) {

        JsonObject request = new JsonObject()
                .put("bookingId", bookingId)
                .put("guestId", guestId);

        return _eventBus.<JsonObject>request(
                "booking.cancel",
                request
        ).mapEmpty();
    }

    public Future<JsonObject> findByListing(
            Long listingId,
            Long hostId) {

        return _eventBus.<JsonObject>request(
                "booking.find.by.listing",
                new JsonObject()
                        .put("listingId", listingId)
                        .put("hostId", hostId)
        ).map(Message::body);
    }
}