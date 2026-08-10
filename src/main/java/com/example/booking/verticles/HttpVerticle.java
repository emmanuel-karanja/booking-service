package com.example.booking.verticles;

import com.example.booking.config.AppConfig;
import com.example.booking.config.HttpServerConfig;
import com.example.booking.config.JwtConfig;
import com.example.booking.middleware.ErrorHandler;
import com.example.booking.middleware.LoggingHandler;
import com.example.booking.middleware.RoleHandler;
import com.example.booking.service.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HttpVerticle extends AbstractVerticle {

    private static final Logger _logger =
            LoggerFactory.getLogger(HttpVerticle.class);

    private IUserService _userService;
    private IListingService _listingService;
    private IBookingService _bookingService;

    private JWTAuth _jwtAuth;

    @Override
    public void start(Promise<Void> startPromise) {

        AppConfig config=config().mapTo(AppConfig.class);
        JwtConfig jwtConfig=config.jwt();
        HttpServerConfig httpConfig = config.http();

        String secret = jwtConfig.secret();

        String encodedSecret =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(secret.getBytes(StandardCharsets.UTF_8));

        JWTAuthOptions jwtOptions = new JWTAuthOptions()
                .addJwk(new JsonObject()
                        .put("kty", "oct")
                        .put("alg", "HS256")
                        .put("k", encodedSecret));

        _jwtAuth = JWTAuth.create(vertx, jwtOptions);

        // Inject services
        _userService=new UserService(vertx, _jwtAuth);
        _listingService = new ListingService(vertx);
        _bookingService = new BookingService(vertx);


        Router router = Router.router(vertx);

        // Middleware
        router.route()
                .handler(BodyHandler.create());
        router.route().handler(new LoggingHandler());

        // Public routes
        router.post("/api/auth/register")
                .handler(this::handleRegister);

        router.post("/api/auth/login")
                .handler(this::handleLogin);

        router.get("/api/health")
                .handler(this::handleHealthCheck);

        router.get("/api/listings")
                .handler(this::handleListings);

        router.get("/api/listings/:id")
                .handler(this::handleListing);

       // Host routes
        router.post("/api/listings")
                .handler(JWTAuthHandler.create(_jwtAuth))
                .handler(new RoleHandler("HOST"))
                .handler(this::handleCreateListing);

        router.put("/api/listings/:id")
                .handler(JWTAuthHandler.create(_jwtAuth))
                .handler(new RoleHandler("HOST"))
                .handler(this::handleUpdateListing);

        // Guest routes
        router.post("/api/listings/:id/bookings")
                .handler(JWTAuthHandler.create(_jwtAuth))
                .handler(new RoleHandler("GUEST"))
                .handler(this::handleCreateBooking);

        router.get("/api/bookings/me")
                .handler(JWTAuthHandler.create(_jwtAuth))
                .handler(new RoleHandler("GUEST"))
                .handler(this::handleGuestBookings);

        router.get("/api/bookings/host")
                .handler(JWTAuthHandler.create(_jwtAuth))
                .handler(new RoleHandler("HOST"))
                .handler(this::handleBookingsByHost);

        // Global Failure Handler
        router.route()
                .failureHandler(new ErrorHandler());

        // Start Http Server
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(httpConfig.port())
                .onSuccess(server -> {

                    _logger.info("HTTP server started listening on: {}", httpConfig.port());

                    startPromise.complete();
                })
                .onFailure(err -> {

                    _logger.error("Failed to start HTTP server", err);
                    startPromise.fail(err);
                });
    }

    private void handleRegister(RoutingContext ctx) {
        _userService.register(ctx.body().asJsonObject())
                .onSuccess(result -> {
                    ctx.response()
                            .setStatusCode(201)
                            .end(result.encode());
                })
                .onFailure(ctx::fail);
    }

    private void handleLogin(RoutingContext ctx) {

        _userService.login(ctx.body().asJsonObject())
                .onSuccess(ctx::json)
                .onFailure(ctx::fail);
    }

    private void handleHealthCheck(RoutingContext ctx) {

        _userService.health()
                .onSuccess(ctx::json)
                .onFailure(ctx::fail);
    }

    private void handleListings(RoutingContext ctx) {

        _listingService.findAll()
                .onSuccess(ctx::json)
                .onFailure(ctx::fail);
    }

    private void handleListing(RoutingContext ctx) {

        Long listingId =
                Long.valueOf(ctx.pathParam("id"));

        _listingService.findById(listingId)
                .onSuccess(ctx::json)
                .onFailure(ctx::fail);
    }

    private void handleCreateListing(RoutingContext ctx) {


        Long hostId = Long.valueOf(
                ctx.user()
                        .principal()
                        .getString("sub")
        );

        _listingService.create(hostId, ctx.body().asJsonObject())
                .onSuccess(result -> {
                    ctx.response()
                            .setStatusCode(201)
                            .end(result.encode());
                })
                .onFailure(ctx::fail);
    }

    private void handleUpdateListing(RoutingContext ctx) {
        Long listingId = Long.valueOf(ctx.pathParam("id"));

        Long hostId = Long.valueOf(ctx.user().principal().getString("sub"));

        _listingService.update(
                        listingId,
                        hostId,
                        ctx.body().asJsonObject()
                )
                .onSuccess(ctx::json)
                .onFailure(ctx::fail);
    }

    private void handleCreateBooking(RoutingContext ctx) {

        Long guestId = Long.valueOf(
                ctx.user()
                        .principal()
                        .getString("sub")
        );

        Long listingId =
                Long.valueOf(ctx.pathParam("id"));

        _bookingService.create(
                        guestId,
                        listingId,
                        ctx.body().asJsonObject()
                )
                .onSuccess(result -> {
                    ctx.response()
                            .setStatusCode(201)
                            .end(result.encode());
                })
                .onFailure(ctx::fail);
    }

    private void handleBookingsByHost(RoutingContext ctx){
        Long hostId = Long.valueOf(ctx.user()
                .principal()
                .getString("sub"));

        _bookingService.findByHost(hostId)
                .onSuccess(result ->
                        ctx.response()
                                .setStatusCode(200)
                                .putHeader("Content-Type", "application/json")
                                .end(result.encode()))
                .onFailure(ctx::fail);
    }

    private void handleGuestBookings(RoutingContext ctx) {

        Long guestId = Long.valueOf(
                ctx.user()
                        .principal()
                        .getString("sub")
        );

        _bookingService.findByGuest(guestId)
                .onSuccess(ctx::json)
                .onFailure(ctx::fail);
    }
}