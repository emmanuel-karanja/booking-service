package com.example.booking.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListingService implements IListingService {

    private final EventBus _eventBus;

    private static final Logger logger= LoggerFactory.getLogger(ListingService.class);

    public ListingService(Vertx vertx) {
        _eventBus = vertx.eventBus();
    }

    public Future<JsonObject> findAll() {

        return _eventBus.<JsonObject>request(
                "listing.find.all",
                new JsonObject()
        ).map(Message::body);
    }

    public Future<JsonObject> findById(Long listingId) {

        return _eventBus.<JsonObject>request(
                "listing.find.by.id",
                new JsonObject()
                        .put("listingId", listingId)
        ).map(Message::body);
    }

    public Future<JsonObject> create(
            Long hostId,
            JsonObject body) {

        JsonObject request = body.copy()
                .put("hostId", hostId);

        System.out.println("Creating a booking:"+request.toString());

        return _eventBus.<JsonObject>request(
                "listing.create",
                request
        ).map(Message::body);
    }

    public Future<JsonObject> update(
            Long listingId,
            Long hostId,
            JsonObject body) {

        JsonObject request = body.copy()
                .put("listingId", listingId)
                .put("hostId", hostId);

        return _eventBus.<JsonObject>request(
                "listing.update",
                request
        ).map(Message::body);
    }

    public Future<Void> delete(
            Long listingId,
            Long hostId) {

        JsonObject request = new JsonObject()
                .put("listingId", listingId)
                .put("hostId", hostId);

        return _eventBus.<JsonObject>request(
                "listing.delete",
                request
        ).mapEmpty();
    }

    public Future<JsonObject> health() {

        return _eventBus.<JsonObject>request(
                "db.health.check",
                new JsonObject()
        ).map(Message::body);
    }
}