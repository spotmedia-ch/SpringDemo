# 01 - Skeleton and pipeline

## What was built

A Spring Boot 4.1.2 / Java 21 Maven project with the package layout from the spec, Actuator health (including liveness and readiness probes), a Flyway baseline migration, a Compose file with PostgreSQL 17, a layered multi-stage Dockerfile that runs as a non-root user, a GitHub Actions workflow, and one integration test that starts the whole application against a Testcontainers PostgreSQL.

## The three most important decisions

1. **Integration tests use a real PostgreSQL container, not an in-memory database.**
   The test imports a `@ServiceConnection` container, so Spring Boot wires the datasource to the container's random port with no properties. The container is pinned to `postgres:17` to match production.
   Rejected: H2 in PostgreSQL mode. It is faster but behaves differently (types, jsonb, partial indexes), and the later milestones depend on exactly those features.
   Production teams: the same, usually with a shared container across the test run.

2. **Hibernate validates the schema; Flyway owns it.**
   `ddl-auto: validate` makes startup fail if an entity does not match the database, and `open-in-view: false` stops the persistence session leaking into the web layer. The baseline migration is deliberately empty, so milestone 2 adds tables as `V2`.
   Rejected: `ddl-auto: update`, which silently changes the schema and leaves no reviewable history.
   Production teams: also forbid editing a merged migration, and often check migrations in CI against a copy of production.

3. **CI contains only the jobs that can pass today.**
   `ci.yml` has `build-test` and `image`. The `contract` job needs `docs/openapi.json` (milestone 2) and the `terraform` job needs `infra/` (milestone 5), so each is added with the milestone that creates its input.
   Rejected: adding jobs that are skipped or always green, which would hide the fact that nothing is checked.
   Production teams: would run these as required status checks on pull requests, with branch protection.

## Smaller choices worth knowing

- Host port 5433 for PostgreSQL, because 5432 was occupied on the development machine. The spec said 5432; this is a deliberate deviation.
- The Docker build skips tests (`-DskipTests`) because Testcontainers needs Docker, which is not available inside a build. CI runs the tests in `build-test` instead.
- `package-info.java` files mark the empty feature packages so the layout exists in Git.
- The local database password is a throwaway value for a container that is only reachable from the developer's machine. AWS uses Secrets Manager (milestone 5).

## Acceptance results

- `./mvnw verify`: 2 integration tests passed, BUILD SUCCESS.
- `docker compose up -d` then `./mvnw spring-boot:run`: `/actuator/health` returned `{"status":"UP"}`.
- `docker build .`: succeeded; the container user is uid 999 (`app`).
- GitHub Actions: not yet run. It needs the owner to push.
