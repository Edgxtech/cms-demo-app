CREATE TABLE blockchain_reader_consignment (
   consignment_id CHAR(64) NOT NULL,
   organisation_id CHAR(64) NOT NULL,
   l1_absolute_slot BIGINT NOT NULL,
   l1_transaction_hash CHAR(64) NOT NULL,

   created_at TIMESTAMP WITHOUT TIME ZONE,
   updated_at TIMESTAMP WITHOUT TIME ZONE,

   PRIMARY KEY (consignment_id)
);

CREATE INDEX idx_consignment_l1_absolute_slot ON blockchain_reader_consignment (l1_absolute_slot);
