--liquibase formatted sql

--changeset mkh_alez:add-unique-short-code-idx runInTransaction:false
CREATE UNIQUE INDEX CONCURRENTLY idx_url_short_code_unique ON url(short_code);

--changeset mkh_alez:drop-old-short-code-idx runInTransaction:false
DROP INDEX CONCURRENTLY url_short_code_index;

--changeset mkh_alez:add-unique-short-code-constraint
ALTER TABLE url ADD CONSTRAINT short_code_unique UNIQUE USING INDEX idx_url_short_code_unique;
