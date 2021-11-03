DO $$
    BEGIN
        IF EXISTS
            ( SELECT 1 from pg_roles where rolname='cloudsqliamuser')
        THEN
            GRANT ALL PRIVILEGES ON TABLE public.bakgrunnsjobb TO cloudsqliamuser;
            GRANT ALL PRIVILEGES ON TABLE public.flyway_schema_history TO cloudsqliamuser;
            GRANT ALL PRIVILEGES ON TABLE public.ytelsesperiode TO cloudsqliamuser;
            alter default privileges in schema public grant all on tables to cloudsqliamuser;
        END IF ;
    END
$$ ;