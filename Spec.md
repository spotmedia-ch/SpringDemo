# API Registry — Sample Project Specification for Claude Code

Nov 10, 2026 · @Andrew Seddon

## 1. Purpose

This project is a small API registry service, built to learn the stack an API management team works with: Java, Spring Boot, PostgreSQL, Kafka, Docker, Terraform, AWS and a CI/CD pipeline.

The service manages the four things an API management platform revolves around: APIs, consumers, subscriptions and API keys. Every change is published to Kafka, and a consumer materialises those events into a PostgreSQL audit table.

The goal is understanding, not production quality. Each milestone must leave a running, tested system and a short explanation of the design decisions in it.

This document is written to be saved as `SPEC.md` in the repository root and read by Claude Code. Section 2 is for the human owner; sections 3 to 16 are the specification.

## 2. Your steps: from this spec to a working example

Milestones 1 to 3 running locally with a green pipeline are the realistic target for Thursday. The AWS deployment (milestone 5) is worth doing only if the first three are finished by Wednesday evening.

### Before you start (about 30 minutes)

- [ ] Install on your machine: JDK 21, Docker Desktop, Git, Terraform, AWS CLI v2 and Claude Code. Maven is not needed; the project uses the Maven wrapper.
- [ ] Check each one runs: `java -version`, `docker info`, `terraform version`, `aws --version`, `claude --version`.
- [ ] Use a personal AWS account, not a work one. Set a billing budget alert of USD 20 before creating anything.
- [ ] Create an empty private GitHub repository named `api-registry` and clone it.
- [ ] In this document, export to Markdown and save the file as `SPEC.md` in the repository root. Commit and push it.

### Build it with Claude Code

1. Open a terminal in the repository and start `claude`.
2. Give it this first prompt: *"Read SPEC.md in full. Create CLAUDE.md from section 16. Then implement milestone 1 only, and stop when its acceptance criteria pass. Explain each design decision as you make it."*
3. When it stops, run the acceptance checks yourself. Do not take its word for it.
4. Ask it to walk you through the code: *"Explain how a request travels from the controller to the database in this milestone."* Your understanding is the deliverable.
5. Commit, push, and watch the GitHub Actions run go green.
6. Repeat steps 2 to 5 for each milestone, one at a time: *"Implement milestone N only."*

### Deploy to AWS (milestone 5)

1. Sign in locally with an administrator profile: `aws configure sso`, or an access key that you delete afterwards.
2. Apply the bootstrap stack once from your machine: `terraform -chdir=infra/bootstrap init`, then `apply`. It creates the state bucket, the image repository and the role GitHub will assume.
3. Copy the role ARN from the Terraform output into GitHub: repository Settings, then Secrets and variables, then Actions, then Variables. Add `AWS_ROLE_ARN` and `AWS_REGION`.
4. Push to `main`. The deploy workflow builds the image, applies the application stack and prints the service URL.
5. Call the health endpoint and one API endpoint against that URL.
6. Run the `destroy` workflow when you have finished. The stack costs roughly USD 3 a day while it runs (approximate; see section 13).

### Before the coffee

- [ ] Write down three things that surprised you while building it.
- [ ] Write down three questions about how their real system differs from this one.
- [ ] Read `docs/gitlab-ci.example.yml` once, so their pipeline syntax looks familiar.

## 3. Scope

The project is one deployable Spring Boot service with its own database, event stream, infrastructure code and pipeline.

**In scope**

- CRUD for APIs and consumers, and lifecycle operations for subscriptions and API keys.
- A key validation endpoint, as a gateway would call it.
- Domain events published to Kafka through a transactional outbox.
- A Kafka consumer that materialises events into a PostgreSQL audit table.
- JWT-protected admin endpoints.
- Docker image, Docker Compose for local work, Terraform for AWS, GitHub Actions for CI/CD.

**Out of scope**

- A real API gateway, a developer portal and request proxying.
- Multi-tenancy, a user interface, and billing.
- A schema registry and Avro. Events are JSON.
- High availability, autoscaling, custom domains and TLS certificates.
- Managed Kafka (Amazon MSK). Section 13 explains the substitute.

## 4. Technology stack

Every choice below mirrors something the target team is known to use, or is the most common companion to it. Use the current stable release of each and pin exact versions in the build.

| Concern | Choice | Notes |
| --- | --- | --- |
| Language | Java 21 (LTS) | Use records for DTOs and events. |
| Framework | Spring Boot 4.x, the default stable version on start.spring.io | The team may run 3.x; at this level the differences are small. |
| Build | Maven with the Maven wrapper (`./mvnw`) | Single module. |
| Web | Spring Web MVC, Bean Validation | Errors as RFC 9457 problem details. |
| API documentation | springdoc-openapi | Serves `/v3/api-docs` and Swagger UI. |
| Persistence | Spring Data JPA with Hibernate | No lazy loading across the web layer. |
| Migrations | Flyway | Hibernate `ddl-auto` set to `validate`. |
| Database | PostgreSQL 17 | Amazon RDS in AWS, a container locally. |
| Messaging | Apache Kafka in KRaft mode, Spring for Apache Kafka | Official `apache/kafka` image. JSON payloads. |
| Security | Spring Security as an OAuth2 resource server | Keycloak locally, Amazon Cognito in AWS. |
| Observability | Spring Boot Actuator, Micrometer, structured JSON logs | Health, info and metrics endpoints. |
| Tests | JUnit 5, AssertJ, Mockito, Testcontainers | Real PostgreSQL and Kafka in integration tests. |
| Container | Docker, multi-stage build | Eclipse Temurin JRE base image, non-root user. |
| Infrastructure | Terraform with the AWS provider | State in S3 with native locking. |
| Runtime | Amazon ECS on Fargate behind an Application Load Balancer | One task, one service. |
| CI/CD | GitHub Actions | A GitLab CI translation is kept in `docs/` for reading. |

Before adding any library, confirm that its current release supports the chosen Spring Boot version. If springdoc or Testcontainers lag behind, say so and propose the nearest working combination.

## 5. Architecture

The system is one Spring Boot service with three dependencies: PostgreSQL for state, Kafka for events and an identity provider for tokens.

&#91;embedded content: architecture · 6 components\]

The service is both the producer and the consumer of its own events. In a real platform the consumer would be a separate service; here it shows the full round trip in one codebase.

**Request flow**

1. A client obtains a JWT from the identity provider using the client-credentials flow.
2. The client calls the REST API with the token. In AWS the call passes through the load balancer.
3. The service validates the token against the provider's published signing keys.
4. The service applies the business rule and writes the state change and an outbox row in one database transaction.
5. The outbox relay publishes the row to Kafka and marks it as published.
6. The audit consumer reads the event from Kafka and inserts a row into `audit_log`.

**Layers inside the service**

- Controller: HTTP, validation, DTO mapping. No business logic.
- Service: business rules and transaction boundaries. Appends outbox events.
- Repository: Spring Data JPA interfaces.
- Events: the relay and the Kafka listener, each in its own package.

## 6. Domain model and PostgreSQL schema

Four business tables hold the registry, and two technical tables support the event flow. All primary keys are UUIDs generated by the application; all timestamps are `timestamptz` in UTC.

| Table | Columns | Constraints |
| --- | --- | --- |
| `api` | `id`, `name`, `version`, `base_path`, `owner_team`, `description`, `status`, `created_at`, `updated_at`, `row_version` | Unique on (`name`, `version`). Status: `DRAFT`, `PUBLISHED`, `DEPRECATED`, `RETIRED`. |
| `consumer` | `id`, `name`, `contact_email`, `status`, `created_at`, `updated_at`, `row_version` | Unique on `name`. Status: `ACTIVE`, `SUSPENDED`. |
| `subscription` | `id`, `api_id`, `consumer_id`, `plan`, `rate_limit_per_minute`, `status`, `created_at`, `updated_at`, `row_version` | Foreign keys to `api` and `consumer`. Unique on (`api_id`, `consumer_id`). Plan: `FREE`, `STANDARD`, `PREMIUM`. Status: `ACTIVE`, `SUSPENDED`, `CANCELLED`. |
| `api_key` | `id`, `subscription_id`, `key_prefix`, `key_hash`, `status`, `created_at`, `expires_at`, `revoked_at`, `last_used_at` | Foreign key to `subscription`. Unique on `key_hash`. Index on `key_prefix`. Status: `ACTIVE`, `REVOKED`. |
| `outbox_event` | `id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload` (jsonb), `created_at`, `published_at` | Partial index on `created_at` where `published_at` is null. |
| `audit_log` | `id`, `event_id`, `event_type`, `aggregate_type`, `aggregate_id`, `payload` (jsonb), `occurred_at`, `received_at` | Unique on `event_id`, which makes the consumer idempotent. |

**Business rules**

- An API moves forward only: `DRAFT` to `PUBLISHED` to `DEPRECATED` to `RETIRED`. Any other transition is rejected with 409.
- A subscription can be created only for a `PUBLISHED` API and an `ACTIVE` consumer.
- The plan sets the default rate limit: `FREE` 60, `STANDARD` 600, `PREMIUM` 6000 requests per minute.
- Retiring an API cancels its subscriptions. Cancelling a subscription revokes its keys. Both happen in the same transaction.
- A subscription holds at most two active keys, so a key can be rotated without downtime.
- `row_version` is a JPA `@Version` column. A stale update returns 409.

**Persistence rules**

- Flyway owns the schema. Migrations live in `src/main/resources/db/migration` and are never edited after merge.
- Entities stay inside the service layer. Controllers accept and return DTO records.
- Enums are stored as strings.

## 7. REST API

All endpoints sit under `/api/v1`, exchange JSON, and require an admin token except where noted.

| Method | Path | Purpose | Success |
| --- | --- | --- | --- |
| POST | `/apis` | Register an API in `DRAFT` | 201 with `Location` |
| GET | `/apis` | List, filter by `status` and `ownerTeam`, paged | 200 |
| GET | `/apis/{id}` | Read one | 200 |
| PUT | `/apis/{id}` | Update description, base path, owner team | 200 |
| POST | `/apis/{id}/status` | Change lifecycle status | 200 |
| POST | `/consumers` | Register a consumer | 201 with `Location` |
| GET | `/consumers` | List, paged | 200 |
| GET | `/consumers/{id}` | Read one | 200 |
| POST | `/consumers/{id}/status` | Suspend or reactivate | 200 |
| POST | `/subscriptions` | Subscribe a consumer to an API with a plan | 201 with `Location` |
| GET | `/subscriptions` | List, filter by `apiId` and `consumerId`, paged | 200 |
| POST | `/subscriptions/{id}/status` | Suspend, reactivate or cancel | 200 |
| POST | `/subscriptions/{id}/keys` | Issue a key; the only response that contains the plaintext key | 201 |
| GET | `/subscriptions/{id}/keys` | List key metadata, never the key itself | 200 |
| POST | `/keys/{id}/revoke` | Revoke a key | 204 |
| POST | `/keys/validate` | Check a key for an API; called by a gateway | 200 |

**Key validation**

The request body is `{"apiKey": "...", "apiId": "..."}`. The response is always 200, so a caller cannot tell a wrong key from a missing one by status code.

```json
{
  "valid": true,
  "consumerId": "7b1e...",
  "subscriptionId": "c41a...",
  "plan": "STANDARD",
  "rateLimitPerMinute": 600
}
```

An invalid result is `{"valid": false, "reason": "REVOKED"}`. Reasons: `UNKNOWN_KEY`, `REVOKED`, `EXPIRED`, `SUBSCRIPTION_INACTIVE`, `API_NOT_AVAILABLE`, `WRONG_API`.

This endpoint requires a token with the `registry.validate` scope instead of the admin scope.

**Conventions**

- Paging uses `page` and `size` query parameters, with `size` capped at 100. Responses carry `items`, `page`, `size` and `totalItems`.
- Errors use `application/problem+json`: 400 for validation with a per-field list, 404 for unknown ids, 409 for rule violations and stale versions.
- Updates send the `rowVersion` they read. A mismatch returns 409.
- Request and response bodies are Java records with Bean Validation annotations.
- The OpenAPI document is generated by springdoc. The build exports it to `docs/openapi.json`, and CI fails if the committed file is out of date.

## 8. Kafka events

Every state change writes an event to the `outbox_event` table in the same database transaction, and a relay publishes it to Kafka afterwards. This is the transactional outbox pattern: the database and the event stream cannot disagree, even if Kafka is down.

**Topics**

| Topic | Partitions | Key | Purpose |
| --- | --- | --- | --- |
| `api-registry.events.v1` | 3 | `aggregateId` | All domain events. Keying by aggregate keeps the events of one entity in order. |
| `api-registry.events.v1.dlt` | 1 | original key | Dead-letter topic for events the consumer could not process. |

**Event types**

`ApiRegistered`, `ApiStatusChanged`, `ConsumerRegistered`, `ConsumerStatusChanged`, `SubscriptionCreated`, `SubscriptionStatusChanged`, `ApiKeyIssued`, `ApiKeyRevoked`.

**Envelope**

```json
{
  "eventId": "0f6c...",
  "eventType": "ApiKeyIssued",
  "occurredAt": "2026-11-10T17:45:00Z",
  "aggregateType": "ApiKey",
  "aggregateId": "91d2...",
  "schemaVersion": 1,
  "data": { "subscriptionId": "c41a...", "keyPrefix": "k3f9a2bc" }
}
```

The `data` object never contains a plaintext key or a key hash.

**Producer (outbox relay)**

- A scheduled task runs every second and reads unpublished rows in `created_at` order with `FOR UPDATE SKIP LOCKED`, in batches of 100.
- It sends each row to Kafka and sets `published_at` only after the broker acknowledges.
- Producer settings: `acks=all`, idempotence enabled.
- Delivery is at-least-once. A crash between send and update causes a duplicate, which the consumer absorbs.
- A property `app.events.relay-enabled` switches the relay off.

**Consumer (audit materialiser)**

- Consumer group `api-registry-audit` reads the topic and inserts one `audit_log` row per event.
- A duplicate `eventId` hits the unique constraint and is skipped, which makes processing idempotent.
- A failing record is retried three times with backoff, then sent to the dead-letter topic.
- Offsets are committed after the database write.

**Explain in the milestone notes**

- Why the service does not call Kafka directly inside the request transaction.
- What Debezium change data capture would replace here in a production system.
- Why JSON was chosen over Avro with a schema registry, and what that costs.

## 9. Security

The service is an OAuth2 resource server: it validates JWT bearer tokens issued by an identity provider and never handles passwords.

**Access rules**

| Endpoints | Requirement |
| --- | --- |
| `/api/v1/keys/validate` | Token with the validate scope |
| All other `/api/v1/**` | Token with the admin scope |
| `/actuator/health/**` | Open, for the load balancer |
| `/v3/api-docs`, `/swagger-ui/**` | Open |
| Everything else, including other actuator endpoints | Denied |

**Identity providers**

- Locally: Keycloak in Docker Compose, with a realm imported from `local/keycloak/realm.json`. It defines two client-credentials clients, `registry-admin` and `gateway`.
- In AWS: an Amazon Cognito user pool with a resource server and the same two clients, created by Terraform.
- The application code is identical in both. Only three properties differ: the issuer URI, `app.security.admin-scope` and `app.security.validate-scope`.

**API keys**

- Format: `ark_<8-character prefix>_<43 characters of base64url>`, from 32 bytes of `SecureRandom`.
- Only the prefix and a SHA-256 hash of the full key are stored. The plaintext is returned once, at issue time.
- Validation looks up by prefix and compares hashes in constant time.
- Keys never appear in logs, events, exceptions or `toString` output.
- The milestone notes must explain why SHA-256 is acceptable for a random 256-bit key when it would not be for a password.

**Other rules**

- No secrets in the repository. Locally they come from Compose environment variables; in AWS from Secrets Manager.
- The session policy is stateless and CSRF protection is disabled, because the API is token-only.
- The container runs as a non-root user.

## 10. Testing

`./mvnw verify` runs every test and must pass before a milestone is called done. Integration tests use real PostgreSQL and Kafka containers through Testcontainers; there is no in-memory database and no embedded broker.

| Level | What it covers | Tools |
| --- | --- | --- |
| Unit | Lifecycle transitions, subscription rules, key generation and hashing | JUnit 5, AssertJ, Mockito |
| Web | Validation, status codes, problem-detail bodies, access rules per scope | MockMvc, `spring-security-test` with mock JWTs |
| Persistence | Flyway migrations apply cleanly, constraints hold, optimistic locking returns a conflict | Testcontainers PostgreSQL |
| Messaging | A state change produces an outbox row, the relay publishes it, the consumer writes one audit row; a duplicate event writes none; a poison record reaches the dead-letter topic | Testcontainers Kafka, Awaitility |
| End to end | One scenario: register API, publish it, register consumer, subscribe, issue key, validate key, revoke key, validate again | Full application context with both containers |

**Rules**

- Test names describe behaviour, for example `issuingAThirdActiveKeyIsRejected`.
- Unit tests end in `Test` and run in the `test` phase. Integration tests end in `IT` and run in `verify`.
- Containers are shared across the test run to keep it under three minutes.
- Tests do not sleep. Asynchronous outcomes are awaited with Awaitility.
- A JaCoCo report is produced and uploaded by CI. There is no coverage threshold.

## 11. Repository layout

The repository is a single Maven module with code packaged by feature, plus top-level folders for infrastructure, pipelines and notes.

```text
api-registry/
  SPEC.md                      this document
  CLAUDE.md                    working rules for Claude Code
  README.md                    how to run, test and deploy
  pom.xml, mvnw, .mvn/
  Dockerfile
  compose.yaml
  src/main/java/dev/sample/registry/
    api/                       controller, service, entity, repository, DTOs
    consumer/
    subscription/
    key/
    events/                    envelope, outbox entity, relay, topic config
    audit/                     Kafka listener, audit entity
    security/                  resource server config, scope properties
    common/                    problem details, paging, clock
  src/main/resources/
    application.yaml
    db/migration/              Flyway scripts
  src/test/java/...            mirrors main; *Test and *IT
  local/
    keycloak/realm.json
    demo.sh                    runs the end-to-end scenario with curl
  infra/
    bootstrap/                 state bucket, ECR, GitHub OIDC role
    app/                       network, database, Cognito, ECS, load balancer
  .github/workflows/
    ci.yml
    deploy.yml
    destroy.yml
  docs/
    openapi.json               exported contract
    gitlab-ci.example.yml      untested translation of the workflows
    decisions/                 one short note per milestone
```

A feature package may depend on `common` and `events`, never on another feature's repository or entity. Cross-feature work goes through the other feature's service.

## 12. Local development

`docker compose up -d` followed by `./mvnw spring-boot:run` gives a complete working system on a laptop, with no AWS account involved.

| Compose service | Image | Port | Purpose |
| --- | --- | --- | --- |
| `postgres` | `postgres:17` | 5432 | Application database, with a named volume |
| `kafka` | `apache/kafka` | 9092 | Single broker in KRaft mode |
| `keycloak` | `quay.io/keycloak/keycloak` | 8081 | Issues tokens; imports the realm file at start |
| `kafka-ui` | a Kafka web console of your choice | 8082 | Optional, under the `tools` profile, to watch events arrive |
| `app` | built from the `Dockerfile` | 8080 | Optional, under the `app` profile, to run the packaged image |

**Requirements**

- Every Compose service has a health check, and dependants wait for it.
- The application creates its Kafka topics at start-up, with a configurable replication factor that defaults to 1.
- The default Spring profile points at the Compose services, so no environment variables are needed for a local run.
- `local/demo.sh` fetches a token from Keycloak and runs the end-to-end scenario from section 10 with `curl`, printing each response.
- The README documents four commands: start dependencies, run the service, run the demo, run all tests.

**Dockerfile**

- Stage one builds the jar with the Maven wrapper. Stage two copies it onto a Temurin JRE image.
- Use Spring Boot layered jars so that dependency layers are cached between builds.
- The image runs as a non-root user and exposes 8080.
- The image builds for `linux/amd64` in CI, so it runs on Fargate whatever the developer's laptop is.

## 13. Infrastructure: Terraform on AWS

Terraform is split into two root modules: `bootstrap`, applied once by hand, and `app`, applied by the pipeline on every deploy. The default region is `eu-central-1`, set by a variable.

**`infra/bootstrap` (local state, applied once from a laptop)**

- An S3 bucket for Terraform state: versioned, encrypted, public access blocked.
- An ECR repository with scan-on-push and a lifecycle rule that keeps the last 10 images.
- A GitHub OIDC identity provider and an IAM role that only the `main` branch of this repository can assume.
- Outputs: role ARN, state bucket name, ECR repository URL.

**`infra/app` (remote state in S3 with `use_lockfile = true`)**

| Area | Resources | Settings |
| --- | --- | --- |
| Network | VPC, two public and two private subnets across two zones, internet gateway | No NAT gateway. Tasks run in public subnets; the database in private ones. |
| Load balancer | Application Load Balancer, target group, HTTP listener on port 80 | Ingress limited to the `allowed_cidr` variable, which is the owner's own IP address. Health check on `/actuator/health/readiness`. |
| Database | RDS PostgreSQL 17, `db.t4g.micro`, 20 GB gp3, single zone | Not publicly reachable. Master password managed by RDS in Secrets Manager. No final snapshot, no deletion protection. |
| Identity | Cognito user pool, domain, resource server with two scopes, two app clients | Client-credentials flow only. |
| Compute | ECS cluster, task definition (1 vCPU, 3 GB), service with one task | Two containers: `app` and a `kafka` sidecar. The app starts after Kafka is healthy. |
| Access | Task execution role, task role, security groups | The app's port accepts traffic from the load balancer only; the database accepts the task only. |
| Logs | CloudWatch log group | 7-day retention. |

Outputs: service URL, Cognito token URL, admin client id.

**Kafka in AWS**

Kafka runs as a single-broker sidecar container in the same ECS task, reached at `localhost:9092`. This is a deliberate shortcut: Amazon MSK would add significant cost and set-up time to a learning project.

The consequence is that events are lost whenever the task restarts. The outbox and audit tables in RDS survive. The README must state this, and name MSK as the production choice.

**Rules**

- The image tag is a variable set by the pipeline to the Git commit SHA. `latest` is never deployed.
- Every resource carries the tags `project = api-registry` and `managed-by = terraform`.
- No hard-coded account ids, ARNs or secrets. `terraform fmt` and `terraform validate` pass.
- The service is served over plain HTTP. That is acceptable only because ingress is limited to one IP address; the README says so.

**Cost**

The running stack costs roughly USD 3 a day: Fargate, the load balancer, the database and public IPv4 addresses. This figure is approximate and from memory of list prices; check the AWS pricing calculator before relying on it. `terraform destroy` on `infra/app` removes everything that bills by the hour.

## 14. CI/CD

Three GitHub Actions workflows cover the path from commit to running service. Only a commit on `main` that passes every check is deployed.

| Workflow | Trigger | Jobs |
| --- | --- | --- |
| `ci.yml` | Every push and pull request | **build-test**: set up Temurin 21 with Maven cache, run `./mvnw verify`, upload test and JaCoCo reports. **contract**: fail if `docs/openapi.json` differs from the generated document. **image**: build the Docker image without pushing. **terraform**: `fmt -check` and `validate` on both modules. |
| `deploy.yml` | Push to `main`, after CI succeeds | Assume the AWS role through OIDC. Build the image and push it to ECR tagged with the commit SHA. Run `terraform plan` then `apply` on `infra/app` with that tag. Wait for the ECS service to become stable. Smoke-test the health endpoint. Print the service URL in the job summary. |
| `destroy.yml` | Manual (`workflow_dispatch`) with a typed confirmation | `terraform destroy` on `infra/app`. |

**Rules**

- No long-lived AWS keys. Authentication uses GitHub's OIDC token and the role from the bootstrap module.
- Configuration comes from repository variables `AWS_ROLE_ARN` and `AWS_REGION`, and a secret `ALLOWED_CIDR`.
- Third-party actions are pinned to a major version at minimum.
- Deploy and destroy share a concurrency group, so two Terraform runs never overlap.
- The deploy job uses a GitHub environment named `aws-sandbox`, so a manual approval can be added later without changing the workflow.

**GitLab CI translation**

The target team uses GitLab. Keep `docs/gitlab-ci.example.yml` as a line-for-line translation of the three workflows, clearly marked as untested. The concepts map as follows.

| GitHub Actions | GitLab CI |
| --- | --- |
| Workflow files in `.github/workflows/` | One `.gitlab-ci.yml`, split with `include` |
| Job, with `needs` | Job, with `stages` and `needs` |
| Reusable action (`uses:`) | CI/CD component or included template |
| Runner (`runs-on`) | Runner, selected with `tags`; each job names an `image` |
| Repository variables and secrets | CI/CD variables, masked and protected |
| `on: push`, `on: pull_request` | `rules:` and merge request pipelines |
| `workflow_dispatch` | `when: manual` |
| Environment with required reviewers | Environment with protected deployments |
| OIDC through `permissions: id-token` | `id_tokens:` |
| Artifacts and cache actions | `artifacts:` and `cache:` keywords |
| `concurrency:` group | `resource_group:` |

## 15. Build milestones

The work is delivered in five milestones, each ending in a running system. Implement one milestone per session and stop when its acceptance criteria pass.

### Milestone 1: skeleton and pipeline

- Generate the project (section 4) with the layout from section 11.
- Add Actuator health, a Flyway baseline migration, `compose.yaml` with PostgreSQL, the `Dockerfile` and `ci.yml`.
- Add one integration test that starts the application against a Testcontainers PostgreSQL.

Accepted when: `./mvnw verify` passes; `docker compose up -d` then `./mvnw spring-boot:run` serves `UP` on `/actuator/health`; `docker build .` succeeds; the CI workflow is green on GitHub.

### Milestone 2: registry domain and REST API

- Implement the four business tables, their rules (section 6) and all endpoints (section 7), without authentication.
- Add springdoc, the exported `docs/openapi.json` and the CI contract check.
- Add `local/demo.sh` running the end-to-end scenario.

Accepted when: unit, web, persistence and end-to-end tests from section 10 pass; `local/demo.sh` completes; Swagger UI lists every endpoint; a key validates before revocation and fails after it.

### Milestone 3: Kafka events

- Add Kafka to Compose, the outbox table, the relay, the topics and the audit consumer (section 8).
- Add the `kafka-ui` tools profile.

Accepted when: messaging tests pass; running the demo produces eight or more rows in `audit_log`; stopping Kafka, running the demo and restarting Kafka delivers the missed events without loss.

### Milestone 4: security

- Add the resource server configuration, the access rules and Keycloak with its realm file (section 9).
- Update the demo script to fetch tokens, and the web tests to cover each scope.

Accepted when: a request without a token returns 401; an admin call with the gateway token returns 403; the demo completes with tokens; the health endpoint stays open.

### Milestone 5: AWS deployment

- Write both Terraform modules (section 13), `deploy.yml`, `destroy.yml` and `docs/gitlab-ci.example.yml` (section 14).
- Add a variant of the demo script that targets the deployed URL with a Cognito token.
- Do not run `terraform apply`. The owner applies the bootstrap module and triggers the pipeline.

Accepted when: `terraform validate` passes on both modules; after the owner's push, the deploy workflow is green; the remote demo completes against the load balancer URL; the destroy workflow leaves no billable resources from `infra/app`.

### Optional extensions

Pick one only after milestone 5, or instead of it if AWS is skipped.

- **Gateway**: a small Spring Cloud Gateway application in front of a dummy backend. It calls `/keys/validate` for each request and enforces the returned rate limit per key.
- **Breaking-change check**: a CI job that compares `docs/openapi.json` with the version on `main` and fails on a breaking change.
- **Avro**: replace the JSON events with Avro and a schema registry in Compose.

## 16. Working rules for Claude Code

Copy these rules into `CLAUDE.md` at the start of milestone 1 and follow them in every session.

**The owner**

- The owner is experienced in release management and CI/CD, and is new to Spring Boot, JPA and Kafka.
- The purpose is learning. Explain each Spring, JPA or Kafka concept in two or three sentences the first time it appears, in chat, as you write the code.

**How to work**

1. Work on one milestone at a time. Never start the next one unprompted.
2. Before writing code, state the plan for the milestone in ten lines or fewer.
3. Build in small steps and run the tests after each one.
4. Finish by running `./mvnw verify` and each acceptance check, and show the real output. Report a failing check as failing.
5. Write `docs/decisions/NN-<milestone>.md`: what was built, the three most important design decisions, the alternative rejected for each, and how a production team would likely differ.
6. Update the README so that its commands are always correct.

**Limits**

- Add nothing that this specification does not ask for: no extra endpoints, libraries or abstractions.
- If the specification is ambiguous, or a library does not support the chosen versions, stop and ask.
- Never write a secret, token or account id into a file. Never weaken a test to make it pass.
- Do not run `git push`, `terraform apply` or `terraform destroy`. The owner runs those.
- Commit locally in small commits with messages in the form `feat: ...`, `test: ...`, `chore: ...`.

**Code style**

- Constructor injection only; no field injection.
- Records for DTOs and events; no Lombok.
- Transactions on service methods, not on controllers or repositories.
- No comments that restate the code. Comment only the reason for a non-obvious choice.
