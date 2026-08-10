package com.example.booking.middleware;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class RoleHandler implements Handler<RoutingContext> {

    private final String requiredRole;

    public RoleHandler(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (ctx.user() == null) {
            ctx.response()
                    .setStatusCode(401)
                    .end();
            return;
        }

        String role = ctx.user()
                .principal()
                .getString("role");

        if (!requiredRole.equals(role)) {
            ctx.response()
                    .setStatusCode(403)
                    .end();
            return;
        }

        ctx.next();
    }
}