package com.example.booking.verticles;

import com.example.booking.config.DatabaseConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class DatabaseVerticle extends AbstractVerticle {

    private static final Logger _logger =
            LoggerFactory.getLogger(DatabaseVerticle.class);

    private Pool _pool;

    @Override
    public void start(Promise<Void> startPromise) {
        try {
            createPool();

            verifyDatabase()
                    .compose(v -> registerHandlers())
                    .onSuccess(v -> {
                        _logger.info("DatabaseVerticle started");
                        startPromise.complete();
                    })
                    .onFailure(err -> {
                        _logger.error("DatabaseVerticle failed", err);
                        startPromise.fail(err);
                    });

        } catch (Exception e) {
            startPromise.fail(e);
        }
    }

    private void createPool() {
        DatabaseConfig dbConfig =
                config().mapTo(DatabaseConfig.class);

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setHost(dbConfig.host())
                .setPort(dbConfig.port())
                .setDatabase(dbConfig.database())
                .setUser(dbConfig.user())
                .setPassword(dbConfig.password());

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(dbConfig.poolSize());

        _pool = Pool.pool(vertx, connectOptions, poolOptions);
    }

    private Future<Void> registerHandlers() {
        vertx.eventBus()
                .<JsonObject>consumer("user.create")
                .handler(this::createUser);

        vertx.eventBus()
                .<JsonObject>consumer("user.find.by.email")
                .handler(this::findUserByEmail);

        vertx.eventBus()
                .<JsonObject>consumer("listing.create")
                .handler(this::createListing);
        vertx.eventBus()
                .<JsonObject>consumer("listing.update")
                .handler(this::updateListing);

        vertx.eventBus()
                .<JsonObject>consumer("listing.find.all")
                .handler(this::findListings);

        vertx.eventBus()
                .<JsonObject>consumer("listing.find.by.id")
                .handler(this::findListingById);

        vertx.eventBus()
                .<JsonObject>consumer("booking.create")
                .handler(this::createBooking);

        vertx.eventBus()
                .<JsonObject>consumer("booking.find.by.guest")
                .handler(this::findBookingsByGuest);

        vertx.eventBus()
                .<JsonObject>consumer("booking.find.by.id")
                .handler(this::findBookingById);
        vertx.eventBus()
                .<JsonObject>consumer("booking.find.by.host")
                .handler(this::findBookingsByHost);
        vertx.eventBus()
                .<JsonObject>consumer("booking.overlap.check")
                .handler(this::bookingOverlapCheck);

        vertx.eventBus()
                .<JsonObject>consumer("db.health.check")
                .handler(this::healthCheck);

        return Future.succeededFuture();
    }

    private void bookingOverlapCheck(Message<JsonObject> message) {
        JsonObject request = message.body();

        Long listingId = request.getLong("listingId");
        LocalDate checkIn = LocalDate.parse(request.getString("startDate"));
        LocalDate checkOut = LocalDate.parse(request.getString("endDate"));

        String sql = """
        SELECT EXISTS (
            SELECT 1
            FROM bookings
            WHERE listing_id = $1
              AND status IN ('PENDING', 'CONFIRMED')
              AND start_date < $3
              AND end_date > $2
        ) AS overlapping
        """;

        _pool.preparedQuery(sql)
                .execute(Tuple.of(
                        listingId,
                        checkIn,
                        checkOut
                ))
                .onSuccess(rows -> {
                    boolean overlapping = rows
                            .iterator()
                            .next()
                            .getBoolean("overlapping");

                    if (overlapping) {
                        message.fail(409, "Listing already has an overlapping booking");
                        return;
                    }

                    message.reply(new JsonObject().put("overlapping", false));
                })
                .onFailure(error ->
                        message.fail(500, error.getMessage())
                );
    }

    private void healthCheck(Message<JsonObject> message) {
        _logger.info("Ping health check");

        _pool.query("SELECT 1")
                .execute()
                .onSuccess(result -> {
                    message.reply(new JsonObject()
                            .put("status", "Up"));
                })
                .onFailure(error ->
                        message.fail(500, error.getMessage()));
    }

    private void createUser(Message<JsonObject> message) {
        JsonObject body = message.body();

        String email = body.getString("email");
        String passwordHash = body.getString("passwordHash");
        String role = body.getString("role");

        _logger.info("Creating user: {}", email);

        _pool.preparedQuery("""
                INSERT INTO users(email, password_hash, role)
                VALUES ($1, $2, $3)
                RETURNING id, email, role, created_at
                """)
                .execute(Tuple.of(email, passwordHash, role))
                .onSuccess(rows -> {

                    Row row = rows.iterator().next();

                    JsonObject response = new JsonObject()
                            .put("id", row.getLong("id"))
                            .put("email", row.getString("email"))
                            .put("role", row.getString("role"))
                            .put("createdAt",
                                    row.getOffsetDateTime("created_at")
                                            .toString());

                    message.reply(response);
                })
                .onFailure(err -> {
                    _logger.error("Failed to create user", err);
                    message.fail(500, err.getMessage());
                });
    }

    private void findUserByEmail(Message<JsonObject> message) {
        String email = message.body().getString("email");

        _logger.info("Fetching user by email: {}", email);

        _pool.preparedQuery("""
                SELECT id, email, password_hash, role, created_at
                FROM users
                WHERE email = $1
                """)
                .execute(Tuple.of(email))
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        message.fail(404, "User not found");
                        return;
                    }

                    Row row = rows.iterator().next();

                    JsonObject response = new JsonObject()
                            .put("id", row.getLong("id"))
                            .put("email", row.getString("email"))
                            .put("passwordHash",
                                    row.getString("password_hash"))
                            .put("role", row.getString("role"))
                            .put("createdAt",
                                    row.getOffsetDateTime("created_at")
                                            .toString());

                    message.reply(response);
                })
                .onFailure(err -> {
                    _logger.error("Failed to find user", err);
                    message.fail(500, err.getMessage());
                });
    }

    private void createListing(Message<JsonObject> message) {
        JsonObject body = message.body();

        Long hostId = body.getLong("hostId");
        String title = body.getString("title");
        String description = body.getString("description");
        String location = body.getString("location");
        Double price = body.getDouble("price");

        _logger.info("Creating listing for host: {}", hostId);

        _pool.preparedQuery("""
                INSERT INTO listings(
                    host_id,
                    title,
                    description,
                    location,
                    price
                )
                VALUES ($1, $2, $3, $4, $5)
                RETURNING id, host_id, title, description,
                          location, price, created_at
                """)
                .execute(Tuple.of(
                        hostId,
                        title,
                        description,
                        location,
                        price
                ))
                .onSuccess(rows -> {
                    Row row = rows.iterator().next();

                    JsonObject response = new JsonObject()
                            .put("id", row.getLong("id"))
                            .put("hostId", row.getLong("host_id"))
                            .put("title", row.getString("title"))
                            .put("description",
                                    row.getString("description"))
                            .put("location",
                                    row.getString("location"))
                            .put("price", row.getBigDecimal("price"))
                            .put("createdAt",
                                    row.getOffsetDateTime("created_at")
                                            .toString());

                    message.reply(response);
                })
                .onFailure(err -> {
                    _logger.error("Failed to create listing", err);
                    message.fail(500, err.getMessage());
                });
    }

    private void updateListing(Message<JsonObject> message) {
        JsonObject body = message.body();

        Long listingId = body.getLong("listingId");
        Long hostId = body.getLong("hostId");
        String title = body.getString("title");
        String description = body.getString("description");
        String location = body.getString("location");
        Double price = body.getDouble("price");

        // The security to ensure that you can only update listing only belonging to you is in the where statement
        _pool.preparedQuery("""
            UPDATE listings
            SET title = $1,
                description = $2,
                location = $3,
                price = $4
            WHERE id = $5
              AND host_id = $6
            RETURNING id, host_id, title, description,
                      location, price, created_at
            """)
                .execute(Tuple.of(
                        title,
                        description,
                        location,
                        price,
                        listingId,
                        hostId
                ))
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        message.fail(404, "Listing not found");
                        return;
                    }

                    Row row = rows.iterator().next();

                    JsonObject response = new JsonObject()
                            .put("id", row.getLong("id"))
                            .put("hostId", row.getLong("host_id"))
                            .put("title", row.getString("title"))
                            .put("description", row.getString("description"))
                            .put("location", row.getString("location"))
                            .put("price", row.getBigDecimal("price"))
                            .put("createdAt",
                                    row.getOffsetDateTime("created_at")
                                            .toString());

                    message.reply(response);
                })
                .onFailure(err -> {
                    _logger.error("Failed to update listing", err);
                    message.fail(500, err.getMessage());
                });
    }

    private void findListings(Message<JsonObject> message) {
        _logger.info("Fetching all listings");

        _pool.query("""
                SELECT id,
                       host_id,
                       title,
                       description,
                       location,
                       price,
                       created_at
                FROM listings
                ORDER BY created_at DESC
                """)
                .execute()
                .onSuccess(rows -> {
                    var listings = new io.vertx.core.json.JsonArray();

                    for (Row row : rows) {
                        listings.add(new JsonObject()
                                .put("id", row.getLong("id"))
                                .put("hostId", row.getLong("host_id"))
                                .put("title", row.getString("title"))
                                .put("description",
                                        row.getString("description"))
                                .put("location",
                                        row.getString("location"))
                                .put("price",
                                        row.getBigDecimal("price"))
                                .put("createdAt",
                                        row.getOffsetDateTime("created_at")
                                                .toString()));
                    }

                    message.reply(new JsonObject()
                            .put("listings", listings));
                })
                .onFailure(err -> {
                    _logger.error("Failed to fetch listings", err);
                    message.fail(500, err.getMessage());
                });
    }

    private void findListingById(Message<JsonObject> message) {
        Long listingId = message.body().getLong("listingId");

        _logger.info("Fetching listing: {}", listingId);

        _pool.preparedQuery("""
                SELECT id,
                       host_id,
                       title,
                       description,
                       location,
                       price,
                       created_at
                FROM listings
                WHERE id = $1
                """)
                .execute(Tuple.of(listingId))
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        message.fail(404, "Listing not found");
                        return;
                    }

                    Row row = rows.iterator().next();

                    JsonObject response = new JsonObject()
                            .put("id", row.getLong("id"))
                            .put("hostId", row.getLong("host_id"))
                            .put("title", row.getString("title"))
                            .put("description",
                                    row.getString("description"))
                            .put("location",
                                    row.getString("location"))
                            .put("price", row.getBigDecimal("price"))
                            .put("createdAt",
                                    row.getOffsetDateTime("created_at")
                                            .toString());

                    message.reply(response);
                })
                .onFailure(err -> {
                    _logger.error("Failed to find listing", err);
                    message.fail(500, err.getMessage());
                });
    }

    private void createBooking(Message<JsonObject> message) {
        JsonObject body = message.body();

        Long listingId = body.getLong("listingId");
        Long guestId = body.getLong("guestId");
        LocalDate startDate =
                LocalDate.parse(body.getString("startDate"));
        LocalDate endDate =
                LocalDate.parse(body.getString("endDate"));

        _logger.info("Creating booking for listing: {}", listingId);

        _pool.preparedQuery("""
                INSERT INTO bookings(
                    listing_id,
                    guest_id,
                    start_date,
                    end_date
                )
                VALUES ($1, $2, $3, $4)
                RETURNING id,
                          listing_id,
                          guest_id,
                          start_date,
                          end_date,
                          status,
                          created_at
                """)
                .execute(Tuple.of(
                        listingId,
                        guestId,
                        startDate,
                        endDate
                ))
                .onSuccess(rows -> {
                    Row row = rows.iterator().next();

                    JsonObject response = new JsonObject()
                            .put("id", row.getLong("id"))
                            .put("listingId",
                                    row.getLong("listing_id"))
                            .put("guestId",
                                    row.getLong("guest_id"))
                            .put("startDate",
                                    row.getLocalDate("start_date")
                                            .toString())
                            .put("endDate",
                                    row.getLocalDate("end_date")
                                            .toString())
                            .put("status", row.getString("status"))
                            .put("createdAt",
                                    row.getOffsetDateTime("created_at")
                                            .toString());

                    message.reply(response);
                })
                .onFailure(err -> {
                    _logger.error("Failed to create booking", err);
                    message.fail(500, err.getMessage());
                });
    }

    private void findBookingsByHost(Message<JsonObject> message) {

        Long hostId = message.body().getLong("hostId");

        _logger.info("Fetching bookings for host: {}", hostId);

        _pool.preparedQuery("""
            SELECT b.id,
                   b.listing_id,
                   b.guest_id,
                   b.start_date,
                   b.end_date,
                   b.status,
                   b.created_at
            FROM bookings b
            JOIN listings l ON l.id = b.listing_id
            WHERE l.host_id = $1
            ORDER BY b.created_at DESC
            """)
                .execute(Tuple.of(hostId))
                .onSuccess(rows -> {

                    var bookings = new io.vertx.core.json.JsonArray();

                    for (Row row : rows) {
                        bookings.add(new JsonObject()
                                .put("id", row.getLong("id"))
                                .put("listingId", row.getLong("listing_id"))
                                .put("guestId", row.getLong("guest_id"))
                                .put("startDate", row.getLocalDate("start_date")
                                                .toString())
                                .put("endDate", row.getLocalDate("end_date")
                                                .toString())
                                .put("status", row.getString("status"))
                                .put("createdAt", row.getOffsetDateTime("created_at")
                                                .toString()));
                    }

                    message.reply(new JsonObject()
                            .put("bookings", bookings));
                })
                .onFailure(err -> {
                    _logger.error(
                            "Failed to fetch host bookings",
                            err
                    );
                    message.fail(500, err.getMessage());
                });
    }

    private void findBookingsByGuest(Message<JsonObject> message) {
        Long guestId = message.body().getLong("guestId");

        _logger.info("Fetching bookings for guest: {}", guestId);

        _pool.preparedQuery("""
                SELECT id,
                       listing_id,
                       guest_id,
                       start_date,
                       end_date,
                       status,
                       created_at
                FROM bookings
                WHERE guest_id = $1
                ORDER BY created_at DESC
                """)
                .execute(Tuple.of(guestId))
                .onSuccess(rows -> {
                    var bookings = new io.vertx.core.json.JsonArray();

                    for (Row row : rows) {
                        bookings.add(new JsonObject()
                                .put("id", row.getLong("id"))
                                .put("listingId",
                                        row.getLong("listing_id"))
                                .put("guestId",
                                        row.getLong("guest_id"))
                                .put("startDate",
                                        row.getLocalDate("start_date")
                                                .toString())
                                .put("endDate",
                                        row.getLocalDate("end_date")
                                                .toString())
                                .put("status",
                                        row.getString("status"))
                                .put("createdAt",
                                        row.getOffsetDateTime("created_at")
                                                .toString()));
                    }

                    message.reply(new JsonObject()
                            .put("bookings", bookings));
                })
                .onFailure(err -> {
                    _logger.error("Failed to fetch guest bookings", err);
                    message.fail(500, err.getMessage());
                });
    }

    private void findBookingById(Message<JsonObject> message) {
        Long bookingId = message.body().getLong("bookingId");

        _logger.info("Fetching booking: {}", bookingId);

        _pool.preparedQuery("""
                SELECT id,
                       listing_id,
                       guest_id,
                       start_date,
                       end_date,
                       status,
                       created_at
                FROM bookings
                WHERE id = $1
                """)
                .execute(Tuple.of(bookingId))
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        message.fail(404, "Booking not found");
                        return;
                    }

                    Row row = rows.iterator().next();

                    JsonObject response = new JsonObject()
                            .put("id", row.getLong("id"))
                            .put("listingId",
                                    row.getLong("listing_id"))
                            .put("guestId",
                                    row.getLong("guest_id"))
                            .put("startDate",
                                    row.getLocalDate("start_date")
                                            .toString())
                            .put("endDate",
                                    row.getLocalDate("end_date")
                                            .toString())
                            .put("status", row.getString("status"))
                            .put("createdAt",
                                    row.getOffsetDateTime("created_at")
                                            .toString());

                    message.reply(response);
                })
                .onFailure(err -> {_logger.error("Failed to find booking", err);message.fail(500, err.getMessage());
                });
    }



    private Future<Void> verifyDatabase() {
        return _pool.query("SELECT 1")
                .execute()
                .mapEmpty();
    }

    @Override
    public void stop() {
        if (_pool != null) {
            _pool.close();
        }
    }
}