package com.example.booking;

import com.example.booking.verticles.MainVerticle;

import io.vertx.core.Vertx;
import org.flywaydb.core.Flyway;

public class Main {

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();

        //migrations();

        vertx.deployVerticle(new MainVerticle())
                .onSuccess(id -> System.out.println("Application started"))
                .onFailure(Throwable::printStackTrace);
    }

    private static void migrations(){
        System.out.println("Starting to run migrations");
        Flyway flyway= Flyway.configure()
                .dataSource("jdbc:postgresql://localhost:5432/booking",
                        "booking_user",
                        "booking_password")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();

        System.out.println("Migration complete.");
    }
}