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

    INSERT INTO ytelsesperiode (data) VALUES
    ('{"grad": 50, "status": "AVSLÅTT", "ytelse": "SP", "dagsats": 5034, "maxdato": "2020-01-20", "merknad": "", "periode": {"fom": "2019-12-12", "tom": "2020-01-01"}, "vedtaksId": "9170f712-68f8-43f2-92dd-c08f1ba12e95", "sistEndret": "2020-06-08", "kafkaOffset": 0, "ferieperioder": [{"fom": "2020-01-01", "tom": "2020-01-18"}], "arbeidsforhold": {"arbeidsgiver": {"navn": "JØA OG SEL", "arbeidsgiverId": "711485759", "organisasjonsnummer": "911366940"}, "arbeidstaker": {"fornavn": "Solan", "etternavn": "Gundersen", "identitetsnummer": "15077921467"}, "arbeidsforholdId": ""}, "refusjonsbeløp": 500}'),
    ('{"grad": 100, "status": "INNVILGET", "ytelse": "SP", "dagsats": 209, "maxdato": "2020-01-20", "merknad": "", "periode": {"fom": "2020-04-05", "tom": "2020-06-07"}, "vedtaksId": "9170f712-68f8-43f2-92dd-c08f1ba12e95", "sistEndret": "2020-05-06", "kafkaOffset": 0, "ferieperioder": [], "arbeidsforhold": {"arbeidsgiver": {"navn": "JØA OG SEL", "arbeidsgiverId": "711485759", "organisasjonsnummer": "911366940"}, "arbeidstaker": {"fornavn": "Solan", "etternavn": "Gundersen", "identitetsnummer": "15077921467"}, "arbeidsforholdId": ""}, "refusjonsbeløp": 1234}'),
    ('{"grad": 80, "status": "INNVILGET", "ytelse": "SP", "dagsats": 765, "maxdato": "2020-01-20", "merknad": "", "periode": {"fom": "2020-01-01", "tom": "2020-01-20"}, "vedtaksId": "9170f712-68f8-43f2-92dd-c08f1ba12e95", "sistEndret": "2020-01-01", "kafkaOffset": 0, "ferieperioder": [], "arbeidsforhold": {"arbeidsgiver": {"navn": "JØA OG SEL", "arbeidsgiverId": "711485759", "organisasjonsnummer": "911366940"}, "arbeidstaker": {"fornavn": "Solan", "etternavn": "Gundersen", "identitetsnummer": "15077921467"}, "arbeidsforholdId": ""}, "refusjonsbeløp": 3684}');

    create table bakgrunnsjobb (
    jobb_id uuid unique not null,
    type VARCHAR(100) not null,
    behandlet timestamp,
    opprettet timestamp not null,

    status VARCHAR(50) not null,
    kjoeretid timestamp not null,

    forsoek int not null default 0,
    maks_forsoek int not null,
    data jsonb
)

EOSQL
