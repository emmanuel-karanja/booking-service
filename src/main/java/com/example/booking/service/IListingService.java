package com.example.booking.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface IListingService {

    Future<JsonObject> findAll();

    Future<JsonObject> findById(Long listingId);

    Future<JsonObject> create(
            Long hostId,
            JsonObject body);

    Future<JsonObject> update(
            Long listingId,
            Long hostId,
            JsonObject body);

    Future<Void> delete(
            Long listingId,
            Long hostId);

    Future<JsonObject> health();
}