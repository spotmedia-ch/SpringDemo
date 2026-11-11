# API Registry

A small API registry service for learning Spring Boot, PostgreSQL, Kafka, Docker, Terraform and AWS. See `Spec.md` for the specification and `docs/decisions/` for the design notes, one per milestone.

Status: milestone 2 (registry domain and REST API, no authentication yet).

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

# Run the end-to-end scenario against the running service (register API, subscribe, issue key, validate, revoke)
./local/demo.sh

# Run all tests (starts its own PostgreSQL container; Docker must be running)
./mvnw verify

# After changing an endpoint or DTO, regenerate the API contract and commit docs/openapi.json
./mvnw verify -Dopenapi.update=true -Dit.test=OpenApiContractIT

# Build the container image
docker build -t api-registry:local .
```

## Endpoints

Everything is under `/api/v1`. Swagger UI is at http://localhost:8080/swagger-ui.html and the OpenAPI document at `/v3/api-docs`; the committed copy is `docs/openapi.json`. `local/demo.sh` needs `curl` and `python3`, and accepts `BASE_URL` to target another host.

| Resource | Endpoints |
|---|---|
| APIs | `POST/GET /apis`, `GET/PUT /apis/{id}`, `POST /apis/{id}/status` |
| Consumers | `POST/GET /consumers`, `GET /consumers/{id}`, `POST /consumers/{id}/status` |
| Subscriptions | `POST/GET /subscriptions`, `POST /subscriptions/{id}/status` |
| Keys | `POST/GET /subscriptions/{id}/keys`, `POST /keys/{id}/revoke`, `POST /keys/validate` |

Updates and status changes must send the `rowVersion` they read; a stale value returns 409.

PostgreSQL is published on host port **5433**, not 5432, so it does not clash with a PostgreSQL already running on the machine. Inside the Compose network it is still 5432.

Reports after `./mvnw verify`: `target/surefire-reports`, `target/failsafe-reports`, `target/site/jacoco/index.html`.

Unit tests are named `*Test` and run in the `test` phase; integration tests are named `*IT` and run in `verify`.
