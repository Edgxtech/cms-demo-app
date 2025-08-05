-- Update id columns to BIGINT
ALTER TABLE IF EXISTS organisation_chart_of_account_sub_type_aud
    ALTER COLUMN id TYPE BIGINT USING id::BIGINT;

ALTER TABLE IF EXISTS organisation_chart_of_account_type_aud
    ALTER COLUMN id TYPE BIGINT USING id::BIGINT;

-- Update opening_balance_date to DATE
ALTER TABLE IF EXISTS organisation_chart_of_account_aud
    ALTER COLUMN opening_balance_date TYPE DATE USING opening_balance_date::DATE;