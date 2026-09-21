CREATE TABLE venues (
                        id BIGSERIAL PRIMARY KEY,
                        code VARCHAR(20) NOT NULL UNIQUE,
                        name VARCHAR(150) NOT NULL,
                        city VARCHAR(100) NOT NULL,
                        address VARCHAR(200),
                        capacity INTEGER NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        CONSTRAINT chk_venue_capacity CHECK (capacity > 0)
);

CREATE TABLE events (
                        id BIGSERIAL PRIMARY KEY,
                        event_code VARCHAR(30) NOT NULL UNIQUE,
                        name VARCHAR(150) NOT NULL,
                        description VARCHAR(1000),
                        event_date DATE NOT NULL,
                        category VARCHAR(30) NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        minimum_age INTEGER,
                        venue_id BIGINT NOT NULL REFERENCES venues(id)
);

CREATE TABLE artists (
                         id BIGSERIAL PRIMARY KEY,
                         stage_name VARCHAR(100) NOT NULL UNIQUE,
                         country VARCHAR(100),
                         genre VARCHAR(50),
                         active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE event_artists (
                               event_id BIGINT NOT NULL REFERENCES events(id),
                               artist_id BIGINT NOT NULL REFERENCES artists(id),
                               PRIMARY KEY (event_id, artist_id)
);

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE user_profiles (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
                               first_name VARCHAR(100) NOT NULL,
                               last_name VARCHAR(100) NOT NULL,
                               phone VARCHAR(20),
                               city VARCHAR(100),
                               birth_date DATE
);

CREATE TABLE tickets (
                         id BIGSERIAL PRIMARY KEY,
                         ticket_code VARCHAR(30) NOT NULL UNIQUE,
                         type VARCHAR(20) NOT NULL,
                         price NUMERIC(10,2) NOT NULL,
                         status VARCHAR(20) NOT NULL,
                         purchase_date TIMESTAMP NOT NULL,
                         user_id BIGINT NOT NULL REFERENCES users(id),
                         event_id BIGINT NOT NULL REFERENCES events(id),
                         CONSTRAINT chk_ticket_price CHECK (price >= 0)
);

CREATE INDEX idx_event_venue ON events(venue_id);
CREATE INDEX idx_event_status ON events(status);
CREATE INDEX idx_event_date ON events(event_date);
CREATE INDEX idx_ticket_user ON tickets(user_id);
CREATE INDEX idx_ticket_event ON tickets(event_id);
CREATE INDEX idx_ticket_status ON tickets(status);