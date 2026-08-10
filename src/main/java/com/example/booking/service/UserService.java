package com.example.booking.service;

import com.example.booking.exceptions.NotFoundException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserService implements IUserService {

    private final EventBus _eventBus;
    private final JWTAuth _jwtAuth;

    public UserService(Vertx vertx, JWTAuth jwtAuth) {
        _eventBus = vertx.eventBus();
        _jwtAuth = jwtAuth;
    }

    public Future<JsonObject> register(JsonObject body) {
        String password = body.getString("password");
        String passwordHash = hashPassword(password);

        JsonObject user = new JsonObject()
                .put("email", body.getString("email"))
                .put("passwordHash", passwordHash)
                .put("role", body.getString("role"));

        return _eventBus.<JsonObject>request("user.create", user)
                .map(Message::body);
    }

    public Future<JsonObject> findByEmail(String email) {
        Promise resPromise= Promise.promise();

        _eventBus.<JsonObject>request(
                "user.find.by.email",
                new JsonObject().put("email", email)
        ).onSuccess(resp->{
            //
            resPromise.complete(resp.body());
        }).onFailure(err->{
            throw new NotFoundException("User not found in the db");
        });

         return resPromise.future();
    }

    public Future<JsonObject> findById(Long userId) {
        return _eventBus.<JsonObject>request(
                "user.find.by.id",
                new JsonObject().put("userId", userId)
        ).map(Message::body);
    }

    public Future<JsonObject> getUsers() {
        return _eventBus.<JsonObject>request(
                "user.find.all",
                new JsonObject()
        ).map(Message::body);
    }

    public Future<JsonObject> login(JsonObject body) {
        String email = body.getString("email");
        String password = body.getString("password");

        return findByEmail(email)
                .compose(user -> {
            String storedPasswordHash = user.getString("passwordHash");
            String passwordHash = hashPassword(password);

            if (!storedPasswordHash.equals(passwordHash)) {
                return Future.failedFuture(
                        new IllegalArgumentException("Invalid email or password")
                );
            }

            String token = _jwtAuth.generateToken(
                    new JsonObject()
                            .put("sub", user.getLong("id").toString())
                            .put("role", user.getString("role"))
            );

            return Future.succeededFuture(
                    new JsonObject()
                            .put("token", token)
                            .put("userId", user.getLong("id"))
                            .put("email", user.getString("email"))
                            .put("role", user.getString("role"))
            );
        });
    }

    public Future<JsonObject> health() {
        return _eventBus.<JsonObject>request(
                "db.health.check",
                new JsonObject()
        ).map(Message::body);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}