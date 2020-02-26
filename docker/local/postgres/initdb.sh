#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER spion WITH PASSWORD 'spion';
    CREATE DATABASE spion;
    CREATE SCHEMA spion;
    GRANT ALL PRIVILEGES ON DATABASE spion TO spion;
EOSQL

psql -v ON_ERROR_STOP=1 --username "spion" --dbname "spion" <<-EOSQL
    CREATE TABLE ytelsesperiode (
                           data jsonb NOT NULL
    );
    CREATE INDEX virksomhetsnummer ON ytelsesperiode ((data ->'arbeidsforhold'-> 'arbeidsgiver' ->> 'arbeidsgiverId'));
    CREATE INDEX arbeidstaker ON ytelsesperiode ((data ->'arbeidsforhold'-> 'arbeidstaker' ->> 'identitetsnummer'));
    CREATE INDEX orgnr ON ytelsesperiode ((data ->'arbeidsforhold'-> 'arbeidsgiver' ->> 'organisasjonsnummer'));

    CREATE FUNCTION get_pk(data jsonb)
    RETURNS  jsonb
    AS
    $BODY$
    SELECT json_build_array(data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId', data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer', data -> 'periode' ->> 'fom', data -> 'periode' ->> 'tom', data ->> 'ytelse')::jsonb;
    $BODY$
    LANGUAGE sql
    IMMUTABLE;

    CREATE UNIQUE INDEX pk ON ytelsesperiode (get_pk(data));

    CREATE TABLE failedvedtaksmelding (
        messageData jsonb NOT NULL,
        kafkaOffset integer,
        errorMessage text,
        id uuid NOT NULL
    );

EOSQL
