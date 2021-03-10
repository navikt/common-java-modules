CREATE TABLE KAFKA_PRODUCER_MESSAGE (
    ID                      BIGSERIAL PRIMARY KEY NOT NULL,
    TOPIC                   VARCHAR(100) NOT NULL,
    KEY                     VARCHAR(40) NOT NULL, -- can also be binary
    VALUE                   TEXT NOT NULL,  -- can also be binary
    CREATED_AT              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE KAFKA_CONSUMER_MESSAGE (
    ID                      BIGSERIAL PRIMARY KEY NOT NULL,
    TOPIC                   VARCHAR(100) NOT NULL,
    KEY                     VARCHAR(40) NOT NULL, -- can also be binary
    VALUE                   TEXT NOT NULL,  -- can also be binary
    OFFSET                  BIGINT NOT NULL,
    PARTITION               INTEGER NOT NULL,
    CREATED_AT              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
