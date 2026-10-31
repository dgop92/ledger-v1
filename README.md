# ledger-v1

A financial ledger backend built with Quarkus, the Supersonic Subatomic Java Framework.

## SDD Frameworks

**BMAD**

**_NOTE:_** This project uses the [BMad Method](https://github.com/bmad-code-org/BMAD-METHOD). BMad `v6.10.0` must be installed locally (it generates the `.agents/skills/bmad-*`, `_bmad/`, and `_bmad-output/` directories, which are gitignored and not committed).

Install bmad under `.agents/skills` by chosing an open source agent during the installation process, like pi dev.

**Superpowers**

We do not commit Superpowers artifacts

## Requirements

- JDK 25
- The Gradle wrapper handles the rest — no separate Gradle install needed.
- Docker (for the local database and for tests)

## Local database

`./gradlew quarkusDev` connects to a dedicated, persistent Postgres defined in `local-env/docker-compose.yml` (Quarkus Dev Services is disabled in dev).

```shell
docker compose -f local-env/docker-compose.yml up -d --wait   # start
docker compose -f local-env/docker-compose.yml stop           # stop (keeps data)
docker compose -f local-env/docker-compose.yml down           # remove container (keeps data)
docker compose -f local-env/docker-compose.yml down -v        # remove container and wipe data
./gradlew quarkusDev
```

Defaults: `localhost:5432`, database/user/password `ledger`. Override the compose side with `LEDGER_DB_NAME`, `LEDGER_DB_USER`, `LEDGER_DB_PASSWORD`, `LEDGER_DB_PORT`, and the app side with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

Tests (`./gradlew test`) need no setup: they use Quarkus Dev Services, which starts a throwaway Postgres (Docker must be running).