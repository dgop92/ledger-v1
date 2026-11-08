# ledger-v1

A financial ledger backend built with Quarkus, the Supersonic Subatomic Java Framework.

## SDD Frameworks

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

## Container image

The image is a Quarkus fast-jar built from `src/main/docker/Dockerfile.jvm`. The Dockerfile copies the prebuilt `build/quarkus-app`, so run the Gradle build first.

Create `local-env/envs-vars/.env` (git-ignored; unset variables fall back to defaults):

```
LEDGER_DB_NAME=ledger
LEDGER_DB_USER=ledger
LEDGER_DB_PASSWORD=change-me
APP_PORT=8080
```

```shell
./gradlew build -x test   # produces build/quarkus-app
# builds ledger-v1:local, starts db + app
docker compose --env-file local-env/envs-vars/.env -f local-env/docker-compose.app.yml up -d --build --wait
docker compose --env-file local-env/envs-vars/.env -f local-env/docker-compose.app.yml logs -f app
# stop and wipe data
docker compose --env-file local-env/envs-vars/.env -f local-env/docker-compose.app.yml down -v
```

The service listens on `http://localhost:8080` (override with `APP_PORT`). The stack has its own Postgres and volume, so it can run next to the dev database; it uses the same `LEDGER_DB_*` variable names. To build only the image: `docker build -f src/main/docker/Dockerfile.jvm -t ledger-v1:local .`

## CI

`.github/workflows/ci.yml` runs on every pull request to `main` and every push to `main`, as three parallel checks:

| Check | Local equivalent |
|-------|------------------|
| `compile` | `./gradlew compileJava compileTestJava` |
| `format` | `./gradlew spotlessCheck` (fix with `./gradlew spotlessApply`) |
| `test` | `./gradlew test` (needs Docker) |

When `test` fails, the Gradle test reports are uploaded as the `test-reports` artifact.
