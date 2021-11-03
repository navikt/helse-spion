CREATE TABLE ytelsesperiode
(
    data jsonb NOT NULL
);
CREATE INDEX arbeidsgiverId ON ytelsesperiode ((data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'arbeidsgiverId') );
CREATE INDEX arbeidstaker ON ytelsesperiode ((data -> 'arbeidsforhold' -> 'arbeidstaker' ->> 'identitetsnummer') );
CREATE INDEX orgnr ON ytelsesperiode ((data -> 'arbeidsforhold' -> 'arbeidsgiver' ->> 'organisasjonsnummer') );
CREATE INDEX fom ON ytelsesperiode ((data -> 'periode' ->> 'fom'));
CREATE INDEX tom ON ytelsesperiode ((data -> 'periode' ->> 'tom'));

ALTER TABLE ytelsesperiode
    ADD CONSTRAINT pk_must_exist CHECK ( data - > 'arbeidsforhold' - > 'arbeidstaker' ? 'identitetsnummer'
    AND data -> 'arbeidsforhold' -> 'arbeidsgiver' ? 'arbeidsgiverId'
    AND data -> 'periode' ? 'fom'
    AND data -> 'periode' ? 'tom'
    AND data ? 'ytelse');

CREATE FUNCTION get_pk(data jsonb)
    RETURNS jsonb AS '
        select json_build_array(data -> ''arbeidsforhold'' -> ''arbeidsgiver'' ->> ''arbeidsgiverId'',data -> ''arbeidsforhold'' -> ''arbeidstaker'' ->> ''identitetsnummer'',data -> ''periode'' ->> ''fom'',data -> ''periode'' ->> ''tom'',data ->> ''ytelse'')::jsonb;
        '
        LANGUAGE sql
        IMMUTABLE;

CREATE UNIQUE INDEX pk ON ytelsesperiode (get_pk(data));

CREATE TABLE bakgrunnsjobb
(
    jobb_id      uuid unique not null,
    type         VARCHAR(100) not null,
    behandlet    timestamp,
    opprettet    timestamp not null,

    status       VARCHAR(50) not null,
    kjoeretid    timestamp not null,

    forsoek      int not null default 0,
    maks_forsoek int not null,
    data         jsonb
)