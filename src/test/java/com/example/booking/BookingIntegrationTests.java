package com.example.booking;

import com.example.booking.verticles.MainVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookingIntegrationTests {

    private static Vertx vertx;
    private static WebClient webClient;

    private static String hostToken;
    private static String guestToken;

    private static Long listingId;

    @BeforeAll
    static void setUp(VertxTestContext testContext) {

        vertx = Vertx.vertx();
        webClient = WebClient.create(vertx);

        TestDatabaseMigrations.migrate();

        vertx.deployVerticle(new MainVerticle())
                .onSuccess(id -> testContext.completeNow())
                .onFailure(testContext::failNow);
    }

    @AfterAll
    static void tearDown(VertxTestContext testContext) {

        webClient.close();
        TestDatabaseMigrations.teardown();

        vertx.close()
                .onSuccess(v -> testContext.completeNow())
                .onFailure(testContext::failNow);
    }




    @Test
    void shouldReturnHealth(VertxTestContext testContext) {

        webClient
                .get(8080, "localhost", "/api/health")
                .send()
                .onSuccess(response -> {
                    assertEquals(200, response.statusCode());
                    assertEquals(
                            "Up",
                            response.bodyAsJsonObject()
                                    .getString("status")
                    );

                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(1)
    void shouldRegisterGuest(VertxTestContext testContext) {

        JsonObject body = new JsonObject()
                .put("email", "guest@test.com")
                .put("password", "password123")
                .put("role", "GUEST");

        webClient
                .post(8080, "localhost", "/api/auth/register")
                .sendJsonObject(body)
                .onSuccess(response -> {
                    assertEquals(201, response.statusCode());
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(2)
    void shouldRegisterHost(VertxTestContext testContext) {

        JsonObject body = new JsonObject()
                .put("email", "host@test.com")
                .put("password", "password123")
                .put("role", "HOST");

        webClient
                .post(8080, "localhost", "/api/auth/register")
                .sendJsonObject(body)
                .onSuccess(response -> {
                    assertEquals(201, response.statusCode());
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(3)
    void shouldLoginGuest(VertxTestContext testContext) {

        JsonObject body = new JsonObject()
                .put("email", "guest@test.com")
                .put("password", "password123");

        webClient
                .post(8080, "localhost", "/api/auth/login")
                .sendJsonObject(body)
                .onSuccess(response -> {
                    assertEquals(200, response.statusCode());

                    JsonObject responseBody = response.bodyAsJsonObject();

                    guestToken = responseBody.getString("token");

                    assertNotNull(guestToken);

                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(4)
    void shouldLoginHost(VertxTestContext testContext) {

        JsonObject body = new JsonObject()
                .put("email", "host@test.com")
                .put("password", "password123");

        webClient
                .post(8080, "localhost", "/api/auth/login")
                .sendJsonObject(body)
                .onSuccess(response -> {
                    assertEquals(200, response.statusCode());

                    JsonObject responseBody = response.bodyAsJsonObject();

                    hostToken = responseBody.getString("token");

                    assertNotNull(hostToken);

                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(5)
    void hostShouldCreateListing(
            VertxTestContext testContext) {

        JsonObject body = new JsonObject()
                .put("title", "Beach House")
                .put("description", "Holiday accommodation")
                .put("location", "Mombasa")
                .put("price", 15000);

        webClient
                .post(8080, "localhost", "/api/listings")
                .putHeader("Authorization", "Bearer " + hostToken)
                .sendJsonObject(body)
                .onSuccess(response -> {
                    assertEquals(201, response.statusCode());

                    JsonObject responseBody = response.bodyAsJsonObject();

                    listingId = responseBody.getLong("id");

                    assertNotNull(listingId);

                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(6)
    void hostShouldUpdateListing(
            VertxTestContext testContext) {

        JsonObject body = new JsonObject()
                .put("title", "Updated Beach House")
                .put("description", "Updated holiday accommodation")
                .put("location", "Mombasa")
                .put("price", 18000);

        webClient
                .put(
                        8080,
                        "localhost",
                        "/api/listings/" + listingId
                )
                .putHeader("Authorization", "Bearer " + hostToken)
                .sendJsonObject(body)
                .onSuccess(response -> {
                    assertEquals(200, response.statusCode());

                    JsonObject responseBody =
                            response.bodyAsJsonObject();

                    assertEquals(
                            "Updated Beach House",
                            responseBody.getString("title")
                    );

                    assertEquals(
                            18000,
                            responseBody.getDouble("price")
                                    .intValue()
                    );

                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(7)
    void guestShouldNotCreateListing(
            VertxTestContext testContext) {

        JsonObject body = new JsonObject()
                .put("title", "Beach House")
                .put("description", "Holiday accommodation")
                .put("location", "Mombasa")
                .put("price", 15000);

        webClient
                .post(8080, "localhost", "/api/listings")
                .putHeader("Authorization", "Bearer " + guestToken)
                .sendJsonObject(body)
                .onSuccess(response -> {
                    assertEquals(403, response.statusCode());
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(8)
    void guestShouldCreateBooking(
            VertxTestContext testContext) {

        JsonObject booking = new JsonObject()
                .put("startDate", "2026-08-15")
                .put("endDate", "2026-08-20");

        createBooking(listingId,booking)
                .onSuccess(response -> {
                    assertEquals(201, response.statusCode());
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @Test
    @Order(9)
    void shouldNotCreateDoubleBookingsOnListingWithPendingOrConfirmedStatus(
            Vertx vertx,
            VertxTestContext testContext) {

        JsonObject booking1 = new JsonObject()
                .put("startDate", "2026-08-21")
                .put("endDate", "2026-08-24");

        JsonObject booking2 = new JsonObject()
                .put("startDate", "2026-08-22")
                .put("endDate", "2026-08-26");


        createBooking(listingId, booking1)
                .onSuccess(resp -> {
                    testContext.verify(() ->
                            assertEquals(201, resp.statusCode())
                    );

                    createBooking(listingId, booking2)
                            .onSuccess(resp2 -> {
                                testContext.verify(() ->
                                        assertEquals(409, resp2.statusCode())
                                );

                                testContext.completeNow();
                            })
                            .onFailure(testContext::failNow);
                })
                .onFailure(testContext::failNow);
    }

    private Future<HttpResponse<Buffer>> createBooking(
            Long listingId,
            JsonObject booking) {

        return webClient
                .post(8080, "localhost", "/api/listings/" + listingId + "/bookings")
                .putHeader("Authorization", "Bearer " + guestToken)
                .sendJsonObject(booking);
    }
}