Spion 
[![Actions Status](https://github.com/navikt/helse-spion/workflows/Bygg%20og%20deploy/badge.svg)](https://github.com/navikt/helse-spleis/actions)
[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=navikt_helse-spion)](https://sonarcloud.io/dashboard?id=navikt_helse-spion)
================

Innsynsløning for arbeidsgivere i sykepenger

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #helse-arbeidsgiver

## OpenAPI/Swagger-dokumentasjon

- [ReDoc referansedokumentasjon](https://navikt.github.io/helse-spion/)
- [SwaggerUI](https://navikt.github.io/helse-spion/swagger-ui/)
- OpenAPI Råfiler: [JSON](https://navikt.github.io/helse-spion/openapi.json) [YAML](https://navikt.github.io/helse-spion/openapi.yaml)

### Installasjon

1. Installer [Node JS](https://nodejs.org/)
2. Clone repoet og kjør `npm install` i repo root

### Bruk

#### `npm start`
Starter lokal devserver for swagger/openAPI på port :4000

#### `npm run build`
Slår sammen spec-filene og oppdaterer web_deploy

#### `npm test`
Validerer spec-filene

#### `npm run gh-pages`
Deploy til GitHub Pages
