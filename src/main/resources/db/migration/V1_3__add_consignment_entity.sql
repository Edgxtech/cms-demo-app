DROP TABLE IF EXISTS blockchain_publisher_consignment;

CREATE TABLE blockchain_publisher_consignment (
    consignment_id VARCHAR(64) NOT NULL,
    id_control VARCHAR(64) NOT NULL,
    ver BIGINT NOT NULL DEFAULT 1,
    goods JSONB NOT NULL,
    sender_id VARCHAR(64) NOT NULL,
    sender_name VARCHAR(255),
    sender_country_code VARCHAR(10),
    sender_tax_id_number VARCHAR(50),
    sender_currency_id VARCHAR(10),
    receiver_id VARCHAR(64) NOT NULL,
    receiver_name VARCHAR(255),
    receiver_country_code VARCHAR(10),
    receiver_tax_id_number VARCHAR(50),
    receiver_currency_id VARCHAR(10),
    l1_transaction_hash VARCHAR(64),
    l1_absolute_slot BIGINT,
    l1_creation_slot BIGINT,
    l1_finality_score blockchain_publisher_finality_score_type,
    l1_publish_status blockchain_publisher_blockchain_publish_status_type,
    tracking_status VARCHAR(50),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    dispatched_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (consignment_id)
);


-- CREATE TYPE blockchain_publisher_finality_score_type AS ENUM ('VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH', 'ULTRA_HIGH', 'FINAL');

--DROP TABLE IF EXISTS blockchain_publisher_consignment;
--
--CREATE TABLE blockchain_publisher_consignment (
--    consignment_id VARCHAR(64) NOT NULL,
--    goods JSONB NOT NULL,
--    sender_id VARCHAR(64) NOT NULL,
--    sender_name VARCHAR(255),
--    sender_country_code VARCHAR(10),
--    sender_tax_id_number VARCHAR(50),
--    sender_currency_id VARCHAR(10),
--    receiver_id VARCHAR(64) NOT NULL,
--    receiver_name VARCHAR(255),
--    receiver_country_code VARCHAR(10),
--    receiver_tax_id_number VARCHAR(50),
--    receiver_currency_id VARCHAR(10),
--    l1_transaction_hash VARCHAR(64),
--    l1_absolute_slot BIGINT,
--    l1_creation_slot BIGINT,
--    l1_finality_score blockchain_publisher_finality_score_type,
--    l1_publish_status blockchain_publisher_blockchain_publish_status_type,
--    created_at TIMESTAMP NOT NULL,
--    updated_at TIMESTAMP NOT NULL,
--    tracking_status VARCHAR(50),
--    latitude DOUBLE PRECISION,
--    longitude DOUBLE PRECISION,
--    PRIMARY KEY (consignment_id)
--);