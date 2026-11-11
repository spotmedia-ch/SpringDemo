# API Registry

A small API registry service for learning Spring Boot, PostgreSQL, Kafka, Docker, Terraform and AWS. See `Spec.md` for the specification and `docs/decisions/` for the design notes, one per milestone.

Status: milestone 1 (skeleton and pipeline).

## Prerequisites

JDK 21 and Docker. Maven is not needed; use the wrapper `./mvnw`.

## Commands

```bash
# Start dependencies (PostgreSQL 17 on host port 5433)
docker compose up -d --wait

# Run the service on http://localhost:8080
./mvnw spring-boot:run

# Check it
curl localhost:8080/actuator/health

# Run all tests (starts its own PostgreSQL container; Docker must be running)
./mvnw verify

# Build the container image
docker build -t api-registry:local .
```

PostgreSQL is published on host port **5433**, not 5432, so it does not clash with a PostgreSQL already running on the machine. Inside the Compose network it is still 5432.

Reports after `./mvnw verify`: `target/surefire-reports`, `target/failsafe-reports`, `target/site/jacoco/index.html`.

Unit tests are named `*Test` and run in the `test` phase; integration tests are named `*IT` and run in `verify`.
