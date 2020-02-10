#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER spion;
    CREATE DATABASE spion;
    CREATE SCHEMA spion;
    GRANT ALL PRIVILEGES ON DATABASE spion TO spion;
EOSQL

psql -v ON_ERROR_STOP=1 --username "spion" --dbname "spion" <<-EOSQL
    CREATE TABLE spiondata (
                           id integer NOT NULL,
                           ytelsesperiode jsonb
    );
EOSQL