Spion [![Actions Status](https://github.com/navikt/helse-spion/workflows/Bygg%20og%20deploy/badge.svg)](https://github.com/navikt/helse-spleis/actions)
================

Innsynsløning for arbeidsgivere i sykepenger

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-arbeidsgiver




## OpenAPI/Swagger-dokumentasjon

- [Reference Documentation (ReDoc)](https://navikt.github.io/helse-spion/)
- [SwaggerUI](https://navikt.github.io/helse-spion/swagger-ui/)
- OpenAPI Raw Files: [JSON](https://navikt.github.io/helse-spion/openapi.json) [YAML](https://navikt.github.io/helse-spion/openapi.yaml)

### Install

1. Install [Node JS](https://nodejs.org/)
2. Clone repo and run `npm install` in the repo root

### Usage

#### `npm start`
Starts the development server.

#### `npm run build`
Bundles the spec and prepares web_deploy folder with static assets.

#### `npm test`
Validates the spec.

#### `npm run gh-pages`
Deploys docs to GitHub Pages.
