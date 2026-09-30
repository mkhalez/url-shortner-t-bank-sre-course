--liquibase formatted sql
--changeset mkh_alez:init-url-table

CREATE TABLE url(
    id int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    short_url text NOT NULL,
    long_url text NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_at TIMESTAMP WITH TIME ZONE NOT NULL
);