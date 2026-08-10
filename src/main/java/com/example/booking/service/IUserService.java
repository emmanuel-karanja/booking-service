package com.example.booking.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface IUserService {

    Future<JsonObject> register(
            JsonObject body);

    Future<JsonObject> findByEmail(
            String email);

    Future<JsonObject> findById(
            Long userId);

    Future<JsonObject> getUsers();

    Future<JsonObject> login(
            JsonObject body);

    Future<JsonObject> health();
}