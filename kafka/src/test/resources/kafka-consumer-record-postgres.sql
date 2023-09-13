CREATE SEQUENCE IF NOT EXISTS KAFKA_CONSUMER_RECORD_ID_SEQ;

CREATE TABLE IF NOT EXISTS KAFKA_CONSUMER_RECORD (
    ID                      BIGINT NOT NULL PRIMARY KEY,
    TOPIC                   VARCHAR(100) NOT NULL,
    PARTITION               INTEGER NOT NULL,
    RECORD_OFFSET           BIGINT NOT NULL,
    RETRIES                 INTEGER NOT NULL DEFAULT 0,
    LAST_RETRY              TIMESTAMP,
    KEY                     BYTEA,
    VALUE                   BYTEA,
    HEADERS_JSON            TEXT,
    RECORD_TIMESTAMP        BIGINT,
    CREATED_AT              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (TOPIC, PARTITION, RECORD_OFFSET)
);



