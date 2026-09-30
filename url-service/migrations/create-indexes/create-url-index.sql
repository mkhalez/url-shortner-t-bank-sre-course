--liquibase formatted sql
--changeset mkh_alez:init-url-table

CREATE INDEX url_short_url_index ON url(short_url);
CREATE INDEX url_long_url_index ON url(long_url);
