CREATE TYPE user_role AS ENUM (
    'GUEST',
    'HOST'
    );

CREATE TYPE booking_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'CANCELLED'
    );

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash TEXT NOT NULL,
                       role user_role NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE listings (
                          id BIGSERIAL PRIMARY KEY,
                          host_id BIGINT NOT NULL
                              REFERENCES users(id)
                              ON DELETE CASCADE,
                          title VARCHAR(255) NOT NULL,
                          description TEXT,
                          location VARCHAR(255) NOT NULL,
                          price NUMERIC(12, 2) NOT NULL
                              CHECK (price >= 0),
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE bookings (
                          id BIGSERIAL PRIMARY KEY,
                          listing_id BIGINT NOT NULL
                              REFERENCES listings(id)
                                  ON DELETE CASCADE,
                          guest_id BIGINT NOT NULL
                              REFERENCES users(id)
                                  ON DELETE CASCADE,
                          start_date DATE NOT NULL,
                          end_date DATE NOT NULL,
                          status booking_status NOT NULL DEFAULT 'PENDING',
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                          CONSTRAINT valid_booking_dates
                              CHECK (end_date > start_date)
);

CREATE INDEX idx_listings_host
    ON listings(host_id);

CREATE INDEX idx_bookings_listing
    ON bookings(listing_id);

CREATE INDEX idx_bookings_guest
    ON bookings(guest_id);