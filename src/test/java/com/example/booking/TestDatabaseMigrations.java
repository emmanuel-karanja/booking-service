package com.example.booking;

import org.flywaydb.core.Flyway;

public class TestDatabaseMigrations {

    public static void migrate(){
            Flyway flyway= Flyway.configure()
                    .dataSource("jdbc:postgresql://localhost:5432/booking_test",
                            "booking_test_user",
                            "booking_test_password")
                    .cleanDisabled(false)
                    .load();

            flyway.clean();
            flyway.migrate();
    }

    public static void teardown(){
        Flyway flyway= Flyway.configure()
                .dataSource("jdbc:postgresql://localhost:5432/booking_test",
                        "booking_test_user",
                        "booking_test_password")
                .cleanDisabled(false)
                .load();

        flyway.clean();
    }

}
