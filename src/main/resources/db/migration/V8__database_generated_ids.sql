-- Rows are now written as immutable records through Spring Data JDBC, which treats a row with an
-- id as one that already exists and updates it. The ids are surrogate keys nothing refers to, so
-- the database assigns them on insert rather than the application choosing one first.

ALTER TABLE zacks_sector_mapping ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;
ALTER TABLE zacks_industry ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;
ALTER TABLE zacks_code ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;
ALTER TABLE stock_lookup ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

-- Hibernate drew ids from this sequence in blocks of 500 and handed them out itself, so the
-- sequence's value says nothing reliable about which ids are taken. Restart it above the highest
-- one in use, and let each insert take the next value.
ALTER SEQUENCE stock_analysis_sequence INCREMENT BY 1;
SELECT setval('stock_analysis_sequence', COALESCE((SELECT MAX(id) FROM stock_analysis), 0) + 1, false);
ALTER TABLE stock_analysis ALTER COLUMN id SET DEFAULT nextval('stock_analysis_sequence');
