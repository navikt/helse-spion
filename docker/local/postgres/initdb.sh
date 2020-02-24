#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER spion WITH PASSWORD 'spion';
    CREATE DATABASE spion;
    CREATE SCHEMA spion;
    GRANT ALL PRIVILEGES ON DATABASE spion TO spion;
EOSQL

psql -v ON_ERROR_STOP=1 --username "spion" --dbname "spion" <<-EOSQL
    CREATE TABLE spiondata (
                           ytelsesperiode jsonb NOT NULL
    );
    CREATE INDEX virksomhetsnummer ON spiondata ((ytelsesperiode ->'arbeidsforhold'-> 'arbeidsgiver' ->> 'virksomhetsnummer'));
    CREATE INDEX arbeidstaker ON spiondata ((ytelsesperiode ->'arbeidsforhold'-> 'arbeidstaker' ->> 'identitetsnummer'));
    CREATE INDEX orgnr ON spiondata ((ytelsesperiode ->'arbeidsforhold'-> 'arbeidsgiver' ->> 'organisasjonsnummer'));

    CREATE TABLE failedvedaksmelding (
        messageData jsonb NOT NULL,
        errorMessage text,
        id uuid NOT NULL
    );

EOSQL
