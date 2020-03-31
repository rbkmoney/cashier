CREATE SCHEMA IF NOT EXISTS cashier;

CREATE TABLE IF NOT EXISTS cashier.cash_register
(
    id              CHARACTER VARYING NOT NULL,
    party_id        CHARACTER VARYING NOT NULL,
    shop_id         CHARACTER VARYING NOT NULL,
    provider_id     BIGINT            NOT NULL,
    provider_params CHARACTER VARYING NOT NULL,
    CONSTRAINT cash_register_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS cash_register_party_id on cashier.cash_register (party_id);
CREATE INDEX IF NOT EXISTS cash_register_shop_id on cashier.cash_register (shop_id);
CREATE INDEX IF NOT EXISTS cash_register_provider_id on cashier.cash_register (provider_id);
