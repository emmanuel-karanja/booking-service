package com.example.booking.middleware;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;

public class JwtHandler {

    private final JWTAuthHandler handler;

    public JwtHandler(JWTAuth jwtAuth) {
        this.handler = JWTAuthHandler.create(jwtAuth);
    }

    public void handle(RoutingContext ctx) {
        handler.handle(ctx);
    }
}