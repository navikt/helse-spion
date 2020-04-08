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
    CREATE INDEX arbeidsgiverId ON ytelsesperiode ((data ->'arbeidsforhold'-> 'arbeidsgiver' ->> 'arbeidsgiverId'));
    CREATE INDEX arbeidstaker ON ytelsesperiode ((data ->'arbeidsforhold'-> 'arbeidstaker' ->> 'identitetsnummer'));
    CREATE INDEX orgnr ON ytelsesperiode ((data ->'arbeidsforhold'-> 'arbeidsgiver' ->> 'organisasjonsnummer'));
    CREATE INDEX fom ON ytelsesperiode ((data ->'periode'->> 'fom'));
    CREATE INDEX tom ON ytelsesperiode ((data -> 'periode' ->> 'tom'));

    ALTER TABLE ytelsesperiode ADD CONSTRAINT pk_must_exist CHECK ( data -> 'arbeidsforhold' -> 'arbeidstaker' ? 'identitetsnummer'
        AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ? 'arbeidsgiverId'
        AND data -> 'periode' ? 'fom'
        AND data -> 'periode' ? 'tom'
        AND data ? 'ytelse');

    CREATE FUNCTION get_pk(data jsonb)
        RETURNS  jsonb AS
        '
        select json_build_array(data -> ''arbeidsforhold'' -> ''arbeidsgiver'' ->> ''arbeidsgiverId'',data -> ''arbeidsforhold'' -> ''arbeidstaker'' ->> ''identitetsnummer'',data -> ''periode'' ->> ''fom'',data -> ''periode'' ->> ''tom'',data ->> ''ytelse'')::jsonb;
        '
        LANGUAGE sql
        IMMUTABLE;

    CREATE UNIQUE INDEX pk ON ytelsesperiode (get_pk(data));

    CREATE TABLE failedvedtaksmelding (
        messageData jsonb NOT NULL,
        kafkaOffset integer,
        errorMessage text,
        id uuid NOT NULL
    );

    CREATE TABLE varsling (
        uuid varchar(64) NOT NULL primary key,
        status bit NOT NULL,
        opprettet timestamp NOT NULL,
        behandlet timestamp,
        aggregatPeriode varchar(64) NOT NULL,
        virksomhetsNr varchar(9) NOT NULL,
        data jsonb NOT NULL
    );

EOSQL
