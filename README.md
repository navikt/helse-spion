Refusjonsportalen 
[![Actions Status](https://github.com/navikt/helse-spion/workflows/Bygg%20og%20deploy/badge.svg)](https://github.com/navikt/helse-spion/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=navikt_helse-spion&metric=alert_status&branch=main)](https://sonarcloud.io/dashboard?id=navikt_helse-spion&branch=main)

================

Refusjonsportalen gir arbeidsgiverne en løpende oversikt over refusjoner som er tilkjent dem, for deres egne arbeidstakere.

# Komme i gang

For å kjøre lokalt kan du starte  `docker-compose up` fra docker/local før du starter prosjektet. 

## Formatering

Prosjektet er formatert med [ktlint](https://github.com/pinterest/ktlint).

Se etter feil med:

```
./gradlew ktlintCheck
```

Automatisk rett feil med:
```
./gradlew ktlintFormat
```

Eller legg til følgende commit-hook:
```
./gradlew addKtlintCheckGitPreCommitHook

# eller

./gradlew addKtlintFormatGitPreCommitHook
```


# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #helse-arbeidsgiver.

