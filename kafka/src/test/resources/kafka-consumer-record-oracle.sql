CREATE SEQUENCE KAFKA_CONSUMER_RECORD_ID_SEQ;

CREATE TABLE KAFKA_CONSUMER_RECORD (
    ID                      NUMBER(18) NOT NULL PRIMARY KEY,
    TOPIC                   VARCHAR(100) NOT NULL,
    PARTITION               NUMBER(9) NOT NULL,
    RECORD_OFFSET           NUMBER(18) NOT NULL,
    RETRIES                 NUMBER(9) NOT NULL DEFAULT 0,
    LAST_RETRY              TIMESTAMP,
    KEY                     BLOB NOT NULL,
    VALUE                   BLOB NOT NULL,
    CREATED_AT              TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (TOPIC, PARTITION, RECORD_OFFSET)
);